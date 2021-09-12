package com.github.whyrising.recompose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.cofx.injectDb
import com.github.whyrising.recompose.events.handle
import com.github.whyrising.recompose.events.register
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.map.IPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// -- Dispatch -----------------------------------------------------------------

/* Statics:
* 1. app-db         | App lifetime
* 2. register       | App lifetime
* 3. subs-cache     | App lifetime
* 4. events-channel | App lifetime
* 5. viewModelScope | App lifetime
* 6. fx-scope       | fx  lifetime
*/

fun dispatch(event: List<Any>) {
    Recompose.viewModelScope.launch(Dispatchers.Main.immediate) {
        Recompose.eventQueue.send(event)
        Log.i(Recompose.TAG, "Event ${event[0]} queued.")
    }.invokeOnCompletion {
        Log.i(Recompose.TAG, "Event ${event[0]} dispatch completed.")
    }
}

fun dispatchSync(event: List<Any>) {
    runBlocking {
        handle(event)
    }
}

val applicationScope = CoroutineScope(SupervisorJob())

object Recompose : ViewModel() {
    const val TAG = "Recompose"
    internal val eventQueue = Channel<List<Any>>()

    init {
        // TODO: make sure to start it from the main dispatcher!
        viewModelScope.launch {
            while (true) {
                val eventVec: List<Any> = eventQueue.receive()
                when {
                    eventVec.isEmpty() -> continue
                    else -> handle(eventVec)
                }
            }
        }.invokeOnCompletion {
            Log.i(TAG, "Queue receiver completed.")
        }
    }

    override fun onCleared() {
        super.onCleared()

        Log.i(TAG, "onCleared() got called.")
        viewModelScope.cancel()
        eventQueue.close()
    }
}

fun recomposeFactory(): Recompose {
    TODO()
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
    interceptors: List<Any> = l(),
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

fun <T> regSub(
    queryId: Any,
    computationFn: (db: T, queryVec: List<Any>) -> Any,
) {
    com.github.whyrising.recompose.subs.regSub(queryId, computationFn)
}

fun <T> regSub(
    queryId: Any,
    signalsFn: (queryVec: List<Any>) -> Reaction<T>,
    computationFn: (input: T, queryVec: List<Any>) -> Any,
) {
    com.github.whyrising.recompose.subs.regSub(
        queryId,
        signalsFn,
        computationFn
    )
}

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


