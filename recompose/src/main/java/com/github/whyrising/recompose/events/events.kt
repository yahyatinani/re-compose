package com.github.whyrising.recompose.events

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.get
import com.github.whyrising.recompose.interceptor.execute
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Event
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector

val kind: Kinds = Event

// TODO: Make it lazy.
// TODO: Move to y
/**
 * Returns a flat list of interceptors, since `interceptors` can be nested as
lists of interceptors (e.g. (i1, i2, (i3)) => [i1, i2, i3]).

 * It preserves the order of `interceptors`.
 */
internal fun flatten(
    interceptors: PersistentVector<Any>
): IPersistentVector<Any> = interceptors.fold(v()) { vec, interceptor ->
    when (interceptor) {
        is PersistentVector<*> -> interceptor.fold(vec) { v, intr ->
            v.conj(intr!!)
        }
        else -> vec.conj(interceptor)
    }
}

/***
 * Associate the given event `id` with the given collection of `interceptors`.
 */
fun register(id: Any, interceptors: IPersistentVector<Any>) {
    registerHandler(id, kind, flatten(interceptors as PersistentVector))
}

/*
-------------  Handle event ----------------------
 */

@Suppress("UNCHECKED_CAST")
suspend fun handle(eventVec: IPersistentVector<Any>) {
    val handler: Any =
        getHandler(kind, eventVec[0]) ?: return

    execute(
        eventVec,
        handler as PersistentVector<IPersistentMap<Keys, Any>>
    )
}
