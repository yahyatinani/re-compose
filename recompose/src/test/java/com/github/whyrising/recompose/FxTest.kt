package com.github.whyrising.recompose

import android.util.Log
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.fx.BuiltInFx.dispatch
import com.github.whyrising.recompose.fx.BuiltInFx.fx
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.fx.regFx
import com.github.whyrising.recompose.fx.registerBuiltinFxHandlers
import com.github.whyrising.recompose.ids.coeffects
import com.github.whyrising.recompose.ids.context
import com.github.whyrising.recompose.ids.interceptor.after
import com.github.whyrising.recompose.ids.recompose.db
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic
import com.github.whyrising.recompose.registrar.register as myRegister

@Suppress("UNCHECKED_CAST")
class FxTest : FreeSpec({
  mockkStatic(Log::class)
  every { Log.w(any(), any<String>()) } returns 0

  beforeAny {
    appDb.reset(m<Any, Any>())
    myRegister.swap { m() }
    registerBuiltinFxHandlers()
  }

  "regFx(id, fxHandler)" {
    val fxHandler: (Any?) -> Unit = { }

    regFx(":fx-test", fxHandler)

    myRegister()[Kinds.Fx]!![":fx-test"] shouldBeSameInstanceAs fxHandler
  }

  "doFx interceptor should update the appDb and apply other effects" {
    appDb.reset(0)
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
    appDb.deref() shouldBe 156
    i shouldBeExactly 15
  }

  "`fx` effect handler should execute, in order, the vector of effects" - {
    "when passing an effect id without a value, execute the effect" {
      appDb.reset(0)
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
      appDb.deref() shouldBe 185
      i shouldBeExactly 0.inc() * 4
    }

    """
      when passing an effect id with a value, execute the effect with given 
      value
    """ {
      var i = 0
      appDb.reset(0)
      regFx(id = ":inc&add-i") { i = i.inc() + it as Int }
      val effects: Effects = m(db to 185, fx to v(v(":inc&add-i", 4)))
      val context = m(
        context.effects to effects,
        context.coeffects to m(db to 0, coeffects.originalEvent to v(""))
      )
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe 185
      i shouldBeExactly 5
    }

    "when passing an effect id with a null value, execute the effect" {
      appDb.reset(0)
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
      appDb.deref() shouldBe 185
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
      appDb.deref() shouldBe m<Any, Any>()
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
