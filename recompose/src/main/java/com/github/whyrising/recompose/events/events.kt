package com.github.whyrising.recompose.events

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.interceptor.execute
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Event
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.concretions.list.PersistentList
import com.github.whyrising.y.concretions.list.l

val kind: Kinds = Event

// TODO: Make it lazy.
/**
 * Returns a flat list of interceptors, since `interceptors` can be nested as
lists of interceptors (e.g. (i1, i2, (i3)) => [i1, i2, i3]).

 * It preserves the order of `interceptors`.
 */
internal fun flatten(interceptors: List<Any>): PersistentList<Any> =
    interceptors.foldRight(l()) { interceptor, list ->
        when (interceptor) {
            is List<*> -> {
                interceptor.foldRight(list) { item, l ->
                    l.conj(item!!)
                }
            }
            else -> list.conj(interceptor)
        }
    }

/***
 * Associate the given event `id` with the given collection of `interceptors`.
 */
fun register(id: Any, interceptors: List<Any>) {
    registerHandler(id, kind, flatten(interceptors))
}

/*
-------------  Handle event ----------------------
 */

fun handle(eventVec: List<Any>) {
    val eventId = eventVec[0]
    val handler: Any = getHandler(kind, eventId) ?: return

    val chainOfInterceptors = handler as List<Map<Keys, Any>>
    execute(eventVec, chainOfInterceptors)
}

fun event(id: Any, vararg args: Any) = arrayListOf(
    id,
    *args
)
