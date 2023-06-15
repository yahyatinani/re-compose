package com.github.whyrising.recompose.router

import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.cofx.registerDbInjectorCofx
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.fx.registerBuiltinFxHandlers
import com.github.whyrising.recompose.ids.recompose
import com.github.whyrising.recompose.multiThreadedRun
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.common.ExperimentalKotest
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalKotest::class)
class EventQueueActionsTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()
  Dispatchers.setMain(testDispatcher)

  beforeEach {
    com.github.whyrising.recompose.registrar.kindIdHandler.reset(m())
    appDb.reset(m<Any, Any>())
    registerDbInjectorCofx()
    injectCofx(recompose.db)
    registerBuiltinFxHandlers()
  }

  "enqueue()" {
    val event1 = v(":id1", 1234)
    val event2 = v(":id2", 1234)
    val eventQueueImp = EventQueueImp()

    eventQueueImp.enqueue(event1)
    eventQueueImp.enqueue(event2)

    eventQueueImp.count shouldBe 2
    eventQueueImp.queue.count shouldBe 2
    eventQueueImp.queue.peek() shouldBe event1
    eventQueueImp.queue.pop().peek() shouldBe event2
  }

  "processFirstEventInQueue() should process first event then pop the queue" - {
    "single thread/coroutine" {
      val event1 = v(":id1", 1234)
      val event2 = v(":id2", 1234)
      val eventQueueImp = EventQueueImp()
      eventQueueImp.enqueue(event1)
      eventQueueImp.enqueue(event2)

      eventQueueImp.processFirstEventInQueue()

      eventQueueImp.count shouldBe 1
      eventQueueImp.queue.peek() shouldBe event2
    }
  }

  "processCurrentEvents() should process all current events in the queue" {
    val event1 = v(":id1", 1234)
    val event2 = v(":id2", 1234)
    val eventQueueImp = EventQueueImp()
    eventQueueImp.enqueue(event1)
    eventQueueImp.enqueue(event2)

    eventQueueImp.processCurrentEvents()

    eventQueueImp.count shouldBe 0
  }

  "purge() should empty the queue" {
    val event1 = v(":id1", 1234)
    val event2 = v(":id2", 1234)
    val eventQueueImp = EventQueueImp()
    eventQueueImp.enqueue(event1)
    eventQueueImp.enqueue(event2)

    eventQueueImp.purge()

    eventQueueImp.count shouldBe 0
  }

  "exception() should empty the queue then throw" {
    val event1 = v(":id1", 1234)
    val event2 = v(":id2", 1234)
    val eventQueueImp = EventQueueImp()
    eventQueueImp.enqueue(event1)
    eventQueueImp.enqueue(event2)
    val ex = RuntimeException()

    shouldThrowExactly<RuntimeException> { eventQueueImp.exception(ex) }
    eventQueueImp.count shouldBe 0
  }

  "multithreading" {
    continually(5.seconds) {
      runTest {
        appDb.reset(0)
        regEventDb<Int>(":test-event-inc") { db, _ -> db.inc() }
        regEventDb<Int>(":test-event-dec") { db, _ -> db.dec() }
        val eventQueueImp = EventQueueImp()
        val eventQueueFSM = EventQueueFSM(
          eventQueue = eventQueueImp,
          context = testDispatcher
        )

        Dispatchers.setMain(testDispatcher)
        multiThreadedRun(coroutinesN = 100, runN = 1001) {
          eventQueueFSM.push(v(":test-event-inc"))
        }

        multiThreadedRun(coroutinesN = 11, runN = 9) {
          eventQueueFSM.push(v(":test-event-inc"))
        }

        multiThreadedRun(coroutinesN = 10, runN = 10) {
//          appDb._state.update { (it as Int).inc() }
          dispatchSync(v(":test-event-dec"))
        }
        advanceUntilIdle()

        eventQueueImp.count shouldBe 0
        appDb.deref() shouldBe 100099
      }
    }
  }
})