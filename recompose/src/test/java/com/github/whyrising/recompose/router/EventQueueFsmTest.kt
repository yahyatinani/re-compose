package com.github.whyrising.recompose.router

import com.github.whyrising.y.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

class EventQueueFsmTest : FreeSpec({
  Dispatchers.setMain(StandardTestDispatcher())

  "initial state of FSM" {
    val eventQueue = EventQueueImp()
    val eventQueueFSM = EventQueueFSM(eventQueue)

    eventQueue.queue.isEmpty() shouldBe true
    eventQueueFSM.state shouldBe State.IDLE
  }

  "Given we're in RUNNING state, when EXCEPTION event happens, " +
    "then go to IDLE and call exception()" {
      val eventQueue = EventQueueImp().apply {
        enqueue(v(":event1", "arg"))
        enqueue(v(":event2", "arg"))
      }
      val eventQueueFSM = EventQueueFSM(eventQueue, State.RUNNING)

      shouldThrowExactly<RuntimeException> {
        eventQueueFSM.handle(FsmEvent.EXCEPTION, RuntimeException())
      }

      eventQueueFSM.state shouldBe State.IDLE
      eventQueue.queue.isEmpty() shouldBe true
    }
})
