package com.github.yahyatinani.recompose.example.fx

import com.github.yahyatinani.recompose.dispatch
import com.github.yahyatinani.recompose.example.Ids.exitApp
import com.github.yahyatinani.recompose.example.Ids.nextTick
import com.github.yahyatinani.recompose.example.Ids.ticker
import com.github.yahyatinani.recompose.regFx
import io.github.yahyatinani.y.core.v
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
