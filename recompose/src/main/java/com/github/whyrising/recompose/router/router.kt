package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle

internal val eventQueueFSM = EventQueueFSM(EventQueueImp())

// -- Dispatching --------------------------------------------------------------

internal fun dispatch(event: Event) {
  if (event.count == 0) {
    throw IllegalArgumentException(
      "$TAG: `dispatch` was called with an empty event vector."
    )
  }
  eventQueueFSM.push(event)
}

internal fun dispatchSync(event: Event) {
  handle(event)
}
