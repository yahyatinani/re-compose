package com.github.whyrising.recompose.subs

import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.Associative
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.get
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
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class)
class ComputationTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()
  Dispatchers.setMain(testDispatcher)

  afterTest {
    testDispatcher.cancel()
  }

  "ctor" {
    val defaultVal = 0

    val id = "id"
    val r = Computation(v(), defaultVal, id) { _, _ -> defaultVal }

    r.isFresh.deref().shouldBeTrue()
    r.toString() shouldBe "rx($id, ${0})"
    r.state.value shouldBe defaultVal
    r.initialValue shouldBe defaultVal
  }

  "deref() should return the computation value of the reaction" {
    val defaultVal = 0
    val reaction = Computation(v(), defaultVal, "") { _, _ -> defaultVal }

    reaction.deref() shouldBe defaultVal
  }

  "collect(action)" {
    continually(10.seconds) {
      runTest {
        val appDb = atom(0)
        val input1 = Computation(
          inputSignals = v(Extraction(appDb, testDispatcher) { it }),
          initial = -1,
          id = "input1",
          context = testDispatcher
        ) { vec, _ -> (vec as IPersistentVector<Int>)[0] }
        val input2 = Computation(
          inputSignals = v(input1),
          initial = -1,
          id = "input2",
          context = testDispatcher
        ) { args, _ ->
          args as IPersistentVector<Int>
          inc(args[0])
        }
        val reaction = Computation(
          inputSignals = v(input2),
          initial = "-1",
          id = "reaction",
          context = testDispatcher
        ) { args, _ ->
          args as IPersistentVector<Int>
          "${inc(args[0])}"
        }
        appDb.reset(5)

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
        val computation = Computation(
          inputSignals = v(Extraction(atom(0)) { 0 }),
          initial = -1,
          id = "computation",
          context = testDispatcher
        ) { _, _ -> 0 }
        val subscriber = Computation(
          inputSignals = v(computation),
          initial = -1,
          id = "subscriber",
          context = testDispatcher
        ) { _, _ -> 0 }
        subscriber.addOnDispose { isDisposed = true }

        advanceUntilIdle()

        val result = computation.dispose()

        computation._state.subscriptionCount.value shouldBeGreaterThan 0
        result.shouldBeFalse()
        isDisposed.shouldBeFalse()
        computation.reactionScope.isActive.shouldBeTrue()
      }
    }

    "when computation is still fresh a.k.a no subscribers yet, skip" {
      runTest {
        val reaction = Computation(
          inputSignals = v(Extraction(atom(0)) { 0 }),
          initial = -1,
          id = "reaction",
          context = testDispatcher
        ) { _, _ -> 0 }
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
        val reaction = Computation(
          inputSignals = v(Extraction(atom(0)) { 0 }),
          initial = -1,
          id = "reaction",
          context = testDispatcher
        ) { _, _ -> 0 }

        val subscriber = Computation(
          inputSignals = v(reaction),
          initial = -1,
          id = "subscriber",
          context = testDispatcher
        ) { _, _ -> 0 }
        val j = launch { subscriber.collect {} }
        advanceUntilIdle()
        reaction.addOnDispose { isSubscriberDisposed = true }
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
        val appDb = atom(0)
        val input = Computation(
          inputSignals = v(Extraction(appDb) { it }),
          initial = -1,
          id = "input",
          context = testDispatcher
        ) { args, _ -> (args as IPersistentVector<Int>)[0] }
        val r = Computation(
          inputSignals = v(input),
          initial = -1,
          id = "r",
          context = testDispatcher
        ) { args, _ -> inc((args as IPersistentVector<Int>)[0]) }
        appDb.reset(5)

        advanceUntilIdle()

        r.deref() shouldBe 6
      }
    }

    "multiple input signals" {
      runTest {
        val in1 = Extraction(atom(0)) { it }
        val in2 = Extraction(atom(0)) { it }

        val node = Computation(
          inputSignals = v(in1, in2),
          initial = -1,
          id = "node",
          context = testDispatcher
        ) { args, _ ->
          val (a, b) = args as IPersistentVector<Int>
          inc(a + b)
        }

        in1.value = 3
        in2.value = 5
        advanceUntilIdle()

        node.deref() shouldBe 9
      }
    }

    "parallel incoming input signals" {
      continually(duration = 10.seconds) {
        runTest {
          val appDb: Atom<Associative<String, Int>> =
            atom(m("a" to 0, "b" to 0))
          val e1 = Extraction(appDb) { get(it, "a") }
          val e2 = Extraction(appDb) { get(it, "b") }
          val input1 = Computation(
            inputSignals = v(e1),
            initial = -1,
            id = "input1",
            context = testDispatcher
          ) { args, _ ->
            (args as IPersistentVector<Int>)[0]
          }
          val input2 = Computation(
            inputSignals = v(e2),
            initial = -1,
            id = "input2",
            context = testDispatcher
          ) { args, _ ->
            (args as IPersistentVector<Int>)[0]
          }
          val reaction = Computation(
            inputSignals = v(input1, input2),
            initial = -1,
            id = "reaction",
            context = testDispatcher
          ) { args, _ ->
            val (a, b) = args as IPersistentVector<Int>
            inc(a + b)
          }
          repeat(100) {
            launch {
              repeat(100) {
                appDb.swap { it.assoc("a", get<Int>(it, "a")!!.inc()) }
              }
            }
          }
          repeat(100) {
            launch {
              repeat(100) {
                appDb.swap { it.assoc("b", get<Int>(it, "b")!!.inc()) }
              }
            }
          }
          advanceUntilIdle()

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
        id = "",
        context = testDispatcher
      ) { _: Any?, _ -> null }.deref() shouldBe -34

      Computation(
        inputSignals = v(),
        initial = null,
        id = "",
        context = testDispatcher
      ) { _, _ -> }.deref() shouldBe null
    }

    "computation value should be the previous one" {
      runTest {
        val appDb = atom(4)
        val input = Computation(
          inputSignals = v(Extraction(appDb, testDispatcher) { it }),
          context = testDispatcher,
          id = "input",
          initial = 7
        ) { args, _ -> (args as IPersistentVector<Int>)[0] }
        val r = Computation(
          inputSignals = v(input),
          id = "r",
          context = testDispatcher,
          initial = -1
        ) { args, currentValue ->
          val x = (args as IPersistentVector<*>)[0] as Int
          if (x < 40) inc(x) else currentValue
        }
        advanceUntilIdle()
        input._state.emit(30)
        advanceUntilIdle()
        val previousValue = r.deref()

        input._state.emit(60)
        advanceUntilIdle()

        val current = r.deref()

        current shouldBeSameInstanceAs previousValue
      }
    }
  }

  "signalObserver" - {
    "should filter out duplicate values via distinctUntilChanged" {
      runTest {
        val callCount = atomic(0)
        val input1 = Channel<Int>()
        val input2 = Channel<Int>()
        val flow1 = input1.receiveAsFlow()
        val flow2 = input2.receiveAsFlow()
        Computation(
          inputSignals = v(flow1, flow2),
          initial = -1,
          id = "computation",
          context = testDispatcher
        ) { _, _ -> callCount.update { it.inc() } }
        advanceUntilIdle()
        input1.send(2)
        input2.send(4)

        input1.send(2)
        input2.send(4)

        input1.send(2)
        input2.send(4)

        input2.send(2)
        input1.send(4)
        advanceUntilIdle()

        callCount.value shouldBeExactly 3
      }
    }
  }
})
