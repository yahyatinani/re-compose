@file:Suppress("UNCHECKED_CAST")

package com.github.yahyatinani.recompose.fx

import android.util.Log
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.v
import com.github.yahyatinani.recompose.TAG
import com.github.yahyatinani.recompose.cofx.Coeffects
import com.github.yahyatinani.recompose.db.appDb
import com.github.yahyatinani.recompose.dispatchSync
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.fx.BuiltInFx.db_async
import com.github.yahyatinani.recompose.ids.coeffects.originalEvent
import com.github.yahyatinani.recompose.ids.context.coeffects
import com.github.yahyatinani.recompose.ids.context.effects
import com.github.yahyatinani.recompose.ids.recompose
import com.github.yahyatinani.recompose.ids.recompose.db
import com.github.yahyatinani.recompose.interceptor.Context
import com.github.yahyatinani.recompose.interceptor.Interceptor
import com.github.yahyatinani.recompose.interceptor.toInterceptor
import com.github.yahyatinani.recompose.registrar.Kinds
import com.github.yahyatinani.recompose.registrar.getHandler
import com.github.yahyatinani.recompose.registrar.registerHandler
import com.github.yahyatinani.recompose.router.dispatch
import com.github.yahyatinani.recompose.router.eventQueueFSM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias Effects = IPersistentMap<Any, Any?>
typealias EffectHandler = (value: Any?) -> Unit
typealias EffectHandlerAsync = suspend (value: Any?) -> Unit
typealias VecOfEffects = IPersistentVector<IPersistentVector<Any?>?>

@Suppress("EnumEntryName")
enum class BuiltInFx {
  fx,
  dispatch,
  dispatch_later,
  ms,
  db_async;

  override fun toString(): String = ":${super.toString()}"
}

// -- Registration -------------------------------------------------------------
val kind: Kinds = Kinds.Fx

fun regFx(id: Any, handler: EffectHandler) {
  registerHandler(id, kind, handler)
}

// -- Interceptor --------------------------------------------------------------

private fun execFx(effectsWithoutDb: Effects) {
  for ((effectKey, effectValue) in effectsWithoutDb) {
    val fxHandler = getHandler(kind, effectKey) as EffectHandler?
    when {
      fxHandler != null -> fxHandler(effectValue)
      else -> Log.w(
        TAG,
        "no handler registered for effect: $effectKey. Ignoring."
      )
    }
  }
}

val doFx: Interceptor = toInterceptor(
  id = recompose.dofx,
  after = { context: Context ->
    val effects: Effects = context[effects] as Effects
    val effectsWithoutDb: Effects = effects.dissoc(db)
    val newDb = effects[db]

    val cofx: Coeffects = context[coeffects] as Coeffects
    val eventVec = cofx[originalEvent] as Event
    val oldDb = cofx[db]

    if (newDb != null) { // new appDb value.
      try {
        (getHandler(kind, db) as EffectHandler)(v(eventVec, oldDb, newDb))
      } catch (e: RaceCondition) {
        Log.w(TAG, e.toString())
        return@toInterceptor context
      }
    }

    execFx(effectsWithoutDb)

    context
  },
  afterAsync = { context ->
    val effects: Effects = context[effects] as Effects
    val effectsWithoutDb: Effects = effects.dissoc(db)
    val newDb = effects[db]

    val cofx: Coeffects = context[coeffects] as Coeffects
    val eventVec = cofx[originalEvent] as Event
    val oldDb = cofx[db]

    if (newDb != null) { // new appDb value.
      try {
        val fx = getHandler(kind, db_async) as EffectHandlerAsync
        fx(v(eventVec, oldDb, newDb))
      } catch (e: RaceCondition) {
        Log.w(TAG, e.toString())
        return@toInterceptor context
      }
    }

    execFx(effectsWithoutDb)

    context
  }
)

// -- Builtin Effect Handlers --------------------------------------------------

object RaceCondition : IllegalStateException("AppDb was changed synchronously.")

internal fun registerBuiltinFxHandlers() {
  /**
   * `[BuiltInFx.dispatch]` one or more events after given delays.
   *
   * Expects a map or a vector of maps with two keys: :ms and :dispatch.
   *
   * usage:
   *   {:fx [[:dispatch_later [{:dispatch :event1 :ms 3000}
   *                           {:dispatch :event2 :ms 1000}]]]}
   *
   * `null` entries in the collection are ignored so events can be added
   * conditionally.
   */

  fun dispatchLater(effect: Map<*, *>) {
    val ms = effect[BuiltInFx.ms] as? Number
    val event = effect[BuiltInFx.dispatch] as? Event
    require(!event.isNullOrEmpty() && ms != null) {
      "$TAG: bad :dispatch_later value: $effect"
    }

    eventQueueFSM.scope.launch {
      delay(ms.toLong())
      dispatch(event)
    }
  }

  regFx(id = BuiltInFx.dispatch_later) { value ->
    when (value) {
      is Map<*, *> -> dispatchLater(value)

      is PersistentVector<*> -> value.forEach { effect ->
        if (effect != null) {
          dispatchLater(effect as Map<*, *>)
        }
      }

      else -> throw IllegalArgumentException(
        "$TAG: bad :dispatch_later value: $value"
      )
    }
  }

  /**
   * :fx
   */

  fun type(vecOfEffects: Any?) = when (vecOfEffects) {
    null -> null
    else -> vecOfEffects::class.java
  }

  regFx(id = BuiltInFx.fx) { effects: Any? ->
    if (effects !is IPersistentVector<*>) {
      Log.w(
        TAG,
        "\":fx\" effect expects a vector, but was given: ${type(effects)}"
      )
      return@regFx
    }

    (effects as VecOfEffects).forEach { effect: IPersistentVector<Any?>? ->
      effect ?: return@forEach // skip null effect

      val effectKey = effect.nth(0, null)
      val effectValue = effect.nth(1, null)
      if (effectKey == db) {
        Log.w(TAG, "\":fx\" effect should not contain a :db effect")
      }

      when (val effectFn = getHandler(kind, effectKey) as? EffectHandler) {
        null -> Log.w(
          TAG,
          "in :fx effect: `$effectKey` has no associated handler. Skip."
        )

        else -> effectFn(effectValue)
      }
    }
  }

  /**
   * :dispatch
   */
  regFx(id = BuiltInFx.dispatch) { event ->
    if (event !is IPersistentVector<*>) {
      throw IllegalArgumentException(
        "$TAG: ignoring bad :dispatch value. Expected Vector, but got: $event"
      )
    }
    dispatch(event as Event)
  }

  /**
   * :db
   *
   * reset appDb with a new value.
   *
   * usage:
   * {:db  {:key1 value1 key2 value2}}
   */
  regFx(id = db) { v ->
    val (event, o, n) = v as PersistentVector<Any>
    val current = appDb.deref()
    if (current != o || !appDb.compareAndSet(current, n)) {
      dispatchSync(event as Event)
      throw RaceCondition
    }
  }

  /**
   * :db_async
   *
   * reset appDb with a new value.
   *
   * usage:
   * {:db  {:key1 value1 key2 value2}}
   */
  val handler: suspend (value: Any?) -> Unit = { v ->
    val (event, o, n) = v as PersistentVector<Any>
    val current = appDb.deref()
    if (current != o || !withContext(Dispatchers.Main) {
      appDb.compareAndSet(current, n)
    }
    ) {
      dispatchSync(event as Event)
      throw RaceCondition
    }
  }
  registerHandler(id = db_async, kind, handler)
}
