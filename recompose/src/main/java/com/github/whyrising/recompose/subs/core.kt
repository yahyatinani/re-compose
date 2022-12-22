package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.util.m
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<I, O> = (Reaction<I>, Query) -> ReactionBase<I, O>

// -- cache --------------------------------------------------------------------
internal val queryToReactionCache = MutableStateFlow(m<Query, Any>())

internal fun <T> cacheReaction(
  queryV: Query,
  reaction: ReactionBase<Any, T>
): Reaction<T> {
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
internal fun <T> subscribe(query: Query): Reaction<T> {
  println("subscribe $query")
  val cachedReaction = queryToReactionCache.value[query]

  if (cachedReaction != null) {
    return cachedReaction as Reaction<T>
  }
  Log.i(TAG, "Cache not found for subscription: $query")

  val (queryId) = query
  val subHandler = getHandler(kind, queryId) as (SubHandler<Any, T>)?
    ?: throw IllegalStateException(
      "no subscription handler registered for id: `$queryId`"
    )

  return cacheReaction(queryV = query, reaction = subHandler(appDb, query))
}

// -- regSub -----------------------------------------------------------------
inline fun <I, O> regDbSubscription(
  queryId: Any,
  crossinline extractorFn: (db: I, queryVec: Query) -> O
) {
  registerHandler(
    id = queryId,
    kind = kind,
    handlerFn = { appDb: Reaction<I>, queryVec: Query ->
      Extraction(inputSignal = appDb as Reaction<Any?>) {
        extractorFn(it as I, queryVec)
      }
    }
  )
}

inline fun <I, O> regCompSubscription(
  queryId: Any,
  crossinline signalsFn: (queryVec: Query) -> IPersistentVector<Reaction<I>>,
  initialValue: O,
  crossinline computationFn: suspend (
    subscriptions: IPersistentVector<I>,
    queryVec: Query
  ) -> O
) {
  registerHandler(
    id = queryId,
    kind = kind,
    handlerFn = { _: Reaction<I>, queryVec: Query ->
      Computation(
        inputSignals = signalsFn(queryVec) as Signals,
        initial = initialValue
      ) { signalsValues ->
        computationFn(signalsValues as IPersistentVector<I>, queryVec)
      }
    }
  )
}
