package io.github.yahyatinani.recompose.subs

import io.github.yahyatinani.y.concurrency.IDeref
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
