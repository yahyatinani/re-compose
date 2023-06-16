@file:Suppress("UNCHECKED_CAST")

package com.github.yahyatinani.recompose.interceptor

import com.github.whyrising.y.core.assocIn
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.conj
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.m
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.ids.InterceptSpec
import com.github.yahyatinani.recompose.ids.coeffects
import com.github.yahyatinani.recompose.ids.context.queue
import com.github.yahyatinani.recompose.ids.context.stack
import com.github.yahyatinani.recompose.ids.context as ctx

typealias Context = IPersistentMap<ctx, Any>

typealias Interceptor = IPersistentMap<InterceptSpec, Any>

typealias InterceptorFn = (context: Context) -> Context

typealias InterceptorFnAsync = suspend (context: Context) -> Context

internal val defaultInterceptorFn: InterceptorFn = { it }

internal val defaultInterceptorAsyncFn: InterceptorFnAsync = { it }

fun toInterceptor(
  id: Any,
  before: InterceptorFn = defaultInterceptorFn,
  after: InterceptorFn = defaultInterceptorFn,
  afterAsync: InterceptorFnAsync = defaultInterceptorAsyncFn
): Interceptor = m(
  InterceptSpec.id to id,
  InterceptSpec.before to before,
  InterceptSpec.after to after,
  InterceptSpec.after_async to afterAsync
)

fun assocCofx(context: Context, key: coeffects, value: Any) =
  assocIn(context, l(ctx.coeffects, key), value) as Context

internal fun enqueue(context: Context, interceptors: ISeq<Interceptor>?) =
  context.assoc(queue, interceptors ?: l<Any>())

/** Create a fresh context. */
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
  direction: InterceptSpec
): Context = (interceptor[direction] as InterceptorFn)(context)

internal fun stackAndQueue(
  context: Context,
  que: ISeq<Interceptor>,
  interceptor: Interceptor
) = context
  .assoc(queue, que.rest())
  .assoc(stack, conj(context[stack] as ISeq<Any>?, interceptor))

/**
 * :queue and :stack in context should be lists/interceptors of type
 * IPersistentVector<*>.
 */
internal tailrec fun invokeInterceptors(
  context: Context,
  direction: InterceptSpec
): Context {
  val que = context[queue] as ISeq<Interceptor>
  if (que.count == 0) return context

  val interceptor = que.first()

  return invokeInterceptors(
    context = invokeInterceptorFn(
      context = stackAndQueue(context, que, interceptor),
      interceptor = interceptor,
      direction = direction
    ),
    direction = direction
  )
}

internal fun changeDirection(context: Context): Context =
  enqueue(context, context[stack] as ISeq<Interceptor>?)

fun execute(event: Event, interceptors: ISeq<Interceptor>): Context =
  context(event, interceptors)
    .let { invokeInterceptors(it, InterceptSpec.before) }
    .let { changeDirection(it) }
    .let { invokeInterceptors(it, InterceptSpec.after) }
