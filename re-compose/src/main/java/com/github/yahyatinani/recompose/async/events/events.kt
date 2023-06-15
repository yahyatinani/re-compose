package com.github.yahyatinani.recompose.async.events

import com.github.whyrising.y.core.collections.ISeq
import com.github.yahyatinani.recompose.async.interceptor.execute
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.events.kind
import com.github.yahyatinani.recompose.interceptor.Interceptor
import com.github.yahyatinani.recompose.registrar.getHandler

@Suppress("UNCHECKED_CAST")
suspend fun handle(event: Event) {
  val interceptors = getHandler(kind, event[0]) as ISeq<Interceptor>? ?: return
  execute(event, interceptors)
}
