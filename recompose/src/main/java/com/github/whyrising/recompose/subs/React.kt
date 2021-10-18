package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.IDeref

interface React<T> : IDeref<T> {
    suspend fun collect(action: suspend (T) -> Unit)
}
