package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
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
                "${get(key, 0)} got removed from cache. ${appDb.watches.count}"
            )
            reactionsCache.remove(key)
        }
    }

    reactionsCache[key] = reaction
    return reaction
}

internal fun <T> subscribe(query: IPersistentVector<Any>): Reaction<T> {
    val cacheKey = v(query, v())
    val cachedReaction = reactionsCache[cacheKey] as Reaction<T>?

    if (cachedReaction != null)
        return cachedReaction

    val queryId = (query as PersistentVector)[0]
    val handlerFn = getHandler(kind, queryId)
        as ((db: Atom<*>, qvec: IPersistentVector<Any>) -> Reaction<T>)?
        ?: throw IllegalArgumentException(
            "no subscription handler registered for id: `$queryId`"
        )

    Log.i(TAG, "No cached reaction was found for subscription `$cacheKey`")

    return cacheReaction(cacheKey, handlerFn(appDb, query))
}

// -- regSub -----------------------------------------------------------------
inline fun <T, R> regExtractor(
    queryId: Any,
    crossinline extractorFn: (db: T, queryVec: IPersistentVector<Any>) -> R,
) {
    val subsHandlerFn = { db: Atom<T>, queryVec: IPersistentVector<Any> ->
        val extractor = { appDb: T -> extractorFn(appDb, queryVec) }
        val reaction = Reaction { extractor(db()) }

        db.addWatch(reaction.id) { key, _, _, newAppDbVal ->
            val nodeOutput = extractor(newAppDbVal)
            reaction.swap { nodeOutput }
            key
        }

        reaction.addOnDispose {
            db.removeWatch(reaction.id)
        }

        reaction
    }

    registerHandler(queryId, kind, subsHandlerFn)
}

inline fun <T, R> regMaterialisedView(
    queryId: Any,
    crossinline signalsFn: (queryVec: PersistentVector<Any>) -> Reaction<T>,
    crossinline computationFn: (input: T, queryVec: PersistentVector<Any>) -> R,
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    val subsHandlerFn = { _: Atom<Any>, queryVec: PersistentVector<Any> ->
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
