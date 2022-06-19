package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.PersistentQueue
import com.github.whyrising.y.core.q

/**
 * Not thread safe!!
 */
internal class EventQueueImp(queue: PersistentQueue<Event> = q()) : EventQueue {
  private val _eventQueueRef: Atom<PersistentQueue<Event>> = atom(queue)

  val queue: PersistentQueue<Event>
    get() = _eventQueueRef()

  override val count: Int
    get() = queue.size

  override fun enqueue(event: Event) {
    _eventQueueRef.swap { it.conj(event) }
  }

  override suspend fun processFirstEventInQueue() {
    val event = queue.peek()
    if (event != null) {
      try {
        handle(event)
        _eventQueueRef.swap { it.pop() }
      } catch (e: Exception) {
        TODO()
      }
    }
  }

  override suspend fun processCurrentEvents() {
    for (i: Int in 0 until count)
      processFirstEventInQueue()
  }

  override fun pause() {
    TODO("Not yet implemented")
  }

  override fun resume() {
    TODO("Not yet implemented")
  }

  override fun exception() {
    TODO("Not yet implemented")
  }
}
