package com.github.whyrising.recompose

import android.util.Log
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.fx.BuiltInFx.dispatch
import com.github.whyrising.recompose.fx.BuiltInFx.fx
import com.github.whyrising.recompose.fx.EffectHandler
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.fx.kind
import com.github.whyrising.recompose.fx.regFx
import com.github.whyrising.recompose.fx.registerBuiltinFxHandlers
import com.github.whyrising.recompose.ids.context
import com.github.whyrising.recompose.ids.interceptor.after
import com.github.whyrising.recompose.ids.recompose.db
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import com.github.whyrising.recompose.registrar.register as myRegister

class FxTest : FreeSpec({
  beforeAny {
    appDb.reset(m<Any, Any>())
    myRegister.swap { m() }
  }

  "regFx(id, fxHandler)" {
    val fxHandler: (Any?) -> Unit = { }

    regFx(":fx-test", fxHandler)

    myRegister()[Kinds.Fx]!![":fx-test"] shouldBeSameInstanceAs fxHandler
  }

  "doFx interceptor should update the appDb and apply other effects" {
    regFx(id = db) { newAppDb ->
      if (newAppDb != null) {
        appDb.reset(newAppDb)
      }
    }
    var i = 0
    regFx(id = ":add-to-i") { i += (it as Int) }
    regFx(id = ":subtract-from-i") { i -= (it as Int) }
    val effects: Effects = m(
      db to 156,
      ":add-to-i" to 18,
      ":subtract-from-i" to 3
    )
    val context = m(context.effects to effects)
    val applyFx = doFx[after] as InterceptorFn

    val newContext = applyFx(context)

    newContext shouldBeSameInstanceAs context
    appDb.deref() shouldBe 156
    i shouldBeExactly 15
  }

  "`fx` effect handler should execute, in order, the vector of effects" - {
    "when passing an effect id without a value, execute the effect" {
      regFx(id = fx) { vecOfFx: Any? ->
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
      regFx(id = db) { newAppDb ->
        if (newAppDb != null) {
          appDb.reset(newAppDb)
        }
      }
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v<IPersistentVector<Any?>>(
          v(db, 185),
          v(":inc-i")
        )
      )
      val context: Context = m(context.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe 185
      i shouldBeExactly 1
    }

    """
            when passing an effect id with a value, execute the effect with
            given value
        """ {
      regFx(id = fx) { vecOfFx: Any? ->
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
      regFx(id = db) { newAppDb ->
        if (newAppDb != null) {
          appDb.reset(newAppDb)
        }
      }
      var i = 0
      regFx(id = ":inc&add-i") { i = i.inc() + it as Int }
      val effects: Effects = m(
        fx to v<IPersistentVector<Any?>>(
          v(db, 185),
          v(":inc&add-i", 4)
        )
      )
      val context: Context = m(context.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe 185
      i shouldBeExactly 5
    }

    "when passing an effect id with a null value, execute the effect" {
      regFx(id = fx) { vecOfFx: Any? ->
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
      regFx(id = db) { newAppDb ->
        if (newAppDb != null) {
          appDb.reset(newAppDb)
        }
      }
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v<IPersistentVector<Any?>>(
          v(db, 185),
          v(":inc-i", null)
        )
      )
      val context: Context = m(context.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe 185
      i shouldBeExactly 1
    }

    "when passing a vec of null effects, it should skip" {
      regFx(id = fx) { vecOfFx: Any? ->
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
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v(null)
      )
      val context: Context = m(context.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe m<Any, Any>()
      i shouldBeExactly 0
    }

    "when passing a null effect id, it should skip" {
      regFx(id = fx) { vecOfFx: Any? ->
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
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v(v(null))
      )
      val context: Context = m(context.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe m<Any, Any>()
      i shouldBeExactly 0
    }
  }

  "initBuiltinEffectHandlers()" {
    registerBuiltinFxHandlers()

    val fxFxHandler: Any? = myRegister()[Kinds.Fx]!![fx]
    val dbFxHandler: Any? = myRegister()[Kinds.Fx]!![db]
    val dispatchFxHandler: Any? = myRegister()[Kinds.Fx]!![dispatch]

    fxFxHandler.shouldNotBeNull()
    dbFxHandler.shouldNotBeNull()
    dispatchFxHandler.shouldNotBeNull()
  }
})
