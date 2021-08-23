package com.github.whyrising.recompose.fx

import android.util.Log
import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.db
import com.github.whyrising.recompose.Keys.dispatch
import com.github.whyrising.recompose.Keys.dispatchN
import com.github.whyrising.recompose.Keys.dofx
import com.github.whyrising.recompose.Keys.effects
import com.github.whyrising.recompose.Keys.fx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.db.resetAppDb
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.map.IPersistentMap

/*
-- Registration ----------------------------------------------------------------
 */
val kind: Kinds = Kinds.Fx

fun regFx(id: Any, handler: (value: Any) -> Unit) {
    registerHandler(id, kind, handler)
}

/*
-- Interceptor -----------------------------------------------------------------
 */

val doFx: IPersistentMap<Keys, Any> = toInterceptor(
    id = dofx,
    after = { context: IPersistentMap<Keys, Any> ->
        val effects = get(context, effects) as IPersistentMap<Any, Any>
        val effectsWithoutDb: IPersistentMap<Any, Any> = effects.dissoc(db)

        val newDb = get(effects, db)
        if (newDb != null) {
            val fxFn = getHandler(kind, db) as (value: Any) -> Unit
            Log.i(
                "doFx",
                "$newDb"
            )
            fxFn(newDb)
        }

        for ((effectKey, effectValue) in effectsWithoutDb) {
            val fxFn = getHandler(kind, effectKey) as ((value: Any) -> Unit)?

            when {
                fxFn != null -> fxFn(effectValue)
                else -> Log.i(
                    "re-compose",
                    "no handler registered for effect: $effectKey. Ignoring."
                )
            }
        }

        context
    }
)

/*
-- Builtin Effect Handlers ----------------------------------------------------
 */
val fx1: Unit = regFx(id = fx) { listOfEffects: Any ->
    if (listOfEffects !is List<*>) {
        val msg = "\":fx\" effect expects a list, but was given " +
            "${listOfEffects::class.java}"
        Log.e("regFx", msg)
    } else {
        val effects: List<List<Any>> = listOfEffects as List<List<Any>>

        effects.forEach { effect: List<Any> ->
            val (effectKey, effectValue) = effect

            if (effectKey == db)
                Log.w("regFx", "\":fx\" effect should not contain a :db effect")

            val fxFn = getHandler(kind, effectKey) as ((value: Any) -> Unit)?

            when {
                fxFn != null -> fxFn(effectValue)
                else -> {
                    val msg = "in :fx no handler registered for effect: " +
                        "$effectKey. Ignoring."
                    Log.i("regFx", msg)
                }
            }
        }
    }
}

val fx2: Unit = regFx(id = db) { value ->
    when {
        appDb != value -> resetAppDb(value)
        else -> Log.i("regFx", "Same appDb value")
    }
}

val fx3: Unit = regFx(id = dispatch) { value ->
    when (value) {
        is ArrayList<*> -> dispatch(value)
        else -> Log.e(
            "regFx",
            "ignoring bad :dispatch value. Expected an array list, but got: " +
                "$value"
        )
    }
}

val fx4: Unit = regFx(id = dispatchN) { value ->
    when (value) {
        is List<*> -> {
            value.forEach { vec: Any? ->
                val event = vec as ArrayList<Any>
                dispatch(event)
            }
        }
        else -> Log.e(
            "regFx",
            "ignoring bad :dispatchN value. Expected a list, but got: " +
                "$value"
        )
    }
}
