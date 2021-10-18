package com.github.whyrising.recompose.subs

interface Disposable<T> {
    fun addOnDispose(f: (Reaction<T>) -> Unit)

    fun dispose()
}
