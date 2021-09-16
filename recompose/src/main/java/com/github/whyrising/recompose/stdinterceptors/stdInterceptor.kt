package com.github.whyrising.recompose.stdinterceptors

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.coeffects
import com.github.whyrising.recompose.Keys.db
import com.github.whyrising.recompose.Keys.effects
import com.github.whyrising.recompose.Keys.event
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector

/*
-- Interceptor Factories -------------------------------------------------------

These 2 factories wrap the 2 kinds of event handlers.
*/

inline fun <T> dbHandlerToInterceptor(
    crossinline eventDbHandler: (db: T, vec: PersistentVector<Any>) -> Any
): IPersistentMap<Keys, Any> = toInterceptor(
    id = ":db-handler",
    before = { context: IPersistentMap<Keys, Any> ->
        val cofx = get(context, coeffects) as IPersistentMap<*, *>
        val oldDb = get(cofx, db) as T
        val event = get(cofx, event) as PersistentVector<Any>

        val effectsMap = (get(context, effects) ?: m<Any, Any>())
            as IPersistentMap<Keys, Any>

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
        val cofx = get(context, coeffects) as IPersistentMap<Any, Any>
        val event = get(cofx, event) as IPersistentVector<Any>

        context.assoc(effects, eventFxHandler(cofx, event))
    }
)
