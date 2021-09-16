package com.github.whyrising.recompose.subs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.y.concurrency.IAtom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.core.str
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class Reaction<T>(val f: () -> T) : ViewModel(), IDeref<T>, IAtom<T> {
    private val disposeFns: MutableList<(Reaction<T>) -> Unit> = mutableListOf()

    // this flag is used to track the last subscriber of this reaction
    private var isFresh = true

    val state: MutableStateFlow<T> by lazy {
        val mutableStateFlow = MutableStateFlow(f())
        mutableStateFlow.subscriptionCount
            .onEach { count ->
                when {
                    // last subscriber just disappeared
                    count == 0 && !isFresh -> onCleared()
                    else -> isFresh = false
                }
            }.launchIn(viewModelScope)
        mutableStateFlow
    }

    val id: String by lazy { str("rx", hashCode()) }

    override fun onCleared() {
        super.onCleared()

        disposeFns.forEach { disposeFn -> disposeFn(this) }
        viewModelScope.cancel("This reaction `$id` got cleared")
    }

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

    fun addOnDispose(f: (Reaction<T>) -> Unit) {
        disposeFns.add(f)
    }
}

/**
 * This function runs the [computation] function every time the [inputNode]
 * changes.
 *
 * @param inputNode reaction which extract data directly from [appDb],
 * but do no further computation.
 * @param context for the coroutines running under [viewModelScope].
 * @param computation a function that obtains data from [inputNode], and compute
 * derived data from it.
 */
inline fun <T, R> Reaction<R>.reactTo(
    inputNode: Reaction<T>,
    context: CoroutineContext,
    crossinline computation: suspend (newInput: T) -> R
) {
    viewModelScope.launch(context) {
        inputNode.state.collect { newInput: T ->
            // Evaluate this only once by leaving it out of swap,
            // since swap can run f multiple times, the output is the same for
            // the same input
            val materializedView = computation(newInput)
            swap { materializedView }
        }
    }
}
