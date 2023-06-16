package com.github.yahyatinani.recompose.httpfx

import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.yahyatinani.recompose.dispatch
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.fx.regFx
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Suppress("EnumEntryName", "ClassName")
enum class bounce {
  fx,
  id,
  event,
  delay,
  time_received;

  override fun toString(): String = ":$name"
}

private val debounceRecord: Atom<IPersistentMap<Any, Any>> = atom(m())

val regBounceFx = run {
  fun dispatchLater(debounce: IPersistentMap<Any?, Any?>) {
    // TODO: pass a CoroutineScope?
    GlobalScope.launch {
      val delayPeriod = get<Any>(debounce, bounce.delay)!!
      delay((delayPeriod as Number).toLong())

      val timeReceived = debounce[bounce.time_received]
      if (timeReceived == get<Any>(debounceRecord(), debounce[bounce.id])) {
        dispatch(debounce[bounce.event] as Event)
      }
    }
  }

  regFx(id = bounce.fx) { debounce ->
    debounce as IPersistentMap<*, *>
    val now = Clock.System.now()
    val id = get<Any>(debounce, bounce.id)!!
    debounceRecord.swap { it.assoc(id, now) }
    dispatchLater(debounce.assoc(bounce.time_received, now))
  }
}
