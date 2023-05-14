@file:Suppress("UNCHECKED_CAST")

package com.github.whyrising.recompose.interceptor

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.ids.coeffects
import com.github.whyrising.recompose.ids.context.queue
import com.github.whyrising.recompose.ids.context.stack
import com.github.whyrising.recompose.ids.interceptor
import com.github.whyrising.y.core.assocIn
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.conj
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.m
import com.github.whyrising.recompose.ids.context as ctx

typealias Context = IPersistentMap<ctx, Any>

typealias Interceptor = IPersistentMap<interceptor, Any>

typealias InterceptorFn = (context: Context) -> Context

internal val defaultInterceptorFn: InterceptorFn = { it }

fun toInterceptor(
  id: Any,
  before: InterceptorFn = defaultInterceptorFn,
  after: InterceptorFn = defaultInterceptorFn
): Interceptor = m(
  interceptor.id to id,
  interceptor.before to before,
  interceptor.after to after
)

fun assocCofx(
  context: Context,
  key: coeffects,
  value: Any
): Context = assocIn(context, l(ctx.coeffects, key), value) as Context

internal fun enqueue(
  context: Context,
  interceptors: ISeq<Interceptor>?
): Context = context.assoc(queue, interceptors ?: l<Any>())

/**
 * Create a fresh context.
 */
internal fun context(
  event: Event,
  interceptors: ISeq<Interceptor>
): Context = m<ctx, Any>()
  .let { assocCofx(it, coeffects.event, event) }
  .let { assocCofx(it, coeffects.originalEvent, event) }
  .let { enqueue(it, interceptors) }

// -- Execute Interceptor Chain  ----------------------------------------------

internal fun invokeInterceptorFn(
  context: Context,
  interceptor: Interceptor,
  direction: interceptor
): Context = when (val fn = interceptor[direction] as InterceptorFn?) {
  null -> context
  else -> fn(context)
}

/**
 * :queue and :stack in context should be lists/interceptors of type
 * IPersistentVector<*>.
 */
internal fun invokeInterceptors(
  context: Context,
  direction: interceptor
): Context {
  tailrec fun invokeInterceptors(context: Context): Context {
    val que = context[queue] as ISeq<Interceptor>
    return when (que.count) {
      0 -> context
      else -> {
        val interceptor = que.first()
        val stk = context[stack] as ISeq<Any>?
        val newContext = context
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
  enqueue(context, context[stack] as ISeq<Interceptor>?)

fun execute(
  event: Event,
  interceptors: ISeq<Interceptor>
): Context = context(event, interceptors)
  .let { invokeInterceptors(it, interceptor.before) }
  .let { changeDirection(it) }
  .let { invokeInterceptors(it, interceptor.after) }
