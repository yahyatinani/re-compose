package com.github.whyrising.recompose.sample

import android.app.Application
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.sample.Keys.initialize
import java.util.Date

fun initialize() {
    regEventDb<Any>(initialize) { _, _ ->
        AppSchema(
            time = Date(),
            timeColor = "Red",
        )
    }

    dispatchSync(event(initialize))
}

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initialize()
    }
}
