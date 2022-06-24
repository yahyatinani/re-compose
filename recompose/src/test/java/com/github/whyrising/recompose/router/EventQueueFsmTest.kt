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
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class EventQueueFsmTest : FreeSpec({
  Dispatchers.setMain(StandardTestDispatcher())

  "initial state of FSM" {
    val eventQueue = EventQueueImp()
    val eventQueueFSM = EventQueueFSM(eventQueue)

    eventQueue.queue.isEmpty() shouldBe true
    eventQueueFSM.state shouldBe IDLE
  }

  "Given we're in IDLE state" - {
    "when ADD_EVENT event, then go to SCHEDULING state and enqueue and run" {
      runTest {
        val eventQueue = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(eventQueue)

        eventQueueFSM.handle(ADD_EVENT, v("new-event"))

        eventQueueFSM.eventQueue.count shouldBe 1
        eventQueueFSM.state shouldBe SCHEDULING
        advanceUntilIdle() // wait for the queue to be exhausted.
        eventQueueFSM.state shouldBe IDLE
        eventQueueFSM.eventQueue.count shouldBe 0
      }
    }
  }

  "Given we're in SCHEDULING state" - {
    "when ADD_EVENT event, then stay in SCHEDULING state and enqueue event" {
      runTest {
        val eventQueue = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(eventQueue, SCHEDULING)

        eventQueueFSM.handle(ADD_EVENT, v("new-event"))
        eventQueueFSM.handle(ADD_EVENT, v("new-event"))

        eventQueueFSM.eventQueue.count shouldBe 2
        eventQueueFSM.state shouldBe SCHEDULING
      }
    }

    "when RUN_QUEUE event, then go to RUNNING state and run queue" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(eventQueue, SCHEDULING)

        eventQueueFSM.handle(RUN_QUEUE)

        eventQueueFSM.state shouldBe RUNNING
        advanceUntilIdle()
        eventQueueFSM.eventQueue.count shouldBe 0
        eventQueueFSM.state shouldBe IDLE
      }
    }
  }

  "Given we're in RUNNING state" - {
    "when EXCEPTION event happens, then go to IDLE and call exception()" {
      val eventQueue = EventQueueImp().apply {
        enqueue(v(":event1", "arg"))
        enqueue(v(":event2", "arg"))
      }
      val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING)

      shouldThrowExactly<RuntimeException> {
        eventQueueFSM.handle(FsmEvent.EXCEPTION, RuntimeException())
      }

      eventQueueFSM.state shouldBe IDLE
      eventQueue.queue.isEmpty() shouldBe true
    }

    """when ADD_EVENT event, then stay in RUNNING and enqueue that event in the
       EventQueue""" {
      val eventQueue = EventQueueImp().apply {
        enqueue(v(":event1", "arg"))
        enqueue(v(":event2", "arg"))
      }
      val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING)

      eventQueueFSM.handle(ADD_EVENT, v("new-event"))

      eventQueueFSM.state shouldBe RUNNING
      eventQueueFSM.eventQueue.count shouldBe 3
    }

    "when FINISH_RUN event and queue is empty, then go IDLE state" {
      val eventQueue = EventQueueImp()
      val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING)

      eventQueueFSM.handle(FsmEvent.FINISH_RUN)

      eventQueueFSM.state shouldBe IDLE
    }

    "when FINISH_RUN event and queue is not empty, then run the queue" {
      runTest {
        val eventQueue = EventQueueImp().apply {
          enqueue(v(":event1", "arg"))
          enqueue(v(":event2", "arg"))
          enqueue(v(":event2", "arg"))
        }
        val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING)

        eventQueueFSM.handle(FsmEvent.FINISH_RUN)

        eventQueueFSM.state shouldBe SCHEDULING
        advanceUntilIdle() // wait for the queue to be exhausted.
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
      regEventDb<Any>("ex-event") { _, _ -> throw IllegalStateException() }
      val eventQueue = EventQueueImp().apply {
        enqueue(v("ex-event"))
      }

      runTest {
        val eventQueueFSM = EventQueueFSM(eventQueue, RUNNING)

        eventQueueFSM.processAllCurrentEvents(null)

        advanceUntilIdle()
        eventQueueFSM.state shouldBe IDLE
      }
    }
  }
})
