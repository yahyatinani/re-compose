package com.github.whyrising.recompose.stdinterceptors

import android.util.Log
import com.github.whyrising.recompose.TAG
import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.DbEventHandler
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.events.FxEventHandler
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.interceptor.Context
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.schemas.CoeffectsSchema.event
import com.github.whyrising.recompose.schemas.ContextSchema.coeffects
import com.github.whyrising.recompose.schemas.ContextSchema.effects
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.recompose.schemas.Schema.notFound
import com.github.whyrising.y.core.assocIn
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.l

// -- Interceptor Factories ----------------------------------------------------

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
  id = ":debug",
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

/**
 *  A built-in [Interceptor] factory function.
 *
 * This interceptor runs after every event handler has run.
 *
 * @param f a side effect function that takes the appDb value from [Effects],
 * if it's existing, otherwise, from [Coeffects], and the [Event] vector.
 *
 * @return the context unchanged.
 */
fun <T> after(f: (db: T, event: Event) -> Unit): Interceptor = toInterceptor(
  id = ":after",
  after = { context ->
    val coeffects = context[coeffects] as Coeffects
    val event = coeffects[event] as Event
    val effects = context[effects] as Effects
    val db = effects[db] as T? ?: coeffects[db] as T

    f(db, event)

    context
  }
)
