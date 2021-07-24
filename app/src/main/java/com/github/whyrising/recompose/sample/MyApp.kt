package com.github.whyrising.recompose.sample

import android.app.Application
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.regEventDb

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        regEventDb(":init") { _, _ ->
            AppSchema(text = "Android", 0)
        }

        dispatchSync(event(":init"))
    }
}