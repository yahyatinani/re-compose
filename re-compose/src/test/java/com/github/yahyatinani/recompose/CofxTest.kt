package com.github.yahyatinani.recompose

import com.github.yahyatinani.recompose.cofx.Coeffects
import com.github.yahyatinani.recompose.cofx.CofxHandler1
import com.github.yahyatinani.recompose.cofx.CofxHandler2
import com.github.yahyatinani.recompose.cofx.injectCofx
import com.github.yahyatinani.recompose.cofx.regCofx
import com.github.yahyatinani.recompose.cofx.registerDbInjectorCofx
import com.github.yahyatinani.recompose.db.appDb
import com.github.yahyatinani.recompose.ids.InterceptSpec.after
import com.github.yahyatinani.recompose.ids.InterceptSpec.before
import com.github.yahyatinani.recompose.ids.context.coeffects
import com.github.yahyatinani.recompose.ids.recompose.db
import com.github.yahyatinani.recompose.interceptor.Context
import com.github.yahyatinani.recompose.interceptor.Interceptor
import com.github.yahyatinani.recompose.interceptor.InterceptorFn
import com.github.yahyatinani.recompose.interceptor.defaultInterceptorFn
import com.github.yahyatinani.recompose.registrar.Kinds
import com.github.yahyatinani.recompose.registrar.getHandler
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m
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
    appDb.reset(-22)
    registerDbInjectorCofx()
    val coeffects: Coeffects = m(db to -1)

    val dbInjectorCofx = getHandler(Kinds.Cofx, db) as CofxHandler1
    val newCofx = dbInjectorCofx(coeffects)

    newCofx[db] shouldBe -22
  }

  "injectCofx(..)" - {
    "injectCofx(id: Any)" - {
      """
            injectCofx(id: Any) should return an Interceptor with before func
            that inject db value in coeffects.
            """ {
        appDb.reset(-22)
        registerDbInjectorCofx()
        val context: Context = m(coeffects to m(db to 10))

        val dbInjector: Interceptor = injectCofx(db)

        val beforeFn =
          dbInjector[before] as InterceptorFn
        beforeFn(context) shouldBe m(coeffects to m(db to -22))
        dbInjector[after] shouldBeSameInstanceAs defaultInterceptorFn
      }

      """
            injectCofx(id: Any) should return an Interceptor with before func
            that inject db value in coeffects, if coeffects doesn't exist in
            passed context, add one.
            """ {
        appDb.reset(-22)
        registerDbInjectorCofx()

        val dbInjector: Interceptor = injectCofx(db)

        val beforeFn =
          dbInjector[before] as InterceptorFn
        beforeFn(m()) shouldBe m(coeffects to m(db to -22))
        dbInjector[after] shouldBeSameInstanceAs defaultInterceptorFn
      }

      """
                when no cofx handler registered for `id`, return the passed
                 context
            """ {
        appDb.reset(-22)
        val context: Context = m(coeffects to m(db to 10))

        val dbInjector: Interceptor = injectCofx("non-existent-id")

        val beforeFn =
          dbInjector[before] as InterceptorFn
        beforeFn(context) shouldBeSameInstanceAs context
        dbInjector[after] shouldBeSameInstanceAs defaultInterceptorFn
      }
    }

    "injectCofx(id: Any, value: Any)" - {
      """
                when no cofx handler registered for `id`, return the passed
                context
            """ {
        appDb.reset(-22)
        val context: Context = m(coeffects to m(db to 10))

        val dbInjector: Interceptor = injectCofx("non-existent-id", 0)

        val beforeFn = dbInjector[before] as InterceptorFn
        beforeFn(context) shouldBeSameInstanceAs context
        dbInjector[after] shouldBeSameInstanceAs defaultInterceptorFn
      }

      """
                should return an Interceptor with before func that inject db
                value in coeffects.
            """ {
        val context: Context = m(coeffects to m(db to 10))
        regCofx("id") { cofx: Coeffects, value: Any ->
          val dbVal = cofx[db]!! as Int
          cofx.assoc(db, dbVal + value as Int)
        }

        val dbInjector: Interceptor = injectCofx("id", 20)

        val beforeFn = dbInjector[before] as InterceptorFn
        beforeFn(context) shouldBe m(coeffects to m(db to 30))
        dbInjector[after] shouldBeSameInstanceAs defaultInterceptorFn
      }

      """
                should return an Interceptor with before func that inject db
                value in coeffects, if coeffects doesn't exist in passed
                context, add one.
            """ {
        regCofx("id") { cofx: Coeffects, value: Any ->
          cofx.assoc(db, value as Int)
        }

        val dbInjector: Interceptor = injectCofx("id", 20)

        val beforeFn = dbInjector[before] as InterceptorFn
        beforeFn(m()) shouldBe m(coeffects to m(db to 20))
        dbInjector[after] shouldBeSameInstanceAs defaultInterceptorFn
      }
    }
  }
})
