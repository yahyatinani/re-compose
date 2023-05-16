@file:Suppress("UNCHECKED_CAST")

package com.github.whyrising.recompose.async.interceptor

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.ids.InterceptSpec
import com.github.whyrising.recompose.ids.InterceptSpec.after
import com.github.whyrising.recompose.ids.InterceptSpec.after_async
import com.github.whyrising.recompose.ids.InterceptSpec.before
import com.github.whyrising.recompose.ids.context.queue
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.InterceptorFnAsync
import com.github.whyrising.recompose.interceptor.changeDirection
import com.github.whyrising.recompose.interceptor.context
import com.github.whyrising.recompose.interceptor.defaultInterceptorAsyncFn
import com.github.whyrising.recompose.interceptor.invokeInterceptorFn
import com.github.whyrising.recompose.interceptor.invokeInterceptors
import com.github.whyrising.recompose.interceptor.stackAndQueue
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.get

// -- Execute Interceptor Chain  ----------------------------------------------

internal suspend fun invokeInterceptorAfterFn(
  context: Context,
  interceptor: Interceptor,
  direction: InterceptSpec
): Context {
  val interceptAsync = interceptor[after_async]
  if (interceptAsync !== defaultInterceptorAsyncFn) {
    // run :after_async.
    return (interceptAsync as InterceptorFnAsync)(context)
  }
  // run :after.
  return invokeInterceptorFn(context, interceptor, direction)
}

/**
 * :queue and :stack in context should be lists/interceptors of type
 * IPersistentVector<*>.
 */
internal tailrec suspend fun invokeInterceptorsAfter(
  context: Context,
  direction: InterceptSpec
): Context {
  val que = context[queue] as ISeq<Interceptor>
  if (que.count == 0) return context

  val interceptor = que.first()

  return invokeInterceptorsAfter(
    context = invokeInterceptorAfterFn(
      context = stackAndQueue(context, que, interceptor),
      interceptor = interceptor,
      direction = direction
    ),
    direction = direction
  )
}

suspend fun execute(event: Event, interceptors: ISeq<Interceptor>): Context =
  context(event, interceptors)
    .let { invokeInterceptors(it, before) }
    .let { changeDirection(it) }
    .let { invokeInterceptorsAfter(it, after) }
