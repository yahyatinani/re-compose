@file:Suppress("UNCHECKED_CAST")

package io.github.yahyatinani.recompose.subs

import android.util.Log
import androidx.compose.runtime.MutableState
import io.github.yahyatinani.recompose.TAG
import io.github.yahyatinani.recompose.db.appDb
import io.github.yahyatinani.recompose.registrar.Kinds
import io.github.yahyatinani.recompose.registrar.Kinds.Sub
import io.github.yahyatinani.recompose.registrar.getHandler
import io.github.yahyatinani.recompose.registrar.registerHandler
import io.github.yahyatinani.y.core.collections.IPersistentVector
import kotlinx.atomicfu.locks.reentrantLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.set

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<V> = (MutableState<Any>, Query) -> Reaction<V>

// -- cache --------------------------------------------------------------------

/**
 * [Reaction]s cache atom/hashmap.
 *
 * [Query] to [Reaction] associations. */
internal val reactionsCache = ConcurrentHashMap<Query, Reaction<*>>()

private val lock = reentrantLock()

internal fun <V> cacheReaction(key: Query, reaction: Reaction<V>): Reaction<V> {
  reaction.addOnDispose { r ->
    if (reactionsCache.remove(key, r)) {
      Log.d(TAG, "${r.id} is removed from cache.")
    }
  }
  reactionsCache[key] = reaction
  return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <V> subscribe(query: Query): Reaction<V> {
  val cachedReaction = reactionsCache[query]

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
  val handlerFn: (MutableState<*>, Query) -> Extraction =
    { appDb: MutableState<*>, queryVec: Query ->
      Extraction(appDb = { appDb.value }, id = queryId) { signalValue: Any? ->
        extractorFn(signalValue as Db, queryVec)
      }
    }
  registerHandler(
    id = queryId,
    kind = kind,
    handlerFn = handlerFn
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
  // warning: leave it unlined, the compiler doesn't catch the new type of appDb
  // if we changed it.
  val handlerFn: (MutableState<*>, Query) -> Computation =
    { _: MutableState<*>, queryVec: Query ->
      Computation(
        inputSignals = signalsFn(queryVec) as Signals,
        initialValue = initialValue,
        id = queryId
      ) { signalsValues, currentValue ->
        computationFn(
          signalsValues as IPersistentVector<I>,
          currentValue as V,
          queryVec
        )
      }
    }
  registerHandler(
    id = queryId,
    kind = kind,
    handlerFn = handlerFn
  )
}
