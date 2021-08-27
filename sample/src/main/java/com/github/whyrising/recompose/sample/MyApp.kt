package com.github.whyrising.recompose.sample

import android.app.Application
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.sample.Keys.initialize

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        regEventDb<Any>(initialize) { _, _ ->
            AppSchema(text = "Android", 0)
        }

        dispatchSync(event(initialize))
    }
}
