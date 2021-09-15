package com.github.whyrising.recompose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.cofx.injectDb
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.recompose.events.register
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun measureTime(action: () -> Unit) {
    val start = System.nanoTime()
    action()
    val finish = System.nanoTime()
    val timeElapsed = finish - start

    Log.i("timeElapsed", "$timeElapsed")
}

// -- Dispatch -----------------------------------------------------------------

fun dispatch(event: IPersistentVector<Any>) {
    Recompose.send(event)
}

/**
 * This is a blocking function normally used to initialize the appDb.
 */
fun dispatchSync(event: IPersistentVector<Any>) {
    runBlocking {
        handle(event)
    }
}

object Recompose : ViewModel() {
    private const val TAG = "re-compose"
    private val eventQueue = Channel<IPersistentVector<Any>>()

    init {
        startEventReceiver()
    }

    private fun startEventReceiver() {
        viewModelScope.launch {
            Log.i(TAG, "event receiver is listening...")
            while (true) {
                val eventVec: IPersistentVector<Any> = eventQueue.receive()
                when (eventVec.count) {
                    0 -> continue
                    else -> viewModelScope.launch { handle(eventVec) }
                }
            }
        }
    }

    internal fun send(event: IPersistentVector<Any>) {
        eventQueue.trySend(event)
    }
}

// -- Events ---------------------------------------------------

/**
 * Register the given event `handler` (function) for the given `id`.
 */
fun <T> regEventDb(
    id: Any,
    interceptors: IPersistentVector<Any> = v(),
    handler: (db: T, vec: PersistentVector<Any>) -> Any
) {
    register(
        id = id,
        interceptors = v(
            injectDb,
            doFx,
            interceptors,
            dbHandlerToInterceptor(handler)
        )
    )
}

fun regEventFx(
    id: Any,
    interceptors: IPersistentVector<IPersistentMap<Keys, Any>> = v(),
    handler: (
        cofx: IPersistentMap<Any, Any>,
        event: IPersistentVector<Any>
    ) -> IPersistentMap<Any, Any>
) {
    register(
        id = id,
        interceptors = v(
            injectDb,
            doFx,
            interceptors,
            fxHandlerToInterceptor(handler)
        )
    )
}

// -- Subscriptions ------------------------------------------------------------

fun <T> subscribe(qvec: IPersistentVector<Any>): Reaction<T> {
    return com.github.whyrising.recompose.subs.subscribe(qvec)
}

/**
 * @param queryId a unique id for the subscription.
 * @param extractor a function which extract data directly from [appDb], with no
 * further computation.
 */
fun <T, R> regSub(
    queryId: Any,
    extractor: (db: T, queryVec: IPersistentVector<Any>) -> R,
) = com.github.whyrising.recompose.subs.regSub(queryId, extractor)

/**
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that
 * @param computationFn a function that obtains data from [signalsFn], and
 * compute derived data from it.
 */
fun <T, R> regSub(
    queryId: Any,
    signalsFn: (queryVec: PersistentVector<Any>) -> Reaction<T>,
    computationFn: (input: T, queryVec: PersistentVector<Any>) -> R,
) = com.github.whyrising.recompose.subs.regSub(
    queryId,
    signalsFn,
    computationFn
)

// -- Effects ------------------------------------------------------------------

/**
 * Register the given effect `handler` for the given `id`
 * @param id
 * @param handler is a side-effecting function which takes a single argument
 * and whose return value is ignored.
 */
fun regFx(id: Any, handler: suspend (value: Any?) -> Unit) {
    com.github.whyrising.recompose.fx.regFx(id, handler)
}

// TODO: Move to y lib
operator fun <E> IPersistentVector<E>.component1(): E = this.nth(1)

operator fun <E> IPersistentVector<E>.component2(): E = this.nth(1)

operator fun <E> IPersistentVector<E>.component3(): E = this.nth(2)

operator fun <E> IPersistentVector<E>.get(index: Int): E =
    this.nth(index)
