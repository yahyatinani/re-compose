package com.github.whyrising.recompose.stdinterceptors

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.schemas.ContextSchema.coeffects
import com.github.whyrising.recompose.schemas.ContextSchema.effects
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.recompose.schemas.Schema.event
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m

/*
-- Interceptor Factories -------------------------------------------------------

These 2 factories wrap 2 kinds of event handlers.
*/

inline fun <T : Any> dbHandlerToInterceptor(
    crossinline dbEventHandler: (db: T, event: Event) -> T
): Interceptor = toInterceptor(
    id = ":db-handler",
    before = { context: Context ->
        val cofx = context[coeffects] as Coeffects
        val oldDb = cofx[db] as T
        val event = cofx[event] as Event
        val effectsMap = (context[effects] ?: m<Any, Any>()) as Effects
        context.assoc(
            effects,
            effectsMap.assoc(db, dbEventHandler(oldDb, event))
        )
    }
)

inline fun fxHandlerToInterceptor(
    crossinline fxEventHandler: (cofx: Coeffects, event: Event) -> Effects
): Interceptor = toInterceptor(
    id = ":fx-handler",
    before = { context ->
        val cofx = context[coeffects] as Coeffects
        val event = cofx[event] as Event
        context.assoc(effects, fxEventHandler(cofx, event))
    }
)
