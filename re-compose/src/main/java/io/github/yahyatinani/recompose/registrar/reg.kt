package io.github.yahyatinani.recompose.registrar

import android.util.Log
import io.github.yahyatinani.recompose.TAG
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

internal val eventsRegistrar = ConcurrentHashMap<Any, Any>()
internal val effectsRegistrar = ConcurrentHashMap<Any, Any>()
internal val coeffectsRegistrar = ConcurrentHashMap<Any, Any>()
internal val subsRegistrar = ConcurrentHashMap<Any, Any>()

enum class Kinds { Event, Fx, Cofx, Sub }

internal fun getRegistrar(kind: Kinds) = when (kind) {
  Kinds.Event -> eventsRegistrar

  Kinds.Fx -> effectsRegistrar

  Kinds.Cofx -> coeffectsRegistrar

  Kinds.Sub -> subsRegistrar
}

fun getHandler(kind: Kinds, id: Any?): Any? {
  if (id == null) return null
  return getRegistrar(kind)[id]
}

fun registerHandler(
  id: Any,
  kind: Kinds,
  handlerFn: Any
): Any {
  getRegistrar(kind)[id] = handlerFn
  return handlerFn
}

internal fun clearHandlers() {
  eventsRegistrar.clear()
  effectsRegistrar.clear()
  coeffectsRegistrar.clear()
  subsRegistrar.clear()
}

internal fun clearHandlers(kind: Kinds) = getRegistrar(kind).clear()

internal fun clearHandlers(kind: Kinds, id: Any) {
  if (getHandler(kind, id) == null) {
    Log.w(TAG, "Can't clear $kind handler for $id. Handler not found.")
    return
  }

  getRegistrar(kind).remove(id)
}
