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
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.interceptor.toInterceptor
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.collections.vector.IPersistentVector

/*
-- Registration ----------------------------------------------------------------
 */
val kind: Kinds = Kinds.Fx

fun regFx(id: Any, handler: suspend (value: Any) -> Unit) {
    registerHandler(id, kind, handler)
}

/*
-- Interceptor -----------------------------------------------------------------
 */

val doFx: IPersistentMap<Keys, Any> = toInterceptor(
    id = dofx,
    after = { context: IPersistentMap<Keys, Any> ->
        val effects = context[effects] as IPersistentMap<Any, Any>
        val effectsWithoutDb: IPersistentMap<Any, Any> = effects.dissoc(db)

        val newDb = effects[db]
        if (newDb != null) {
            val dbFxHandler =
                getHandler(kind, db) as suspend (newDb: Any) -> Unit
            dbFxHandler(newDb)
        }

        for ((effectKey, effectValue) in effectsWithoutDb) {
            val fxHandler =
                getHandler(kind, effectKey) as (suspend (Any) -> Unit)?

            if (fxHandler != null)
                fxHandler(effectValue)
            else Log.i(
                "re-compose",
                "no handler registered for effect: $effectKey. Ignoring."
            )
        }

        context
    }
)

/*
-- Builtin Effect Handlers ----------------------------------------------------
 */
val executeOrderedEffectsFx: Unit = regFx(id = fx) { listOfEffects: Any ->
    if (listOfEffects !is IPersistentVector<*>) {
        Log.e(
            "regFx",
            "\":fx\" effect expects a list, but was given " +
                "${listOfEffects::class.java}"
        )

        return@regFx
    }

    val effects = listOfEffects as PersistentVector<PersistentVector<Any>>

    effects.forEach { effect: PersistentVector<Any?> ->
        val (effectKey, effectValue) = effect

        if (effectKey == db)
            Log.w("regFx", "\":fx\" effect should not contain a :db effect")

        val fxFn = getHandler(kind, effectKey!!) as (suspend (Any?) -> Unit)?

        if (fxFn != null)
            fxFn(effectValue)
        else Log.i(
            "regFx",
            "in :fx no handler registered for effect: $effectKey. Ignoring."
        )
    }
}

val updateDbFx: Unit = regFx(id = db) { newAppDb ->
    // emit() doesn't set if the newVal == the currentVal
    appDb.state.emit(newAppDb)
}

val dispatchEventFx: Unit = regFx(id = dispatch) { event ->
    if (event !is IPersistentVector<*>) {
        Log.e(
            "regFx",
            "ignoring bad :dispatch value. Expected Vector, but got: $event"
        )
        return@regFx
    }

    dispatch(event as IPersistentVector<Any>)
}

val dispatchNeventFx: Unit = regFx(id = dispatchN) { events ->
    if (events !is PersistentVector<*>) {
        Log.e(
            "regFx",
            "ignoring bad :dispatchN value. Expected a list, but got: $events"
        )
        return@regFx
    }

    events.forEach { event: Any? ->
        dispatch(event as IPersistentVector<Any>)
    }
}
