package com.github.whyrising.recompose.events

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.interceptor.execute
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Event
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler

val kind: Kinds = Event

private fun flatten(interceptors: ArrayList<Any>) =
    interceptors.flatMap { element: Any ->
        when (element) {
            is ArrayList<*> -> element as ArrayList<Map<Keys, Any>>
            else -> arrayListOf(element as Map<Keys, Any>)
        }
    }

/***
 * Associate the given event `id` with the given collection of `interceptors`.
 */
fun register(id: Any, interceptors: ArrayList<Any>) {
    registerHandler(id, kind, flatten(interceptors))
}

/*
-------------  Handle event ----------------------
 */

fun handle(eventVec: ArrayList<Any>) {
    val eventId = eventVec[0]
    val handler: Any = getHandler(kind, eventId) ?: return

    val chainOfInterceptors = handler as List<Map<Keys, Any>>
    execute(eventVec, chainOfInterceptors)
}

fun event(id: Any, vararg args: Any) = arrayListOf(
    id,
    *args
)
