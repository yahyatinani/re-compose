package com.github.whyrising.recompose

import android.app.Application
import com.github.whyrising.recompose.events.event

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Framework()
        regEventDb(":init") { _, _ ->
            AppSchema(text = "Android", 0)
        }

        dispatchSync(event(":init"))
    }
}