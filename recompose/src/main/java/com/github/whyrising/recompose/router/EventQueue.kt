package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event

internal interface EventQueue {
  val count: Int

//  suspend fun runAsync() // TODO: wtf is this?

  fun enqueue(event: Event)
  suspend fun processFirstEventInQueue()
  suspend fun processCurrentEvents()
  fun pause()
  fun resume()
  fun exception()
}
