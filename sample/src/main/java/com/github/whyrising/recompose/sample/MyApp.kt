package com.github.whyrising.recompose.sample

import android.app.Application
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.sample.Keys.initialize
import com.github.whyrising.y.collections.core.v
import java.util.Date

fun initialize() {
    regEventDb<Any>(initialize) { _, _ ->
        AppSchema(
            time = Date(),
            timeColor = "Pink",
        )
    }

    dispatchSync(v(initialize))
}

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        System.setProperty("kotlinx.coroutines.debug", "on")

        initialize()
    }
}
