package com.github.whyrising.recompose.sample.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Surface
import androidx.lifecycle.lifecycleScope
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.sample.app.composables.TimeApp
import com.github.whyrising.recompose.sample.app.db.regCofxs
import com.github.whyrising.recompose.sample.app.events.regEvents
import com.github.whyrising.recompose.sample.app.fx.regFx
import com.github.whyrising.recompose.sample.app.subs.regSubs
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme
import com.github.whyrising.y.collections.core.v

@Suppress("EnumEntryName")
enum class Keys {
    initialize,
    timer,
    time,
    formattedTime,
    primaryColor,
    timeColorName,
    timeColorChange,
    startTicks,
    timeticker,
    materialThemeColors,
    statusBarDarkIcons,
    now
}

fun init() {
    regEvents()
    dispatchSync(v(Keys.initialize))
    regSubs()
    regCofxs()
}

class MainActivity : ComponentActivity() {
    init {
        init()
        regFx(lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dispatch(v(Keys.startTicks))
        setContent {
            RecomposeTheme {
                Surface {
                    TimeApp()
                }
            }
        }
    }
}
