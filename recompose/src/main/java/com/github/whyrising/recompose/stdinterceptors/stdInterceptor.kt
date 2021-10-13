package com.github.whyrising.recompose.stdinterceptors

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.schemas.ContextSchema.coeffects
import com.github.whyrising.recompose.schemas.ContextSchema.effects
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.recompose.schemas.Schema.event
import com.github.whyrising.recompose.schemas.Schema.notFound
import com.github.whyrising.y.collections.core.assocIn
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.l

// -- Interceptor Factories ----------------------------------------------------

typealias DbEventHandler<T> = (db: T, event: Event) -> T
typealias FxEventHandler = (cofx: Coeffects, event: Event) -> Effects

// These 2 factories wrap 2 kinds of event handlers.

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

// -- Built-in Interceptors ----------------------------------------------------

val debug = toInterceptor(
    ":debug",
    before = {
        Log.d(TAG, "Handling event: ${(it[coeffects] as Coeffects)[event]}")
        it
    },
    after = { context ->
        val cofx = context[coeffects] as Coeffects
        val fx = context[effects] as Effects?
        val event = cofx[event]
        val oldDb = cofx[db]

        when (val newDb = if (fx == null) notFound else fx[db] ?: notFound) {
            notFound -> Log.i(TAG, "No appDb changes in: $event")
            else -> if (oldDb != newDb) {
                Log.i(TAG, "db for: $event")
                Log.i(TAG, "appDb before: $oldDb")
                Log.i(TAG, "appDb after: $newDb")
            } else Log.i(
                TAG,
                "No appDb changes in: $event, since the new appDb value is " +
                    "equal to the previous."
            )
        }
        context
    }
)
