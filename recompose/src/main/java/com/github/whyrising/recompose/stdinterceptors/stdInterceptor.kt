package com.github.whyrising.recompose.stdinterceptors

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.schemas.ContextSchema.coeffects
import com.github.whyrising.recompose.schemas.ContextSchema.effects
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.recompose.schemas.Schema.event
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector

/*
-- Interceptor Factories -------------------------------------------------------

These 2 factories wrap 2 kinds of event handlers.
*/

inline fun <T> dbHandlerToInterceptor(
    crossinline eventDbHandler: (db: T, vec: IPersistentVector<Any>) -> Any
): IPersistentMap<Schema, Any> = toInterceptor(
    id = ":db-handler",
    before = { context: Context ->
        val cofx = context[coeffects] as Coeffects
        val oldDb = cofx[db] as T
        val event = cofx[event] as IPersistentVector<Any>

        val effectsMap = (context[effects] ?: m<Any, Any>()) as Effects

        context.assoc(
            effects,
            effectsMap.assoc(db, eventDbHandler(oldDb, event))
        )
    }
)

inline fun fxHandlerToInterceptor(
    crossinline eventFxHandler: (
        cofx: IPersistentMap<Any, Any>,
        event: IPersistentVector<Any>
    ) -> IPersistentMap<Any, Any>
): Any = toInterceptor(
    id = ":fx-handler",
    before = { context ->
        val cofx = context[coeffects] as Coeffects
        val event = cofx[event] as IPersistentVector<Any>

        context.assoc(effects, eventFxHandler(cofx, event))
    }
)
