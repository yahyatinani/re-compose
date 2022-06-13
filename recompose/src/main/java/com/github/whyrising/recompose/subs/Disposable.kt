package com.github.whyrising.recompose.subs

interface Disposable {
  fun addOnDispose(f: (ReactionBase<*, *>) -> Unit)

  fun dispose()
}
