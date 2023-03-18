package com.github.whyrising.recompose.subs

import com.github.whyrising.recompose.db.RAtom
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ExtractorReactionTest : FreeSpec({
  val dispatcher = StandardTestDispatcher()
  Dispatchers.setMain(dispatcher)

  "ctor" {
    val reaction = Extraction(RAtom(1)) { (it as Int).inc() }

    reaction.deref() shouldBe 2
  }

  "deref()" {
    runTest {
      val reaction = Extraction(RAtom(1)) { (it as Int).inc() }

      val value = reaction.deref()

      value shouldBe 2
      reaction._state.value shouldBe value
      reaction.state.value shouldBe value
    }
  }

  "computationJob" {
    runTest {
      val inputSignal = RAtom(0)
      val reaction = Extraction(inputSignal as Reaction<Any?>) {
        (it as Int).inc()
      }

      reaction.signalObserver // launch the flow.
      launch { inputSignal.emit(4) }
      advanceUntilIdle()

      reaction._state.value shouldBe 5
    }
  }

  "collect()" {
    runTest {
      val db = RAtom(0)
      val inputSignal = Extraction(db as Reaction<Any?>) {
        (it as Int).inc()
      }
      val reaction = Extraction(inputSignal as Reaction<Any?>) {
        (it as Int).inc().toString()
      }
      reaction.signalObserver // launch the flow.

      launch { db.emit(4) }
      advanceUntilIdle()

      reaction._state.value shouldBe "6"
    }
  }
})
