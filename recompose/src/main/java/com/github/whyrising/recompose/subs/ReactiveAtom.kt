package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.IDeref

interface ReactiveAtom<T> : IDeref<T> {
    suspend fun collect(action: suspend (T) -> Unit)

    suspend fun emit(value: T)
}
