package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

val kind: Kinds = Sub

// -- cache ---------------------------------------------------------------------
// TODO: Needs thread synchronization? and remove dead cache?
class SoftReferenceDelegate<T : Any>(
    val initialization: () -> T
) {
    private var reference: SoftReference<T>? = null

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T {
        val stored = reference?.get()
        if (stored != null)
            return stored

        val new = initialization()
        reference = SoftReference(new)
        return new
    }
}

val subsCache by SoftReferenceDelegate { ConcurrentHashMap<Any, Any>() }

// -- subscribe -----------------------------------------------------------------

internal fun <T> subscribe(qvec: List<Any>): T {
    val queryId = qvec[0]

    return when (val handlerFn = getHandler(kind, queryId)) {
        null -> throw IllegalArgumentException(
            "No query function was found for the given id: `$queryId`"
        )
        is Array<*> -> {
            val inputFn = handlerFn[0] as (List<Any>) -> Any
            val computationFn = handlerFn[1] as (Any, List<Any>) -> Any

            // TODO: Implement input with [v1 v2] return
            val input = inputFn(qvec)
            val cache = subsCache[input]

            if (cache == null) {
                Log.i("subscribe", "no cache for $input")
                val computation = computationFn(input, qvec)

                subsCache[input] = computation
                computation as T
            } else {
                Log.i("subscribe", "cache: $cache")
                cache as T
            }
        }
        else -> {
            val function = handlerFn as (Any, List<Any>) -> Any
            function(appDb(), qvec) as T
        }
    }

}

// -- regSub -----------------------------------------------------------------
// TODO: Reimplement maybe!
internal fun <T> regSub(
    queryId: Any,
    computationFn: (db: T, queryVec: ArrayList<Any>) -> Any,
) {
    registerHandler(queryId, kind, computationFn)
}

internal fun regSub(
    queryId: Any,
    inputFn: (queryVec: ArrayList<Any>) -> Any,
    computationFn: (input: Any, queryVec: ArrayList<Any>) -> Any,
) {
    registerHandler(queryId, kind, arrayOf(inputFn, computationFn))
}
