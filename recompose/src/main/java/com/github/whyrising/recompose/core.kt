package com.github.whyrising.recompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.github.whyrising.recompose.subs.Computation
import com.github.whyrising.recompose.subs.Extraction
import com.github.whyrising.recompose.subs.Query
import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.recompose.subs.ReactionBase
import com.github.whyrising.recompose.subs.regCompSubscription
import com.github.whyrising.recompose.subs.regDbSubscription
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias ComputationFn1<T, V> = suspend (
  input: T,
  currentValue: V,
  queryVec: Query
) -> V

typealias ComputationFn2<T> = suspend (
  subscriptions: IPersistentVector<Any?>,
  currentValue: T,
  queryVec: Query
) -> T

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
inline fun <Db : Any> regEventDb(
  id: Any,
  interceptors: IPersistentVector<Interceptor> = v(),
  crossinline handler: DbEventHandler<Db>
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

fun <V> subscribe(qvec: Query): Reaction<V> =
  com.github.whyrising.recompose.subs.subscribe(qvec)

/**
 * @param queryId a unique id for the subscription.
 * @param extractor a function which extract data directly from [appDb], with no
 * further computation.
 */
inline fun <Db> regSub(
  queryId: Any,
  crossinline extractor: (db: Db, queryVec: Query) -> Any?
) = regDbSubscription(queryId, extractor)

/**
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that returns a [Reaction], by subscribing to
 * other nodes, and provides [computationFn] function with new input whenever
 * it changes.
 * @param initialValue is the first value for this [Reaction] so the UI can
 * render until the right value is done calculating asynchronously. If null
 * then the first computation happens synchronously on the main thread.
 * executed. It's set to [Dispatchers.Default] by default.
 * @param computationFn a suspend function that obtains input data from
 * [signalsFn], and computes derived data from it. Consider using [withContext]
 * with [Dispatchers.Main] when you have computations that should run on the UI
 * thread to avoid snapshots exceptions.
 */
inline fun <S, V> regSub(
  queryId: Any,
  crossinline signalsFn: (queryVec: Query) -> Reaction<S>,
  initialValue: V,
  crossinline computationFn: ComputationFn1<S, V>
) = regCompSubscription(
  queryId = queryId,
  initialValue = initialValue,
  signalsFn = { queryVec -> v(signalsFn(queryVec)) }
) { persistentVector, currentValue, qVec ->
  computationFn(persistentVector[0], currentValue, qVec)
}

/**
 * This is basically the multi-version of [regSub] since it takes multiple
 * signal inputs in vector.
 *
 * @param queryId a unique id for the subscription.
 * @param signalsFn a function that returns a vector of [Reaction]s,
 * by subscribing to other nodes, and provides [computationFn] function with new
 * set of input whenever it one of them changes.
 * @param initialValue is the first value for this [Reaction] so the UI can
 * render until the right value is done calculating asynchronously. If null
 * then the first computation happens synchronously on the main thread.
 * @param computationFn a suspend function that obtains data from [signalsFn],
 * and compute derived data from it. Consider using [withContext] with
 * [Dispatchers.Main] when you have computations that should run on the UI
 * thread to avoid snapshots exceptions.
 */
inline fun <V> regSubM(
  queryId: Any,
  crossinline signalsFn: (queryVec: Query) -> IPersistentVector<Reaction<Any?>>,
  initialValue: V,
  crossinline computationFn: ComputationFn2<V>
) = regCompSubscription(queryId, signalsFn, initialValue, computationFn)

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> watch(query: Query): T {
  val reaction = remember(Unit) { subscribe<T>(query) as ReactionBase<*, T> }

  DisposableEffect(Unit) {
    reaction.incUiSubCount()
    onDispose { reaction.decUiSubCount() }
  }

  return when (reaction) {
    is Extraction -> reaction.value as T
    else -> remember {
      (reaction as Computation).state
    }.collectAsStateWithLifecycle().value as T
  }
}

// -- Effects ------------------------------------------------------------------

/**
 * Register the given effect `handler` for the given `id`
 * @param id
 * @param handler is a side-effecting function which takes a single argument
 * and whose return value is ignored.
 */
fun regFx(id: Any, handler: EffectHandler) =
  com.github.whyrising.recompose.fx.regFx(id, handler)
