package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.subs.Ids.computation_value
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Suppress("EnumEntryName")
internal enum class Ids {
  signals_value,
  computation_value
}

fun <T> deref(refs: IPersistentVector<IDeref<T>>): PersistentVector<T> =
  refs.fold(v()) { acc, r ->
    acc.conj(r.deref())
  }

/**
 * @param inputSignals are the nodes (Reactions) that signal the current
 * [ComputationReaction] to recalculate its value when new values are provided.
 * @param initial when it's null, the first value calculation of this
 * [ComputationReaction] happens on the main thread.
 * @param context on which further values' calculations of this
 * [ComputationReaction] will happen.
 * @param f is the function that calculates the sequence of values of this
 * [ComputationReaction].
 */
class ComputationReaction<I, O>(
  inputSignals: IPersistentVector<Reaction<I>>,
  private val context: CoroutineContext,
  private val initial: O,
  private val context2: CoroutineContext = Dispatchers.Default,
  val f: suspend (signalsValues: IPersistentVector<I>) -> O
) : ReactionBase<IPersistentMap<Any, Any?>, O>() {
  override val state: MutableStateFlow<IPersistentMap<Any, Any?>> by lazy {
    initState(m<Any, Any?>(computation_value to initial))
  }

  private fun isSameInput(
    currentInputs: PersistentVector<I>,
    index: Int,
    newInput: I
  ) = currentInputs.count > index && currentInputs[index] == newInput

  internal suspend fun recompute(input: I, inputIndex: Int) {
    while (true) {
      val currentValue = state.value
      val currInputs =
        currentValue[Ids.signals_value] as PersistentVector<I>? ?: v()

      if (isSameInput(currInputs, inputIndex, input)) {
        return
      }

      val newInputs = currInputs.assoc(inputIndex, input)
      val materializedView = f(newInputs)
      val newState = m(
        computation_value to materializedView,
        Ids.signals_value to newInputs
      )

      if (state.compareAndSet(currentValue, newState)) {
        return
      }
    }
  }

  // init should be after state property.
  init {
    viewModelScope.launch(context) {
      val inputs = deref(inputSignals)
      state.value = m(
        Ids.signals_value to inputs,
        computation_value to f(inputs)
      )
      for ((i, inputNode) in inputSignals.withIndex())
        viewModelScope.launch(context2) {
          inputNode.collect { newInput: I ->
            recompute(newInput, i)
          }
        }
    }
  }

  override fun deref(state: State<IPersistentMap<Any, Any?>>): O =
    state.value[computation_value] as O

  override fun deref(): O = state.value[computation_value] as O

  override suspend fun collect(action: suspend (O) -> Unit) = state.collect {
    action(it[computation_value] as O)
  }
}
