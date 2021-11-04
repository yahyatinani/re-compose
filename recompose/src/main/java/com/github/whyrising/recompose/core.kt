package com.github.whyrising.recompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.github.whyrising.recompose.cofx.injectDb
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.events.DbEventHandler
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.FxEventHandler
import com.github.whyrising.recompose.events.register
import com.github.whyrising.recompose.fx.EffectHandler
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.whyrising.recompose.subs.Query
import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.recompose.subs.ReactiveAtom
import com.github.whyrising.recompose.subs.regDbExtractor
import com.github.whyrising.recompose.subs.regSubscription
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.vector.IPersistentVector
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal const val TAG = "re-compose"

// -- Dispatch -----------------------------------------------------------------

fun dispatch(event: Event) =
    com.github.whyrising.recompose.router.dispatch(event)

/**
 * This is a blocking function normally used to initialize the appDb.
 */
fun dispatchSync(event: Event) {
    com.github.whyrising.recompose.router.dispatchSync(event)
}

// -- Events -------------------------------------------------------------------

/**
 * Register the given event `handler` (function) for the given `id`.
 */
inline fun <T : Any> regEventDb(
    id: Any,
    interceptors: IPersistentVector<Interceptor> = v(),
    crossinline handler: DbEventHandler<T>
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

inline fun regEventFx(
    id: Any,
    interceptors: IPersistentVector<Interceptor> = v(),
    crossinline handler: FxEventHandler
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
inline fun <T, R> regSub(
    queryId: Any,
    crossinline extractor: (db: T, queryVec: Query) -> R,
) = regDbExtractor(queryId, extractor)

/**
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that returns a Reaction by subscribing to other
 * nodes.
 * @param computationFn a function that obtains data from [signalsFn], and
 * compute derived data from it.
 */
inline fun <T, R> regSub(
    queryId: Any,
    crossinline signalsFn: (queryVec: Query) -> ReactiveAtom<T>,
    crossinline computationFn: (input: T, queryVec: Query) -> R,
) = regSubscription(
    queryId,
    { queryVec -> v(signalsFn(queryVec)) },
    { persistentVector, qVec -> computationFn(persistentVector[0], qVec) }
)

/**
 * This is basically the multi-version of [regSub] since it takes multiple
 * signal inputs in vector.
 *
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that returns a vector of Reactions by subscribing
 * to other nodes.
 * @param computationFn a function that obtains data from [signalsFn], and
 * compute derived data from it.
 */
inline fun <R> regSubM(
    queryId: Any,
    crossinline signalsFn: (
        queryVec: Query
    ) -> IPersistentVector<ReactiveAtom<Any>>,
    crossinline computationFn: (
        subscriptions: IPersistentVector<Any>,
        queryVec: Query,
    ) -> R,
) = regSubscription(queryId, signalsFn, computationFn)

/**
 * Collects values from this Reaction and represents its latest value.
 *
 * @param context CoroutineContext to use for collecting.
 *
 * @return the current value of this reaction. Every time there would be
 * new value posted into the Reaction it's going to cause a recomposition.
 */
@Composable
fun <T> Reaction<T>.w(
    context: CoroutineContext = EmptyCoroutineContext
): T = state.collectAsState(context = context).value

// -- Effects ------------------------------------------------------------------

/**
 * Register the given effect `handler` for the given `id`
 * @param id
 * @param handler is a side-effecting function which takes a single argument
 * and whose return value is ignored.
 */
fun regFx(id: Any, handler: EffectHandler) {
    com.github.whyrising.recompose.fx.regFx(id, handler)
}
