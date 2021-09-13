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
import com.github.whyrising.y.concurrency.IAtom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.updateAndGet
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

class Reaction<T>(val f: () -> T) : ViewModel(), IDeref<T>, IAtom<T> {
    internal val state: MutableStateFlow<T> by lazy { MutableStateFlow(f()) }

    val id: String by lazy { str("rx", hashCode()) }

    override fun deref(): T = state.value

    override fun reset(newValue: T): T = state.updateAndGet { newValue }

    /**
     * This function use a regular comparison using [Any.equals].
     * If [f] returns a value that is equal to the current stored value in the
     * reaction, this function does not actually change the reference that is
     * stored in the [state].
     *
     * [f] may be evaluated multiple times, if [state] is being concurrently
     * updated.
     *
     * This method is **thread-safe** and can be safely invoked from concurrent
     * coroutines without external synchronization.
     */
    override fun swap(f: (currentVal: T) -> T): T = state.updateAndGet(f)

    override fun <A> swap(arg: A, f: (currentVal: T, arg: A) -> T): T {
        while (true) {
            val currentVal = state.value
            val newVal = f(currentVal, arg)

            if (state.compareAndSet(currentVal, newVal))
                return newVal
        }
    }

    override fun <A1, A2> swap(
        arg1: A1,
        arg2: A2,
        f: (currentVal: T, arg1: A1, arg2: A2) -> T
    ): T {
        while (true) {
            val currentVal = state.value
            val newVal = f(currentVal, arg1, arg2)

            if (state.compareAndSet(currentVal, newVal))
                return newVal
        }
    }
}

/**
 * This function runs the [computation] function every time the [inputNode]
 * changes.
 *
 * @param inputNode reaction which extract data directly from [appDb],
 * but do no further computation.
 * @param context for the coroutines running under [viewModelScope].
 * @param computation a function that obtains data from [inputNode], and compute
 * derived data from it.
 */
internal inline fun <T, R> Reaction<R>.reactTo(
    inputNode: Reaction<T>,
    context: CoroutineContext,
    crossinline computation: suspend (newInput: T) -> R
) {
    viewModelScope.launch(context) {
        inputNode.state.collect { newInput: T ->
            // Evaluate this only once by leaving it out of swap,
            // since swap can run f multiple times, the output is the same for
            // the same input
            val materializedView = computation(newInput)
            swap { materializedView }
        }
    }
}

@Composable
fun <T> Reaction<T>.watch(
    context: CoroutineContext = EmptyCoroutineContext
): T = state.collectAsState(context = context).value
