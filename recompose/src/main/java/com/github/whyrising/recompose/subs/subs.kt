package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.Kinds.Sub
import com.github.whyrising.recompose.registrar.getHandler
import com.github.whyrising.recompose.registrar.registerHandler
import java.util.concurrent.ConcurrentHashMap

val kind: Kinds = Sub

// -- cache ---------------------------------------------------------------------
val memSubComp = ConcurrentHashMap<Any, Any>()

// -- subscribe -----------------------------------------------------------------

internal fun <T> subscribe(qvec: List<Any>): T = qvec[0].let { id ->
    when (val r = getHandler(kind, id)) {
        null -> throw IllegalArgumentException(
            "No query function was found for the given id: `$id`"
        )
        is Array<*> -> {
            val inputFn = r[0] as (List<Any>) -> Any
            val computationFn = r[1] as (Any, List<Any>) -> Any

            // TODO: Implement input with [v1 v2] return
            val input = inputFn(qvec)
            val cache = memSubComp[input]

            if (cache == null) {
                Log.i("input", "$input")
                val computation = computationFn(input, qvec)

                memSubComp[input] = computation
                computation as T
            } else {
                Log.i("cache", "$cache")
                cache as T
            }
        }
        else -> {
            val function = r as (Any, List<Any>) -> Any
            function(appDb, qvec) as T
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
