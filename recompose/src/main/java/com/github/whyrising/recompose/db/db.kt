package com.github.whyrising.recompose.db

import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.y.core.m
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class RAtom<T>(v: T) : Reaction<T>, SynchronizedObject() {
  private val _state = MutableStateFlow(v)

  override val state: StateFlow<Any?> = _state

  override fun deref(): T = _state.value

  override suspend fun collect(collector: FlowCollector<T>) =
    state.collect(collector as FlowCollector<Any?>)

  /** It doesn't emit the value if the newVal == the currentVal */
  fun reset(value: T) = _state.update { value }
}

/**
 * ------------------ Application State ---------------
 *
 * Should not be accessed directly by application code.
 *
 * Read access goes through subscriptions.
 *
 * Updates via event handlers.
 *
 * It is set to a default token until it gets initialized via an event handler.
 * */
internal val appDb: RAtom<Any> = RAtom(m<Any, Any>())
