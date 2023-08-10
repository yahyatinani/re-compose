package io.github.yahyatinani.recompose

import io.github.yahyatinani.recompose.cofx.Coeffects
import io.github.yahyatinani.recompose.cofx.CofxHandler1
import io.github.yahyatinani.recompose.cofx.CofxHandler2
import io.github.yahyatinani.recompose.cofx.injectCofx
import io.github.yahyatinani.recompose.cofx.regCofx
import io.github.yahyatinani.recompose.cofx.registerDbInjectorCofx
import io.github.yahyatinani.recompose.db.appDb
import io.github.yahyatinani.recompose.ids.InterceptSpec.after
import io.github.yahyatinani.recompose.ids.InterceptSpec.before
import io.github.yahyatinani.recompose.ids.context.coeffects
import io.github.yahyatinani.recompose.ids.recompose.db
import io.github.yahyatinani.recompose.interceptor.Context
import io.github.yahyatinani.recompose.interceptor.Interceptor
import io.github.yahyatinani.recompose.interceptor.InterceptorFn
import io.github.yahyatinani.recompose.interceptor.defaultInterceptorFn
import io.github.yahyatinani.recompose.registrar.Kinds
import io.github.yahyatinani.recompose.registrar.getHandler
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
    appDb.value = -22
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
        appDb.value = -22
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
        appDb.value = -22
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
        appDb.value = -22
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
        appDb.value = -22
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
