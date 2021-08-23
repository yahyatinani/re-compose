package com.github.whyrising.recompose

import com.github.whyrising.recompose.Keys.db
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.db.resetAppDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class CofxTest : FreeSpec({
    "regCofx(id, handler)" {
        val coeffects = m("db" to 0)
        regCofx("id") { it }

        val cofxHandler = getHandler(Kinds.Cofx, "id") as ((Any) -> Any)

        cofxHandler(coeffects) shouldBeSameInstanceAs coeffects
    }

    "cofxDb" {
        resetAppDb(-22)
        val coeffects = m(db to -1)

        val cofxDbHandler = getHandler(Kinds.Cofx, db) as ((Any) -> Any)
        val newCofx = cofxDbHandler(coeffects) as IPersistentMap<Keys, Any>

        get(newCofx, db) shouldBe -22
    }
})
