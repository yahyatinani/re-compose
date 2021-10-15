package com.github.whyrising.recompose.sample.app.events

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.fx.FxIds.fx
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regEventFx
import com.github.whyrising.recompose.sample.app.Keys.now
import com.github.whyrising.recompose.sample.app.Keys.primaryColorChange
import com.github.whyrising.recompose.sample.app.Keys.secondaryColorChange
import com.github.whyrising.recompose.sample.app.Keys.startTicks
import com.github.whyrising.recompose.sample.app.Keys.timer
import com.github.whyrising.recompose.sample.app.Keys.timeticker
import com.github.whyrising.recompose.sample.app.db.AppSchema
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import java.util.Date

fun <T> initDBHandler(db: T, event: Event): AppSchema = defaultAppDB

fun primaryColorHandler(db: AppSchema, event: Event): AppSchema {
    val (_, color) = event
    return db.copy(primaryColor = (color as String))
}

fun secondaryColorHandler(db: AppSchema, event: Event): AppSchema {
    val (_, color) = event
    return db.copy(secondaryColor = (color as String))
}

fun startTicksHandler(cofx: Coeffects, event: Event): Effects = m(
    fx to v(v(timeticker))
)

fun timerHandler(cofx: Coeffects, event: Event): Effects {
    val appDb = cofx[db] as AppSchema
    return m(db to appDb.copy(time = cofx[now] as Date))
}

fun regEvents() {
    regEventDb(id = primaryColorChange, handler = ::primaryColorHandler)
    regEventDb(id = secondaryColorChange, handler = ::secondaryColorHandler)
    regEventFx(id = startTicks, handler = ::startTicksHandler)
    regEventFx(
        id = timer,
        interceptors = v(injectCofx(now)),
        handler = ::timerHandler
    )
}
