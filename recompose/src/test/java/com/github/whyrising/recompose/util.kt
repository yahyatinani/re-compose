package com.github.whyrising.recompose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun measureTime(id: String, action: () -> Unit) {
  val start = System.nanoTime()
  action()
  val timeElapsed = System.nanoTime() - start

  println("$id timeElapsed: $timeElapsed")
}

suspend fun multiThreadedRun(
  coroutinesN: Int = 100,
  runN: Int = 1000,
  context: CoroutineContext = Dispatchers.Default,
  action: suspend () -> Unit
) {
  coroutineScope {
    repeat(coroutinesN) {
      launch(context) {
        repeat(runN) { action() }
      }
    }
  }
}
