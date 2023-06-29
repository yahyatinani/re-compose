package com.github.yahyatinani.recompose.example.events

import com.github.yahyatinani.recompose.cofx.Coeffects
import com.github.yahyatinani.recompose.cofx.injectCofx
import com.github.yahyatinani.recompose.events.Event
import com.github.yahyatinani.recompose.example.Ids
import com.github.yahyatinani.recompose.example.Ids.exitApp
import com.github.yahyatinani.recompose.example.Ids.nextTick
import com.github.yahyatinani.recompose.example.Ids.setPrimaryColor
import com.github.yahyatinani.recompose.example.Ids.setSecondaryColor
import com.github.yahyatinani.recompose.example.Ids.startTicking
import com.github.yahyatinani.recompose.example.Ids.ticker
import com.github.yahyatinani.recompose.example.db.AppDb
import com.github.yahyatinani.recompose.fx.BuiltInFx
import com.github.yahyatinani.recompose.ids.recompose.db
import com.github.yahyatinani.recompose.regEventDb
import com.github.yahyatinani.recompose.regEventFx
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
import java.util.Date

fun regAllEvents() {
  regEventFx(
    id = nextTick,
    interceptors = v(injectCofx(Ids.now))
  ) { cofx, _ ->
    val appDb = cofx[db] as AppDb
    m(db to appDb.copy(time = cofx[Ids.now] as Date))
  }

  regEventFx(id = startTicking) { _, _ ->
    m(BuiltInFx.fx to v(v(ticker, null), null))
  }

  regEventDb<AppDb>(id = setPrimaryColor) { db, (_, colorInput) ->
    db.copy(primaryColor = colorInput as String)
  }

  regEventDb<AppDb>(id = setSecondaryColor) { db, (_, colorInput) ->
    db.copy(secondaryColor = colorInput as String)
  }

  regEventFx(exitApp) { _: Coeffects, _: Event ->
    m(BuiltInFx.fx to v(v(exitApp, null)))
  }

  regEventDb<AppDb>(id = Ids.about_dialog) { db, (_, flag) ->
    db.copy(showAboutDialog = flag as Boolean)
  }
}
