package com.github.whyrising.recompose.fx

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.dispatch
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
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.get

typealias Effects = IPersistentMap<Any, Any?>
typealias EffectHandler = (value: Any?) -> Unit

@Suppress("EnumEntryName")
enum class FxIds {
  fx,
  dispatch;

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

/**
 * Registers an [EffectHandler] called [FxIds.fx] which is responsible for
 * executing, in the given order, every effect in the seq of effects.
 */
fun regExecuteOrderedEffectsFx() = regFx(id = FxIds.fx) { vecOfFx: Any? ->
  if (vecOfFx is IPersistentVector<*>) {
    val effects = vecOfFx as IPersistentVector<IPersistentVector<Any?>?>
    for (effect: IPersistentVector<Any?>? in effects) {
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

internal fun registerBuiltinEffectHandlers() {
  regExecuteOrderedEffectsFx()
  regFx(id = db) { newAppDb ->
    if (newAppDb != null) {
      appDb.emit(newAppDb)
    }
  }
  regFx(id = FxIds.dispatch) { event ->
    if (event !is IPersistentVector<*>) {
      Log.e(
        "regFx",
        "ignoring bad :dispatch value. Expected Vector, but got: $event"
      )
      return@regFx
    }

    dispatch(event as Event)
  }
}
