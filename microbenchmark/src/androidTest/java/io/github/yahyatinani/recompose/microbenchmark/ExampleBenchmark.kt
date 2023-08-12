package io.github.yahyatinani.recompose.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio
 * will output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

  @get:Rule
  val benchmarkRule = BenchmarkRule()

  internal enum class State {
    IDLE,
    SCHEDULING,
    RUNNING,
    PAUSED
  }
  internal enum class FsmEvent {
    ADD_EVENT,
    RUN_QUEUE,
    FINISH_RUN,
    PAUSE,
    RESUME,
    EXCEPTION
  }

  @Test
  fun vecVsPair() {
    /**
     * Pairs: 44.2 ns           1 allocs    trace    ExampleBenchmark.log
     *        40.7 ns           0 allocs    trace    ExampleBenchmark.log
     *        44.5 ns           0 allocs    trace    ExampleBenchmark.log
     *
     * Vec:   510   ns           6 allocs    trace    ExampleBenchmark.log
     *        537   ns           6 allocs    trace    ExampleBenchmark.log
     *        522   ns           6 allocs    trace    ExampleBenchmark.VecVsPair
     */
    benchmarkRule.measureRepeated {
      Pair(State.IDLE, FsmEvent.ADD_EVENT)
//      v(State.IDLE, FsmEvent.ADD_EVENT)
    }
  }
}
