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
import com.github.whyrising.recompose.subs.ReactionBase
import com.github.whyrising.recompose.subs.ReactiveAtom
import com.github.whyrising.recompose.subs.regCompSubscription
import com.github.whyrising.recompose.subs.regDbSubscription
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias ComputationFn1<T, V> = suspend (input: T, queryVec: Query) -> V

typealias ComputationFn2<T> =
  suspend (subscriptions: IPersistentVector<Any?>, queryVec: Query) -> T

internal const val TAG = "re-compose"

// -- Dispatch -----------------------------------------------------------------

fun dispatch(event: Event) =
  com.github.whyrising.recompose.router.dispatch(event)

/**
 * This is a blocking function normally used to initialize the appDb.
 */
fun dispatchSync(event: Event) =
  com.github.whyrising.recompose.router.dispatchSync(event)

// -- Events -------------------------------------------------------------------

/**
 * Register the given event `handler` (function) for the given `id`.
 */
inline fun <T : Any> regEventDb(
  id: Any,
  interceptors: IPersistentVector<Interceptor> = v(),
  crossinline handler: DbEventHandler<T>
) = register(
  id = id,
  interceptors = v(
    injectDb,
    doFx,
    interceptors,
    dbHandlerToInterceptor(handler)
  )
)

inline fun regEventFx(
  id: Any,
  interceptors: IPersistentVector<Interceptor> = v(),
  crossinline handler: FxEventHandler
) = register(
  id = id,
  interceptors = v(
    injectDb,
    doFx,
    interceptors,
    fxHandlerToInterceptor(handler)
  )
)

// -- Subscriptions ------------------------------------------------------------

fun <T> subscribe(qvec: IPersistentVector<Any>): ReactionBase<Any, T> =
  com.github.whyrising.recompose.subs.subscribe(qvec)

/**
 * @param queryId a unique id for the subscription.
 * @param extractor a function which extract data directly from [appDb], with no
 * further computation.
 */
inline fun <T, R> regSub(
  queryId: Any,
  crossinline extractor: (db: T, queryVec: Query) -> R
) = regDbSubscription(queryId, extractor)

/**
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that returns a [ReactiveAtom], by subscribing to
 * other nodes, and provides [computationFn] function with new input whenever
 * it changes.
 * @param initial is the first value for this [Reaction] so the UI can
 * render until the right value is done calculating asynchronously. If null
 * then the first computation happens synchronously on the main thread.
 * @param context on which the first value calculation/initialization will be
 * executed. It's set to [Dispatchers.Default] by default.
 * @param computationFn a suspend function that obtains input data from
 * [signalsFn], and computes derived data from it. Consider using [withContext]
 * with [Dispatchers.Main] when you have computations that should run on the UI
 * thread to avoid snapshots exceptions.
 */
inline fun <T, R> regSub(
  queryId: Any,
  initial: R? = null,
  context: CoroutineContext = Dispatchers.Default,
  crossinline signalsFn: (queryVec: Query) -> Reaction<T>,
  crossinline computationFn: ComputationFn1<T, R>
) = regCompSubscription(
  queryId = queryId,
  signalsFn = { queryVec -> v(signalsFn(queryVec)) },
  initial = initial,
  context = context
) { persistentVector, qVec ->
  computationFn(persistentVector[0], qVec)
}

/**
 * This is basically the multi-version of [regSub] since it takes multiple
 * signal inputs in vector.
 *
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that returns a vector of [ReactiveAtom]s,
 * by subscribing to other nodes, and provides [computationFn] function with new
 * set of input whenever it one of them changes.
 * @param initial is the first value for this [Reaction] so the UI can
 * render until the right value is done calculating asynchronously. If null
 * then the first computation happens synchronously on the main thread.
 * @param context on which the first value calculation/initialization will be
 * executed. It's set to [Dispatchers.Default] by default.
 * @param computationFn a suspend function that obtains data from [signalsFn],
 * and compute derived data from it. Consider using [withContext] with
 * [Dispatchers.Main] when you have computations that should run on the UI
 * thread to avoid snapshots exceptions.
 */
inline fun <R> regSubM(
  queryId: Any,
  initial: R? = null,
  context: CoroutineContext = Dispatchers.Default,
  crossinline signalsFn: (queryVec: Query) -> IPersistentVector<Reaction<Any?>>,
  crossinline computationFn: ComputationFn2<R>
) = regCompSubscription(queryId, signalsFn, initial, context, computationFn)

/**
 * Collects values from this Reaction and represents its latest value.
 *
 * @param context CoroutineContext to use for collecting.
 *
 * @return the current value of this reaction. Every time there would be
 * new value posted into the Reaction it's going to cause a recomposition.
 */
@Composable
fun <T> ReactionBase<Any, T>.w(
  context: CoroutineContext = EmptyCoroutineContext
): T = deref(state.collectAsState(context = context))

// -- Effects ------------------------------------------------------------------

/**
 * Register the given effect `handler` for the given `id`
 * @param id
 * @param handler is a side-effecting function which takes a single argument
 * and whose return value is ignored.
 */
fun regFx(id: Any, handler: EffectHandler) =
  com.github.whyrising.recompose.fx.regFx(id, handler)
