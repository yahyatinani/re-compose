package com.github.whyrising.recompose.db

import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom

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
internal var appDb: Atom<Any> = atom(DEFAULT_APP_DB_VALUE)
