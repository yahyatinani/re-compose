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

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<I, O> = (ReactiveAtom<I>, Query) -> ReactionBase<I, O>

// -- cache --------------------------------------------------------------------
internal val reactionsCache = ConcurrentHashMap<Any, Any>()

// -- subscribe ----------------------------------------------------------------
internal fun <T> cacheReaction(
    key: IPersistentVector<Any>,
    reaction: ReactionBase<Any, T>
): ReactionBase<Any, T> {
    reaction.addOnDispose { r ->
        if (r === reactionsCache[key]) reactionsCache.remove(key)
    }
    reactionsCache[key] = reaction
    return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <T> subscribe(query: Query): ReactionBase<Any, T> {
    val cacheKey = v(query, v())
    val cachedReaction = reactionsCache[cacheKey] as ReactionBase<Any, T>?

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
            ExtractorReaction(inputSignal = appDb) {
                extractorFn(it, queryVec)
            }
        }
    )
}

inline fun <I, O> regCompSubscription(
    queryId: Any,
    crossinline signalsFn: (
        queryVec: Query
    ) -> IPersistentVector<Reaction<I>>,
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
        handlerFn = { _: Reaction<I>, queryVec: Query ->
            ComputationReaction(
                inputSignals = signalsFn(queryVec),
                context = context,
                initial = initial
            ) { signalsValues ->
                computationFn(signalsValues, queryVec)
            }
        }
    )
}
