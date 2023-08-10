package io.github.yahyatinani.recompose.example.fx

import io.github.yahyatinani.recompose.example.Ids.exitApp
import io.github.yahyatinani.recompose.regFx
import kotlin.system.exitProcess

fun regAllFx() {
  regFx(id = exitApp) {
    exitProcess(-1)
  }
}
