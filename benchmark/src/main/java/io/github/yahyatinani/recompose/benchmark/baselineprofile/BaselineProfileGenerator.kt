package io.github.yahyatinani.recompose.benchmark.baselineprofile

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  @Test
  fun generate() = baselineProfileRule.collectBaselineProfile(
    packageName = "io.github.yahyatinani.recompose.example"
  ) {
    pressHome()

    startActivityAndWait()
  }
}
