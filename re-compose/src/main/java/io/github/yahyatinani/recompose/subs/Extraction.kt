package io.github.yahyatinani.recompose.subs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transform

/**
 * @param context - The context of this [MutableState] flow by [snapshotFlow].
 * @param f - It takes the app db value as an input.
 */
class Extraction(
  val appDb: () -> Any?,
  id: Any,
  val context: CoroutineDispatcher = Dispatchers.Default,
  override val f: (signalValue: Any?) -> Any?
) : ReactionBase<Any?, Any?>(id) {

  override val initialValue: Any? = f(appDb())

  override val reactionScope: CoroutineScope = MainScope()

  override val category: Char = 'e'

  val state: State<Any?> = derivedStateOf { f(appDb()) }

  /**
   * This property is lazy because it is only realized if this [Extraction] was
   * collected/subscribed-to by at least one [Computation].
   */
  override val stateFlow: StateFlow<Any?> by lazy {
    snapshotFlow { state.value }
      .flowOn(context)
      .transform<Any?, Unit> { _state.value.emit(it) }
      .launchIn(reactionScope)

    _state.value
  }

  override suspend fun collect(collector: FlowCollector<Any?>) =
    stateFlow.collect(collector)

  override fun deref(): Any? = state.value

  @Composable
  override fun watch(): Any? {
    super.watch()
    return remember { state }.value
  }
}
