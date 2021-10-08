package com.github.whyrising.recompose

import com.github.whyrising.recompose.RKeys.after
import com.github.whyrising.recompose.RKeys.before
import com.github.whyrising.recompose.RKeys.db
import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.CofxHandler1
import com.github.whyrising.recompose.cofx.CofxHandler2
import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.cofx.registerDbInjectorCofx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.interceptor.defaultInterceptorFn
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

@Suppress("UNCHECKED_CAST")
class CofxTest : FreeSpec({
    "regCofx(id, handler) should register a cofx of type CofxHandler1 by id" {
        val coeffects: Coeffects = m(db to 0)

        regCofx("id") { cofx: Coeffects -> cofx }

        val cofxHandler = getHandler(Kinds.Cofx, "id") as CofxHandler1
        cofxHandler(coeffects) shouldBeSameInstanceAs coeffects
    }

    "regCofx(id, handler) should register a cofx of type CofxHandler2 by id" {
        val coeffects: Coeffects = m(db to 3)

        regCofx("id") { cofx: Coeffects, value: Any ->
            val dbVal = cofx[db]!! as Int
            cofx.assoc(db, dbVal + value as Int)
        }

        val cofxHandler = getHandler(Kinds.Cofx, "id") as CofxHandler2
        cofxHandler(coeffects, 2) shouldBe m(db to 5)
    }

    "when `cofxDb` loaded, it should register the appDb injector cofx" {
        appDb.state.value = -22
        registerDbInjectorCofx
        val coeffects: Coeffects = m(db to -1)

        val dbInjectorCofx = getHandler(Kinds.Cofx, db) as CofxHandler1
        val newCofx = dbInjectorCofx(coeffects)

        newCofx[db] shouldBe -22
    }

    "injectCofx(..)" - {
        """
            injectCofx(id: Any) should return an Interceptor with before func 
            that inject db value in coeffects.
        """ {
            appDb.state.value = -22
            registerDbInjectorCofx
            val context: Context = m(RKeys.coeffects to m(db to 10))

            val dbInjectorCofx: Interceptor = injectCofx(db)

            val beforeFn = dbInjectorCofx[before] as InterceptorFn
            beforeFn(context) shouldBe m(RKeys.coeffects to m(db to -22))
            dbInjectorCofx[after] shouldBeSameInstanceAs defaultInterceptorFn
        }

        """
            injectCofx(id: Any) should return an Interceptor with before func 
            that inject db value in coeffects, if coeffects doesn't exist in 
            passed context, add one.
        """ {
            appDb.state.value = -22
            registerDbInjectorCofx

            val dbInjectorCofx: Interceptor = injectCofx(db)

            val beforeFn = dbInjectorCofx[before] as InterceptorFn
            beforeFn(m()) shouldBe m(RKeys.coeffects to m(db to -22))
            dbInjectorCofx[after] shouldBeSameInstanceAs defaultInterceptorFn
        }

        "when no cofx handler registered for `id`, return the passed context" {
            appDb.state.value = -22
            val context: Context = m(RKeys.coeffects to m(db to 10))

            val dbInjectorCofx: Interceptor = injectCofx("non-existent-id")

            val beforeFn = dbInjectorCofx[before] as InterceptorFn
            beforeFn(context) shouldBeSameInstanceAs context
            dbInjectorCofx[after] shouldBeSameInstanceAs defaultInterceptorFn
        }
    }
})
