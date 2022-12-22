package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.IDeref
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Reaction<T> : IDeref<T>, Flow<T> {
  val state: StateFlow<Any?>
}
