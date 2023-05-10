package com.github.whyrising.recompose.subs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.github.whyrising.y.concurrency.Atom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.withContext

/**
 * @param f - It takes app db [Atom] as an input.
 */
class Extraction(
  val inputSignal: Atom<*>,
  val context: CoroutineDispatcher = Dispatchers.Main,
  override val f: (signalValue: Any?) -> Any?
) : Reaction<Any?> {
  init {
    inputSignal.addWatch(key = hashCode()) { key, _, _, new ->
      ms.value = f(new)
      key
    }
  }

  override val initialValue: Any? = f(inputSignal.deref())

  private var ms: MutableState<Any?> = mutableStateOf(initialValue)

  var value: Any? by ms
    internal set

  override fun deref(): Any? = value

  override suspend fun collect(collector: FlowCollector<Any?>) {
    // Must run on Main dispatcher or throws an exception.
    withContext(context) { snapshotFlow { ms.value }.collect(collector) }
  }
}
