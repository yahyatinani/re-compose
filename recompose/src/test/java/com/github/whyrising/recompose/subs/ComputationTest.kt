package com.github.whyrising.recompose.subs

import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.recompose.multiThreadedRun
import com.github.whyrising.recompose.subs.Computation.Companion.Ids.computation_value
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
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ComputationTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()
  Dispatchers.setMain(testDispatcher)

  afterTest {
    testDispatcher.cancel()
  }

  "ctor" {
    val defaultVal = 0

    val reaction = Computation(v(), defaultVal) { _, _ -> defaultVal }

    reaction.isFresh.deref().shouldBeTrue()
    reaction.toString() shouldBe "rx(${reaction.hashCode()}, 0)"
    reaction.state.value shouldBe defaultVal
    reaction.initialValue shouldBe m(computation_value to defaultVal)
  }

  "deref() should return the computation value of the reaction" {
    val defaultVal = 0
    val reaction = Computation(v(), defaultVal) { _, _ -> defaultVal }

    reaction.deref() shouldBe defaultVal
  }

  "collect(action)" {
    continually(10.seconds) {
      runTest {
        val input1 = Extraction(RAtom(0)) { 0 }
        val input2 = Computation(
          inputSignals = v(input1),
          initial = -1,
          context = testDispatcher
        ) { args, _ ->
          args as IPersistentVector<Int>
          inc(args[0])
        }
        val reaction = Computation(
          inputSignals = v(input2),
          context = testDispatcher,
          initial = "-1"
        ) { args, _ ->
          args as IPersistentVector<Int>
          "${inc(args[0])}"
        }
        reaction.signalObserver
        advanceUntilIdle()

        input1._state.emit(5)
        advanceUntilIdle()

        input2.deref() shouldBe 6
        reaction.deref() shouldBe "7"
      }
    }
  }

  "addOnDispose(f)" {
    val reaction = Computation(v(), -1, testDispatcher) { _, _ -> 1 }
    val f: (Reaction<*>) -> Unit = {}

    reaction.addOnDispose(f)

    reaction.disposeFns.deref() shouldBe l(f)
  }

  "dispose()" - {
    "when computation still has subscribers, skip" {
      runTest {
        var isDisposed = false
        val reaction = Extraction(RAtom(0)) { 0 }
        val subscriber = Computation(
          inputSignals = v(reaction),
          initial = -1,
          context = testDispatcher
        ) { _, _ -> 0 }
        subscriber.addOnDispose { isDisposed = true }
        subscriber.signalObserver
        advanceUntilIdle()

        val result = reaction.dispose()

        reaction._state.subscriptionCount.value shouldBeGreaterThan 0
        result.shouldBeFalse()
        isDisposed.shouldBeFalse()
        reaction.reactionScope.isActive.shouldBeTrue()
      }
    }

    "when computation is still fresh a.k.a no subscribers yet, skip" {
      runTest {
        val reaction = Extraction(RAtom(0)) { 0 }
        advanceUntilIdle()

        val result = reaction.dispose()
        advanceUntilIdle()

        reaction.isFresh.deref().shouldBeTrue()
        reaction._state.subscriptionCount.value shouldBeExactly 0
        result.shouldBeFalse()
        reaction.reactionScope.isActive.shouldBeTrue()
      }
    }

    "should call all functions in disposeFns and cancel reactionScope" {
      runTest {
        var isSubscriberDisposed = false
        val reaction = Extraction(RAtom(0)) { 0 }
        val subscriber = Computation(v(reaction), -1, testDispatcher) { _, _ ->
          0
        }
        val j = launch { subscriber.collect {} }
        advanceUntilIdle()
        reaction.addOnDispose { isSubscriberDisposed = true }
        reaction.signalObserver
        subscriber.signalObserver
        advanceUntilIdle()

        j.cancel()
        advanceUntilIdle()
        val subscriberDisposed = subscriber.dispose()
        advanceUntilIdle()
        val reactionDisposed = reaction.dispose()
        advanceUntilIdle()

        subscriberDisposed.shouldBeTrue()
        subscriber.isFresh().shouldBeFalse()
        subscriber._state.subscriptionCount.value shouldBeExactly 0
        subscriber.reactionScope.isActive.shouldBeFalse()

        isSubscriberDisposed.shouldBeTrue()
        reactionDisposed.shouldBeTrue()
        reaction.isFresh().shouldBeFalse()
        reaction._state.subscriptionCount.value shouldBeExactly 0
        reaction.reactionScope.isActive.shouldBeFalse()
      }
    }
  }

  "input signals" - {
    "one input signal" {
      runTest {
        val input = Extraction(RAtom(0)) { 0 }
        val r = Computation(v(input), initial = -1, testDispatcher) { args, _ ->
          args as IPersistentVector<Int>
          inc(args[0])
        }
        r.signalObserver
        advanceUntilIdle()

        input._state.emit(5)

        advanceUntilIdle()

        r.deref() shouldBe 6
      }
    }

    "multiple input signals" {
      runTest {
        val input1 = Extraction(RAtom(0)) { 0 }
        val input2 = Extraction(RAtom(0)) { 0 }
        val node =
          Computation(v(input1, input2), -1, testDispatcher) { args, _ ->
            val (a, b) = args as IPersistentVector<Int>
            inc(a + b)
          }
        node.signalObserver
        advanceUntilIdle()

        input1._state.emit(3)
        input2._state.emit(5)
        advanceUntilIdle()

        node.deref() shouldBe 9
      }
    }

    "parallel incoming input signals" {
      continually(duration = 30.seconds) {
        runTest {
          val standardTestDispatcher = StandardTestDispatcher()
          val input1 = Extraction(RAtom(0)) { 0 }
          val input2 = Extraction(RAtom(0)) { 0 }
          val reaction = Computation(
            inputSignals = v(input1, input2),
            initial = -1,
            context = standardTestDispatcher
          ) { args, _ ->
            val (a, b) = args as IPersistentVector<Int>
            inc(a + b)
          }
          reaction.signalObserver
          advanceUntilIdle()

          multiThreadedRun(100, 100, standardTestDispatcher) {
            input1._state.update { (it as Int).inc() }
            advanceUntilIdle()
          }
          multiThreadedRun(100, 100, standardTestDispatcher) {
            input2._state.update { ((it as Int)).inc() }
            advanceUntilIdle()
          }

          input1.deref() shouldBe 10000
          input2.deref() shouldBe 10000
          reaction.deref() shouldBe 20001
        }
      }
    }
  }

  "f()" - {
    "computation value should be the initial value" {
      Computation(
        inputSignals = v(),
        initial = -34,
        context = testDispatcher
      ) { _: Any?, _ -> null }.deref() shouldBe -34

      Computation(
        inputSignals = v(),
        initial = null,
        context = testDispatcher
      ) { _, _ -> }.deref() shouldBe null
    }

    "computation value should be the previous one" {
      runTest {
        val input = Extraction(RAtom(0)) { 30 }
        val reaction = Computation(
          inputSignals = v(input),
          initial = -1,
          context = testDispatcher
        ) { args, currentValue ->
          val x = (args as IPersistentVector<*>)[0] as Int
          if (x < 40) {
            inc(x)
          } else {
            currentValue!!
          }
        }
        reaction.signalObserver // reaction value becomes 31 here.
        advanceUntilIdle()

        input._state.emit(60)
        advanceUntilIdle()

        reaction.deref() shouldBe 31
      }
    }
  }
})
