package com.github.whyrising.recompose.subs

interface Disposable<I, O> {
    fun addOnDispose(f: (Reaction<I, O>) -> Unit)

    fun dispose()
}
