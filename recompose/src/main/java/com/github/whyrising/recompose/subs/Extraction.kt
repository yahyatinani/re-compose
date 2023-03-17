package com.github.whyrising.recompose.subs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transform

/**
 * @param f - It takes the value of [appDb] as an input.
 */
class Extraction(
  val inputSignal: Reaction<*>,
  override val f: (signalValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>() {
  override val reactionScope: CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override val initialValue: Any? = f(inputSignal.deref())

  override val signalObserver: Job = inputSignal
    .transform<Any?, Any?> { _state.emit(f(it)) }
    .launchIn(reactionScope)

  override fun deref(): Any? = state.value

  override suspend fun collect(collector: FlowCollector<Any?>) {
    state.collect(collector)
  }
}
