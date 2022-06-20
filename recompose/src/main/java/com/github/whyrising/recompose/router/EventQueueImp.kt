package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.q

/** Not thread safe!! */
internal class EventQueueImp(queue: EventQueue = q()) : EventQueueActions {
  private val _eventQueueRef: Atom<EventQueue> = atom(queue)

  val queue: EventQueue
    get() = _eventQueueRef()

  override val count: Int
    get() = queue.size

  override fun enqueue(event: Event): EventQueue {
    return _eventQueueRef.swap { it.conj(event) }
  }

  override fun processFirstEventInQueue(): EventQueue {
    val event = queue.peek()
    
    if (event != null) try {
      handle(event)
      return _eventQueueRef.swap { it.pop() }
    } catch (e: Exception) {
      TODO()
    }
    return queue
  }

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

  override fun exception() {
    TODO("Not yet implemented")
  }
}
