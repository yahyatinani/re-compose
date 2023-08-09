package io.github.yahyatinani.recompose.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yahyatinani.recompose.regSub
import io.github.yahyatinani.recompose.subs.Computation
import io.github.yahyatinani.recompose.subs.Extraction
import io.github.yahyatinani.recompose.subs.ReactionBase
import io.github.yahyatinani.recompose.subscribe
import io.github.yahyatinani.y.core.collections.IPersistentMap
import io.github.yahyatinani.y.core.v
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
  fun watchTest() {
    repeat(11) {
      regSub<IPersistentMap<*, *>>(queryId = "info$it") { db, _ -> db }
    }

    benchmarkRule.measureRepeated {
      repeat(9) {
        val reaction = subscribe<Any>(v("info$it")) as ReactionBase<*, *>
        when (reaction) {
          is Extraction -> reaction
          else -> (reaction as Computation).state.value
        }
      }
    }
  }

  @Test
  fun regSubTest() {
    /*
      5 times: 45,678   ns         290 allocs    trace
      6 times: 60,850   ns         378 allocs    trace
      7 times: 76,202   ns         476 allocs    trace
      8 times: 100,513   ns        584 allocs   trace
      9 times: 138,976   ns         764 allocs
     */
    benchmarkRule.measureRepeated {
      repeat(11) {
        regSub<IPersistentMap<*, *>>(queryId = "info$it") { db, _ -> db }
      }
    }
  }
}
