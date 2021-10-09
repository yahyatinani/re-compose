package com.github.whyrising.recompose.sample.app.events

import com.github.whyrising.recompose.RKeys
import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regEventFx
import com.github.whyrising.recompose.sample.app.Keys
import com.github.whyrising.recompose.sample.app.db.AppSchema
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.map.IPersistentMap
import java.util.Date

fun regEvents() {
    regEventDb<Any>(Keys.initialize) { _, _ ->
        defaultAppDB
    }

    regEventFx(Keys.startTicks) { _, _ ->
        m(RKeys.fx to v(v(Keys.timeticker, null)))
    }

    regEventFx(
        id = Keys.timer,
        interceptors = v(injectCofx(Keys.now))
    ) { cofx: IPersistentMap<Any, Any>, _ ->
        val db =
            cofx[RKeys.db] as AppSchema
        m(RKeys.db to db.copy(time = cofx[Keys.now] as Date))
    }

    regEventDb<AppSchema>(
        Keys.timeColorChange
    ) { db, (_, color) ->
        db.copy(timeColor = (color as String))
    }

    regEventDb<AppSchema>(":a") { db, (_, a) ->
        db.copy(a = a as String)
    }

    regEventDb<AppSchema>(":b") { db, (_, b) ->
        db.copy(b = b as String)
    }
}
