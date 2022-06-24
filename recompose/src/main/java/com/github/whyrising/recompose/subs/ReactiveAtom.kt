package com.github.whyrising.recompose.subs

interface ReactiveAtom<T> : Reaction<T> {
  fun emit(value: T)
}
