package com.github.whyrising.recompose.subs

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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

        db.addWatch(reactionId) { key, _, _, new ->
            var currentV = reaction.deref()
            val nodeOutput = fn(new)
            Log.i(TAG, "extraction of: $nodeOutput")

            // TODO: Move to Reaction, feature envy alert!!
            while (true) {
                if (nodeOutput == currentV)
                    return@addWatch key

                Log.i(TAG, "new nodeOutput: $nodeOutput")

                // TODO: Start a coroutine here maybe?
                if (reaction.state.compareAndSet(currentV, nodeOutput)) {
                    return@addWatch key
                } else currentV = reaction.deref()
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
    context: CoroutineContext = Dispatchers.Main.immediate,
) {
    val subsHandlerFn = { _: Atom<Any>, queryVec: List<Any> ->
        val subscriptions = signalsFn(queryVec)
        val fn = { input: T -> computationFn(input, queryVec) }
        val reaction = Reaction { fn(subscriptions.deref()) }

        // TODO: Move to Reaction, feature envy alert!!
        reaction.viewModelScope.launch(context) {
            subscriptions.state.collect { new ->
                Log.i(
                    "subscriptionCount",
                    "${queryVec[0]}: ${reaction.state.subscriptionCount.value}"
                )
                var currentV = reaction.deref()
                val computation = fn(new)
                while (true)
                    if (reaction.state.compareAndSet(currentV, computation)) {
                        return@collect
                    } else currentV = reaction.deref()
            }
        }

        reaction
    }

    registerHandler(queryId, kind, subsHandlerFn)
}

class Reaction<T>(val f: () -> T) : IDeref<T>, ViewModel() {
    internal val state: MutableStateFlow<T> by lazy { MutableStateFlow(f()) }

    override fun deref(): T = state.value

    operator fun invoke(): MutableStateFlow<T> = state

    companion object {
        const val TAG = "reaction"
    }
}

@Composable
fun <T> Reaction<T>.watch(
    context: CoroutineContext = EmptyCoroutineContext
): T = state.collectAsState(context = context).value
