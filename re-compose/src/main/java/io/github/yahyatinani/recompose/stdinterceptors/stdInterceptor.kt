@file:Suppress("UNCHECKED_CAST")

package io.github.yahyatinani.recompose.stdinterceptors

import android.util.Log
import io.github.yahyatinani.recompose.TAG
import io.github.yahyatinani.recompose.cofx.Coeffects
import io.github.yahyatinani.recompose.events.DbEventHandler
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.events.FxEventHandler
import io.github.yahyatinani.recompose.fx.Effects
import io.github.yahyatinani.recompose.ids.coeffects.event
import io.github.yahyatinani.recompose.ids.context.coeffects
import io.github.yahyatinani.recompose.ids.context.effects
import io.github.yahyatinani.recompose.ids.recompose.db
import io.github.yahyatinani.recompose.ids.recompose.notFound
import io.github.yahyatinani.recompose.interceptor.Context
import io.github.yahyatinani.recompose.interceptor.Interceptor
import io.github.yahyatinani.recompose.interceptor.toInterceptor
import io.github.yahyatinani.y.core.assocIn
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.l

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
      } else {
        Log.i(
          TAG,
          "No appDb changes in: $event, since the new appDb value is " +
            "equal to the previous."
        )
      }
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
