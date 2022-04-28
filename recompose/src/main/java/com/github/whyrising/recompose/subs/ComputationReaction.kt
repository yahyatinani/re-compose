package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.get
import com.github.whyrising.y.m
import com.github.whyrising.y.v
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal const val stateKey = "state"
internal const val inputsKey = "input"

fun <T> deref(refs: IPersistentVector<IDeref<T>>): PersistentVector<T> =
    refs.fold(v()) { acc, r ->
        acc.conj(r.deref())
    }

/**
 * @param inputSignals are the nodes (Reactions) that signal the current
 * [ComputationReaction] to recalculate its value when new values are provided.
 * @param initial when it's null, the first value calculation of this [ComputationReaction]
 * happens on the main thread.
 * @param context on which further values' calculations of this [ComputationReaction]
 * will happen.
 * @param f is the function that calculates the sequence of values of this
 * [ComputationReaction].
 */
class ComputationReaction<I, O>(
    inputSignals: IPersistentVector<Reaction<I>>,
    val context: CoroutineContext,
    private val initial: O?,
    val f: (signalsValues: IPersistentVector<I>) -> O
) : ReactionBase<IPersistentMap<Any, Any?>, O>() {
    override val state: MutableStateFlow<IPersistentMap<Any, Any?>> by lazy {
        val inputs = deref(inputSignals)
        initState(
            m<String, Any?>(
                inputsKey to inputs,
                stateKey to (initial ?: f(inputs))
            )
        )
    }

    internal suspend fun recompute(arg: I, index: Int) {
        while (true) {
            val old = state.value
            val oldArgs = old[inputsKey] as PersistentVector<I>? ?: v()
            if (oldArgs.count > index && oldArgs[index] == arg)
                return

            val newArgs = oldArgs.assoc(index, arg)
            val materializedView = withContext(context) { f(newArgs) }
            val new = m(stateKey to materializedView, inputsKey to newArgs)

            if (state.compareAndSet(old, new))
                return
        }
    }

    // init should be after state property.
    init {
        for ((i, inputNode) in inputSignals.withIndex())
            viewModelScope.launch {
                inputNode.collect { newInput: I ->
                    recompute(newInput, i)
                }
            }
    }

    override fun deref(state: State<IPersistentMap<Any, Any?>>): O =
        state.value[stateKey] as O

    override fun deref(): O = state.value[stateKey] as O

    override suspend fun collect(action: suspend (O) -> Unit) = state.collect {
        action(it[stateKey] as O)
    }
}
