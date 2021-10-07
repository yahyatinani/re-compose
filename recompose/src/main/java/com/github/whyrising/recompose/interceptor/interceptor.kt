package com.github.whyrising.recompose.interceptor

import com.github.whyrising.recompose.RKeys
import com.github.whyrising.recompose.RKeys.after
import com.github.whyrising.recompose.RKeys.before
import com.github.whyrising.recompose.RKeys.coeffects
import com.github.whyrising.recompose.RKeys.event
import com.github.whyrising.recompose.RKeys.originalEvent
import com.github.whyrising.recompose.RKeys.queue
import com.github.whyrising.recompose.RKeys.stack
import com.github.whyrising.y.collections.core.assocIn
import com.github.whyrising.y.collections.core.conj
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.seq.ISeq
import com.github.whyrising.y.collections.vector.IPersistentVector

typealias InterceptorFn =
    suspend (IPersistentMap<RKeys, Any>) -> IPersistentMap<RKeys, Any>

typealias Interceptor = IPersistentMap<RKeys, Any>

typealias Context = IPersistentMap<RKeys, Any>

fun toInterceptor(
    id: Any,
    before: suspend (context: Context) -> Context = { it },
    after: suspend (context: Context) -> Context = { it }
): Interceptor = m(
    RKeys.id to id,
    RKeys.before to before,
    RKeys.after to after,
)

fun assocCofx(
    context: Context,
    key: RKeys,
    value: Any
): Context = assocIn(context, l(coeffects, key), value) as Context

private fun enqueue(
    context: Context,
    interceptors: Any
): Context = context.assoc(queue, interceptors)

/**
 * Create a fresh context.
 */
internal fun context(
    eventVec: Any,
    interceptors: ISeq<Interceptor>
): Context {
    val context0 = m<RKeys, Any>()
    val context1 = assocCofx(context0, event, eventVec)
    val context2 = assocCofx(context1, originalEvent, eventVec)

    return enqueue(context2, interceptors)
}

// -- Execute Interceptor Chain  ----------------------------------------------

@Suppress("UNCHECKED_CAST")
internal suspend fun invokeInterceptorFn(
    context: Context,
    interceptor: Interceptor,
    direction: RKeys
) = when (val interceptorFn = interceptor[direction] as InterceptorFn?) {
    null -> context
    else -> interceptorFn(context)
}

/**
 * :queue and :stack in context should be lists/interceptors of type
 * IPersistentVector<*>.
 */
@Suppress("UNCHECKED_CAST")
internal suspend fun invokeInterceptors(
    context: Context,
    direction: RKeys
): Context {
    tailrec suspend fun invokeInterceptors(context: Context): Context {
        val que = context[queue] as ISeq<Interceptor>
        return when (que.count) {
            0 -> context
            else -> {
                val interceptor: Interceptor = que.first()
                val stk = context[stack] as ISeq<Any>?

                val newContext = context
                    .assoc(queue, que.rest())
                    .assoc(queue, que.rest())
                    .assoc(stack, conj(stk, interceptor))
                    .let { invokeInterceptorFn(it, interceptor, direction) }

                invokeInterceptors(newContext)
            }
        }
    }

    return invokeInterceptors(context)
}

internal fun changeDirection(context: Context): Context =
    enqueue(context, context[stack]!!)

suspend fun execute(
    eventVec: IPersistentVector<Any>,
    interceptors: ISeq<Interceptor>
) {
    context(eventVec, interceptors)
        .let { invokeInterceptors(it, before) }
        .let { changeDirection(it) }
        .let { invokeInterceptors(it, after) }
}
