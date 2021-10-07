package com.github.whyrising.recompose.sample

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.setProperty("kotlinx.coroutines.debug", "on")
    }
}
