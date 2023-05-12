package com.github.whyrising.recompose.subs

import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

typealias Signals = IPersistentVector<Flow<*>>

class Computation(
  inputSignals: Signals,
  initial: Any?,
  id: Any,
  val context: CoroutineContext = Default,
  override val reactionScope: CoroutineScope = CoroutineScope(
    SupervisorJob() + context
  ),
  override val f: suspend (signalsValues: Any?, currentValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>(id) {

  override val initialValue: Any? = initial

  override val signalObserver: Job = combineV(inputSignals)
    .distinctUntilChanged()
    .transform<IPersistentVector<Any?>, Unit> { newSignals ->
      _state.update { f(newSignals, it) }
    }
    .catch { th: Throwable -> throw RuntimeException(super.toString(), th) }
    .launchIn(reactionScope)

  override suspend fun collect(collector: FlowCollector<Any?>) =
    _state.collect(collector)

  override val state: StateFlow<Any?> = _state

  override fun deref(): Any? = _state.value

  companion object {
    /**
     * Same as [combine] but with [IPersistentVector] instead of [Array].
     */
    inline fun <reified T> combineV(flows: Iterable<Flow<T>>) =
      combine(flows) { ts: Array<T> ->
        var ret: PersistentVector.TransientVector<T> = v<T>().asTransient()
        ts.forEach { ret = ret.conj(it) }
        ret.persistent()
      }
  }
}
