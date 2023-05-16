package com.github.whyrising.recompose

import android.util.Log
import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.DbEventHandler
import com.github.whyrising.recompose.events.FxEventHandler
import com.github.whyrising.recompose.ids.InterceptSpec.after
import com.github.whyrising.recompose.ids.InterceptSpec.before
import com.github.whyrising.recompose.ids.InterceptSpec.id
import com.github.whyrising.recompose.ids.coeffects
import com.github.whyrising.recompose.ids.context.effects
import com.github.whyrising.recompose.ids.recompose
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.stdinterceptors.after
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.recompose.stdinterceptors.debug
import com.github.whyrising.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic
import com.github.whyrising.recompose.ids.context as ctx

@Suppress("UNCHECKED_CAST")
class StdInterceptorTest : FreeSpec({
  mockkStatic(Log::class)
  every { Log.d(any(), any()) } returns 0
  every { Log.i(any(), any()) } returns 0

  "dbHandlerToInterceptor() should add new db value to effects in Context" {
    val coeffects: Coeffects =
      m(recompose.db to 1, coeffects.event to v("id", 5))
    val context: Context = m(ctx.coeffects to coeffects)
    val addToDbHandler: DbEventHandler<Int> = { db, event ->
      db + event[1] as Int
    }

    val interceptor: Interceptor = dbHandlerToInterceptor(addToDbHandler)
    val fn: InterceptorFn = interceptor[before] as InterceptorFn
    val newContext = fn(context)

    interceptor[id] shouldBe ":db-handler"
    newContext shouldBe m(
      com.github.whyrising.recompose.ids.context.coeffects to coeffects,
      effects to m(recompose.db to 6)
    )
  }

  "fxHandlerToInterceptor() should assoc new effects value to Context" {
    val coeffects: Coeffects =
      m(recompose.db to 1, coeffects.event to v("id", 5))
    val context: Context = m(ctx.coeffects to coeffects)
    val newEffects = m("fx-test" to 15)
    val handler: FxEventHandler = { _, _ -> newEffects }

    val interceptor: Interceptor = fxHandlerToInterceptor(handler)
    val fn: InterceptorFn = interceptor[before] as InterceptorFn
    val newContext = fn(context)

    interceptor[id] shouldBe ":fx-handler"
    newContext shouldBe m(
      ctx.coeffects to coeffects,
      effects to newEffects
    )
  }

  "debug interceptor" - {
    "before() should log the event and return the same context" {
      val event = v<Any>("id", 45)
      val context: Context =
        m(ctx.coeffects to m(coeffects.event to event))
      val before = debug[before] as InterceptorFn

      debug[id] shouldBe ":debug"
      before(context) shouldBeSameInstanceAs context
    }

    "after()" {
      val event = v<Any>("id", 45)
      val context: Context =
        m(ctx.coeffects to m(coeffects.event to event))
      val after = debug[after] as InterceptorFn

      debug[id] shouldBe ":debug"
      after(context) shouldBeSameInstanceAs context
    }
  }

  "after interceptor should run after every handler" - {
    "when effects doesn't have db, pass db value from coeffects to `f`" {
      val expectedEvent = v<Any>("id", 45)
      val expectedDbVal = 1
      var dbVal = -1
      var eventVal = v<Any>()
      val context: Context = m(
        effects to m(),
        ctx.coeffects to m(
          coeffects.event to expectedEvent,
          recompose.db to expectedDbVal
        )
      )

      val interceptor = after { db: Int, event ->
        dbVal = db
        eventVal = event as PersistentVector<Any>
      }
      val afterFn = interceptor[after] as InterceptorFn

      interceptor[id] shouldBe ":after"
      afterFn(context) shouldBeSameInstanceAs context
      dbVal shouldBe expectedDbVal
      eventVal shouldBeSameInstanceAs expectedEvent
    }

    "when effects have db, pass its value to the side effect function `f`" {
      val expectedEvent = v<Any>("id", 45)
      val expectedDbVal = 20
      var dbVal = -1
      var eventVal = v<Any>()
      val context: Context = m(
        effects to m(recompose.db to expectedDbVal),
        ctx.coeffects to m(
          coeffects.event to expectedEvent,
          recompose.db to 10
        )
      )

      val interceptor = after { db: Int, event ->
        dbVal = db
        eventVal = event as PersistentVector<Any>
      }
      val afterFn = interceptor[after] as InterceptorFn

      interceptor[id] shouldBe ":after"
      afterFn(context) shouldBeSameInstanceAs context
      dbVal shouldBe expectedDbVal
      eventVal shouldBeSameInstanceAs expectedEvent
    }
  }
})
