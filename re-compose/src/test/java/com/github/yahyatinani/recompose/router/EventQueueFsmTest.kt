package com.github.yahyatinani.recompose.router

import com.github.yahyatinani.recompose.regEventDb
import com.github.yahyatinani.recompose.router.FsmEvent.ADD_EVENT
import com.github.yahyatinani.recompose.router.FsmEvent.RUN_QUEUE
import com.github.yahyatinani.recompose.router.State.IDLE
import com.github.yahyatinani.recompose.router.State.RUNNING
import com.github.yahyatinani.recompose.router.State.SCHEDULING
import io.github.yahyatinani.y.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FreeSpec
import io.kotest.framework.concurrency.continually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKotest::class)
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
          context = testDispatcher
        )
        var state1: State? = null
        var isEventQueued = false
        eventQueueFSM._state.addWatch("scheduling") { _, _, _, new ->
          if (new == SCHEDULING) state1 = new
        }
        eventQueue._eventQueueRef.addWatch("count") { _, _, _, new ->
          if (new.count > 0) isEventQueued = true
        }

        eventQueueFSM.fsmTrigger(ADD_EVENT, v("new-event"))
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
          context = testDispatcher
        )

        eventQueueFSM.fsmTrigger(ADD_EVENT, v("new-event"))
        eventQueueFSM.fsmTrigger(ADD_EVENT, v("new-event"))
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
          context = testDispatcher
        )
        var state1: State? = null
        eventQueueFSM._state.addWatch("running") { _, _, _, new ->
          if (new == RUNNING) state1 = new
        }
        eventQueueFSM.fsmTrigger(RUN_QUEUE)
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
          context = testDispatcher
        )

        shouldThrowExactly<RuntimeException> {
          eventQueueFSM.fsmTrigger(FsmEvent.EXCEPTION, RuntimeException())
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
          context = testDispatcher
        )

        eventQueueFSM.fsmTrigger(ADD_EVENT, v("new-event"))
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
          context = testDispatcher
        )

        eventQueueFSM.fsmTrigger(FsmEvent.FINISH_RUN)
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
          context = testDispatcher
        )
        var state1: State? = null
        eventQueueFSM._state.addWatch("scheduling") { _, _, _, new ->
          if (new == SCHEDULING) state1 = new
        }

        eventQueueFSM.fsmTrigger(FsmEvent.FINISH_RUN)
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
        val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING, testDispatcher)

        eventQueueFSM.processAllCurrentEvents(null)

        advanceUntilIdle()

        eventQueueFSM.eventQueue.count shouldBe 0
      }
    }

    "should throw an exception with the event id that caused that exception" {
      val eventId = "ex-event"
      regEventDb<Any>(eventId) { _, _ ->
        throw IllegalStateException("test")
      }
      continually(5.seconds) {
        runTest {
          val eventQueue = EventQueueImp().apply { enqueue(v(eventId)) }
          val handler = CoroutineExceptionHandler { _, exception ->
            exception.shouldBeTypeOf<RuntimeException>()
            exception.message shouldBe "event: [$eventId]"
          }
          val eventQueueFSM = EventQueueFSM(
            eventQueue = eventQueue,
            start = RUNNING,
            context = testDispatcher + handler
          )

          eventQueueFSM.processAllCurrentEvents(null).join()

          eventQueueFSM.state shouldBe IDLE
        }
      }
    }
  }
})
