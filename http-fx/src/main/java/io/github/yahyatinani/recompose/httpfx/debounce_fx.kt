package io.github.yahyatinani.recompose.httpfx

import io.github.yahyatinani.recompose.dispatch
import io.github.yahyatinani.recompose.events.Event
import io.github.yahyatinani.recompose.fx.regFx
import io.github.yahyatinani.y.concurrency.Atom
import io.github.yahyatinani.y.concurrency.atom
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m
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
