@file:Suppress("UNCHECKED_CAST")

package io.github.yahyatinani.recompose.interceptor

import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.ids.InterceptSpec
import io.github.yahyatinani.recompose.ids.coeffects
import io.github.yahyatinani.recompose.ids.context.queue
import io.github.yahyatinani.recompose.ids.context.stack
import io.github.yahyatinani.y.core.assocIn
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.collections.ISeq
import io.github.yahyatinani.y.core.conj
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.l
import io.github.yahyatinani.y.core.m
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import io.github.yahyatinani.recompose.ids.context as ctx

typealias Context = IPersistentMap<ctx, Any>

typealias Interceptor = IPersistentMap<InterceptSpec, Any>

typealias InterceptorFn = (context: Context) -> Context

internal val defaultInterceptorFn: InterceptorFn = { it }

fun toInterceptor(
  id: Any,
  before: InterceptorFn = defaultInterceptorFn,
  after: InterceptorFn = defaultInterceptorFn
): Interceptor = m(
  InterceptSpec.id to id,
  InterceptSpec.before to before,
  InterceptSpec.after to after
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

internal val lock = reentrantLock()

fun execute(event: Event, interceptors: ISeq<Interceptor>): Context {
  val context = context(event, interceptors)
  return lock.withLock {
    context
      .let { invokeInterceptors(it, InterceptSpec.before) }
      .let { changeDirection(it) }
      .let { invokeInterceptors(it, InterceptSpec.after) }
  }
}
