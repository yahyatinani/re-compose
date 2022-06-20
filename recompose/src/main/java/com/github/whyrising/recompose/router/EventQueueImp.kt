package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.q

internal class EventQueueImp(queue: EventQueue = q()) : EventQueueActions,
  IEventQueue {
  private val _eventQueueRef: Atom<EventQueue> = atom(queue)

  val queue: EventQueue
    get() = _eventQueueRef()

  override val count: Int
    get() = queue.size

  override fun enqueue(event: Event): EventQueue = _eventQueueRef.swap {
    it.conj(event)
  }

  /** This function is thread safe. */
  override fun processFirstEventInQueue() = synchronized(_eventQueueRef) {
    when (val event = queue.peek()) {
      null -> queue
      else -> {
        handle(event)
        _eventQueueRef.swap { it.pop() }
      }
    }
  }

  /** This function is thread safe since it calls [processFirstEventInQueue],
   * which is already thread safe. */
  override fun processCurrentEvents() {
    for (i: Int in 0 until count)
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
