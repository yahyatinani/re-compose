package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.IDeref
import kotlinx.coroutines.flow.Flow

interface Disposable {
  fun addOnDispose(f: (Reaction<*>) -> Unit)

  fun dispose(): Boolean
}

interface Reaction<T> : IDeref<T>, Flow<T>, Disposable {
  val f: Any?
  val initialValue: Any?
  val id: Any
}
