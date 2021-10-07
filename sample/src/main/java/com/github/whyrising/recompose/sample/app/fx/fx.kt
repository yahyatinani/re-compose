package com.github.whyrising.recompose.sample.app.fx

import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.regFx
import com.github.whyrising.recompose.sample.app.Keys.timer
import com.github.whyrising.recompose.sample.app.Keys.timeticker
import com.github.whyrising.y.collections.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val HH_MM_SS = "HH:mm:ss"

fun regFx(scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)) {
    regFx(timeticker) {
        scope.launch(Dispatchers.Default) {
            while (true) {
                dispatch(v(timer))
                delay(1000)
            }
        }
    }
}
