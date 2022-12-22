package com.github.whyrising.recompose.subs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

/**
 * @param f - It takes the value of [appDb] as an input.
 */
class Extraction(
  val inputSignal: Reaction<Any?>,
  override val f: (signalValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>() {
  override val reactionScope: CoroutineScope = MainScope()

  override val initialValue: Any? = f(inputSignal.deref())

  override val computationJob: Job by lazy {
    inputSignal
      .distinctUntilChanged()
      .map { f(it) }
      .distinctUntilChanged()
      .transform<Any?, Any?> { _state.emit(it) }
      .launchIn(reactionScope)
  }

  override fun deref(): Any? = state.value

  override suspend fun collect(collector: FlowCollector<Any?>) =
    state.collect(collector)
}
