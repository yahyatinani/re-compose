package com.github.whyrising.recompose.interceptor

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.after
import com.github.whyrising.recompose.Keys.before
import com.github.whyrising.recompose.Keys.coeffects
import com.github.whyrising.recompose.Keys.event
import com.github.whyrising.recompose.Keys.originalEvent
import com.github.whyrising.recompose.Keys.queue
import com.github.whyrising.recompose.Keys.stack
import com.github.whyrising.y.collections.concretions.list.PersistentList
import com.github.whyrising.y.collections.core.assocIn
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap

fun toInterceptor(
    id: Any,
    before: (
        context: IPersistentMap<Keys, Any>
    ) -> IPersistentMap<Keys, Any> = { it },
    after: (
        context: IPersistentMap<Keys, Any>
    ) -> IPersistentMap<Keys, Any> = { it }
): IPersistentMap<Keys, Any> = m(
    Keys.id to id,
    Keys.before to before,
    Keys.after to after,
)

fun assocCofx(
    context: IPersistentMap<Keys, Any>,
    key: Keys,
    value: Any
): IPersistentMap<Keys, Any> =
    assocIn(context, l(coeffects, key), value) as IPersistentMap<Keys, Any>

private fun enqueue(
    context: IPersistentMap<Keys, Any>,
    interceptors: Any
): IPersistentMap<Keys, Any> = context.assoc(queue, interceptors)

/**
 * Create a fresh context.
 */
internal fun context(
    eventVec: Any,
    interceptors: List<IPersistentMap<Keys, Any>>
): IPersistentMap<Keys, Any> {
    val context0 = m<Keys, Any>()
    val context1 = assocCofx(context0, event, eventVec)
    val context2 = assocCofx(context1, originalEvent, eventVec)

    return enqueue(context2, interceptors)
}

// -- Execute Interceptor Chain  ----------------------------------------------

internal fun invokeInterceptorFn(
    context: IPersistentMap<Keys, Any>,
    interceptor: IPersistentMap<Keys, Any>,
    direction: Keys
): IPersistentMap<Keys, Any> {
    val f = get(interceptor, direction) as (IPersistentMap<Keys, Any>) -> Any

    return when (val r = f(context)) {
        is IPersistentMap<*, *> -> (r as IPersistentMap<Keys, Any>)
        else -> context
    }
}

/**
 * :queue and :stack in context should be lists/interceptors of type
 * PersistentList<*>.
 */
internal fun invokeInterceptors(
    context: IPersistentMap<Keys, Any>,
    direction: Keys
): IPersistentMap<Keys, Any> {
    tailrec fun invokeInterceptors(
        context: IPersistentMap<Keys, Any>
    ): IPersistentMap<Keys, Any> {
        val qu = get(context, queue)
            as PersistentList<IPersistentMap<Keys, Any>>

        return when {
            qu.isEmpty() -> context
            else -> {
                val interceptor: IPersistentMap<Keys, Any> = qu.first()
                val stk =
                    (get(context, stack) ?: l<Any>()) as PersistentList<Any>

                val c = context
                    .assoc(queue, qu.rest())
                    .assoc(stack, stk.conj(interceptor))

                val newContext = invokeInterceptorFn(c, interceptor, direction)

                invokeInterceptors(newContext)
            }
        }
    }

    return invokeInterceptors(context)
}

internal fun changeDirection(
    context: IPersistentMap<Keys, Any>
): IPersistentMap<Keys, Any> = enqueue(context, get(context, stack)!!)

fun execute(
    eventVec: List<Any>,
    interceptors: List<IPersistentMap<Keys, Any>>
) {
    val context0 = context(eventVec, interceptors)
    val context1 = invokeInterceptors(context0, before)
    val context2 = changeDirection(context1)
    val context3 = invokeInterceptors(context2, after)
}
