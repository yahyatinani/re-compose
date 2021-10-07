package com.github.whyrising.recompose

import com.github.whyrising.recompose.Framework.db
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

@Suppress("UNCHECKED_CAST")
class CofxTest : FreeSpec({
    "regCofx(id, handler)" {
        val coeffects = m("db" to 0)
        regCofx("id") { it }

        val cofxHandler = getHandler(Kinds.Cofx, "id")
            as suspend ((Any) -> Any)

        cofxHandler(coeffects) shouldBeSameInstanceAs coeffects
    }

    "cofxDb" {
        appDb.state.value = -22
        val coeffects = m(db to -1)
        regCofx("id") { it }

        val cofxDbHandler = getHandler(Kinds.Cofx, db) as suspend ((Any) -> Any)
        val newCofx = cofxDbHandler(coeffects) as IPersistentMap<Recompose, Any>

        newCofx[db] shouldBe -22
    }
})
