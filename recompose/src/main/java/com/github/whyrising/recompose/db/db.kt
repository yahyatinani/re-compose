package com.github.whyrising.recompose.db

import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.y.core.m
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal val DEFAULT_APP_DB_VALUE = m<Any, Any>()

class RAtom<T>(v: T) : Reaction<T> {
  private val _state = MutableStateFlow(v)

  override val state: StateFlow<Any?> = _state

  override fun deref(): T = _state.value

  override suspend fun collect(collector: FlowCollector<T>) =
    state.collect(collector as FlowCollector<Any?>)

  /** It doesn't emit the value if the newVal == the currentVal */
  fun emit(value: T) {
    _state.tryEmit(value)
  }
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
internal val appDb: RAtom<Any> = RAtom(DEFAULT_APP_DB_VALUE)
