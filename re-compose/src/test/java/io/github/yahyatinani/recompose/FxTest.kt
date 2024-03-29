package io.github.yahyatinani.recompose

import android.util.Log
import io.github.yahyatinani.recompose.db.appDb
import io.github.yahyatinani.recompose.fx.BuiltInFx.dispatch
import io.github.yahyatinani.recompose.fx.BuiltInFx.fx
import io.github.yahyatinani.recompose.fx.Effects
import io.github.yahyatinani.recompose.fx.doFx
import io.github.yahyatinani.recompose.fx.regFx
import io.github.yahyatinani.recompose.fx.registerBuiltinFxHandlers
import io.github.yahyatinani.recompose.ids.InterceptSpec.after
import io.github.yahyatinani.recompose.ids.coeffects
import io.github.yahyatinani.recompose.ids.context
import io.github.yahyatinani.recompose.ids.recompose.db
import io.github.yahyatinani.recompose.interceptor.InterceptorFn
import io.github.yahyatinani.recompose.registrar.Kinds
import io.github.yahyatinani.recompose.registrar.clearHandlers
import io.github.yahyatinani.recompose.registrar.getRegistrar
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic

@Suppress("UNCHECKED_CAST")
class FxTest : FreeSpec({
  mockkStatic(Log::class)
  every { Log.w(any(), any<String>()) } returns 0

  beforeAny {
    appDb.value = m<Any, Any>()
    clearHandlers()
    registerBuiltinFxHandlers()
  }

  "regFx(id, fxHandler)" {
    val fxHandler: (Any?) -> Unit = { }

    regFx(":fx-test", fxHandler)

    getRegistrar(Kinds.Fx)[":fx-test"] shouldBeSameInstanceAs fxHandler
  }

  "doFx interceptor should update the appDb and apply other effects" {
    appDb.value = 0
    var i = 0
    regFx(id = ":add-to-i") { i += (it as Int) }
    regFx(id = ":subtract-from-i") { i -= (it as Int) }
    val effects: Effects = m(
      db to 156,
      ":add-to-i" to 18,
      ":subtract-from-i" to 3
    )
    val context = m(
      context.effects to effects,
      context.coeffects to m(db to 0, coeffects.originalEvent to v(""))
    )
    val applyFx = doFx[after] as InterceptorFn

    val newContext = applyFx(context)

    newContext shouldBeSameInstanceAs context
    appDb.value shouldBe 156
    i shouldBeExactly 15
  }

  "`fx` effect handler should execute, in order, the vector of effects" - {
    "when passing an effect id without a value, execute the effect" {
      appDb.value = 0
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      regFx(id = ":multi-i") { i *= 4 }
      val effects: Effects = m(db to 185, fx to v(v(":inc-i"), v(":multi-i")))
      val context = m(
        context.effects to effects,
        context.coeffects to m(db to 0, coeffects.originalEvent to v(":inc-i"))
      )
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.value shouldBe 185
      i shouldBeExactly 0.inc() * 4
    }

    """
      when passing an effect id with a value, execute the effect with given 
      value
    """ {
      var i = 0
      appDb.value = 0
      regFx(id = ":inc&add-i") { i = i.inc() + it as Int }
      val effects: Effects = m(db to 185, fx to v(v(":inc&add-i", 4)))
      val context = m(
        context.effects to effects,
        context.coeffects to m(db to 0, coeffects.originalEvent to v(""))
      )
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.value shouldBe 185
      i shouldBeExactly 5
    }

    "when passing an effect id with a null value, execute the effect" {
      appDb.value = 0
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(db to 185, fx to v(v(":inc-i", null)))
      val context = m(
        context.effects to effects,
        context.coeffects to m(db to 0, coeffects.originalEvent to v(""))
      )
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.value shouldBe 185
      i shouldBeExactly 1
    }

    "when passing a vec of null effects, it should skip" {
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(fx to v(null, null))
      val context = m(
        context.effects to effects,
        context.coeffects to m(db to 0, coeffects.originalEvent to v(""))
      )
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.value shouldBe m<Any, Any>()
      i shouldBeExactly 0
    }

    "when passing a null effect id, it should skip" {
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(fx to v(v(null)))
      val context = m(
        context.effects to effects,
        context.coeffects to m(db to 0, coeffects.originalEvent to v(""))
      )
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.value shouldBe m<Any, Any>()
      i shouldBeExactly 0
    }
  }

  "initBuiltinEffectHandlers()" {
    registerBuiltinFxHandlers()

    val fxFxHandler: Any? = getRegistrar(Kinds.Fx)[fx]
    val dbFxHandler: Any? = getRegistrar(Kinds.Fx)[db]
    val dispatchFxHandler: Any? = getRegistrar(Kinds.Fx)[dispatch]

    fxFxHandler.shouldNotBeNull()
    dbFxHandler.shouldNotBeNull()
    dispatchFxHandler.shouldNotBeNull()
  }
})
