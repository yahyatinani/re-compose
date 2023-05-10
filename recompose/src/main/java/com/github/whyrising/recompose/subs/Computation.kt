package com.github.whyrising.recompose.subs

import com.github.whyrising.recompose.subs.Computation.Companion.Ids
import com.github.whyrising.recompose.subs.Computation.Companion.Ids.computation_value
import com.github.whyrising.recompose.subs.Computation.Companion.Ids.signals_value
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.PersistentVector
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformWhile
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

/*    override val signalObserver: Job

    private fun deref(inputSignals: Signals) =
      inputSignals.fold(v<Any?>()) { acc, reaction -> acc.conj(reaction.deref()) }

    init {
      signalObserver = reactionScope.launch(context = context) {
        inputSignals.forEachIndexed { i, signal ->
          reactionScope.launch(context) {
            signal.collect { newSignal ->
              val oldState = _state.value as State
              val signals = deref(inputSignals).assoc(i, newSignal)
              while (true) {
                val newState = m(
                  signals_value to signals, // cache
                  computation_value to f(signals, compVal(oldState))
                )
                if (_state.compareAndSet(oldState, newState)) break
              }
            }
          }
        }
      }
    }*/

  override val signalObserver: Job = combine(inputSignals) { it }
    .distinctUntilChanged()
    .map { Pair(it, _state.value as State) }
    .transformWhile { pair ->
      emit(pair)
      pair.first != pair.second[signals_value]
    }
    .map { (signals, oldState) ->
      // FIXME: write new combine function to work with PersistentVectors.
      signals.fold(v<Any?>()) { acc, signal -> acc.conj(signal) }.conj(oldState)
    }
    .transform<PersistentVector<Any?>, State> { vec ->
      val oldState = vec.peek() as State
      val signals = vec.pop()
      while (true) {
        val newState = m(
          signals_value to signals, // cache
          computation_value to f(signals, compVal(oldState))
        )
        if (_state.compareAndSet(oldState, newState)) break
      }
    }
    .catch { th: Throwable -> throw RuntimeException(super.toString(), th) }
    .launchIn(reactionScope)

  override suspend fun collect(collector: FlowCollector<Any?>) =
    _state.collect { collector.emit((it as State)[computation_value]) }
//    _state.collect { collector.emit(it) }

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
  }
}
