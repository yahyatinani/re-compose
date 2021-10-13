package com.github.whyrising.recompose

import android.util.Log
import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.schemas.ContextSchema
import com.github.whyrising.recompose.schemas.ContextSchema.coeffects
import com.github.whyrising.recompose.schemas.InterceptorSchema
import com.github.whyrising.recompose.schemas.InterceptorSchema.before
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.recompose.stdinterceptors.DbEventHandler
import com.github.whyrising.recompose.stdinterceptors.FxEventHandler
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.recompose.stdinterceptors.debug
import com.github.whyrising.recompose.stdinterceptors.fxHandlerToInterceptor
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic

class StdInterceptorTest : FreeSpec({
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0

    "dbHandlerToInterceptor() should add new db value to effects in Context" {
        val coeffects: Coeffects = m(Schema.db to 1, Schema.event to v("id", 5))
        val context: Context = m(ContextSchema.coeffects to coeffects)
        val addToDbHandler: DbEventHandler<Int> = { db, event ->
            db + event[1] as Int
        }

        val interceptor: Interceptor = dbHandlerToInterceptor(addToDbHandler)
        val fn: InterceptorFn = interceptor[before] as InterceptorFn
        val newContext = fn(context)

        newContext shouldBe m(
            ContextSchema.coeffects to coeffects,
            ContextSchema.effects to m(Schema.db to 6)
        )
    }

    "fxHandlerToInterceptor() should assoc new effects value to Context" {
        val coeffects: Coeffects = m(Schema.db to 1, Schema.event to v("id", 5))
        val context: Context = m(ContextSchema.coeffects to coeffects)
        val newEffects = m("fx-test" to 15)
        val handler: FxEventHandler = { _, _ -> newEffects }

        val interceptor: Interceptor = fxHandlerToInterceptor(handler)
        val fn: InterceptorFn = interceptor[before] as InterceptorFn
        val newContext = fn(context)

        newContext shouldBe m(
            ContextSchema.coeffects to coeffects,
            ContextSchema.effects to newEffects
        )
    }

    "debug interceptor" - {
        "before() should log the event and return the same context" {
            val event = v<Any>("id", 45)
            val context: Context = m(coeffects to m(Schema.event to event))
            val before = debug[before] as InterceptorFn

            before(context) shouldBeSameInstanceAs context
        }

        "after()" {
            val event = v<Any>("id", 45)
            val context: Context = m(coeffects to m(Schema.event to event))
            val after = debug[InterceptorSchema.after] as InterceptorFn

            after(context) shouldBeSameInstanceAs context
        }
    }
})
