package io.github.yahyatinani.recompose.db

import io.github.yahyatinani.y.concurrency.Atom
import io.github.yahyatinani.y.concurrency.atom
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
internal val appDb: Atom<Any> = atom(m<Any, Any>())
