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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
  id: Any,
  val context: CoroutineContext = Default,
  override val reactionScope: CoroutineScope = CoroutineScope(
    SupervisorJob() + context
  ),
  override val f: suspend (signalsValues: Any?, currentValue: Any?) -> Any?
) : ReactionBase<State, Any?>(id) {

  override val initialValue: State = m(computation_value to initial)

  private fun compVal(it: Any?) = get<Any?>(it, computation_value)

  override val signalObserver: Job = combineV(inputSignals) { it }
    .distinctUntilChanged()
    .transform<IPersistentVector<Any?>, State> { newSignals ->
      while (true) {
        val oldState = _state.value as State
        val oldSignals = oldState[signals_value] as Signals?
        if (oldSignals != null && newSignals.fold(true) { acc, signal ->
          if (!oldSignals.contains(signal)) return@fold false else acc
        }
        ) {
          return@transform // skip
        }

        val newState = m(
          signals_value to newSignals, // cache
          computation_value to f(newSignals, compVal(oldState))
        )
        if (_state.compareAndSet(oldState, newState)) return@transform
      }
    }
    .catch { th: Throwable -> throw RuntimeException(super.toString(), th) }
    .launchIn(reactionScope)

  override suspend fun collect(collector: FlowCollector<Any?>) =
    _state.collect { collector.emit((it as State)[computation_value]) }

  override val state: StateFlow<Any?> by lazy {
    _state
      .map { compVal(it) }
      .stateIn(
        reactionScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = compVal(initialValue)
      )
  }

  override fun deref(): Any? = compVal(_state.value)

  companion object {
    @Suppress("EnumEntryName")
    enum class Ids { signals_value, computation_value }

    /**
     * Same as [combine] but with [IPersistentVector] instead of [Array].
     */
    inline fun <reified T, R> combineV(
      flows: Iterable<Flow<T>>,
      crossinline transform: suspend (IPersistentVector<T>) -> R
    ): Flow<R> = combine(flows) { ts: Array<T> ->
      transform(ts.fold(v()) { acc, signal -> acc.conj(signal) })
    }
  }
}
