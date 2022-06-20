package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.events.Event
import com.github.whyrising.y.core.v
import io.kotest.assertions.timing.continually
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

class EventQueueFsmTest : FreeSpec({
  Dispatchers.setMain(RouterTest.testDispatcher)

  "EventQueueImp tests" - {
    "init state" {
      val eventQueue = EventQueueImp()

      eventQueue.queue.isEmpty() shouldBe true
    }

    "enqueue() should add given event to the queue" {
      val eventQueue = EventQueueImp()
      val testEvent: Event = v(":id", "arg")

      val queue = eventQueue.enqueue(testEvent)

      eventQueue.queue shouldBeSameInstanceAs queue
      eventQueue.queue.peek() shouldBe testEvent
    }

    "processEvents(count)" {
      val eventQueue = EventQueueImp()
      val testEvent1: Event = v(":event1", "arg")
      val testEvent2: Event = v(":event2", "arg")
      val testEvent3: Event = v(":event3", "arg")
      val testEvent4: Event = v(":event4", "arg")
      eventQueue.enqueue(testEvent1)
      eventQueue.enqueue(testEvent2)
      eventQueue.enqueue(testEvent3)
      eventQueue.enqueue(testEvent4)

//      eventQueue.processCurrentEvents(2)

      eventQueue.queue.size shouldBe 2
    }
//    "runAsync()" {
//      val eventQueue = EventQueueImp()
//      val testEvent: Event = v(":id", "arg")
//
//      eventQueue.runAsync()
//
//    }
  }

  "initial state of FSM" {
    val eventQueue = EventQueueImp()
    val eventQueueFSM = EventQueueFSM(eventQueue)

    eventQueue.queue.isEmpty() shouldBe true
    eventQueueFSM.state shouldBe State.IDLE
  }

  "given state IDLE" - {
    "when ADD_EVENT event, then go to SCHEDULING state and enqueue the event" {
      continually(10.seconds) {
        val eventQueue = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(eventQueue)
        val testEvent: Event = v(":id", "arg")

        runTest {
          val job = eventQueueFSM.handle(FsmEvent.ADD_EVENT, testEvent)

          eventQueueFSM.state shouldBe State.SCHEDULING
//          eventQueue.queue.peek() shouldBe testEvent
//          job!!.join()
          eventQueueFSM.state shouldBe State.RUNNING
          eventQueue.queue.isEmpty() shouldBe true
        }
      }
    }
  }
})
