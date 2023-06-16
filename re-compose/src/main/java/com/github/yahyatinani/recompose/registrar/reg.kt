package com.github.yahyatinani.recompose.registrar

import android.util.Log
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.assocIn
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.getIn
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.m
import com.github.yahyatinani.recompose.TAG

typealias Register = IPersistentMap<Any, IPersistentMap<Any, Any>?>

/**
 * This atom contains a register of all handlers.
 * It's a two layer map, keyed first by `Kinds` (of handler), and then `id` of
 * handler.
 * { Event to { id to handler },
 *  Fx to { id to handler },
 *  Cofx to { id to handler },
 *  Sub to { id to handler } }
 * Leaf nodes are handlers.
 */
internal var kindIdHandler: Atom<Register> = atom(m())

enum class Kinds { Event, Fx, Cofx, Sub }

fun getHandler(kind: Kinds, id: Any?): Any? =
  getIn(kindIdHandler(), l(kind, id))

@Suppress("UNCHECKED_CAST")
fun registerHandler(
  id: Any,
  kind: Kinds,
  handlerFn: Any
): Any {
  kindIdHandler.swap(l(kind, id), handlerFn) { currentVal, ks, v ->
    assocIn(currentVal, ks, v) as Register
  }
  return handlerFn
}

internal fun clearHandlers() {
  kindIdHandler.reset(m())
}

internal fun clearHandlers(kind: Kinds) {
  kindIdHandler.swap { it.dissoc(kind) }
}

internal fun clearHandlers(kind: Kinds, id: Any) {
  if (getHandler(kind, id) == null) {
    Log.w(TAG, "Can't clear $kind handler for $id. Handler not found.")
    return
  }

  kindIdHandler.swap {
    val kindWithoutId = it[kind]!!.dissoc(id)
    it.assoc(kind, kindWithoutId)
  }
}
