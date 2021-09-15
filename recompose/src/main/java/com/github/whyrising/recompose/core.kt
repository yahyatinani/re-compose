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
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.map.IPersistentMap
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

fun dispatch(event: List<Any>) {
    Recompose.send(event)
}

/**
 * This is a blocking function normally used to initialize the appDb.
 */
fun dispatchSync(event: List<Any>) {
    runBlocking {
        handle(event)
    }
}

object Recompose : ViewModel() {
    private const val TAG = "re-compose"
    private val eventQueue = Channel<List<Any>>()

    init {
        startEventReceiver()
    }

    private fun startEventReceiver() {
        viewModelScope.launch {
            Log.i(TAG, "event receiver is listening...")
            while (true) {
                val eventVec: List<Any> = eventQueue.receive()
                when {
                    eventVec.isEmpty() -> continue
                    else -> viewModelScope.launch { handle(eventVec) }
                }
            }
        }
    }

    internal fun send(event: List<Any>) {
        eventQueue.trySend(event)
    }
}

// -- Events ---------------------------------------------------

/**
 * Register the given event `handler` (function) for the given `id`.
 */
fun <T> regEventDb(
    id: Any,
    interceptors: List<Any> = l(),
    handler: (db: T, vec: List<Any>) -> Any
) {
    register(
        id = id,
        interceptors = l(
            injectDb,
            doFx,
            interceptors,
            dbHandlerToInterceptor(handler)
        )
    )
}

fun regEventFx(
    id: Any,
    interceptors: List<IPersistentMap<Keys, Any>> = l(),
    handler: (
        cofx: IPersistentMap<Any, Any>,
        event: List<Any>
    ) -> IPersistentMap<Any, Any>
) {
    register(
        id = id,
        interceptors = l(
            injectDb,
            doFx,
            interceptors,
            fxHandlerToInterceptor(handler)
        )
    )
}

// -- Subscriptions ------------------------------------------------------------

fun <T> subscribe(qvec: List<Any>): Reaction<T> {
    return com.github.whyrising.recompose.subs.subscribe(qvec)
}

/**
 * @param queryId a unique id for the subscription.
 * @param extractor a function which extract data directly from [appDb], with no
 * further computation.
 */
fun <T, R> regSub(
    queryId: Any,
    extractor: (db: T, queryVec: List<Any>) -> R,
) = com.github.whyrising.recompose.subs.regSub(queryId, extractor)

/**
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that
 * @param computationFn a function that obtains data from [signalsFn], and
 * compute derived data from it.
 */
fun <T, R> regSub(
    queryId: Any,
    signalsFn: (queryVec: List<Any>) -> Reaction<T>,
    computationFn: (input: T, queryVec: List<Any>) -> R,
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

// -- Framework ----------------------------------------------------------------
