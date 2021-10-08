package com.github.whyrising.recompose.cofx

import android.util.Log
import com.github.whyrising.recompose.RKeys
import com.github.whyrising.recompose.RKeys.coeffects
import com.github.whyrising.recompose.RKeys.db
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Cofx
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap

/*
---------- Registration ----------------
 */
val kind: Kinds = Cofx

typealias Coeffects = IPersistentMap<RKeys, Any>

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

/*
------------- Interceptor ---------------
 */
// TODO: Consider adding an optional second argument
fun injectCofx(id: Any) = toInterceptor(
    id = coeffects,
    before = { context ->
        val injectCofx = getHandler(kind, id) as CofxHandler?
        if (injectCofx != null) {
            val cofx: Coeffects = context[coeffects] as Coeffects? ?: m()
            val newCofx = injectCofx(cofx)

            context.assoc(coeffects, newCofx)
        } else {
            Log.e("injectCofx", "No cofx handler registered for $id")
            context
        }
    }
)

/*
------------ Builtin CoEffects Handlers --------------
 */

/**
 * Adds to coeffects the value in `appDdb`, under the key [db]
 */
val cofxDb = regCofx(id = db) { coeffects ->
    coeffects.assoc(db, appDb.deref())
}

// Because this interceptor is used so much, we reify it
val injectDb = injectCofx(id = db)
