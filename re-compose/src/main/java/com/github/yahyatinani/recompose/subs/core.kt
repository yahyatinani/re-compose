@file:Suppress("UNCHECKED_CAST")

package com.github.yahyatinani.recompose.subs

import android.util.Log
import com.github.yahyatinani.recompose.TAG
import com.github.yahyatinani.recompose.db.appDb
import com.github.yahyatinani.recompose.registrar.Kinds
import com.github.yahyatinani.recompose.registrar.Kinds.Sub
import com.github.yahyatinani.recompose.registrar.getHandler
import com.github.yahyatinani.recompose.registrar.registerHandler
import io.github.yahyatinani.y.concurrency.Atom
import io.github.yahyatinani.y.concurrency.atom
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.util.m

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<V> = (Atom<Any>, Query) -> Reaction<V>

// -- cache --------------------------------------------------------------------

/**
 * [Reaction]s cache atom/hashmap.
 *
 * [Query] to [Reaction] associations. */
internal val reactionsCache = atom(m<Query, Reaction<*>>())

internal fun <V> cacheReaction(key: Query, reaction: Reaction<V>): Reaction<V> {
  reaction.addOnDispose { r ->
    reactionsCache.swap { cache ->
      val cr = cache[key]
      if (cr != null && r === cr) {
        Log.d(TAG, "${r.id} is removed from cache.")
        cache.dissoc(key)
      } else cache
    }
  }
  reactionsCache.swap { cache -> cache.assoc(key, reaction) }
  return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <V> subscribe(query: Query): Reaction<V> {
  val cachedReaction = reactionsCache.deref()[query]

  if (cachedReaction != null) return cachedReaction as Reaction<V>

  Log.d(TAG, "Cache not found for subscription: $query")

  val (queryId) = query
  val subHandler = getHandler(kind, queryId) as (SubHandler<V>)?
    ?: throw IllegalStateException(
      "No subscription handler registered for id: `$queryId`"
    )

  return cacheReaction(key = query, reaction = subHandler(appDb, query))
}

// -- regSub -----------------------------------------------------------------
inline fun <Db, V> regDbSubscription(
  queryId: Any,
  crossinline extractorFn: (db: Db, queryVec: Query) -> V
) {
  registerHandler(
    id = queryId,
    kind = kind,
    handlerFn = { appDb: Atom<*>, queryVec: Query ->
      Extraction(appDb = appDb, id = queryId) { signalValue: Any? ->
        extractorFn(signalValue as Db, queryVec)
      }
    }
  )
}

inline fun <I, V> regCompSubscription(
  queryId: Any,
  crossinline signalsFn: (queryVec: Query) -> IPersistentVector<Reaction<I>>,
  initialValue: V,
  crossinline computationFn: suspend (
    subscriptions: IPersistentVector<I>,
    currentValue: V,
    queryVec: Query
  ) -> V
) {
  registerHandler(
    id = queryId,
    kind = kind,
    handlerFn = { _: Atom<*>, queryVec: Query ->
      Computation(
        inputSignals = signalsFn(queryVec) as Signals,
        initial = initialValue,
        id = queryId
      ) { signalsValues, currentValue ->
        computationFn(
          signalsValues as IPersistentVector<I>,
          currentValue as V,
          queryVec
        )
      }
    }
  )
}
