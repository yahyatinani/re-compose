package com.github.whyrising.recompose.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.y.collections.core.q
import com.github.whyrising.y.concurrency.atom
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * This class is a FIFO PersistentQueue that allows us to handle incoming events
 * according to the producer-consumer pattern.
 *
 * We have only one consumer that can be run from [consumeEventQueue] function,
 * and multiple producers that can be run from [enqueue] function.
 */
internal data class EventQueue(
    internal val context: CoroutineContext = EmptyCoroutineContext
) : ViewModel() {
    internal val queueState = atom(q<Event>())

    @Volatile
    internal var deferredUntilEvent = CompletableDeferred<Unit>()
    private suspend fun suspendUntilEventOccurs() = deferredUntilEvent.await()

    suspend fun processFirstEvent() {
        val event = queueState().peek()
        if (event != null) {
            try {
                handle(event)
                queueState.swap { it.pop() }
            } catch (e: Exception) {
                purge()
                throw e
            }
        } else {
            suspendUntilEventOccurs()
            deferredUntilEvent = CompletableDeferred()
        }
    }

    internal fun consumeEventQueue(): Job = viewModelScope.launch(context) {
        while (true)
            processFirstEvent()
    }

    internal val consumerJob: Job = consumeEventQueue()

    internal fun enqueue(event: Event) {
        viewModelScope.launch(context) {
            queueState.swap { it.conj(event) }
            deferredUntilEvent.complete(Unit)
        }
    }

    fun purge() {
        queueState.reset(q())
    }

    fun halt() {
        onCleared()
        queueState.reset(q())
        viewModelScope.cancel()
    }

    /**
     * [EventQueue]s are equal if the internal [queueState] queues are equal
     */
    override fun equals(other: Any?): Boolean = when (other) {
        is EventQueue -> queueState() == other.queueState()
        else -> false
    }

    override fun hashCode(): Int = 31 * 1 + queueState().hashCode()
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

fun dispatch(event: Event) {
    validate(event)
    EVENT_QUEUE().enqueue(event)
}

fun dispatchSync(event: Event) {
    runBlocking {
        handle(event)
    }
}
