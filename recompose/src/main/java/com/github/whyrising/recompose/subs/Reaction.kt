package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.IDeref
import kotlinx.coroutines.flow.Flow

interface Reaction<T> : IDeref<T>, Flow<T> {
  val f: Any?
  val initialValue: Any?
}
