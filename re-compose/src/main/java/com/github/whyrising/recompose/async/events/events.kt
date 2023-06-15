package com.github.whyrising.recompose.async.events

import com.github.whyrising.recompose.async.interceptor.execute
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.kind
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.y.core.collections.ISeq

@Suppress("UNCHECKED_CAST")
suspend fun handle(event: Event) {
  val interceptors = getHandler(kind, event[0]) as ISeq<Interceptor>? ?: return
  execute(event, interceptors)
}
