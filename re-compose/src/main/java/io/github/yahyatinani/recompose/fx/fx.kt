@file:Suppress("UNCHECKED_CAST")

package io.github.yahyatinani.recompose.fx

import android.util.Log
import io.github.yahyatinani.recompose.TAG
import io.github.yahyatinani.recompose.db.appDb
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.ids.context.effects
import io.github.yahyatinani.recompose.ids.recompose
import io.github.yahyatinani.recompose.ids.recompose.db
import io.github.yahyatinani.recompose.interceptor.Context
import io.github.yahyatinani.recompose.interceptor.Interceptor
import io.github.yahyatinani.recompose.interceptor.toInterceptor
import io.github.yahyatinani.recompose.registrar.Kinds
import io.github.yahyatinani.recompose.registrar.getHandler
import io.github.yahyatinani.recompose.registrar.registerHandler
import io.github.yahyatinani.recompose.router.dispatch
import io.github.yahyatinani.recompose.router.eventQueueFSM
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.collections.PersistentVector
import io.github.yahyatinani.y.core.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

typealias Effects = IPersistentMap<Any, Any?>
typealias EffectHandler = (value: Any?) -> Unit
typealias VecOfEffects = IPersistentVector<IPersistentVector<Any?>?>

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

    if (newDb != null) { // new appDb value.
      (getHandler(kind, db) as EffectHandler)(newDb)
    }

    execFx(effectsWithoutDb)

    context
  }
)

// -- Builtin Effect Handlers --------------------------------------------------

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
  regFx(id = db) { newDb ->
    if (newDb != null) {
      appDb.value = newDb
    }
  }
}
