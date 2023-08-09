package io.github.yahyatinani.recompose.subs

import io.github.yahyatinani.y.concurrency.atom
import io.github.yahyatinani.y.core.collections.Associative
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
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
    r.toString() shouldBe "rxc($id)"
    r.stateFlow.value shouldBe defaultVal
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
          args[0].inc()
        }
        val reaction = Computation(
          inputSignals = v(input2),
          initial = "-1",
          id = "reaction",
          context = testDispatcher
        ) { args, _ ->
          args as IPersistentVector<Int>
          "${args[0].inc()}"
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

    reaction.disposeFns.deref().first() shouldBeSameInstanceAs f
  }

  "dispose()" - {
    "when reaction still has subscribers, skip" {
      runTest {
        var isDisposed = false
        val computation = Computation(
          inputSignals = v(Extraction(atom(0), "Extraction") { 0 }),
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

        computation._state.value.subscriptionCount.value shouldBeGreaterThan 0
        result.shouldBeFalse()
        isDisposed.shouldBeFalse()
        computation.reactionScope.isActive.shouldBeTrue()
      }
    }

    "when reaction is active, skip" {
      runTest {
        val reaction = Computation(
          inputSignals = v(Extraction(atom(0), "Extraction") { 0 }),
          initial = -1,
          id = "reaction",
          context = testDispatcher
        ) { _, _ -> 0 }
        reaction.incUiSubCount()
        advanceUntilIdle()

        val isDisposed = reaction.dispose()
        advanceUntilIdle()

        isDisposed.shouldBeFalse()
        reaction.isDisposed.deref().shouldBeFalse()
        reaction.isFresh.deref().shouldBeTrue()
        reaction._state.value.subscriptionCount.value shouldBeExactly 0
        reaction.reactionScope.isActive.shouldBeTrue()
      }
    }

    "should call all functions in disposeFns and cancel reactionScope" {
      runTest {
        var isSubscriberDisposed = false
        val reaction = Computation(
          inputSignals = v(Extraction(atom(0), "Extraction") { 0 }),
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

        subscriber.isDisposed.deref().shouldBeTrue()
        subscriber.isFresh.deref().shouldBeFalse()
        subscriber._state.value.subscriptionCount.value shouldBeExactly 0
        subscriber.reactionScope.isActive.shouldBeFalse()

        reaction.isDisposed.deref().shouldBeTrue()
        isSubscriberDisposed.shouldBeTrue()
        reaction.isFresh.deref().shouldBeFalse()
        reaction._state.value.subscriptionCount.value shouldBeExactly 0
        reaction.reactionScope.isActive.shouldBeFalse()
      }
    }
  }

  "input signals" - {
    "one input signal" {
      runTest {
        val appDb = atom(0)
        val input = Computation(
          inputSignals = v(Extraction(appDb, "Extraction") { it }),
          initial = -1,
          id = "input",
          context = testDispatcher
        ) { args, _ -> (args as IPersistentVector<Int>)[0] }
        val r = Computation(
          inputSignals = v(input),
          initial = -1,
          id = "r",
          context = testDispatcher
        ) { args, _ -> (args as IPersistentVector<Int>)[0].inc() }
        appDb.reset(5)

        advanceUntilIdle()

        r.deref() shouldBe 6
      }
    }

    "multiple input signals" {
      runTest {
        val in1 = Extraction(atom(0), "Extraction") { it }
        val in2 = Extraction(atom(0), "Extraction") { it }

        val node = Computation(
          inputSignals = v(in1, in2),
          initial = -1,
          id = "node",
          context = testDispatcher
        ) { args, _ ->
          val (a, b) = args as IPersistentVector<Int>
          (a + b).inc()
        }

        in1.state = 3
        in2.state = 5
        advanceUntilIdle()

        node.deref() shouldBe 9
      }
    }

    "parallel input signals" {
      continually(duration = 7.seconds) {
        runTest {
          val appDb = atom<Associative<String, Int>>(m("a" to 0, "b" to 0))
          val e1 = Extraction(appDb, "e1", testDispatcher) { get(it, "a") }
          val e2 = Extraction(appDb, "e2", testDispatcher) { get(it, "b") }
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
            (a + b).inc()
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
          if (x < 40) x.inc() else currentValue
        }
        advanceUntilIdle()
        input._state.value.emit(30)
        advanceUntilIdle()
        val previousValue = r.deref()

        input._state.value.emit(60)
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
