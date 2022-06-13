package com.github.whyrising.recompose.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.recompose.router.EventQueue.enqueue
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.q
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * This class is a FIFO PersistentQueue that allows us to handle incoming events
 * according to the producer-consumer pattern.
 */
internal object EventQueue : ViewModel() {
    internal val qAtom = atom(q<Event>())

    @Volatile
    internal var deferredUntilEvent = CompletableDeferred<Unit>()

    private suspend fun dequeue() {
        val queue = qAtom()
        val event = queue.peek()
        if (event != null) {
            try {
                if (qAtom.compareAndSet(queue, queue.pop()))
                    handle(event)
            } catch (e: Exception) {
                purge()
                throw e
            }
        } else {
            deferredUntilEvent.await()
            deferredUntilEvent = CompletableDeferred()
        }
    }

    // TODO: maybe more consumers?
    internal val consumerJob = viewModelScope.launch {
        while (true)
            dequeue()
    }

    internal fun enqueue(event: Event): Job {
        return viewModelScope.launch {
            qAtom.swap { it.conj(event) }
            deferredUntilEvent.complete(Unit)
        }
    }

    /**
     * Empties the event queue.
     */
    fun purge() {
        qAtom.reset(q())
    }

    // TODO: remove
    override fun onCleared() {
        super.onCleared()

        qAtom.reset(q())
        viewModelScope.cancel()
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
    enqueue(event)
}

fun dispatchSync(event: Event) {
    runBlocking {
        handle(event)
    }
}
