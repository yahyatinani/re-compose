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

fun regCofx(id: Any, handler: (coeffects: IPersistentMap<Any, Any>) -> Any) {
    registerHandler(id, kind, handler)
}

/*
------------- Interceptor ---------------
 */
fun injectCofx(id: Any): IPersistentMap<Keys, Any> = toInterceptor(
    id = coeffects,
    before = { context ->
        val handler = getHandler(kind, id) as ((Any) -> Any)?
        if (handler != null) {
            val newCofx = handler(get(context, coeffects) ?: m<Any, Any>())
            val newContext = context.assoc(coeffects, newCofx)

            newContext
        } else {
            Log.e("injectCofx", "No cofx handler registered for $id")
            context
        }
    }
)

/*
------------ Builtin CoEffects Handlers --------------
 */

// Because this interceptor is used so much, we reify it
val injectDb = injectCofx(id = db)

/*
 Adds to coeffects the value in `appDdb`, under the key `Db`
 */
val cofx1 = regCofx(id = db) { coeffects ->
    coeffects.assoc(db, appDb)
}
