package com.github.whyrising.recompose.sample.app.events

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.fx.FxIds
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regEventFx
import com.github.whyrising.recompose.sample.app.Keys
import com.github.whyrising.recompose.sample.app.db.AppSchema
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import java.util.Date

fun regEvents() {
    regEventDb<Any>(Keys.initialize) { _, _ ->
        defaultAppDB
    }

    regEventFx(Keys.startTicks) { _, _ ->
        m(FxIds.fx to v(v(Keys.timeticker, null)))
    }

    regEventFx(
        id = Keys.timer,
        interceptors = v(injectCofx(Keys.now))
    ) { cofx: Coeffects, _ ->
        val db = cofx[Schema.db] as AppSchema
        m(Schema.db to db.copy(time = cofx[Keys.now] as Date))
    }

    regEventDb<AppSchema>(
        Keys.primaryColorChange
    ) { db, (_, color) ->
        db.copy(primaryColor = (color as String))
    }

    regEventDb<AppSchema>(
        Keys.secondaryColorChange
    ) { db, (_, color) ->
        db.copy(secondaryColor = (color as String))
    }
}
