package com.github.whyrising.recompose

import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.register
import com.github.whyrising.y.collections.core.get
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs

class FxTest : FreeSpec({
    "regFx(id, fxHandler)" {
        val fxHandler: suspend (Any?) -> Unit = { }

        regFx(":fx-test", fxHandler)

        register()[Kinds.Fx]!![":fx-test"] shouldBeSameInstanceAs fxHandler
    }
})
