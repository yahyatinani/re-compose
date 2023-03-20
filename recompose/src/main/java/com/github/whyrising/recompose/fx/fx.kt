package com.github.whyrising.recompose.fx

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.ids.context.effects
import com.github.whyrising.recompose.ids.recompose
import com.github.whyrising.recompose.ids.recompose.db
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.recompose.router.dispatch
import com.github.whyrising.recompose.router.eventQueueFSM
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

typealias Effects = IPersistentMap<Any, Any?>
typealias EffectHandler = (value: Any?) -> Unit

@Suppress("EnumEntryName")
enum class BuiltInFx {
  fx,
  dispatch,
  dispatch_later,
  ms;

  override fun toString(): String = ":${super.toString()}"
}

// -- Registration -------------------------------------------------------------
val kind: Kinds = Kinds.Fx

fun regFx(id: Any, handler: EffectHandler) {
  registerHandler(id, kind, handler)
}

// -- Interceptor --------------------------------------------------------------

val doFx: Interceptor = toInterceptor(
  id = recompose.dofx,
  after = { context: Context ->
    val effects: Effects = context[effects] as Effects
    val effectsWithoutDb: Effects = effects.dissoc(db)
    val newDb = effects[db]

    if (newDb != null) {
      val updateDbFxHandler = getHandler(kind, db) as EffectHandler
      updateDbFxHandler(newDb)
    }

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

    context
  }
)

// -- Builtin Effect Handlers --------------------------------------------------

internal fun registerBuiltinFxHandlers() {
  fun dispatchLater(effect: Map<*, *>) {
    val ms = effect[BuiltInFx.ms] as? Number
    val event = effect[BuiltInFx.dispatch] as Event
    if (event.isEmpty() || ms == null) {
      throw IllegalArgumentException(
        "$TAG: bad :dispatch_later value: $effect"
      )
    }

    eventQueueFSM.scope.launch {
      delay(ms.toLong())
      dispatch(event)
    }
  }

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

  regFx(id = BuiltInFx.fx) { vecOfFx: Any? ->
    if (vecOfFx is IPersistentVector<*>) {
      val effects = vecOfFx as IPersistentVector<IPersistentVector<Any?>?>
      for (effect: IPersistentVector<Any?>? in effects) {
        // FIXME: just skip nulls, don't abort!
        if (effect == null) return@regFx

        val effectKey = effect.nth(0, null)
        val effectValue = effect.nth(1, null)

        if (effectKey == db) {
          Log.w(TAG, "\":fx\" effect should not contain a :db effect")
        }

        if (effectKey == null) {
          Log.w(TAG, "in :fx effect, null is not a valid effectKey. Skip.")
          return@regFx
        }

        val fxFn = getHandler(kind, effectKey) as EffectHandler?

        if (fxFn != null) {
          fxFn(effectValue)
        } else {
          Log.w(
            TAG,
            "in :fx, effect: $effectKey has no associated handler. Skip."
          )
        }
      }
    } else {
      val type: Class<out Any>? = when (vecOfFx) {
        null -> null
        else -> vecOfFx::class.java
      }
      Log.e(TAG, "\":fx\" effect expects a vector, but was given $type")
    }
  }

  regFx(id = BuiltInFx.dispatch) { event ->
    if (event !is IPersistentVector<*>) {
      Log.e(
        "regFx",
        "ignoring bad :dispatch value. Expected Vector, but got: $event"
      )
      return@regFx
    }

    dispatch(event as Event)
  }

  /*
   * :db
   *
   * reset appDb with a new value.
   *
   * usage:
   * {:db  {:key1 value1 key2 value2}}
   */
  regFx(id = db) { newAppDb ->
    if (newAppDb != null) {
      appDb.reset(newAppDb)
    }
  }
}
