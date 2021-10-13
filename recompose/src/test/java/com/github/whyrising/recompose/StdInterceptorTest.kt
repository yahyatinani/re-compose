package com.github.whyrising.recompose

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.InterceptorFn
import com.github.whyrising.recompose.schemas.ContextSchema
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.recompose.stdinterceptors.dbHandlerToInterceptor
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class StdInterceptorTest : FreeSpec({
    "dbHandlerToInterceptor() should add new db value to effects in Context" {
        val coeffects: Coeffects = m(Schema.db to 1, Schema.event to v("id", 5))
        val context: Context = m(ContextSchema.coeffects to coeffects)
        val addToDbHandler = { db: Int, event: Event ->
            db + event[1] as Int
        }

        val interceptor: Interceptor = dbHandlerToInterceptor(addToDbHandler)
        val fn: InterceptorFn = interceptor[Schema.before] as InterceptorFn
        val newContext = fn(context)

        newContext shouldBe m(
            ContextSchema.coeffects to coeffects,
            ContextSchema.effects to m(Schema.db to 6)
        )
    }
})
