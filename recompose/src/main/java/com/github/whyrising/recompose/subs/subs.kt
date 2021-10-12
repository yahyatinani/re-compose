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

// -- cache --------------------------------------------------------------------
internal val reactionsCache = ConcurrentHashMap<Any, Any>()

// -- subscribe ----------------------------------------------------------------
private fun <T> cacheReaction(
    key: IPersistentVector<Any>,
    reaction: Reaction<T>
): Reaction<T> {
    reaction.addOnDispose { r: Reaction<T> ->
        if (reactionsCache.containsKey(key) && r === reactionsCache[key]) {
            reactionsCache.remove(key)
            val value = appDb.state.subscriptionCount.value
            Log.i(
                "reactionsCache",
                "${key[0]} got removed from cache. $value"
            )
        }
    }

    reactionsCache[key] = reaction
    return reaction
}

@Suppress("UNCHECKED_CAST")
internal fun <T> subscribe(query: IPersistentVector<Any>): Reaction<T> {
    val cacheKey = v(query, v())
    val cachedReaction = reactionsCache[cacheKey] as Reaction<T>?

    if (cachedReaction != null)
        return cachedReaction

    val queryId = query[0]
    val handlerFn = getHandler(kind, queryId)
        as ((React<*>, IPersistentVector<Any>) -> Reaction<T>)?
        ?: throw IllegalArgumentException(
            "no subscription handler registered for id: `$queryId`"
        )

    Log.i(TAG, "No cached reaction was found for subscription `$cacheKey`")

    return cacheReaction(cacheKey, handlerFn(appDb, query))
}

// -- regSub -----------------------------------------------------------------

fun <R, T> reaction(
    inputNode: React<T>,
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
    crossinline extractorFn: (db: T, queryVec: IPersistentVector<Any>) -> R,
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    registerHandler(
        queryId,
        kind,
        { appDb: React<T>, queryVec: IPersistentVector<Any> ->
            reaction(appDb, context) { inputSignal: T ->
                extractorFn(inputSignal, queryVec)
            }
        }
    )
}

inline fun <T, R> regSubscription(
    queryId: Any,
    crossinline signalsFn: (
        queryVec: IPersistentVector<Any>
    ) -> IPersistentVector<React<T>>,
    crossinline computationFn: (
        subscriptions: IPersistentVector<T>,
        queryVec: IPersistentVector<Any>
    ) -> R,
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    registerHandler(
        queryId,
        kind,
        { _: React<Any>, queryVec: IPersistentVector<Any> ->
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
