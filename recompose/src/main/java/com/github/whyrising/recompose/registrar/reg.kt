package com.github.whyrising.recompose.registrar

import com.github.whyrising.y.collections.core.assocIn
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom

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
internal var register: Atom<IPersistentMap<Any, Any>> = atom(m())

enum class Kinds { Event, Fx, Cofx, Sub }

fun getHandler(kind: Kinds, id: Any): Any? = register().valAt(kind).let {
    (it as IPersistentMap<Any, Any>?)?.valAt(id)
}

internal fun registerHandler(
    id: Any,
    kind: Kinds,
    handlerFn: Any
): Any {
    register.swap(l(kind, id), handlerFn) { currentVal, ks, v ->
        assocIn(currentVal, ks, v) as IPersistentMap<Any, Any>
    }
    return handlerFn
}
