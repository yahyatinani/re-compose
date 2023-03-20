package com.github.whyrising.recompose.subs

import com.github.whyrising.recompose.subs.Computation.Companion.Ids
import com.github.whyrising.recompose.subs.Computation.Companion.Ids.computation_value
import com.github.whyrising.recompose.subs.Computation.Companion.Ids.signals_value
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlin.coroutines.CoroutineContext

typealias State = IPersistentMap<Ids, *>
typealias Signals = IPersistentVector<Reaction<*>>

class Computation(
  inputSignals: Signals,
  initial: Any?,
  val context: CoroutineContext = Default,
  override val f: suspend (signalsValues: Any?, currentValue: Any?) -> Any?
) : ReactionBase<State, Any?>() {

  override val reactionScope: CoroutineScope =
    CoroutineScope(SupervisorJob() + context)

  override val initialValue: State = m(computation_value to initial)

  override val signalObserver: Job = combine(inputSignals) { it }
    .distinctUntilChanged()
    .transform<Array<Any?>, State> { newSignals ->
      while (true) {
        val currentState = _state.value as State

        if (newSignals == currentState[signals_value]) { // skip
          return@transform
        }

        // TODO: write new combine function to work with vectors.
        val v = newSignals.fold(v<Any?>()) { acc, signal -> acc.conj(signal) }

        val newState = m(
          signals_value to v,
          computation_value to f(v, currentState[computation_value])
        )
        if (_state.compareAndSet(currentState, newState)
        ) {
          return@transform
        }
      }
    }
    .launchIn(reactionScope)

  override fun deref(): Any? = get(_state.value, computation_value)

  override suspend fun collect(collector: FlowCollector<Any?>) =
    _state.collect { collector.emit(deref()) }

  override val state: StateFlow<Any?> by lazy {
    _state
      .map { get<Any?>(it, computation_value) }
      .stateIn(
        reactionScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue[computation_value]
      )
  }

  companion object {
    @Suppress("EnumEntryName")
    enum class Ids { signals_value, computation_value }
  }
}