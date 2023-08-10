package io.github.yahyatinani.recompose.subs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.collections.PersistentVector
import io.github.yahyatinani.y.core.v
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
  val inputSignals: Signals,
  override val initialValue: Any?,
  id: Any,
  val context: CoroutineContext = Default,
  override val reactionScope: CoroutineScope = CoroutineScope(
    SupervisorJob() + context
  ),
  override val f: suspend (signalsValues: Any?, currentValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>(id) {

  val signalObserver: Job = combineV(inputSignals)
    .distinctUntilChanged()
    .transform<IPersistentVector<Any?>, Unit> { newSignals ->
      _state.value.update { f(newSignals, it) }
    }
    .catch { th: Throwable -> throw RuntimeException(super.toString(), th) }
    .launchIn(reactionScope)

  override suspend fun collect(collector: FlowCollector<Any?>) =
    _state.value.collect(collector)

  override val stateFlow: StateFlow<Any?> = _state.value

  override val category: Char = 'c'

  override fun deref(): Any? = _state.value.value

  @Composable
  override fun watch(): Any? {
    super.watch()
    return remember { stateFlow }.collectAsStateWithLifecycle().value
  }

  companion object {
    /** Same as [combine] but with [IPersistentVector] instead of [Array]. */
    inline fun <reified T> combineV(flows: Iterable<Flow<T>>) =
      combine(flows) { ts: Array<T> ->
        var ret: PersistentVector.TransientVector<T> = v<T>().asTransient()
        ts.forEach { ret = ret.conj(it) }
        ret.persistent()
      }
  }
}
