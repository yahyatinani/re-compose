package com.github.whyrising.recompose.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.recompose.router.Recompose.enqueue
import com.github.whyrising.y.collections.core.q
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

object Recompose : ViewModel() {
    private val eventQueue = MutableStateFlow(q<Event>())

    init {
        eventQueue
            .onEach { queue ->
                if (queue.isNotEmpty()) {
                    val event = queue.peek()!!
                    handle(event)
                    eventQueue.compareAndSet(queue, queue.pop())
                }
            }
            .launchIn(viewModelScope)
    }

    internal fun enqueue(event: Event) {
        eventQueue.update { it.conj(event) }
    }
}

private fun validate(event: Event) {
    if (event.count == 0)
        throw RuntimeException(
            "$TAG: `dispatch` was called with an empty event vector."
        )
}

fun dispatch(event: Event) {
    validate(event)
    enqueue(event)
}

fun dispatchSync(event: Event) {
    runBlocking {
        handle(event)
    }
}
