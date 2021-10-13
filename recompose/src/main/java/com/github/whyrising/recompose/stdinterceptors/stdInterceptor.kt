package com.github.whyrising.recompose.stdinterceptors

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.schemas.ContextSchema.coeffects
import com.github.whyrising.recompose.schemas.ContextSchema.effects
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.recompose.schemas.Schema.event
import com.github.whyrising.y.collections.core.assocIn
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.l

/*
-- Interceptor Factories -------------------------------------------------------

These 2 factories wrap 2 kinds of event handlers.
*/

typealias DbEventHandler<T> = (db: T, event: Event) -> T
typealias FxEventHandler = (cofx: Coeffects, event: Event) -> Effects

inline fun <T : Any> dbHandlerToInterceptor(
    crossinline handler: DbEventHandler<T>
) = toInterceptor(
    id = ":db-handler",
    before = { context: Context ->
        val cofx = context[coeffects] as Coeffects
        assocIn(
            context,
            l(effects, db),
            handler(cofx[db] as T, cofx[event] as Event)
        ) as Context
    }
)

inline fun fxHandlerToInterceptor(crossinline handler: FxEventHandler) =
    toInterceptor(
        id = ":fx-handler",
        before = { context ->
            val cofx = context[coeffects] as Coeffects
            val event = cofx[event] as Event
            context.assoc(effects, handler(cofx, event))
        }
    )
