package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.async.events.handle
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.PersistentQueue
import com.github.whyrising.y.core.q

typealias EventQueue = PersistentQueue<Event>

/** Internal API of the EventQueue for the FSM to consume. */
internal interface EventQueueActions {
  val count: Int
  fun enqueue(event: Event): EventQueue
  suspend fun processFirstEventInQueue(): Unit
  suspend fun processCurrentEvents()
  fun pause()
  fun resume()
  fun exception(ex: Exception)
}

/* Implementation */

internal class EventQueueImp(queue: EventQueue = q()) :
  EventQueueActions,
  IEventQueue {
  internal val _eventQueueRef: Atom<EventQueue> = atom(queue)

  val queue: EventQueue
    get() = _eventQueueRef()

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

  override fun exception(ex: Exception) {
    purge()
    throw ex
  }

  override fun purge() {
    _eventQueueRef.reset(q())
  }
}
