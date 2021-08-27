package com.github.whyrising.recompose.db

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal val DEFAULT_APP_DB_VALUE = Any()

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
var appDb by mutableStateOf(DEFAULT_APP_DB_VALUE)
    internal set
