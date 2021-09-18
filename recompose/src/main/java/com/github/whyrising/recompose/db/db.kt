package com.github.whyrising.recompose.db

import com.github.whyrising.recompose.subs.React
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect

internal val DEFAULT_APP_DB_VALUE = Any()

class RAtom<T>(v: T) : React<T> {
    internal val state: MutableStateFlow<T> = MutableStateFlow(v)

    override fun deref(): T = state.value

    override suspend fun collect(action: suspend (T) -> Unit) = state.collect {
        action(it)
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
internal val appDb = RAtom(DEFAULT_APP_DB_VALUE)
