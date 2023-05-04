package com.github.whyrising.recompose.subs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.github.whyrising.y.concurrency.Atom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @param f - It takes the value of [appDb] as an input.
 */
class Extraction(
  val inputSignal: Atom<*>,
  override val f: (signalValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>() {
  override val reactionScope: CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override val initialValue: Any? = f(inputSignal.deref())

  private var ms: MutableState<Any?> = mutableStateOf(initialValue)

  init {
    inputSignal.addWatch(key = hashCode()) { key, _, _, new ->
      ms.value = f(new)
      key
    }
  }

  val value: Any? by ms

  override val signalObserver: Job = reactionScope.launch {} // fixme

  override val state: StateFlow<Any?>
    get() = _state

  override fun deref(): Any? = value

  override suspend fun collect(collector: FlowCollector<Any?>) {
    withContext(Dispatchers.Main) {
      snapshotFlow { ms.value }.collect(collector)
    }
  }
}
