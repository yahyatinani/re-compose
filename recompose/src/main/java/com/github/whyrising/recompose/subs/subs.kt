package com.github.whyrising.recompose.subs

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.whyrising.recompose.applicationScope
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.str
import kotlinx.coroutines.launch
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
    val cachedReaction = get(queryReaction(), cacheKey)

    if (cachedReaction != null) {
        Log.i(TAG, "cache was found for subscription `$cacheKey`")
        return cachedReaction as Reaction<T>
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
internal fun <T> regSub(
    queryId: Any,
    extractorFn: (db: T, queryVec: List<Any>) -> Any,
) {
    val subsHandlerFn = { db: Atom<T>, queryVec: List<Any> ->
        val fn = { appDb: T -> extractorFn(appDb, queryVec) }
        val reaction = Reaction { fn(db()) }

        val reactionId = str("rs", reaction.hashCode())

        db.addWatch(reactionId) { _, _, _, new ->
            val nodeOutput = fn(new)
            Log.i(TAG, "extraction of: $nodeOutput")

            val old = reaction.atom()
            if (nodeOutput != old) {
                // we launch a new coroutine only when the output of this node
                // change.
                Log.i(TAG, "new nodeOutput: $nodeOutput")
                applicationScope.launch {
                    while (true) {
                        if (reaction.atom.compareAndSet(old, nodeOutput)) {
                            Log.i("launch", "${Thread.currentThread()}")

                            for ((k, notifyWatchers) in reaction.watches())
                                notifyWatchers(k, reaction, old, nodeOutput)

                            return@launch
                        }
                    }
                }
            }
        }

        reaction
    }

    registerHandler(queryId, kind, subsHandlerFn)
}

internal fun <T> regSub(
    queryId: Any,
    signalsFn: (queryVec: List<Any>) -> Reaction<T>,
    computationFn: (input: T, queryVec: List<Any>) -> Any,
) {
    val subsHandlerFn = { _: Atom<Any>, queryVec: List<Any> ->
        val subscriptions = signalsFn(queryVec)
        val fn = { input: T -> computationFn(input, queryVec) }
        val reaction = Reaction { fn(subscriptions.deref()) }
        val reactionId = str("rx", reaction.hashCode())

        subscriptions.addWatch(reactionId) { k, _, _, new ->
            val old = reaction.atom()
            applicationScope.launch {
                val computation = fn(new)
                while (true) {
                    if (reaction.atom.compareAndSet(old, computation)) {
                        for ((key, callback) in reaction.watches())
                            callback(key, reaction, old, computation)

                        return@launch
                    }
                }
            }
            k
        }

        reaction
    }

    registerHandler(queryId, kind, subsHandlerFn)
}

class Reaction<T>(val f: () -> T) : IDeref<T> {
    internal val atom: Atom<T> by lazy { atom(f()) }
    private val state: MutableState<T> by lazy { mutableStateOf(atom()) }

    //    val watches: ConcurrentHashMap<Any, (Any, Reaction<T>, T, T) -> Any> =
    //        ConcurrentHashMap()
    internal val watches = atom(m<Any, (Any, Reaction<T>, T, T) -> Any>())

    init {
        atom.addWatch(":state-updater") { k, _, _, new ->
            if (state.value != new)
                state.value = new
            k
        }
    }

    fun addWatch(
        key: Any,
        callback: (Any, Reaction<T>, T, T) -> Any
    ): Reaction<T> {
        watches.swap {
            it.assoc(key, callback)
        }
//        watches[key] = callback
        return this
    }

    override fun deref(): T = state.value

    companion object {
        const val TAG = "reaction"
    }
}
