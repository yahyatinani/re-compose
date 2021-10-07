package com.github.whyrising.recompose.stdinterceptors

import com.github.whyrising.recompose.Framework
import com.github.whyrising.recompose.Framework.coeffects
import com.github.whyrising.recompose.Framework.db
import com.github.whyrising.recompose.Framework.effects
import com.github.whyrising.recompose.Framework.event
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector

/*
-- Interceptor Factories -------------------------------------------------------

These 2 factories wrap the 2 kinds of event handlers.
*/

inline fun <T> dbHandlerToInterceptor(
    crossinline eventDbHandler: (db: T, vec: IPersistentVector<Any>) -> Any
): IPersistentMap<Framework, Any> = toInterceptor(
    id = ":db-handler",
    before = { context: IPersistentMap<Framework, Any> ->
        val cofx = context[coeffects] as IPersistentMap<*, *>
        val oldDb = cofx[db] as T
        val event = cofx[event] as IPersistentVector<Any>

        val effectsMap = (context[effects] ?: m<Any, Any>())
            as IPersistentMap<Framework, Any>

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
        val cofx = context[coeffects] as IPersistentMap<Any, Any>
        val event = cofx[event] as IPersistentVector<Any>

        context.assoc(effects, eventFxHandler(cofx, event))
    }
)
