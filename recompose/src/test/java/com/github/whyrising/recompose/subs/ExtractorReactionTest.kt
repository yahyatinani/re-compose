package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.atom
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ExtractorReactionTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()
  Dispatchers.setMain(testDispatcher)

  "ctor" {
    val reaction = Extraction(atom(1)) { (it as Int).inc() }

    reaction.deref() shouldBe 2
  }

  "deref()" {
    runTest {
      val reaction = Extraction(atom(1)) { (it as Int).inc() }

      reaction.deref() shouldBe 2
      reaction.deref() shouldBe reaction.deref()
    }
  }

  "collect()" {
    runTest {
      val db = atom(0)
      var result: Int? = null
      val inputSignal = Extraction(db, testDispatcher) { (it as Int).inc() }

      val job: Job = launch { inputSignal.collect { result = it as Int } }
      launch { db.reset(4) }

      advanceUntilIdle()

      result shouldBe 5
      job.cancel()
    }
  }
})
