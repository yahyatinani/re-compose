package com.github.whyrising.recompose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun measureTime(id: String, action: () -> Unit) {
    val start = System.nanoTime()
    action()
    val timeElapsed = System.nanoTime() - start

    println("$id timeElapsed: $timeElapsed")
}

suspend fun multiThreadedRun(
    n: Int = 100,
    k: Int = 1000,
    context: CoroutineContext = EmptyCoroutineContext,
    action: suspend () -> Unit
) {
    withContext(context) {
        coroutineScope {
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
}
