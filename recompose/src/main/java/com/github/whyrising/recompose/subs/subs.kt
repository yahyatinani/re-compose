package com.github.whyrising.recompose.subs

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.IAtom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.str
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

val kind: Kinds = Sub

// -- cache ---------------------------------------------------------------------
// TODO: Needs thread synchronization? and remove dead cache?
class SoftReferenceDelegate<T : Any>(
    val initialization: () -> T
) {
    private var reference: SoftReference<T>? = null

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T {
        val stored = reference?.get()
        if (stored != null)
            return stored

        val new = initialization()
        reference = SoftReference(new)
        return new
    }
}

val subsCache by SoftReferenceDelegate { ConcurrentHashMap<Any, Any>() }

val queryReaction = atom(m<Any, Any>())

// -- subscribe -----------------------------------------------------------------
internal const val TAG = "re-compose"

internal fun <T> subscribe(qvec: List<Any>): Reaction<T> {
    val queryId = qvec[0]
    val handlerFn = getHandler(kind, queryId)
        as ((db: Atom<*>, qvec: List<Any>) -> Reaction<Any>)?

    if (handlerFn == null) {
        Log.e(TAG, "no subscription handler registered for id: `$queryId`")
        throw IllegalArgumentException(
            "no subscription handler registered for id: `$queryId`"
        )
    }
    val cacheKey = v(qvec, v<Any>())
//    val cache = subsCache[cacheKey]
    val cached = get(queryReaction(), cacheKey)

    if (cached != null) {
        Log.i(TAG, "cache was found for subscription `$cacheKey`")
        return cached as Reaction<T>
    }

    Log.i(TAG, "No cache was found for subscription `$cacheKey`")

    val reaction = handlerFn(appDb, qvec)

    queryReaction.swap { qCache ->
        qCache.assoc(cacheKey, reaction)
    }

//    if (subsCache.contains(cacheKey) && r == subsCache[cacheKey]) {
//        Log.i(TAG, "replace cache `$cacheKey`")
//        subsCache.remove(cacheKey)
//    }

//    subsCache[cacheKey] = r

    return reaction as Reaction<T>
}

// -- regSub -----------------------------------------------------------------
// TODO: Reimplement maybe!
internal fun <T> regSub(
    queryId: Any,
    extractorFn: (db: T, queryVec: List<Any>) -> Any,
) {
    val subsHandlerFn = { db: Atom<T>, queryVec: List<Any> ->
        Reaction { extractorFn(db(), queryVec) }
    }

    registerHandler(queryId, kind, subsHandlerFn)
}

internal fun <T> regSub(
    queryId: Any,
    signalsFn: (queryVec: List<Any>) -> Reaction<T>,
    computationFn: (input: T, queryVec: List<Any>) -> Any,
) {
    val subsHandlerFn = { db: Any, queryVec: List<Any> ->
        val subscriptions = signalsFn(queryVec)

        Reaction { computationFn(subscriptions.deref(), queryVec) }
    }
    registerHandler(queryId, kind, subsHandlerFn)
}

class Reaction<T>(val f: () -> T) : IDeref<T>, IAtom<T> {
    private val state: MutableState<T> by lazy { mutableStateOf(f()) }

    init {
        // Add a watcher to app-db so the reaction can recalculate its value
        // when the app-db changes.
        // TODO: Remove the reaction watcher when reaction  is no longer
        //  being used.
        // TODO: Use a legit reaction id
        // TODO: hash reactions
        val reactionId = str("rx", hashCode())
        appDb.addWatch(reactionId) { key, atom, old, new ->
            val computation = f()
            if (state.value != computation) {
                Log.i(TAG, "$computation")
                state.value = computation
            }
            key
        }
    }

    override fun reset(newValue: T): T {
        TODO("Not yet implemented")
    }

    override fun swap(f: (currentVal: T) -> T): T {
        TODO("Not yet implemented")
    }

    override fun <A> swap(arg: A, f: (currentVal: T, arg: A) -> T): T {
        TODO("Not yet implemented")
    }

    override fun <A1, A2> swap(
        arg1: A1,
        arg2: A2,
        f: (currentVal: T, arg1: A1, arg2: A2) -> T
    ): T {
        TODO("Not yet implemented")
    }

    override fun deref(): T = state.value

    companion object {
        const val TAG = "reaction"
    }
}
