package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.v
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<I, O> = (ReactiveAtom<I>, Query) -> Reaction<I, O>

// -- cache --------------------------------------------------------------------
internal val reactionsCache = ConcurrentHashMap<Any, Any>()

// -- subscribe ----------------------------------------------------------------
private fun <T> cacheReaction(
    key: IPersistentVector<Any>,
    reaction: Reaction<Any, T>
): Reaction<Any, T> {
    reaction.addOnDispose { r: Reaction<Any, T> ->
        // TODO: maybe remove contains predicate
        if (reactionsCache.containsKey(key) && r === reactionsCache[key])
            reactionsCache.remove(key)
    }
    reactionsCache[key] = reaction
    return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <T> subscribe(query: Query): Reaction<Any, T> {
    val cacheKey = v(query, v())
    val cachedReaction = reactionsCache[cacheKey] as Reaction<Any, T>?

    if (cachedReaction != null)
        return cachedReaction

    val (queryId) = query
    val subHandler = getHandler(kind, queryId) as (SubHandler<Any, T>)?
        ?: throw IllegalArgumentException(
            "no subscription handler registered for id: `$queryId`"
        )

    Log.i(TAG, "No cached reaction was found for subscription `$cacheKey`")

    return cacheReaction(cacheKey, subHandler(appDb, query))
}

// -- regSub -----------------------------------------------------------------
inline fun <I, O> regDbSubscription(
    queryId: Any,
    crossinline extractorFn: (db: I, queryVec: Query) -> O
) {
    registerHandler(
        id = queryId,
        kind = kind,
        handlerFn = { appDb: ReactiveAtom<I>, queryVec: Query ->
            Reaction(
                v(appDb),
                EmptyCoroutineContext,
                null
            ) { signalsValues ->
                extractorFn(signalsValues[0], queryVec)
            }
        }
    )
}

inline fun <I, O> regCompSubscription(
    queryId: Any,
    crossinline signalsFn: (
        queryVec: Query
    ) -> IPersistentVector<ReactiveAtom<I>>,
    initial: O?,
    context: CoroutineContext,
    crossinline computationFn: (
        subscriptions: IPersistentVector<I>,
        queryVec: Query
    ) -> O
) {
    registerHandler(
        id = queryId,
        kind = kind,
        handlerFn = { _: ReactiveAtom<I>, queryVec: Query ->
            val inputSignals = signalsFn(queryVec)
            Reaction(
                inputSignals,
                context,
                initial
            ) { signalsValues ->
                computationFn(signalsValues, queryVec)
            }
        }
    )
}
