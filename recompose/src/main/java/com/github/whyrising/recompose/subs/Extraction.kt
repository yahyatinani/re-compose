package com.github.whyrising.recompose.subs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.github.whyrising.y.concurrency.Atom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * @param appDb - The [Atom] containing [appDb] value.
 * @param context - The context of this [MutableState] flow by [snapshotFlow].
 * @param f - It takes the app db value as an input.
 */
class Extraction(
  val appDb: Atom<*>,
  val context: CoroutineDispatcher = Dispatchers.Main,
  override val f: (signalValue: Any?) -> Any?
) : Reaction<Any?> {
  init {
    appDb.addWatch(key = hashCode()) { key, _, _, new ->
      ms.value = f(new)
      key
    }
  }

  override val initialValue: Any? = f(appDb.deref())

  private var ms: MutableState<Any?> = mutableStateOf(initialValue)

  var value: Any? by ms
    internal set

  override fun deref(): Any? = value

  /**
   * This property is lazy because it is only realized if this [Extraction] was
   * collected/subscribed to by at least one [Computation].
   */
  internal val stateIn by lazy {
    /*
     * The use of [stateIn] is to avoid creating multiple flows via snapshotFlow
     * when collected by multiple Computations.
     */
    snapshotFlow { ms.value }
      .flowOn(context) // important to run on Main, else IllegalStateException.
      .stateIn(
        initialValue = initialValue,
        scope = MainScope(),
        started = SharingStarted.WhileSubscribed(5000)
      )
  }

  override suspend fun collect(collector: FlowCollector<Any?>) =
    stateIn.collect(collector)
}
