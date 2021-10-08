package com.github.whyrising.recompose

import com.github.whyrising.recompose.RKeys.db
import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.CofxHandler
import com.github.whyrising.recompose.cofx.cofxDb
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

@Suppress("UNCHECKED_CAST")
class CofxTest : FreeSpec({
    "regCofx(id, handler) should register a cofx handler by id" {
        val coeffects: Coeffects = m(db to 0)

        regCofx("id") { it }

        val cofxHandler = getHandler(Kinds.Cofx, "id") as CofxHandler
        cofxHandler(coeffects) shouldBeSameInstanceAs coeffects
    }

    "when `cofxDb` loaded, it should register the appDb injector cofx" {
        cofxDb
        appDb.state.value = -22
        val coeffects: Coeffects = m(db to -1)

        val dbInjectorCofx = getHandler(Kinds.Cofx, db) as CofxHandler
        val newCofx = dbInjectorCofx(coeffects) as Coeffects

        newCofx[db] shouldBe -22
    }
})
