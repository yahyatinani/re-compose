package com.github.whyrising.recompose.example

import com.github.whyrising.recompose.Recompose
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.example.Ids.initDb
import com.github.whyrising.recompose.example.db.AppDb
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.y.core.v

class App : Recompose() {
  override fun onCreate() {
    super.onCreate()
    System.setProperty("kotlinx.coroutines.debug", "on")

    regEventDb<Any>(id = initDb) { _, _ -> AppDb() }
    dispatchSync(v(initDb))
  }
}
