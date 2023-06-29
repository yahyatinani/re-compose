package com.github.yahyatinani.recompose.router

import com.github.yahyatinani.recompose.async.events.handle
import com.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.y.concurrency.Atom
import io.github.yahyatinani.y.concurrency.atom
import io.github.yahyatinani.y.core.collections.PersistentQueue
import io.github.yahyatinani.y.core.q

typealias EventQueue = PersistentQueue<Event>

/** Internal API of the EventQueue for the FSM to consume. */
internal interface EventQueueActions {
  val count: Int
  fun enqueue(event: Event): EventQueue
  suspend fun processFirstEventInQueue()
  suspend fun processCurrentEvents()
  fun pause()
  fun resume()
  fun exception(ex: Throwable)
}

/* Implementation */

internal class EventQueueImp(queue: EventQueue = q() as EventQueue) :
  EventQueueActions,
  IEventQueue {
  internal val _eventQueueRef: Atom<EventQueue> = atom(queue)

  val queue: EventQueue
    get() = _eventQueueRef.deref()

  override val count: Int
    get() = queue.size

  override fun enqueue(event: Event): EventQueue = _eventQueueRef.swap {
    it.conj(event)
  }

  /** This function is NOT thread safe. */
  override suspend fun processFirstEventInQueue() {
    val event = queue.peek()
    if (event != null) {
      handle(event)
      _eventQueueRef.swap { it.pop() }
    }
  }

  /** This function is NOT thread safe. */
  override suspend fun processCurrentEvents() {
    val n = count
    for (i: Int in 0 until n)
      processFirstEventInQueue()
  }

  override fun pause() {
    TODO("Not yet implemented")
  }

  override fun resume() {
    TODO("Not yet implemented")
  }

  override fun exception(ex: Throwable) {
    val event = queue.peek()
    purge()
    throw RuntimeException("event: $event", ex)
  }

  override fun purge() {
    _eventQueueRef.reset(q() as EventQueue)
  }
}
