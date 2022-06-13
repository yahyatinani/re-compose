package com.github.whyrising.recompose.events

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.execute
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.concat
import com.github.whyrising.y.core.conj
import com.github.whyrising.y.core.lazySeq

val kind: Kinds = Kinds.Event

typealias DbEventHandler<T> = (db: T, event: Event) -> Any
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
suspend fun handle(event: Event) {
  val interceptors = getHandler(kind, event[0]) as ISeq<Interceptor>?

  execute(event, interceptors ?: return)
}
