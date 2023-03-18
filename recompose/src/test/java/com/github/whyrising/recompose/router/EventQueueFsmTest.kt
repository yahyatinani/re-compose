package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.router.FsmEvent.ADD_EVENT
import com.github.whyrising.recompose.router.FsmEvent.RUN_QUEUE
import com.github.whyrising.recompose.router.State.IDLE
import com.github.whyrising.recompose.router.State.RUNNING
import com.github.whyrising.recompose.router.State.SCHEDULING
import com.github.whyrising.y.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.framework.concurrency.continually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class EventQueueFsmTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()
  Dispatchers.setMain(testDispatcher)

  "initial state of FSM" {
    val eventQueue = EventQueueImp()
    val eventQueueFSM = EventQueueFSM(eventQueue)

    eventQueue.queue.isEmpty() shouldBe true
    eventQueueFSM.state shouldBe IDLE
  }

  "Given we're in IDLE state" - {
    "when ADD_EVENT event, set state to SCHEDULING, enqueue and run" {
      runTest {
        val eventQueue = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          dispatcher = testDispatcher
        )
        var state1: State? = null
        var isEventQueued = false
        eventQueueFSM._state.addWatch("scheduling") { _, _, _, new ->
          if (new == SCHEDULING) state1 = new
        }
        eventQueue._eventQueueRef.addWatch("count") { _, _, _, new ->
          if (new.count > 0) isEventQueued = true
        }

        eventQueueFSM.handle(ADD_EVENT, v("new-event"))
        advanceUntilIdle()

        state1 shouldBe SCHEDULING
        isEventQueued shouldBe true
        eventQueueFSM.state shouldBe IDLE
        eventQueueFSM.eventQueue.count shouldBe 0
      }
    }
  }

  "Given we're in SCHEDULING state" - {
    "when ADD_EVENT event, stay in SCHEDULING state and enqueue the new event" {
      runTest {
        val eventQueue = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          start = SCHEDULING,
          dispatcher = testDispatcher
        )

        eventQueueFSM.handle(ADD_EVENT, v("new-event"))
        eventQueueFSM.handle(ADD_EVENT, v("new-event"))
        advanceUntilIdle()

        eventQueueFSM.eventQueue.count shouldBe 2
        eventQueueFSM.state shouldBe SCHEDULING
      }
    }

    "when RUN_QUEUE event, go to RUNNING state and run queue" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          start = SCHEDULING,
          dispatcher = testDispatcher
        )
        var state1: State? = null
        eventQueueFSM._state.addWatch("running") { _, _, _, new ->
          if (new == RUNNING) state1 = new
        }
        eventQueueFSM.handle(RUN_QUEUE)
        advanceUntilIdle()

        state1 shouldBe RUNNING
        eventQueueFSM.eventQueue.count shouldBe 0
        eventQueueFSM.state shouldBe IDLE
      }
    }
  }

  "Given we're in RUNNING state" - {
    "when EXCEPTION event happens, then go to IDLE and call exception()" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          start = RUNNING,
          dispatcher = testDispatcher
        )

        shouldThrowExactly<RuntimeException> {
          eventQueueFSM.handle(FsmEvent.EXCEPTION, RuntimeException())
        }
        eventQueueFSM.state shouldBe IDLE
        eventQueue.queue.isEmpty() shouldBe true
      }
    }

    "when ADD_EVENT event, stay in RUNNING and enqueue that event" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          start = RUNNING,
          dispatcher = testDispatcher
        )

        eventQueueFSM.handle(ADD_EVENT, v("new-event"))
        advanceUntilIdle()

        eventQueueFSM.state shouldBe RUNNING
        eventQueueFSM.eventQueue.count shouldBe 3
      }
    }

    "when FINISH_RUN event and queue is empty, then go IDLE state" {
      runTest {
        val eventQueue = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          start = RUNNING,
          dispatcher = testDispatcher
        )

        eventQueueFSM.handle(FsmEvent.FINISH_RUN)
        advanceUntilIdle()

        eventQueueFSM.state shouldBe IDLE
      }
    }

    "when FINISH_RUN event and queue is not empty, then run the queue" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueue,
          start = RUNNING,
          dispatcher = testDispatcher
        )
        var state1: State? = null
        eventQueueFSM._state.addWatch("scheduling") { _, _, _, new ->
          if (new == SCHEDULING) state1 = new
        }

        eventQueueFSM.handle(FsmEvent.FINISH_RUN)
        advanceUntilIdle()

        state1 shouldBe SCHEDULING
        eventQueueFSM.state shouldBe IDLE
        eventQueueFSM.eventQueue.count shouldBe 0
      }
    }
  }

  "processAllCurrentEvents()" - {
    "should process all current events in EventQueue" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING)

        eventQueueFSM.processAllCurrentEvents(null)

        advanceUntilIdle()

        eventQueueFSM.eventQueue.count shouldBe 0
      }
    }

    "should throw an exception" {
      regEventDb<Any>("ex-event") { _, _ ->
        throw IllegalStateException("test")
      }
      continually(5.seconds) {
        runTest {
          val eventQueue = EventQueueImp().apply { enqueue(v("ex-event")) }
          val eventQueueFSM = EventQueueFSM(
            eventQueue = eventQueue,
            start = RUNNING,
            dispatcher = testDispatcher
          )

          shouldThrowExactly<IllegalStateException> {
            eventQueueFSM.processAllCurrentEvents(null)
          }
          eventQueueFSM.state shouldBe IDLE
        }
      }
    }
  }
})
