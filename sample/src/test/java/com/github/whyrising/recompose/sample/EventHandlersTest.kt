package com.github.whyrising.recompose.sample

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.Effects
import com.github.whyrising.recompose.fx.FxIds.fx
import com.github.whyrising.recompose.sample.app.Keys.now
import com.github.whyrising.recompose.sample.app.Keys.primaryColorChange
import com.github.whyrising.recompose.sample.app.Keys.secondaryColorChange
import com.github.whyrising.recompose.sample.app.Keys.startTicks
import com.github.whyrising.recompose.sample.app.Keys.timer
import com.github.whyrising.recompose.sample.app.Keys.timeticker
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.recompose.sample.app.events.initDBHandler
import com.github.whyrising.recompose.sample.app.events.primaryColorHandler
import com.github.whyrising.recompose.sample.app.events.secondaryColorHandler
import com.github.whyrising.recompose.sample.app.events.startTicksHandler
import com.github.whyrising.recompose.sample.app.events.timerHandler
import com.github.whyrising.recompose.schemas.Schema.db
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.Date

class EventHandlersTest : FreeSpec({
    "initDBHandler() should return `defaultAppDB`" {
        initDBHandler(Any(), v()) shouldBeSameInstanceAs defaultAppDB
    }

    "primaryColorHandler() should return new appDb with new primary color" {
        val newDb = primaryColorHandler(
            db = defaultAppDB,
            event = v(primaryColorChange, "red")
        )

        newDb shouldBe defaultAppDB.copy(primaryColor = "red")
    }

    "secondaryColorHandler() should return new appDb with new secondary color" {
        val newDb = secondaryColorHandler(
            db = defaultAppDB,
            event = v(secondaryColorChange, "red")
        )

        newDb shouldBe defaultAppDB.copy(secondaryColor = "red")
    }

    "startTicksHandler(cofx,event) should return a map of `timeticker` effect" {
        val cofx: Coeffects = m()
        val event: Event = v(startTicks)

        val effects: Effects = startTicksHandler(cofx, event)

        effects shouldBe m(fx to v(v(timeticker)))
    }

    """
        timerHandler(cofx,event) should take the `time` from cofx and build 
        with it a new `db`, then return a map of that db
    """ {
        val date = Date()
        val cofx: Coeffects = m(
            db to defaultAppDB,
            now to date
        )
        val event: Event = v(timer)

        val effects: Effects = timerHandler(cofx, event)

        effects shouldBe m(db to defaultAppDB.copy(time = date))
    }
})
