package com.github.whyrising.recompose.example.fx

import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.example.Ids.exitApp
import com.github.whyrising.recompose.example.Ids.ticktack
import com.github.whyrising.recompose.regFx
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

fun regAllFx(scope: CoroutineScope) {
    regFx(id = ticktack) {
        scope.launch {
            while (true) {
                dispatch(v(ticktack))
                delay(1_000)
            }
        }
    }

    regFx(id = exitApp) {
        exitProcess(-1)
    }
}
