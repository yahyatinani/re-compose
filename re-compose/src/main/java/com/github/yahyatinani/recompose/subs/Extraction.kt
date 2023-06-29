package com.github.yahyatinani.recompose.subs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.yahyatinani.y.concurrency.Atom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * @param appDb - The [Atom] containing [appDb] value.
 * @param context - The context of this [MutableState] flow by [snapshotFlow].
 * @param f - It takes the app db value as an input.
 */
class Extraction(
  val appDb: Atom<*>,
  id: Any,
  val context: CoroutineDispatcher = Dispatchers.Main,
  override val f: (signalValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>(id) {
  override val initialValue: Any? = f(appDb.deref())

  private var ms: MutableState<Any?> = mutableStateOf(initialValue)

  init {
    appDb.addWatch(key = id) { key, _, _, new ->
      ms.value = f(new)
      key
    }
  }

  var value: Any? by ms
    internal set

  override fun deref(): Any? = value

  override val reactionScope: CoroutineScope = MainScope()

  override val category: Char = 'e'

  private fun <T> Flow<T>.mutableStateIn(): MutableStateFlow<Any?> {
    val mutableStateFlow = _state.value
    reactionScope.launch { this@mutableStateIn.collect(mutableStateFlow) }
    return mutableStateFlow
  }

  /**
   * This property is lazy because it is only realized if this [Extraction] was
   * collected/subscribed to by at least one [Computation].
   */
  override val state: StateFlow<Any?> by lazy {
    /*
     * The use of [stateIn] is to avoid creating multiple flows via snapshotFlow
     * when collected by multiple Computations.
     */
    snapshotFlow { ms.value }
      .flowOn(context) // important to run on Main, else IllegalStateException.
      .mutableStateIn()
  }

  override suspend fun collect(collector: FlowCollector<Any?>) =
    state.collect(collector)
}
