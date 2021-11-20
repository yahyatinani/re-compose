package com.github.whyrising.recompose

import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.registrar.Kinds
import com.github.whyrising.recompose.registrar.register
import com.github.whyrising.recompose.router.EVENT_QUEUE
import com.github.whyrising.recompose.router.EventQueue
import com.github.whyrising.recompose.router.eventQueueFactory
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.q
import com.github.whyrising.y.collections.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
class RouterTest : FreeSpec({
    val testDispatcher = TestCoroutineDispatcher()

    beforeEach {
        Dispatchers.setMain(testDispatcher)
        register.reset(register.deref().assoc(Kinds.Event, m()))
    }

    afterEach {
        Dispatchers.resetMain()
    }

    "enqueue(event) should add the event to the event queue" {
        val eventQueue = EventQueue()
        eventQueue.viewModelScope.cancel()
        val event1 = v(":test-event1", 1)
        val event2 = v(":test-event2", 2)

        eventQueue.enqueue(event1)
        eventQueue.enqueue(event2)

        eventQueue.state.value shouldBe q<Event>().conj(event1).conj(event2)
    }

    "purge() should set the event queue to empty queue" {
        val eventQueue = EventQueue()
        val event1 = v(":test-event1", 3)
        val event2 = v(":test-event2", 4)
        eventQueue.enqueue(event1)
        eventQueue.enqueue(event2)

        eventQueue.purge()

        eventQueue.state.value shouldBeSameInstanceAs q<Event>()
    }

    "processFirstEvent()" - {
        "should handle the first event in the queue" {
            val eventQueue = EventQueue()
            var isEventHandled = false
            regEventDb(":test-event1") { db: Any, _: Event ->
                isEventHandled = true
                db
            }
            eventQueue.enqueue(v<Any>(":test-event1", 5))

            eventQueue.processFirstEvent(eventQueue.state.value)

            eventQueue.state.value shouldBeSameInstanceAs q<Event>()
            isEventHandled.shouldBeTrue()
        }

        "when queue is empty, it should skip any handling" {
            val eventQueue = EventQueue()
            var isEventHandled = false
            regEventDb(":test-event1") { db: Any, _: Event ->
                isEventHandled = true
                db
            }

            eventQueue.processFirstEvent(eventQueue.state.value)

            eventQueue.state.value shouldBeSameInstanceAs q<Event>()
            isEventHandled.shouldBeFalse()
        }

        "when exception thrown, purge the event queue then re-throw" {
            val eventQueue = EventQueue()
            eventQueue.viewModelScope.cancel()
            regEventDb<Any>(":test-event1") { _, _ ->
                throw RuntimeException("Test exception")
            }
            eventQueue.enqueue(v<Any>(":test-event1", 6))
            eventQueue.enqueue(v<Any>(":test-event2", 7))

            shouldThrowExactly<RuntimeException> {
                eventQueue.processFirstEvent(eventQueue.state.value)
            }.message shouldBe "Test exception"

            eventQueue.state.value shouldBeSameInstanceAs q<Event>()
        }
    }

    "consumeEventQueue() should process all events in the queue" {
        val eventQueue = EventQueue()
        eventQueue.enqueue(v<Any>(":test-event1", 8))
        eventQueue.enqueue(v<Any>(":test-event2", 9))

        eventQueue.consumeEventQueue()

        eventQueue.state.value shouldBeSameInstanceAs q<Event>()
    }

    "eventQueueFactory()" {
        val oldQueue = EVENT_QUEUE()

        eventQueueFactory(testDispatcher)

        EVENT_QUEUE().context shouldBeSameInstanceAs testDispatcher
        oldQueue.context shouldBeSameInstanceAs EmptyCoroutineContext
        oldQueue.viewModelScope.isActive.shouldBeFalse()
    }
})
