package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import com.github.whyrising.recompose.subs.ComputationReaction.Companion.Ids.computation_value
import com.github.whyrising.recompose.subs.ComputationReaction.Companion.Ids.signals_value
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.PersistentArrayMap
import com.github.whyrising.y.core.collections.PersistentVector
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

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
  private val initial: O?,
  context: CoroutineContext = Dispatchers.Default,
  val f: suspend (signalsValues: IPersistentVector<I>, oldComp: O?) -> O
) : ReactionBase<IPersistentMap<Any, Any?>, O>() {
  override val reactionScope = CoroutineScope(SupervisorJob() + context)

  private fun withInitial() = initial != null

  private suspend fun calcFirstValue(
    inputSignals: IPersistentVector<Reaction<I>>
  ): PersistentArrayMap<Ids, Any?> {
    val inputs = deref(inputSignals)
    return m(
      signals_value to inputs,
      computation_value to f(inputs, initial)
    )
  }

  override val state: MutableStateFlow<IPersistentMap<Any, Any?>> by lazy {
    initState(
      stateValue = when {
        withInitial() -> m<Any, Any?>(computation_value to initial)
        else -> runBlocking { calcFirstValue(inputSignals) }
      }
    )
  }

  private fun isSameInput(
    currentInputs: PersistentVector<I>,
    index: Int,
    newInput: I
  ) = currentInputs.count > index && currentInputs[index] == newInput

  internal suspend fun recompute(input: I, inputIndex: Int) {
    while (true) {
      val oldState = state.value
      val oldInputs = oldState[signals_value] as PersistentVector<I>? ?: v()

      if (isSameInput(oldInputs, inputIndex, input)) {
        return
      }

      val oldCompVal = oldState[computation_value] as O?
      val newInputs = oldInputs.assoc(inputIndex, input)
      val newState = m(
        computation_value to f(newInputs, oldCompVal),
        signals_value to newInputs
      )

      if (state.compareAndSet(oldState, newState)) {
        return
      }
    }
  }

  // init should be after state property.
  init {
    reactionScope.launch {
      if (withInitial()) {
        // calc first value
        state.value = calcFirstValue(inputSignals)
      }
      for ((i, inputNode) in inputSignals.withIndex())
        reactionScope.launch {
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

  companion object {
    fun <T> deref(refs: IPersistentVector<IDeref<T>>): PersistentVector<T> =
      refs.fold(v()) { acc, r ->
        acc.conj(r.deref())
      }

    @Suppress("EnumEntryName")
    internal enum class Ids {
      signals_value,
      computation_value
    }
  }
}
