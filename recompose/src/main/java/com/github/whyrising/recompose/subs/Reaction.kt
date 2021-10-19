package com.github.whyrising.recompose.subs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.seq.ISeq
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.IAtom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.str
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class Reaction<T>(val f: () -> T) :
    ViewModel(),
    IAtom<T>,
    ReactiveAtom<T>,
    Disposable<T> {
    internal val disposeFns: Atom<ISeq<(Reaction<T>) -> Unit>> = atom(l())

    // this flag is used to track the last subscriber of this reaction
    internal var isFresh = true

    internal val state: MutableStateFlow<T> by lazy {
        val mutableStateFlow = MutableStateFlow(f())

        mutableStateFlow
            .subscriptionCount
            .onEach { count ->
                when {
                    // last subscriber just disappeared => composable left
                    // the Composition tree.
                    count == 0 && !isFresh -> onCleared()
                    else -> isFresh = false
                }
            }.launchIn(viewModelScope)

        mutableStateFlow
    }

    val id: String by lazy { str("rx", hashCode()) }

    override fun deref(): T = state.value

    override fun reset(newValue: T): T = state.updateAndGet { newValue }

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
    override fun swap(f: (currentVal: T) -> T): T = state.updateAndGet(f)

    override fun <A> swap(arg: A, f: (currentVal: T, arg: A) -> T): T {
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
        f: (currentVal: T, arg1: A1, arg2: A2) -> T
    ): T {
        while (true) {
            val currentVal = state.value
            val newVal = f(currentVal, arg1, arg2)

            if (state.compareAndSet(currentVal, newVal))
                return newVal
        }
    }

    override fun addOnDispose(f: (Reaction<T>) -> Unit) {
        disposeFns.swap { it.cons(f) }
    }

    override fun dispose() {
        var s: ISeq<(Reaction<T>) -> Unit>? = disposeFns()
        while (s != null && s.count > 0) {
            s.first()(this)
            s = s.next()
        }

        viewModelScope.cancel("This reaction `$id` just got canceled.")
    }

    public override fun onCleared() {
        super.onCleared()

        dispose()
    }

    override suspend fun collect(action: suspend (T) -> Unit) = state.collect {
        action(it)
    }

    override suspend fun emit(value: T) = state.emit(value)

    /**
     * This function runs the [computation] function every time the [inputNode]
     * changes.
     *
     * @param inputNode reaction which extract data directly from [appDb],
     * but do no further computation.
     * @param context for the coroutines running under [viewModelScope].
     * @param computation a function that obtains data from [inputNode], and
     * compute derived data from it.
     */
    inline fun <R> reactTo(
        inputNode: ReactiveAtom<R>,
        context: CoroutineContext,
        crossinline computation: suspend (newInput: R) -> T
    ) {
        viewModelScope.launch(context) {
            inputNode.collect { newInput: R ->
                // Evaluate this only once by leaving it out of swap,
                // since swap can run f multiple times, the output is the same
                // for the same input
                val materializedView = computation(newInput)
                swap { materializedView }
            }
        }
    }

    inline fun <R> reactTo(
        inputNodes: IPersistentVector<ReactiveAtom<R>>,
        context: CoroutineContext,
        crossinline computation: suspend (newInput: IPersistentVector<R>) -> T
    ) {
        for ((i, inputNode) in inputNodes.withIndex()) {
            viewModelScope.launch(context) {
                inputNode.collect { newInput: R ->
                    // Evaluate this only once by leaving it out of swap since
                    // swap can run f multiple times. The output is the same for
                    // the same newInput.
                    val materializedView = deref(inputNodes)
                        .assoc(i, newInput)
                        .let { computation(it as IPersistentVector<R>) }

                    swap { materializedView }
                }
            }
        }
    }
}

fun <T> deref(refs: IPersistentVector<IDeref<T>>): IPersistentVector<T> =
    refs.fold(v()) { acc, r ->
        acc.conj(r.deref())
    }
