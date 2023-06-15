package com.github.yahyatinani.recompose.router

import com.github.yahyatinani.recompose.TAG
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.events.handle

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
