package com.github.whyrising.recompose.example

import android.app.Application

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    System.setProperty("kotlinx.coroutines.debug", "on")

    initAppDb()
  }
}
