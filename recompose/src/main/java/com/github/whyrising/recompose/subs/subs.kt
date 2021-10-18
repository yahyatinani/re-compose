package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.vector.IPersistentVector
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

val kind: Kinds = Sub

typealias Query = IPersistentVector<Any>

typealias SubHandler<T, U> = (ReactiveAtom<T>, Query) -> Reaction<U>

// -- cache --------------------------------------------------------------------
internal val reactionsCache = ConcurrentHashMap<Any, Any>()

// -- subscribe ----------------------------------------------------------------
private fun <T> cacheReaction(
    key: IPersistentVector<Any>,
    reaction: Reaction<T>
): Reaction<T> {
    reaction.addOnDispose { r: Reaction<T> ->
        if (reactionsCache.containsKey(key) && r === reactionsCache[key])
            reactionsCache.remove(key)
    }

    reactionsCache[key] = reaction
    return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <T> subscribe(query: Query): Reaction<T> {
    val cacheKey = v(query, v())
    val cachedReaction = reactionsCache[cacheKey] as Reaction<T>?

    if (cachedReaction != null)
        return cachedReaction

    val (queryId) = query
    val subHandler = getHandler(kind, queryId) as (SubHandler<*, T>)?
        ?: throw IllegalArgumentException(
            "no subscription handler registered for id: `$queryId`"
        )

    Log.i(TAG, "No cached reaction was found for subscription `$cacheKey`")

    return cacheReaction(cacheKey, subHandler(appDb, query))
}

// -- regSub -----------------------------------------------------------------

fun <R, T> reaction(
    inputNode: ReactiveAtom<T>,
    context: CoroutineContext,
    f: (T) -> R
): Reaction<R> {
    val reaction = Reaction { f(inputNode.deref()) }
    reaction.reactTo(inputNode, context) { newInput ->
        f(newInput)
    }
    return reaction
}

inline fun <T, R> regDbExtractor(
    queryId: Any,
    crossinline extractorFn: (db: T, queryVec: Query) -> R,
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    registerHandler(
        id = queryId,
        kind = kind,
        handlerFn = { appDb: ReactiveAtom<T>, queryVec: Query ->
            reaction(appDb, context) { inputSignal: T ->
                extractorFn(inputSignal, queryVec)
            }
        }
    )
}

inline fun <T, R> regSubscription(
    queryId: Any,
    crossinline signalsFn: (
        queryVec: Query
    ) -> IPersistentVector<ReactiveAtom<T>>,
    crossinline computationFn: (
        subscriptions: IPersistentVector<T>,
        queryVec: Query
    ) -> R,
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    registerHandler(
        queryId,
        kind,
        { _: ReactiveAtom<Any>, queryVec: Query ->
            val subscriptions = signalsFn(queryVec)
            val reaction = Reaction {
                val deref = deref(subscriptions)
                computationFn(deref, queryVec)
            }
            reaction.reactTo(subscriptions, context) { newSubscriptions ->
                computationFn(newSubscriptions, queryVec)
            }
            reaction
        }
    )
}
