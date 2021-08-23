package com.github.whyrising.recompose.cofx

import android.util.Log
import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.coeffects
import com.github.whyrising.recompose.Keys.db
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

/**
 * @param id for the given cofx handler.
 * @param handler is a function that takes a coeffects map and returns a
 * modified one.
 */
fun regCofx(
    id: Any,
    handler: (coeffects: IPersistentMap<Any, Any>) -> IPersistentMap<Any, Any>
) {
    println("regCofx registered $id")
    registerHandler(id, kind, handler)
}

/*
------------- Interceptor ---------------
 */
fun injectCofx(id: Any): IPersistentMap<Keys, Any> = toInterceptor(
    id = coeffects,
    before = { context ->
        val injectCofxDb = getHandler(kind, id) as ((Any) -> Any)?
        if (injectCofxDb != null) {
            val cofx = get(context, coeffects) ?: m<Any, Any>()
            val newCofx = injectCofxDb(cofx)

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

/*
 Adds to coeffects the value in `appDdb`, under the key `Db`
 */
val cofxDb = regCofx(id = db) { coeffects ->
    coeffects.assoc(db, appDb)
}

// Because this interceptor is used so much, we reify it
val injectDb = injectCofx(id = db)
