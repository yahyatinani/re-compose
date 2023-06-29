package io.github.yahyatinani.recompose.events

import io.github.yahyatinani.recompose.cofx.Coeffects
import io.github.yahyatinani.recompose.fx.Effects
import io.github.yahyatinani.recompose.interceptor.Interceptor
import io.github.yahyatinani.recompose.interceptor.execute
import io.github.yahyatinani.recompose.registrar.Kinds
import io.github.yahyatinani.recompose.registrar.getHandler
import io.github.yahyatinani.recompose.registrar.registerHandler
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.collections.ISeq
import io.github.yahyatinani.y.core.concat
import io.github.yahyatinani.y.core.conj
import io.github.yahyatinani.y.core.lazySeq

val kind: Kinds = Kinds.Event

typealias DbEventHandler<Db> = (db: Db, event: Event) -> Any
typealias FxEventHandler = (cofx: Coeffects, event: Event) -> Effects

// TODO: Move flatten to y library?
/**
 * Returns a flat nested seq of interceptors.
 * (e.g. (i1, i2, (i3)) => [i1, i2, i3]).

 * It preserves the order of `interceptors`.
 */
internal fun flatten(interceptors: IPersistentVector<Any>): ISeq<Any> =
  lazySeq {
    interceptors.foldRight<Any, ISeq<Any>>(lazySeq()) { interceptor, seq ->
      when (interceptor) {
        is IPersistentVector<*> -> concat(interceptor, seq)
        else -> conj(seq, interceptor) as ISeq<Any>
      }
    }
  }

/***
 * Associate the given event `id` with the given collection of `interceptors`.
 */
fun register(id: Any, interceptors: IPersistentVector<Any>) {
  registerHandler(id, kind, flatten(interceptors))
}

/*
-------------  Handle event ----------------------
 */

typealias Event = IPersistentVector<Any>

@Suppress("UNCHECKED_CAST")
fun handle(event: Event) {
  val interceptors = getHandler(kind, event[0]) as ISeq<Interceptor>? ?: return
  execute(event, interceptors)
}
