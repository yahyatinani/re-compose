package com.github.whyrising.recompose.subs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.seq.ISeq
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.get
import com.github.whyrising.y.l
import com.github.whyrising.y.m
import com.github.whyrising.y.str
import com.github.whyrising.y.v
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal const val stateKey = "state"
internal const val inputsKey = "input"

/**
 * @param inputSignals are the nodes (Reactions) that signal the current
 * [Reaction] to recalculate its value when new values are provided.
 * @param initial when it's null, the first value calculation of this [Reaction]
 * happens on the main thread.
 * @param context on which further values' calculations of this [Reaction]
 * will happen.
 * @param f is the function that calculates the sequence of values of this
 * [Reaction].
 */
class Reaction<Input, Output>(
    inputSignals: IPersistentVector<ReactiveAtom<Input>>,
    val context: CoroutineContext,
    private val initial: Output?,
    val f: (signalsValues: IPersistentVector<Input>) -> Output
) : ViewModel(),
    ReactiveAtom<Output>,
    Disposable<Input, Output> {
    internal val disposeFns = atom<ISeq<(Reaction<Input, Output>) -> Unit>>(l())

    // this flag is used to track the last subscriber of this reaction
    internal var isFresh = atom(true)

    // TODO: Maybe replace Flow with an atom or something because collecting
    //  takes time and the concurrency test fail.
    internal val state: MutableStateFlow<IPersistentMap<Any, Any?>> by lazy {
        val inputs = deref(inputSignals)
        val m: IPersistentMap<Any, Any?> = m(
            inputsKey to inputs,
            stateKey to (initial ?: f(inputs))
        )
        MutableStateFlow(m).apply {
            subscriptionCount
                .onEach { subCount ->
                    when {
                        // last subscriber just disappeared => composable left
                        // the Composition tree.
                        subCount == 0 && !isFresh.deref() -> onCleared()
                        else -> isFresh.swap { false }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    internal suspend fun update(newArg: Input, index: Int) {
        while (true) {
            val old = state.value
            val oldArgs = old[inputsKey] as PersistentVector<Input>? ?: v()
            if (oldArgs.count > index && oldArgs[index] == newArg)
                return

            val newArgs = oldArgs.assoc(index, newArg)
            val materializedView = withContext(context) { f(newArgs) }
            val new = m(stateKey to materializedView, inputsKey to newArgs)

            if (state.compareAndSet(old, new))
                return
        }
    }

    init {
        for ((i, inputNode) in inputSignals.withIndex())
            viewModelScope.launch {
                inputNode.collect { newInput: Input ->
                    update(newInput, i)
                }
            }
    }

    val id: String by lazy { str("rx", hashCode()) }

    override fun deref(): Output = state.value[stateKey] as Output

    override fun addOnDispose(f: (Reaction<Input, Output>) -> Unit) {
        disposeFns.swap { it.cons(f) }
    }

    override fun dispose() {
        var fs: ISeq<(Reaction<Input, Output>) -> Unit>? = disposeFns()
        while (fs != null && fs.count > 0) {
            val f = fs.first()
            f(this)
            fs = fs.next()
        }

        viewModelScope.cancel("This reaction `$id` just got canceled.")
    }

    public override fun onCleared() {
        super.onCleared()

        dispose()
    }

    override suspend fun collect(action: suspend (Output) -> Unit) =
        state.collect {
            action(it[stateKey] as Output)
        }

    /**
     * It sets the state to [value] and resets the inputs of this reaction.
     */
    override suspend fun emit(value: Output) = state.emit(m(stateKey to value))
}

fun <T> deref(refs: IPersistentVector<IDeref<T>>): PersistentVector<T> =
    refs.fold(v()) { acc, r ->
        acc.conj(r.deref())
    }
