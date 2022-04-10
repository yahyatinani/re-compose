package com.github.whyrising.recompose

import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.cofx.registerDbInjectorCofx
import com.github.whyrising.recompose.db.DEFAULT_APP_DB_VALUE
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.initBuiltinEffectHandlers
import com.github.whyrising.recompose.registrar.register
import com.github.whyrising.recompose.router.EVENT_QUEUE
import com.github.whyrising.recompose.router.EventQueue
import com.github.whyrising.recompose.router.eventQueueFactory
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.y.m
import com.github.whyrising.y.q
import com.github.whyrising.y.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
class RouterTest : FreeSpec({
    val testDispatcher = StandardTestDispatcher()

    beforeEach {
        Dispatchers.setMain(testDispatcher)
        register.reset(m())
        appDb.emit(DEFAULT_APP_DB_VALUE)
        EVENT_QUEUE.swap { EventQueue() }
        registerDbInjectorCofx()
        injectCofx(Schema.db)
        initBuiltinEffectHandlers()
    }

    afterEach {
        Dispatchers.resetMain()
    }

    "enqueue(event) should add the event to the event queue" {
        val eventQueue = EventQueue()
        eventQueue.consumerJob.cancel()
        val event1 = v(":test-event1", 1)
        val event2 = v(":test-event2", 2)

        eventQueue.enqueue(event1)
        eventQueue.enqueue(event2)

        eventQueue.queueState() shouldBe q<Event>().conj(event1).conj(event2)
    }

    "purge() should set the event queue to empty queue" {
        val eventQueue = EventQueue()
        val event1 = v(":test-event1", 3)
        val event2 = v(":test-event2", 4)
        eventQueue.enqueue(event1)
        eventQueue.enqueue(event2)

        eventQueue.purge()

        eventQueue.queueState() shouldBeSameInstanceAs q<Event>()
    }

    "processFirstEvent()" - {
        "should handle the first event in the queue" {
            val eventQueue = EventQueue()
            eventQueue.consumerJob.cancel()
            var isEventHandled = false
            regEventDb(":test-event1") { db: Any, _: Event ->
                isEventHandled = true
                db
            }
            eventQueue.enqueue(v<Any>(":test-event1", 5))

            eventQueue.processFirstEvent()

            eventQueue.queueState() shouldBeSameInstanceAs q<Event>()
            isEventHandled.shouldBeTrue()
        }

        "when queue is empty, it should skip any handling" {
            val eventQueue = EventQueue()
            eventQueue.consumerJob.cancel()
            var isEventHandled = false
            regEventDb(":test-event1") { db: Any, _: Event ->
                isEventHandled = true
                db
            }

            eventQueue.deferredUntilEvent.complete(Unit)
            eventQueue.processFirstEvent()

            eventQueue.queueState() shouldBeSameInstanceAs q<Event>()
            isEventHandled.shouldBeFalse()
        }

        "when exception thrown, purge the event queue then re-throw" {
            val eventQueue = EventQueue()
            eventQueue.consumerJob.cancel()
            regEventDb<Any>(":test-event1") { _, _ ->
                throw RuntimeException("Test exception")
            }
            eventQueue.enqueue(v<Any>(":test-event1", 6))
            eventQueue.enqueue(v<Any>(":test-event2", 7))

            shouldThrowExactly<RuntimeException> {
                eventQueue.processFirstEvent()
            }.message shouldBe "Test exception"

            eventQueue.queueState() shouldBeSameInstanceAs q<Event>()
        }
    }

    "consumeEventQueue() should process all events in the queue" {
        val eventQueue = EventQueue()
        eventQueue.consumerJob.cancel()
        eventQueue.enqueue(v<Any>(":test-event1", 8))
        eventQueue.enqueue(v<Any>(":test-event2", 9))
        eventQueue.enqueue(v<Any>(":test-event2", 10))

        eventQueue.consumeEventQueue()

        eventQueue.queueState() shouldBeSameInstanceAs q<Event>()
    }

    "eventQueueFactory()" {
        val oldQueue = EVENT_QUEUE()
        oldQueue.consumerJob.cancel()
        oldQueue.enqueue(v<Any>(":test-event1", 8))
        oldQueue.enqueue(v<Any>(":test-event2", 9))

        eventQueueFactory(testDispatcher)

        EVENT_QUEUE().context shouldBeSameInstanceAs testDispatcher
        oldQueue.context shouldBeSameInstanceAs EmptyCoroutineContext
        oldQueue.queueState() shouldBeSameInstanceAs q<Event>()
        oldQueue.viewModelScope.isActive.shouldBeFalse()
    }

    "concurrency test for producer-consumer in EventQueue" {
        val queue = EventQueue()
        appDb.emit(0)
        regEventDb<Any>(":test-event1") { db, _ ->
            runBlocking { delay(100) }
            db as Int + 3
        }
        regEventDb<Any>(":test-event2") { db, _ ->
            runBlocking { delay(1000) }
            db as Int + 5
        }
        val job: Job
        val job1: Job

        runBlocking {
            job = launch { queue.enqueue(v<Any>(":test-event2")) }
            job1 = launch { queue.enqueue(v<Any>(":test-event1")) }
        }

        job.isCompleted.shouldBeTrue()
        job1.isCompleted.shouldBeTrue()
        queue.queueState() shouldBe q<Any>()
        appDb.deref() shouldBe 8
    }

    "dispatch(event)" - {
        "when the event is empty, it should throw an exception" {
            EVENT_QUEUE().consumerJob.cancel()

            val e =
                shouldThrowExactly<IllegalArgumentException> {
                    com.github.whyrising.recompose.router.dispatch(v())
                }
            val msg = "$TAG: `dispatch` was called with an empty event vector."
            e.message shouldBe msg
        }

        "it should enqueue an event" {
            EVENT_QUEUE().consumerJob.cancel()

            com.github.whyrising.recompose.router.dispatch(v(":event"))

            EVENT_QUEUE().queueState() shouldBe q<Event>().conj(v(":event"))
        }
    }

    "dispatchSync(event)" {
        regEventDb<Any>(":test-event") { _, _ ->
            12
        }

        dispatchSync(v<Any>(":test-event"))

        appDb.deref() shouldBe 12
    }

    "equals()" {
        (EventQueue() == EventQueue()).shouldBeTrue()

        val eq1 = EventQueue()
        val eq2 = EventQueue()
        eq1.queueState.swap { it.conj(v("event-1")) }
        (eq1 == eq2).shouldBeFalse()

        (EventQueue().equals("EventQueue()")).shouldBeFalse()
    }

    "hashCode()" {
        EventQueue().hashCode() shouldBe 31 * 1 + q<Event>().hashCode()

        val e = v("event-1")
        val q = q<Event>().conj(e)
        val hash = 31 * 1 + q.hashCode()
        val eventQueue = EventQueue()
        eventQueue.queueState.swap { it.conj(e) }

        eventQueue.hashCode() shouldBe hash
    }
})
