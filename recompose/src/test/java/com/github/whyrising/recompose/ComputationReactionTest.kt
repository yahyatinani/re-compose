package com.github.whyrising.recompose

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.recompose.subs.ComputationReaction
import com.github.whyrising.recompose.subs.ExtractorReaction
import com.github.whyrising.recompose.subs.ReactionBase
import com.github.whyrising.recompose.subs.deref
import com.github.whyrising.recompose.subs.inputsKey
import com.github.whyrising.recompose.subs.stateKey
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.inc
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.assertions.timing.continually
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ComputationReactionTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()
  Dispatchers.setMain(testDispatcher)

  afterTest {
    testDispatcher.cancel()
  }

  "ctor" - {
    "default values" {
      val defaultVal = 0

      val reaction = ComputationReaction(
        inputSignals = v(),
        context = testDispatcher,
        initial = defaultVal
      ) { _: IPersistentVector<Int> ->
        defaultVal
      }

      reaction.isFresh.deref().shouldBeTrue()
      reaction.id shouldBe "rx${reaction.hashCode()}"
      reaction.state.value shouldBe m(stateKey to defaultVal)
    }
  }

  "deref() should return the computation value of the reaction" {
    val defaultVal = 0
    val f = { _: IPersistentVector<Int> -> defaultVal }
    val reaction =
      ComputationReaction(v(), testDispatcher, initial = defaultVal, f = f)

    reaction.deref() shouldBe defaultVal
  }

  "deref(state) should return the computation value of the reaction" {
    val defaultVal = 0
    val f = { _: IPersistentVector<Int> -> defaultVal }
    val reaction =
      ComputationReaction(v(), testDispatcher, initial = defaultVal, f = f)

    reaction.deref(mutableStateOf(m(inputsKey to 2, stateKey to "2"))) shouldBe
      "2"
  }

  "collect(action) should return the computation value of the reaction" {
    runTest {
      val input1 = ExtractorReaction(RAtom(0)) { 0 }
      val input2 = ComputationReaction(
        inputSignals = v(input1),
        context = testDispatcher,
        initial = -1,
        context2 = testDispatcher
      ) { args ->
        inc(args[0])
      }
      val r2 = ComputationReaction(
        inputSignals = v(input2),
        context = testDispatcher,
        initial = -1,
        context2 = testDispatcher
      ) { args ->
        "${inc(args[0])}"
      }
      advanceUntilIdle()

      input1.state.emit(5)
      advanceUntilIdle()

      input2.deref() shouldBe 6
      r2.deref() shouldBe "7"
    }
  }

  "addOnDispose(f)" {
    val reaction =
      ComputationReaction(
        v(),
        testDispatcher,
        -1
      ) { _: IPersistentVector<Int> -> 1 }
    val f: (ReactionBase<*, *>) -> Unit = { }

    reaction.addOnDispose(f)

    reaction.disposeFns.deref() shouldBe l(f)
  }

  "dispose()" - {
    """should call all functions in disposeFns atom and cancel the
       viewModelScope""" {
      var isDisposed = false
      val reaction =
        ComputationReaction<Int, Int>(v(), testDispatcher, -1) { 0 }
      val f: (ReactionBase<*, *>) -> Unit = { isDisposed = true }
      reaction.addOnDispose(f)
      reaction.state.value

      reaction.dispose()

      isDisposed.shouldBeTrue()
      reaction.viewModelScope.isActive.shouldBeFalse()
    }

    "when no dispose functions added and scope not active, skip " {
      val reaction =
        ComputationReaction<Int, Int>(v(), testDispatcher, -1) { 0 }

      reaction.dispose()

      reaction.viewModelScope.isActive.shouldBeFalse()
    }
  }

  "onCleared() should call dispose" {
    var isDisposed = false
    val reaction =
      ComputationReaction<Int, Int>(v(), testDispatcher, -1) { 0 }
    val f: (ReactionBase<*, *>) -> Unit = { isDisposed = true }
    reaction.addOnDispose(f)
    reaction.state.value

    reaction.onCleared()

    isDisposed.shouldBeTrue()
    reaction.viewModelScope.isActive.shouldBeFalse()
  }

  "recompute()" - {
    "should recompute the reaction using the new arg" {
      runTest {
        val initial = -1
        val input1 = ExtractorReaction(RAtom(1)) { it }
        val input2 = ExtractorReaction(RAtom(2)) { it }
        val reaction = ComputationReaction(
          inputSignals = v(input1, input2),
          context = testDispatcher,
          context2 = testDispatcher,
          initial = initial
        ) { (a, b) ->
          a.inc() + b.inc()
        }

        advanceUntilIdle()

        reaction.deref() shouldBeExactly 5

        reaction.recompute(-5, 0)

        reaction.deref() shouldBe -1
        reaction.state.value shouldBe m(
          stateKey to -1,
          inputsKey to v(-5, 2)
        )
      }
    }

    "when the same arg passed it should not recompute" {
      runTest {
        val initial = -1
        val r1 = ComputationReaction<Int, Int>(
          inputSignals = v(),
          context = testDispatcher,
          initial = initial,
          context2 = testDispatcher
        ) { 1 }
        val r2 = ComputationReaction<Int, Int>(
          inputSignals = v(),
          context = testDispatcher,
          context2 = testDispatcher,
          initial = initial
        ) { 2 }
        val reaction = ComputationReaction(
          inputSignals = v(r1, r2),
          context = testDispatcher,
          initial = initial,
          context2 = testDispatcher
        ) { (a, b) ->
          a.inc() + b.inc()
        }
        advanceUntilIdle()

        reaction.deref() shouldBe 5

        reaction.recompute(1, 0)

        reaction.deref() shouldBe 5
      }
    }
  }

  "deref(subscriptions) should return a vector of dereferenced reactions" {
    runTest {
      val initial = -1
      val reaction1 = ComputationReaction<Int, Int>(
        v(),
        testDispatcher,
        initial,
        context2 = testDispatcher
      ) { 1 }
      val reaction2 = ComputationReaction<Int, Int>(
        inputSignals = v(),
        context = testDispatcher,
        initial,
        context2 = testDispatcher
      ) { 2 }
      val reaction3 = ComputationReaction<Int, Int>(
        v(),
        testDispatcher,
        initial = initial,
        context2 = testDispatcher
      ) { 3 }

      advanceUntilIdle()

      deref(v(reaction1, reaction2, reaction3)) shouldBe v(1, 2, 3)
    }
  }

  "input signals" - {
    "one input signal" {
      runTest {
        val input = ExtractorReaction(RAtom(0)) { 0 }
        val r = ComputationReaction(
          inputSignals = v(input),
          context = testDispatcher,
          initial = -1,
          context2 = testDispatcher
        ) { args ->
          inc(args[0])
        }
        advanceUntilIdle()

        input.state.emit(5)

        advanceUntilIdle()

        r.deref() shouldBe 6
      }
    }

    "multiple input signals" {
      runTest {
        val input1 = ExtractorReaction(RAtom(0)) { 0 }
        val input2 = ExtractorReaction(RAtom(0)) { 0 }
        val node = ComputationReaction(
          inputSignals = v(input1, input2),
          context = testDispatcher,
          initial = -1,
          context2 = testDispatcher
        ) { (a, b) ->
          inc(a + b)
        }
        advanceUntilIdle()

        input1.state.emit(3)
        input2.state.emit(5)
        advanceUntilIdle()

        node.deref() shouldBe 9
      }
    }

    "parallel incoming input signals" {
      continually(duration = 60.seconds) {
        runTest {
          val standardTestDispatcher = StandardTestDispatcher()
          val input1 = ExtractorReaction(RAtom(0)) { 0 }
          val input2 = ExtractorReaction(RAtom(0)) { 0 }
          val r = ComputationReaction(
            inputSignals = v(input1, input2),
            context = standardTestDispatcher,
            initial = -1,
            context2 = standardTestDispatcher
          ) { (a, b) ->
            inc(a + b)
          }

          multiThreadedRun(100, 100, standardTestDispatcher) {
            input1.state.update { it.inc() }
          }
          multiThreadedRun(100, 100, standardTestDispatcher) {
            input2.state.update { it.inc() }
          }
          advanceUntilIdle()

          input1.deref() shouldBeExactly 10000
          input2.deref() shouldBeExactly 10000
          r.deref() shouldBeExactly 20001
        }
      }
    }
  }
})
