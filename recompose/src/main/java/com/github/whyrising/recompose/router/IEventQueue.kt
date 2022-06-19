package com.github.whyrising.recompose.router.fsm

/* Public API. */
interface IEventQueue {
  fun purge()
  // TODO: addPostEventCallback
  // TODO: removePostEventCallback
}
