package com.github.whyrising.recompose.subs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.seq.ISeq
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.concurrency.IAtom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.l
import com.github.whyrising.y.str
import com.github.whyrising.y.v
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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
    private val inputSignals: IPersistentVector<ReactiveAtom<Input>>,
    val context: CoroutineContext,
    private val initial: Output?,
    val f: (signalsValues: IPersistentVector<Input>) -> Output
) : ViewModel(),
    IAtom<Output>,
    ReactiveAtom<Output>,
    Disposable<Input, Output> {

    internal val disposeFns = atom<ISeq<(Reaction<Input, Output>) -> Unit>>(l())

    // this flag is used to track the last subscriber of this reaction
    internal var isFresh = atom(true)

    internal val state: MutableStateFlow<Output> by lazy {
        MutableStateFlow(initial ?: f(deref(inputSignals))).apply {
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

    init {
        for ((i, inputNode) in inputSignals.withIndex())
            viewModelScope.launch {
                inputNode.collect { newInput: Input ->
                    withContext(context) {
                        while (true) {
                            val oldVal = state.value
                            val newInputs = deref(inputSignals)
                                .assoc(i, newInput)
                            val materializedView = f(newInputs)
                            if (state.compareAndSet(oldVal, materializedView))
                                return@withContext
                        }
                    }
                }
            }
    }

    val id: String by lazy { str("rx", hashCode()) }

    override fun deref(): Output = state.value

    override fun reset(newValue: Output): Output =
        state.updateAndGet { newValue }

    /**
     * This function use a regular comparison using [Any.equals].
     * If [f] returns a value that is equal to the current stored value in the
     * reaction, this function does not actually change the reference that is
     * stored in the [state].
     *
     * [f] may be evaluated multiple times, if [state] is being concurrently
     * updated.
     *
     * This method is **thread-safe** and can be safely invoked from concurrent
     * coroutines without external synchronization.
     */
    override fun swap(f: (currentVal: Output) -> Output): Output =
        state.updateAndGet(f)

    override fun <A> swap(
        arg: A,
        f: (currentVal: Output, arg: A) -> Output
    ): Output {
        while (true) {
            val currentVal = state.value
            val newVal = f(currentVal, arg)

            if (state.compareAndSet(currentVal, newVal))
                return newVal
        }
    }

    override fun <A1, A2> swap(
        arg1: A1,
        arg2: A2,
        f: (currentVal: Output, arg1: A1, arg2: A2) -> Output
    ): Output {
        while (true) {
            val currentVal = state.value
            val newVal = f(currentVal, arg1, arg2)

            if (state.compareAndSet(currentVal, newVal))
                return newVal
        }
    }

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
            action(it)
        }

    override suspend fun emit(value: Output) = state.emit(value)
}

fun <T> deref(refs: IPersistentVector<IDeref<T>>): PersistentVector<T> =
    refs.fold(v()) { acc, r ->
        acc.conj(r.deref())
    }
