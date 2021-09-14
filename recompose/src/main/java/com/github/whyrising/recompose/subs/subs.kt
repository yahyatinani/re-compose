package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.concurrency.Atom
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

val kind: Kinds = Sub

// -- cache ---------------------------------------------------------------------
internal val reactionsCache = ConcurrentHashMap<Any, Any>()

// -- subscribe -----------------------------------------------------------------
internal const val TAG = "re-compose"

private fun <T> cacheReaction(
    key: IPersistentVector<Any>,
    reaction: Reaction<T>
): Reaction<T> {
    reaction.addOnDispose { r: Reaction<T> ->
        if (reactionsCache.containsKey(key) && r === reactionsCache[key]) {
            Log.i(
                "reactionsCache",
                "${get(key, 0)} got removed from cache."
            )
            reactionsCache.remove(key)
        }
    }

    reactionsCache[key] = reaction
    return reaction
}

internal fun <T> subscribe(qvec: List<Any>): Reaction<T> {
    val queryId = qvec[0]
    val handlerFn = getHandler(kind, queryId)
        as ((db: Atom<*>, qvec: List<Any>) -> Reaction<T>)?

    if (handlerFn == null) {
        Log.e(TAG, "no subscription handler registered for id: `$queryId`")
        throw IllegalArgumentException(
            "no subscription handler registered for id: `$queryId`"
        )
    }
    val cacheKey = v(qvec, v<Any>())
    val cachedReaction = reactionsCache[cacheKey]

    if (cachedReaction != null) {
        Log.i(TAG, "cache was found for subscription `$cacheKey`")
        return cachedReaction as Reaction<T>
    }

    Log.i(TAG, "No cache was found for subscription `$cacheKey`")
    val reaction = handlerFn(appDb, qvec)

    return cacheReaction(cacheKey, reaction)
}

// -- regSub -----------------------------------------------------------------
internal fun <T, R> regSub(
    queryId: Any,
    extractorFn: (db: T, queryVec: List<Any>) -> R,
) {
    val subsHandlerFn = { db: Atom<T>, queryVec: List<Any> ->
        val extractor = { appDb: T -> extractorFn(appDb, queryVec) }
        val reaction = Reaction { extractor(db()) }

        db.addWatch(reaction.id) { key, _, _, newAppDbVal ->
            val nodeOutput = extractor(newAppDbVal)
            reaction.swap { nodeOutput }
            key
        }

        reaction
    }

    registerHandler(queryId, kind, subsHandlerFn)
}

internal fun <T, R> regSub(
    queryId: Any,
    signalsFn: (queryVec: List<Any>) -> Reaction<T>,
    computationFn: (input: T, queryVec: List<Any>) -> R,
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    val subsHandlerFn = { _: Atom<Any>, queryVec: List<Any> ->
        val inputNode = signalsFn(queryVec)
        val materialisedView = { input: T -> computationFn(input, queryVec) }
        val reaction = Reaction { materialisedView(inputNode.deref()) }

        reaction.reactTo(inputNode, context) { newInput ->
            materialisedView(newInput)
        }

        reaction
    }

    registerHandler(queryId, kind, subsHandlerFn)
}
