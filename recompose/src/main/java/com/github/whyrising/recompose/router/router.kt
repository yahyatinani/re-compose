package com.github.whyrising.recompose.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object Recompose : ViewModel() {
    // TODO: Use a persistent queue from y
    internal val eventQueue = Channel<Event>()

    init {
        viewModelScope.launch {
            while (true)
                handle(eventQueue.receive())
        }
    }
}

fun dispatch(event: Event) {
    if (event.count == 0)
        throw RuntimeException(
            "$TAG: You called `dispatch` with an empty event vector"
        )

    Recompose.eventQueue.trySend(event)
}

fun dispatchSync(event: Event) {
    runBlocking {
        handle(event)
    }
}
