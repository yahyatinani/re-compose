package com.github.whyrising.recompose

import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.fx.doFx
import com.github.whyrising.recompose.fx.regFx
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.register
import com.github.whyrising.recompose.schemas.ContextSchema
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class FxTest : FreeSpec({
    "regFx(id, fxHandler)" {
        val fxHandler: suspend (Any?) -> Unit = { }

        regFx(":fx-test", fxHandler)

        register()[Kinds.Fx]!![":fx-test"] shouldBeSameInstanceAs fxHandler
    }

    "doFx interceptor should update the appDb and apply other effects" {
        var i = 0
        regFx(id = ":add-to-i") { i += (it as Int) }
        regFx(id = ":subtract-from-i") { i -= (it as Int) }
        val effects: Effects = m(
            Schema.db to 156,
            ":add-to-i" to 18,
            ":subtract-from-i" to 3
        )
        val context = m(ContextSchema.effects to effects)
        val applyFx = doFx[Schema.after] as InterceptorFn

        val newContext = applyFx(context)

        newContext shouldBeSameInstanceAs context
        appDb.deref() shouldBe 156
        i shouldBeExactly 15
    }
})
