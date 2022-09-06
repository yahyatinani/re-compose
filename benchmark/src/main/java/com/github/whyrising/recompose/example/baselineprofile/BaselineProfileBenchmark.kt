package com.github.whyrising.recompose.example.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileBenchmark {
  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  private fun startup(compilationMode: CompilationMode) {
    benchmarkRule.measureRepeated(
      packageName = "com.github.whyrising.recompose.example",
      metrics = listOf(StartupTimingMetric()),
      iterations = 10,
      startupMode = StartupMode.COLD,
      compilationMode = compilationMode
    ) { // this = MacrobenchmarkScope
      pressHome()
      startActivityAndWait()
    }
  }

  @Test
  fun startupNoCompilation() {
    startup(CompilationMode.None())
  }

  @Test
  fun startupBaselineProfile() {
    startup(
      CompilationMode.Partial(
        baselineProfileMode = BaselineProfileMode.Require
      )
    )
  }
}

// BaselineProfileBenchmark_startupNoCompilation
// timeToInitialDisplayMs   min 1,150.3,   median 1,207.9,   max 1,239.9
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// BaselineProfileBenchmark_startupBaselineProfile
// timeToInitialDisplayMs   min   974.7,   median 1,013.7,   max 1,061.6
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9

// BaselineProfileBenchmark_startupNoCompilation
// timeToInitialDisplayMs   min 1,128.2,   median 1,203.0,   max 1,259.5
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// BaselineProfileBenchmark_startupBaselineProfile
// timeToInitialDisplayMs   min 815.8,   median 874.9,   max 912.5
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
