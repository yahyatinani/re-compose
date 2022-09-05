package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.handle
import kotlinx.coroutines.runBlocking

internal val eventQueueFSM = EventQueueFSM(EventQueueImp())

// -- Dispatching --------------------------------------------------------------

private fun validate(event: Event) {
  if (event.count == 0) {
    throw IllegalArgumentException(
      "$TAG: `dispatch` was called with an empty event vector."
    )
  }
}

internal fun dispatch(event: Event) {
  validate(event)
  eventQueueFSM.handle(FsmEvent.ADD_EVENT, event)
}

internal fun dispatchSync(event: Event) {
  runBlocking {
    handle(event)
  }
}
