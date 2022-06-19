package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.y.core.collections.PersistentQueue

internal interface EventQueue {
  val count: Int

  /** Push an [Event] into the [EventQueue].*/
  fun enqueue(event: Event)
//  suspend fun runAsync() // TODO: wtf is this?

  /**
   * Process all the events currently in the [EventQueue], but not any new ones.
   */
  suspend fun processCurrentEvents()
  fun pause()
  fun resume()
  fun exception()
  suspend fun processFirstEventInQueue()
}
