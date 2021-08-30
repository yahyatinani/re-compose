package com.github.whyrising.recompose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun multiThreadedRun(
    n: Int = 100,
    k: Int = 1000,
    action: suspend () -> Unit
) {
    withContext(Dispatchers.Default) {
        coroutineScope {
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
}
