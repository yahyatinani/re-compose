package io.github.yahyatinani.recompose.db

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.yahyatinani.y.core.m

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

internal var appDb: MutableState<Any> = mutableStateOf(m<Any, Any>())
