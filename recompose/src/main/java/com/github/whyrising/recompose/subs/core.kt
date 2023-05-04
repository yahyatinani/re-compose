package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.util.m
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<I, O> = (Atom<Any>, Query) -> ReactionBase<I, O>

// -- cache --------------------------------------------------------------------
internal val queryToReactionCache = MutableStateFlow(m<Query, Any>())

internal fun <V> cacheReaction(
  queryV: Query,
  reaction: ReactionBase<Any, V>
): Reaction<V> {
  reaction.addOnDispose { r ->
    queryToReactionCache.update { qToR ->
      val cachedR = qToR[queryV]
      when {
        cachedR == null || cachedR !== r -> qToR
        else -> qToR.dissoc(queryV)
      }
    }
    Log.i(TAG, "$r is removed from cache.")
  }
  queryToReactionCache.update { qToR -> qToR.assoc(queryV, reaction) }
  return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <V> subscribe(query: Query): Reaction<V> {
  val cachedReaction = queryToReactionCache.value[query]

  if (cachedReaction != null) {
    return cachedReaction as Reaction<V>
  }
  Log.i(TAG, "Cache not found for subscription: $query")

  val (queryId) = query
  val subHandler = getHandler(kind, queryId) as (SubHandler<Any, V>)?
    ?: throw IllegalStateException(
      "no subscription handler registered for id: `$queryId`"
    )

  return cacheReaction(queryV = query, reaction = subHandler(appDb, query))
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
      Extraction(inputSignal = appDb) { signalValue: Any? ->
        extractorFn(signalValue as Db, queryVec)
      }
    }
  )
}

inline fun <S, V> regCompSubscription(
  queryId: Any,
  crossinline signalsFn: (queryVec: Query) -> IPersistentVector<Reaction<S>>,
  initialValue: V,
  crossinline computationFn: suspend (
    subscriptions: IPersistentVector<S>,
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
        initial = initialValue
      ) { signalsValues, currentValue ->
        computationFn(
          signalsValues as IPersistentVector<S>,
          currentValue as V,
          queryVec
        )
      }
    }
  )
}
