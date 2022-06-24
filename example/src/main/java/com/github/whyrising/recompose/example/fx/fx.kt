package com.github.whyrising.recompose.example.fx

import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.example.Ids.exitApp
import com.github.whyrising.recompose.example.Ids.nextTick
import com.github.whyrising.recompose.example.Ids.ticker
import com.github.whyrising.recompose.regFx
import com.github.whyrising.y.core.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

fun regAllFx(scope: CoroutineScope) {
  regFx(id = ticker) {
    scope.launch(Dispatchers.Default) {
      while (true) {
        dispatch(v(nextTick))
        delay(1_000)
      }
    }
  }

  regFx(id = exitApp) {
    exitProcess(-1)
  }
}
