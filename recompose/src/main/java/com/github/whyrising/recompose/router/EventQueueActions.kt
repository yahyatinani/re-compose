package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.y.core.collections.PersistentQueue

typealias EventQueue = PersistentQueue<Event>

internal interface EventQueueActions {
  val count: Int
  fun enqueue(event: Event): EventQueue
  fun processFirstEventInQueue(): EventQueue
  fun processCurrentEvents()
  fun pause()
  fun resume()
  fun exception(ex: Exception)
}
