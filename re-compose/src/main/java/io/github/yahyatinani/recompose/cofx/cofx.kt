@file:Suppress("UNCHECKED_CAST")

package io.github.yahyatinani.recompose.cofx

import android.util.Log
import io.github.yahyatinani.recompose.db.appDb
import io.github.yahyatinani.recompose.ids.context.coeffects
import io.github.yahyatinani.recompose.ids.recompose.db
import io.github.yahyatinani.recompose.interceptor.Interceptor
import io.github.yahyatinani.recompose.interceptor.toInterceptor
import io.github.yahyatinani.recompose.registrar.Kinds
import io.github.yahyatinani.recompose.registrar.Kinds.Cofx
import io.github.yahyatinani.recompose.registrar.getHandler
import io.github.yahyatinani.recompose.registrar.registerHandler
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m

// -- Registration -------------------------------------------------------------

val kind: Kinds = Cofx

typealias Coeffects = IPersistentMap<Any, Any>

typealias CofxHandler1 = (coeffects: Coeffects) -> Coeffects
typealias CofxHandler2 = (coeffects: Coeffects, value: Any) -> Coeffects

/**
 * @param id for the given cofx handler.
 * @param handler is a function that takes a coeffects map and returns a
 * modified one.
 */
fun regCofx(id: Any, handler: CofxHandler1) {
  registerHandler(id, kind, handler)
}

fun regCofx(id: Any, handler: CofxHandler2) {
  registerHandler(id, kind, handler)
}

// -- Interceptor --------------------------------------------------------------

fun injectCofx(id: Any) = toInterceptor(
  id = coeffects,
  before = { context ->
    val cofxHandler = getHandler(kind, id) as CofxHandler1?

    if (cofxHandler == null) {
      Log.e("injectCofx", "No cofx handler registered for id: $id")
      return@toInterceptor context
    }

    val cofx: Coeffects = (context[coeffects] as Coeffects? ?: m()) as Coeffects
    val newCofx = cofxHandler(cofx)
    context.assoc(coeffects, newCofx)
  }
)

fun injectCofx(id: Any, value: Any): Interceptor = toInterceptor(
  id = coeffects,
  before = { context ->
    val cofxHandler = getHandler(kind, id) as CofxHandler2?

    if (cofxHandler == null) {
      Log.e("injectCofx", "No cofx handler registered for id: $id")
      return@toInterceptor context
    }

    val cofx: Coeffects = (context[coeffects] as Coeffects? ?: m()) as Coeffects
    val newCofx = cofxHandler(cofx, value)
    context.assoc(coeffects, newCofx)
  }
)

// -- Builtin CoEffects Handlers -----------------------------------------------

/**
 * Register [appDb] cofx handler under the key [db]
 * It injects the [appDb] value into a coeffects map.
 */
fun registerDbInjectorCofx() {
  regCofx(id = db) { coeffects ->
    coeffects.assoc(db, appDb.deref())
  }
}

// Because this interceptor is used so much, we reify it
val injectDb = injectCofx(db)
