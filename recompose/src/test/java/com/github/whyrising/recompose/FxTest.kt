package com.github.whyrising.recompose

import com.github.whyrising.recompose.db.DEFAULT_APP_DB_VALUE
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.fx.FxIds.dispatch
import com.github.whyrising.recompose.fx.FxIds.dispatchN
import com.github.whyrising.recompose.fx.FxIds.fx
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.fx.initBuiltinEffectHandlers
import com.github.whyrising.recompose.fx.regDispatchEventFxHandler
import com.github.whyrising.recompose.fx.regDispatchNeventFxHandler
import com.github.whyrising.recompose.fx.regExecuteOrderedEffectsFx
import com.github.whyrising.recompose.fx.regFx
import com.github.whyrising.recompose.fx.regUpdateDbFx
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.schemas.ContextSchema
import com.github.whyrising.recompose.schemas.InterceptorSchema.after
import com.github.whyrising.recompose.schemas.Schema.db
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
    appDb.emit(DEFAULT_APP_DB_VALUE)
    myRegister.swap { m() }
  }

  "regFx(id, fxHandler)" {
    val fxHandler: (Any?) -> Unit = { }

    regFx(":fx-test", fxHandler)

    myRegister()[Kinds.Fx]!![":fx-test"] shouldBeSameInstanceAs fxHandler
  }

  "doFx interceptor should update the appDb and apply other effects" {
    regUpdateDbFx()
    var i = 0
    regFx(id = ":add-to-i") { i += (it as Int) }
    regFx(id = ":subtract-from-i") { i -= (it as Int) }
    val effects: Effects = m(
      db to 156,
      ":add-to-i" to 18,
      ":subtract-from-i" to 3
    )
    val context = m(ContextSchema.effects to effects)
    val applyFx = doFx[after] as InterceptorFn

    val newContext = applyFx(context)

    newContext shouldBeSameInstanceAs context
    appDb.deref() shouldBe 156
    i shouldBeExactly 15
  }

  "`fx` effect handler should execute, in order, the vector of effects" - {
    "when passing an effect id without a value, execute the effect" {
      regExecuteOrderedEffectsFx()
      regUpdateDbFx()
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v<IPersistentVector<Any?>>(
          v(db, 185),
          v(":inc-i")
        )
      )
      val context: Context = m(ContextSchema.effects to effects)
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
      regExecuteOrderedEffectsFx()
      regUpdateDbFx()
      var i = 0
      regFx(id = ":inc&add-i") { i = i.inc() + it as Int }
      val effects: Effects = m(
        fx to v<IPersistentVector<Any?>>(
          v(db, 185),
          v(":inc&add-i", 4)
        )
      )
      val context: Context = m(ContextSchema.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe 185
      i shouldBeExactly 5
    }

    "when passing an effect id with a null value, execute the effect" {
      regExecuteOrderedEffectsFx()
      regUpdateDbFx()
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v<IPersistentVector<Any?>>(
          v(db, 185),
          v(":inc-i", null)
        )
      )
      val context: Context = m(ContextSchema.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe 185
      i shouldBeExactly 1
    }

    "when passing a vec of null effects, it should skip" {
      regExecuteOrderedEffectsFx()
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v(null)
      )
      val context: Context = m(ContextSchema.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe DEFAULT_APP_DB_VALUE
      i shouldBeExactly 0
    }

    "when passing a null effect id, it should skip" {
      regExecuteOrderedEffectsFx()
      var i = 0
      regFx(id = ":inc-i") { i = i.inc() }
      val effects: Effects = m(
        fx to v(v(null))
      )
      val context: Context = m(ContextSchema.effects to effects)
      val applyFx = doFx[after] as InterceptorFn

      val newContext = applyFx(context)

      newContext shouldBeSameInstanceAs context
      appDb.deref() shouldBe DEFAULT_APP_DB_VALUE
      i shouldBeExactly 0
    }
  }

  "regDispatchEventFxHandler()" {
    regDispatchEventFxHandler()

    val fxHandler: Any? = myRegister()[Kinds.Fx]!![dispatch]

    fxHandler.shouldNotBeNull()
  }

  "regDispatchNeventFxHandler()" {
    regDispatchNeventFxHandler()

    val fxHandler: Any? = myRegister()[Kinds.Fx]!![dispatchN]

    fxHandler.shouldNotBeNull()
  }

  "initBuiltinEffectHandlers()" {
    initBuiltinEffectHandlers()

    val fxFxHandler: Any? = myRegister()[Kinds.Fx]!![fx]
    val dbFxHandler: Any? = myRegister()[Kinds.Fx]!![db]
    val dispatchFxHandler: Any? = myRegister()[Kinds.Fx]!![dispatch]
    val dispatchNfxHandler: Any? = myRegister()[Kinds.Fx]!![dispatchN]

    fxFxHandler.shouldNotBeNull()
    dbFxHandler.shouldNotBeNull()
    dispatchFxHandler.shouldNotBeNull()
    dispatchNfxHandler.shouldNotBeNull()
  }
})
