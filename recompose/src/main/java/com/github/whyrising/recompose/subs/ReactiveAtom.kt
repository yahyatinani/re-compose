package com.github.whyrising.recompose.subs

interface ReactiveAtom<T> : Reaction<T> {
    suspend fun emit(value: T)
}
