package com.github.whyrising.recompose.router

/* Public API. */
interface IEventQueue {
  fun purge()
  // TODO: addPostEventCallback
  // TODO: removePostEventCallback
}
