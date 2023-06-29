package com.github.yahyatinani.recompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yahyatinani.recompose.cofx.injectDb
import com.github.yahyatinani.recompose.db.appDb
import com.github.yahyatinani.recompose.events.DbEventHandler
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.events.FxEventHandler
import com.github.yahyatinani.recompose.events.register
import com.github.yahyatinani.recompose.fx.EffectHandler
import com.github.yahyatinani.recompose.fx.doFx
import com.github.yahyatinani.recompose.interceptor.Interceptor
import com.github.yahyatinani.recompose.registrar.Kinds
import com.github.yahyatinani.recompose.registrar.clearHandlers
import com.github.yahyatinani.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.yahyatinani.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.yahyatinani.recompose.subs.Computation
import com.github.yahyatinani.recompose.subs.Extraction
import com.github.yahyatinani.recompose.subs.Query
import com.github.yahyatinani.recompose.subs.Reaction
import com.github.yahyatinani.recompose.subs.ReactionBase
import com.github.yahyatinani.recompose.subs.regCompSubscription
import com.github.yahyatinani.recompose.subs.regDbSubscription
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.collections.PersistentVector
import io.github.yahyatinani.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlin.reflect.KSuspendFunction3
import kotlin.reflect.KSuspendFunction4
import kotlin.reflect.KSuspendFunction5
import kotlin.reflect.KSuspendFunction6

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
  com.github.yahyatinani.recompose.router.dispatch(event)

/**
 * This is a blocking function normally used to initialize the appDb.
 */
fun dispatchSync(event: Event) =
  com.github.yahyatinani.recompose.router.dispatchSync(event)

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

/**
 * Unregisters all currently registered event handlers via [regEventDb] or
 * [regEventFx].
 */
fun clearEvents() = clearHandlers(Kinds.Event)

/**
 * Unregisters the event handler associated with `id`. Will produce a warning
 * to console if it finds no matching registration.
 *
 * @param id The `id` of a previously registered event handler.
 */
fun clearEvent(id: Any) = clearHandlers(Kinds.Event, id)

// -- Subscriptions ------------------------------------------------------------

fun <V> subscribe(qvec: Query): Reaction<V> =
  com.github.yahyatinani.recompose.subs.subscribe(qvec)

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
 * This is just a syntactic sugar if you don't need the [Query] in the lambda.
 *
 * @param extractor This is the extraction function that should take one
 * argument which is your appDb, then you pass it like this: `::functionName`.
 */
fun <Db, V> regSub(
  queryId: Any,
  extractor: KFunction1<Db, V>
) = regDbSubscription(queryId) { db: Db, _ -> extractor(db) }

/**
 * This is just a syntactic sugar if you don't need the [Query] in the lambda
 * and you're only extracting a value from the appDb [Map].
 *
 * CAUTION: This function should be used ONLY when your appDb is a [Map].
 *
 * @param queryId a unique id for the subscription.
 * @param key The key that is used on appDb [Map] to extract the desired data.
 */
fun regSub(queryId: Any, key: Any) =
  regDbSubscription(queryId) { db: Map<*, *>, _ -> db[key] }

/**
 * @param queryId a unique id for the subscription.
 * @param initialValue is the first value for this [Reaction] so the UI can
 * render until the right value is done calculating asynchronously. If null
 * then the first computation happens synchronously on the main thread.
 * executed. It's set to [Dispatchers.Default] by default.
 * @param inputSignal a function that returns a [Reaction], by subscribing to
 * other nodes, and provides [computationFn] function with new input whenever
 * it changes.
 * @param computationFn a suspend function that obtains input data from
 * [inputSignal], and computes derived data from it. Consider using
 * [withContext] with [Dispatchers.Main] when you have computations that should
 * run on the UI so it doesn't crash.
 */
inline fun <S, V> regSub(
  queryId: Any,
  initialValue: V,
  crossinline inputSignal: (queryVec: Query) -> Reaction<S>,
  crossinline computationFn: ComputationFn1<S, V>
) = regCompSubscription(
  queryId = queryId,
  initialValue = initialValue,
  signalsFn = { queryVec -> v(inputSignal(queryVec)) },
  computationFn = { persistentVector, currentValue, qVec ->
    computationFn(persistentVector[0], currentValue, qVec)
  }
)

inline fun <S, V> regSub(
  queryId: Any,
  initialValue: V,
  inputSignal: PersistentVector<Any>,
  crossinline computationFn: ComputationFn1<S, V>
) = regCompSubscription(
  queryId = queryId,
  initialValue = initialValue,
  signalsFn = { _ -> v(subscribe(inputSignal)) },
  computationFn = { persistentVector, currentValue, qVec ->
    computationFn(persistentVector[0], currentValue, qVec)
  }
)

/**
 * This is basically the multi-version of [regSub] since it takes multiple
 * signal inputs in vector.
 *
 * @param queryId a unique id for the subscription.
 * @param initialValue is the first value for this [Reaction] so the UI can
 * render until the right value is done calculating asynchronously. If null
 * then the first computation happens synchronously on the main thread.
 * @param inputSignals a function that returns a vector of [Reaction]s,
 * by subscribing to other nodes, and provides [computationFn] function with new
 * set of input whenever it one of them changes.
 * @param computationFn a suspend function that obtains data from
 * [inputSignals], and compute derived data from it. Consider using
 * [withContext] with [Dispatchers.Main] when you have computations that should
 * run on the UI thread so it doesn't crash.
 */
@JvmName("regSubWithMultipleInputSignals")
inline fun <V> regSub(
  queryId: Any,
  initialValue: V,
  crossinline inputSignals: (qVec: Query) -> PersistentVector<Reaction<Any?>>,
  crossinline computationFn: ComputationFn2<V>
) = regCompSubscription(
  queryId = queryId,
  signalsFn = inputSignals,
  initialValue = initialValue,
  computationFn = computationFn
)

/**
 * @param inputSignals is a set of [Query] vectors that represent other
 * subscriptions to use as input signals.
 */
inline fun <V> regSub(
  queryId: Any,
  initialValue: V,
  vararg inputSignals: Query,
  crossinline computationFn: ComputationFn2<V>
) = regCompSubscription(
  queryId = queryId,
  signalsFn = { inputSignals.fold(v()) { acc, q -> acc.conj(subscribe(q)) } },
  initialValue = initialValue,
  computationFn = computationFn
)

@Suppress("UNCHECKED_CAST")
private suspend inline fun <V> com(
  arity: Int,
  f: KFunction<Any?>,
  args: IPersistentVector<Any?>
): V = when (arity) {
  1 -> if (f.isSuspend) (f as KSuspendFunction1<Any?, Any?>)(args[0])
  else (f as KFunction1<Any?, Any?>)(args[0])

  2 -> if (f.isSuspend) (f as KSuspendFunction2<Any?, Any?, Any?>)(
    args[0],
    args[1]
  )
  else (f as KFunction2<Any?, Any?, Any?>)(args[0], args[1])

  3 -> if (f.isSuspend) (f as KSuspendFunction3<Any?, Any?, Any?, Any?>)(
    args[0],
    args[1],
    args[2]
  )
  else (f as KFunction3<Any?, Any?, Any?, Any?>)(
    args[0],
    args[1],
    args[2]
  )

  4 -> if (f.isSuspend) (f as KSuspendFunction4<Any?, Any?, Any?, Any?, Any?>)(
    args[0],
    args[1],
    args[2],
    args[3]
  )
  else (f as KFunction4<Any?, Any?, Any?, Any?, Any?>)(
    args[0],
    args[1],
    args[2],
    args[3]
  )

  5 -> if (f.isSuspend) {
    (f as KSuspendFunction5<Any?, Any?, Any?, Any?, Any?, Any?>)(
      args[0],
      args[1],
      args[2],
      args[3],
      args[4]
    )
  } else (f as KFunction5<Any?, Any?, Any?, Any?, Any?, Any?>)(
    args[0],
    args[1],
    args[2],
    args[3],
    args[4]
  )

  6 -> if (f.isSuspend) {
    (f as KSuspendFunction6<Any?, Any?, Any?, Any?, Any?, Any?, Any?>)(
      args[0],
      args[1],
      args[2],
      args[3],
      args[4],
      args[5]
    )
  } else (f as KFunction6<Any?, Any?, Any?, Any?, Any?, Any?, Any?>)(
    args[0],
    args[1],
    args[2],
    args[3],
    args[4],
    args[5]
  )

  else -> TODO("Registration not supported yet for $arity inputSignals")
} as V

fun regSub(
  queryId: Any,
  initialValue: Any?,
  vararg inputSignals: Query,
  computationFn: KFunction<Any?>
) = regCompSubscription(
  queryId = queryId,
  signalsFn = { inputSignals.fold(v()) { acc, q -> acc.conj(subscribe(q)) } },
  initialValue = initialValue,
  computationFn = { subs, _, _ -> com(inputSignals.size, computationFn, subs) }
)

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
  com.github.yahyatinani.recompose.fx.regFx(id, handler)

/**
 * Unregisters all currently registered event handlers via [regFx].
 */
fun clearFx() = clearHandlers(Kinds.Fx)

/**
 * Unregisters the effect handler associated with `id`. Will produce a warning
 * to console if it finds no matching registration.
 *
 * @param id The `id` of a previously registered effect handler.
 */
fun clearFx(id: Any) = clearHandlers(Kinds.Fx, id)
