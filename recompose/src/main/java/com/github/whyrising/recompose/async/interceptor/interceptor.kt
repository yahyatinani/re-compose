@file:Suppress("UNCHECKED_CAST")

package com.github.whyrising.recompose.async.interceptor

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.ids.InterceptSpec
import com.github.whyrising.recompose.ids.InterceptSpec.after
import com.github.whyrising.recompose.ids.InterceptSpec.after_async
import com.github.whyrising.recompose.ids.InterceptSpec.before
import com.github.whyrising.recompose.ids.context.queue
import com.github.whyrising.recompose.ids.context.stack
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.interceptor.InterceptorFnAsync
import com.github.whyrising.recompose.interceptor.changeDirection
import com.github.whyrising.recompose.interceptor.context
import com.github.whyrising.recompose.interceptor.defaultInterceptorAsyncFn
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.conj
import com.github.whyrising.y.core.get

// -- Execute Interceptor Chain  ----------------------------------------------

internal suspend fun invokeInterceptorFn(
  context: Context,
  interceptor: Interceptor,
  direction: InterceptSpec
): Context {
  if (direction === after) {
    val interceptAsync = interceptor[after_async]
    if (interceptAsync !== defaultInterceptorAsyncFn) {
      return (interceptAsync as InterceptorFnAsync)(context)
    }
  }
  return (interceptor[direction] as InterceptorFn)(context)
}

/**
 * :queue and :stack in context should be lists/interceptors of type
 * IPersistentVector<*>.
 */
internal suspend fun invokeInterceptors(
  context: Context,
  direction: InterceptSpec
): Context {
  tailrec suspend fun invokeInterceptors(context: Context): Context {
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

suspend fun execute(event: Event, interceptors: ISeq<Interceptor>): Context =
  context(event, interceptors)
    .let { invokeInterceptors(it, before) }
    .let { changeDirection(it) }
    .let { invokeInterceptors(it, after) }
