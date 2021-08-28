package com.github.whyrising.recompose.stdinterceptors

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.coeffects
import com.github.whyrising.recompose.Keys.db
import com.github.whyrising.recompose.Keys.effects
import com.github.whyrising.recompose.Keys.event
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap

/*
-- Interceptor Factories -----------------------------------------------------

These 2 factories wrap the 2 kinds of event handlers.

 */

fun <T> dbHandlerToInterceptor(
    handlerFn: (db: T, vec: List<Any>) -> Any
): IPersistentMap<Keys, Any> = toInterceptor(
    id = ":db-handler",
    before = { context: IPersistentMap<Keys, Any> ->
        val cofx = get(context, coeffects) as IPersistentMap<*, *>
        val oldDb = get(cofx, db)
        val event = get(cofx, event) as List<Any>

        val newDb = handlerFn(oldDb as T, event)

        val fx = (get(context, effects) ?: m<Any, Any>())
            as IPersistentMap<Keys, Any>

        val newFx = fx.assoc(db, newDb)

        context.assoc(effects, newFx)
    }
)

fun fxHandlerToInterceptor(
    handlerFn: (
        cofx: IPersistentMap<Any, Any>,
        event: List<Any>
    ) -> IPersistentMap<Any, Any>
): Any = toInterceptor(
    id = ":fx-handler",
    before = { context ->
        val cofx = get(context, coeffects) as IPersistentMap<Any, Any>
        val event = get(cofx, event) as List<Any>

        val fxData = handlerFn(cofx, event)

        val newContext = context.assoc(effects, fxData)

        newContext
    }
)
