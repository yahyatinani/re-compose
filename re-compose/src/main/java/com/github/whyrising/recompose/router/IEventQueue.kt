package com.github.whyrising.recompose.router

/* Public API for the event queue. */
interface IEventQueue {
  fun purge()
  // TODO: addPostEventCallback
  // TODO: removePostEventCallback
}
