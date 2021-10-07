package com.github.whyrising.recompose.events

import com.github.whyrising.recompose.RKeys
import com.github.whyrising.recompose.interceptor.execute
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Event
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.concat
import com.github.whyrising.y.collections.core.conj
import com.github.whyrising.y.collections.core.lazySeq
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.seq.ISeq
import com.github.whyrising.y.collections.vector.IPersistentVector

val kind: Kinds = Event

// TODO: Move flatten to y library?
/**
 * Returns a flat nested seq of interceptors.
 * (e.g. (i1, i2, (i3)) => [i1, i2, i3]).

 * It preserves the order of `interceptors`.
 */
internal fun flatten(interceptors: IPersistentVector<Any>): ISeq<Any> =
    lazySeq {
        interceptors.foldRight<Any, ISeq<Any>>(lazySeq()) { interceptor, seq ->
            when (interceptor) {
                is IPersistentVector<*> -> concat(interceptor, seq)
                else -> conj(seq, interceptor) as ISeq<Any>
            }
        }
    }

/***
 * Associate the given event `id` with the given collection of `interceptors`.
 */
fun register(id: Any, interceptors: IPersistentVector<Any>) {
    registerHandler(id, kind, flatten(interceptors))
}

/*
-------------  Handle event ----------------------
 */

@Suppress("UNCHECKED_CAST")
suspend fun handle(eventVec: IPersistentVector<Any>) {
    val interceptors = getHandler(kind, eventVec[0])
        as ISeq<IPersistentMap<RKeys, Any>>?

    execute(eventVec, interceptors ?: return)
}
