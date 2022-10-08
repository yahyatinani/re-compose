package com.github.whyrising.recompose

import androidx.compose.runtime.mutableStateOf
import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.recompose.subs.ComputationReaction
import com.github.whyrising.recompose.subs.ExtractorReaction
import com.github.whyrising.recompose.subs.Ids.computation_value
import com.github.whyrising.recompose.subs.Ids.signals_value
import com.github.whyrising.recompose.subs.ReactionBase
import com.github.whyrising.recompose.subs.deref
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
    "default values with initial" {
      val defaultVal = 0

      val reaction = ComputationReaction(
        inputSignals = v(),
        context = testDispatcher,
        initial = defaultVal
      ) { _: IPersistentVector<Int>, _ -> defaultVal }

      reaction.isFresh.deref().shouldBeTrue()
      reaction.id shouldBe "rx${reaction.hashCode()}"
      reaction.state.value shouldBe m(computation_value to defaultVal)
    }

    "when initial's null, calculate the first value" {
      val reaction = ComputationReaction<Int, Int>(
        inputSignals = v(ExtractorReaction(RAtom(93)) { it }),
        context = testDispatcher,
        initial = null
      ) { (i), _ ->
        i + 1
      }

      reaction.isFresh.deref().shouldBeTrue()
      reaction.id shouldBe "rx${reaction.hashCode()}"
      reaction.state.value shouldBe m(
        signals_value to v(93),
        computation_value to 94
      )
    }
  }

  "deref() should return the computation value of the reaction" {
    val defaultVal = 0
    val f = { _: IPersistentVector<Int>, _: Int? -> defaultVal }
    val reaction = ComputationReaction(
      inputSignals = v(),
      initial = defaultVal,
      context = testDispatcher,
      f = f
    )

    reaction.deref() shouldBe defaultVal
  }

  "deref(state) should return the computation value of the reaction" {
    val defaultVal = 0
    val f = { _: IPersistentVector<Int>, _: Int? -> defaultVal }
    val state = mutableStateOf(m(signals_value to 2, computation_value to "2"))
    val reaction = ComputationReaction(
      inputSignals = v(),
      context = testDispatcher,
      initial = defaultVal,
      f = f
    )

    reaction.deref(state) shouldBe "2"
  }

  "collect(action) should return the computation value of the reaction" {
    continually(10.seconds) {
      runTest {
        val input1 = ExtractorReaction(RAtom(0), testDispatcher) { 0 }
        advanceUntilIdle()
        val input2 = ComputationReaction(
          inputSignals = v(input1),
          initial = -1,
          context = testDispatcher
        ) { args, _ ->
          inc(args[0])
        }
        advanceUntilIdle()
        val r2 = ComputationReaction(
          inputSignals = v(input2),
          context = testDispatcher,
          initial = "-1"
        ) { args, _ ->
          "${inc(args[0])}"
        }
        advanceUntilIdle()

        input1.state.emit(5)
        advanceUntilIdle()

        input2.deref() shouldBe 6
        r2.deref() shouldBe "7"
      }
    }
  }

  "addOnDispose(f)" {
    val reaction = ComputationReaction<Int, Int>(
      inputSignals = v(),
      initial = -1,
      context = testDispatcher
    ) { _, _ -> 1 }
    val f: (ReactionBase<*, *>) -> Unit = { }

    reaction.addOnDispose(f)

    reaction.disposeFns.deref() shouldBe l(f)
  }

  "dispose()" - {
    """should call all functions in disposeFns and cancel reactionScope""" {
      var isDisposed = false
      val reaction = ComputationReaction<Int, Int>(
        inputSignals = v(),
        initial = -1,
        context = testDispatcher
      ) { _, _ -> 0 }
      val f: (ReactionBase<*, *>) -> Unit = { isDisposed = true }
      reaction.addOnDispose(f)
      reaction.state.value

      val b = reaction.dispose()

      b.shouldBeTrue()
      isDisposed.shouldBeTrue()
      reaction.reactionScope.isActive.shouldBeFalse()
    }

    "when no dispose functions added and scope not active, skip " {
      val reaction = ComputationReaction<Int, Int>(
        inputSignals = v(),
        initial = -1,
        context = testDispatcher
      ) { _, _ -> 0 }

      reaction.dispose()

      reaction.reactionScope.isActive.shouldBeFalse()
    }
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
          initial = initial
        ) { (a, b), _ ->
          a.inc() + b.inc()
        }

        advanceUntilIdle()

        reaction.deref() shouldBeExactly 5

        reaction.recompute(-5, 0)

        reaction.deref() shouldBe -1
        reaction.state.value shouldBe m(
          computation_value to -1,
          signals_value to v(-5, 2)
        )
      }
    }

    "when the same arg passed it should not recompute" {
      runTest {
        val initial = -1
        val r1 = ComputationReaction<Int, Int>(
          inputSignals = v(),
          context = testDispatcher,
          initial = initial
        ) { _, _ -> 1 }
        val r2 = ComputationReaction<Int, Int>(
          inputSignals = v(),
          context = testDispatcher,
          initial = initial
        ) { _, _ -> 2 }
        val reaction = ComputationReaction(
          inputSignals = v(r1, r2),
          context = testDispatcher,
          initial = initial
        ) { (a, b), _ ->
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
        inputSignals = v(),
        initial = initial,
        context = testDispatcher
      ) { _, _ -> 1 }
      val reaction2 = ComputationReaction<Int, Int>(
        inputSignals = v(),
        initial = initial,
        context = testDispatcher
      ) { _, _ -> 2 }
      val reaction3 = ComputationReaction<Int, Int>(
        inputSignals = v(),
        initial = initial,
        context = testDispatcher
      ) { _, _ -> 3 }

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
          initial = -1,
          context = testDispatcher
        ) { args, _ ->
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
          initial = -1,
          context = testDispatcher
        ) { (a, b), _ ->
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
            initial = -1,
            context = standardTestDispatcher
          ) { (a, b), _ ->
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

  "f()" - {
    "computation value should be the initial value" {
      val reaction = ComputationReaction(
        inputSignals = v(),
        initial = -34,
        context = testDispatcher
      ) { _: IPersistentVector<Int>, oldComp: Int? ->
        oldComp
      }

      reaction.deref() shouldBe -34
    }

    "computation value should be null with no initial value provided" {
      val reaction = ComputationReaction(
        inputSignals = v(),
        initial = null,
        context = testDispatcher
      ) { _: IPersistentVector<Int>, oldComp: Int? ->
        oldComp
      }

      reaction.deref() shouldBe null
    }

    "computation value should be the previous one" {
      runTest {
        val input = ExtractorReaction(RAtom(0), testDispatcher) { 0 }
        advanceUntilIdle()
        val reaction = ComputationReaction(
          inputSignals = v(input),
          initial = -1,
          context = testDispatcher
        ) { args, oldComp ->
          val x = args[0]
          if (x < 1) inc(x)
          else oldComp!!
        }
        advanceUntilIdle()
        val oldComp = reaction.deref()
        oldComp shouldBe 1

        input.state.emit(2)
        advanceUntilIdle()

        reaction.deref() shouldBe oldComp
      }
    }
  }
})
