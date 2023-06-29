package io.github.yahyatinani.recompose.async.events

import io.github.yahyatinani.recompose.async.interceptor.execute
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.events.kind
import io.github.yahyatinani.recompose.interceptor.Interceptor
import io.github.yahyatinani.recompose.registrar.getHandler
import io.github.yahyatinani.y.core.collections.ISeq

@Suppress("UNCHECKED_CAST")
suspend fun handle(event: Event) {
  val interceptors = getHandler(kind, event[0]) as ISeq<Interceptor>? ?: return
  execute(event, interceptors)
}
