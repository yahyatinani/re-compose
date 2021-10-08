package com.github.whyrising.recompose.cofx

import android.util.Log
import com.github.whyrising.recompose.RKeys.coeffects
import com.github.whyrising.recompose.RKeys.db
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.interceptor.Interceptor
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Cofx
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap

//-- Registration --------------------------------------------------------------

val kind: Kinds = Cofx

typealias Coeffects = IPersistentMap<Any, Any>

typealias CofxHandler =
    suspend (coeffects: Coeffects) -> IPersistentMap<Any, Any>

/**
 * @param id for the given cofx handler.
 * @param handler is a function that takes a coeffects map and returns a
 * modified one.
 */
fun regCofx(id: Any, handler: CofxHandler) {
    registerHandler(id, kind, handler)
}

//-- Interceptor ---------------------------------------------------------------

fun injectCofx(id: Any) = toInterceptor(
    id = coeffects,
    before = { context ->
        val cofxHandler = getHandler(kind, id) as CofxHandler?

        if (cofxHandler == null) {
            Log.e("injectCofx", "No cofx handler registered for id: $id")
            return@toInterceptor context
        }

        val cofx: Coeffects = context[coeffects] as Coeffects? ?: m()
        val newCofx = cofxHandler(cofx)
        context.assoc(coeffects, newCofx)
    }
)

fun injectCofx(id: Any, value: Any): Interceptor = TODO()

//-- Builtin CoEffects Handlers ------------------------------------------------

/**
 * Register [appDb] cofx handler under the key [db]
 * It injects the [appDb] value into a coeffects map.
 */
val registerDbInjectorCofx = regCofx(id = db) { coeffects ->
    coeffects.assoc(db, appDb.deref())
}

// Because this interceptor is used so much, we reify it
val injectDb = injectCofx(db)
