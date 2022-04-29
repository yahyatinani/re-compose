package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ExtractorReaction<I, O>(
    inputSignal: Reaction<I>,
    val f: (signalValue: I) -> O
) : ReactionBase<O, O>() {
    override val state: MutableStateFlow<O> = initState(f(inputSignal.deref()))

    internal fun recompute(arg: I) {
        state.tryEmit(f(arg))
    }

    // init should be after state property.
    init {
        viewModelScope.launch {
            inputSignal.collect { newInput: I ->
                recompute(newInput)
            }
        }
    }

    override fun deref(state: State<O>): O = state.value

    override fun deref(): O = state.value

    override suspend fun collect(action: suspend (O) -> Unit) = state.collect {
        action(it)
    }
}
