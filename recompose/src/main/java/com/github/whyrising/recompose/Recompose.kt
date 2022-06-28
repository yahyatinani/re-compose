package com.github.whyrising.recompose

import android.app.Application
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.fx.registerBuiltinEffectHandlers
import com.github.whyrising.recompose.schemas.Schema.db

/**
 * You must use or inherit this class in your app as your [Application] class
 * and add it to your manifest for re-compose to work correctly.
 */
open class Recompose : Application() {
  override fun onCreate() {
    super.onCreate()

    init()
  }

  companion object {
    internal fun init() {
      regCofx(id = db) { coeffects -> coeffects.assoc(db, appDb.deref()) }

      registerBuiltinEffectHandlers()
    }
  }
}
