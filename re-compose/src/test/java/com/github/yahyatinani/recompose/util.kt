package com.github.yahyatinani.recompose

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
  context: CoroutineContext = StandardTestDispatcher(),
  action: suspend () -> Unit
) = coroutineScope {
  repeat(coroutinesN) {
    launch { repeat(runN) { action() } }
  }
}
