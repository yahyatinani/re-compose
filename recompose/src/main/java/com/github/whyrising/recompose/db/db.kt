package com.github.whyrising.recompose.db

import com.github.whyrising.recompose.subs.ReactiveAtom
import com.github.whyrising.y.core.m
import kotlinx.coroutines.flow.MutableStateFlow

internal val DEFAULT_APP_DB_VALUE = m<Any, Any>()

class RAtom<T>(v: T) : ReactiveAtom<T> {
  private val state: MutableStateFlow<T> = MutableStateFlow(v)

  override fun deref(): T = state.value

  override suspend fun collect(action: suspend (T) -> Unit) = state.collect {
    action(it)
  }

  /** It doesn't emit the value if the newVal == the currentVal */
  override fun emit(value: T) {
    state.tryEmit(value)
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
