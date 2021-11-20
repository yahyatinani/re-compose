package com.github.whyrising.recompose.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.y.collections.PersistentQueue
import com.github.whyrising.y.collections.core.q
import com.github.whyrising.y.concurrency.atom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

data class EventQueue(
    internal val context: CoroutineContext = EmptyCoroutineContext
) : ViewModel() {
    internal val state = MutableStateFlow(q<Event>())

    suspend fun processFirstEvent(queue: PersistentQueue<Event>) {
        val event = queue.peek()
        if (event != null) {
            try {
                handle(event)
                state.value = queue.pop()
            } catch (e: RuntimeException) {
                purge()
                throw e
            }
        }
    }

    internal fun consumeEventQueue(): Job = viewModelScope.launch(context) {
        state.collect {
            if (it.isNotEmpty())
                processFirstEvent(it)
        }
    }

    internal val consumerJob: Job = consumeEventQueue()

    internal fun enqueue(event: Event) {
        state.update { it.conj(event) }
    }

    fun purge() {
        state.value = q()
    }

    fun halt() {
        onCleared()
        state.value = q()
        viewModelScope.cancel()
    }
}

internal val EVENT_QUEUE = atom(EventQueue())

/**
 * Halt and replace the previous [EventQueue] with a new one.
 *
 * You should probably call this function at the very start of your app, since
 * you only need it if you want to run the [EventQueue] consumer from a
 * different Dispatcher. (eg. [Dispatchers.Default])
 *
 * The [EventQueue] consumer coroutine is running on [viewModelScope] with
 * [EmptyCoroutineContext] by default.
 *
 * @param context to launch the [EventQueue] consumer coroutine.
 */
fun eventQueueFactory(context: CoroutineContext) {
    EVENT_QUEUE.swap {
        it.halt()
        EventQueue(context)
    }
}

// -- Dispatching --------------------------------------------------------------

private fun validate(event: Event) {
    if (event.count == 0)
        throw IllegalArgumentException(
            "$TAG: `dispatch` was called with an empty event vector."
        )
}

// TODO: consider using a coroutine to enqueue
//  order don't matter since the dispatch can be called from a different
//  coroutine or thread, eg. from queue consumer coroutine
fun dispatch(event: Event) {
    validate(event)
    EVENT_QUEUE().enqueue(event)
}

fun dispatchSync(event: Event) {
    runBlocking {
        handle(event)
    }
}
