package io.github.yahyatinani.recompose.router

import io.github.yahyatinani.recompose.cofx.injectCofx
import io.github.yahyatinani.recompose.cofx.registerDbInjectorCofx
import io.github.yahyatinani.recompose.db.appDb
import io.github.yahyatinani.recompose.fx.registerBuiltinFxHandlers
import io.github.yahyatinani.recompose.ids.recompose
import io.github.yahyatinani.recompose.registrar.kindIdHandler
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EventQueueActionsTest : FreeSpec({
  val testDispatcher = StandardTestDispatcher()

  beforeEach {
    Dispatchers.setMain(testDispatcher)
    kindIdHandler.reset(m())
    appDb.value = m<Any, Any>()
    registerDbInjectorCofx()
    injectCofx(recompose.db)
    registerBuiltinFxHandlers()
  }

  afterEach {
    Dispatchers.resetMain()
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

  // FIXME:
  /*
    "multithreading" {
      continually(4.seconds) {
        runTest {
          appDb.reset(0)
          regEventDb<Int>(":test-event-inc") { db, _ -> db.inc() }
          regEventDb<Int>(":test-event-dec") { db, _ -> db.dec() }
          val eventQueueImp = EventQueueImp()
          val eventQueueFSM = EventQueueFSM(
            eventQueue = eventQueueImp,
            context = testDispatcher,
            scope = this
          )

          repeat(100) {
            launch {
              repeat(1001) {
                eventQueueFSM.push(v(":test-event-inc"))
              }
            }
          }

          advanceUntilIdle()

          repeat(11) {
            launch {
              repeat(9) {
                eventQueueFSM.push(v(":test-event-inc"))
              }
            }
          }
          advanceUntilIdle()

          repeat(10) {
            launch {
              repeat(10) {
                dispatchSync(v(":test-event-dec"))
              }
            }
          }

          advanceUntilIdle()

          eventQueueImp.count shouldBe 0
          appDb.deref() shouldBe 100099

          testDispatcher.cancel()
        }
      }
    }
  */
})
