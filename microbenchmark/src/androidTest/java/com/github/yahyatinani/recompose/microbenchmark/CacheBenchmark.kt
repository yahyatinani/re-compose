package com.github.yahyatinani.recompose.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.whyrising.recompose.regSub
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.v
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class CacheBenchmark {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun subscribe() {
    regSub<IPersistentMap<*, *>>(queryId = "info") { db, _ ->
      db
    }

    benchmarkRule.measureRepeated {
      com.github.whyrising.recompose.subscribe<Any>(v("info"))
    }
  }
}
