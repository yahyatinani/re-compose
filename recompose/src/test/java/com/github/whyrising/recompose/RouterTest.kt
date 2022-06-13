package com.github.whyrising.recompose

import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.cofx.registerDbInjectorCofx
import com.github.whyrising.recompose.db.DEFAULT_APP_DB_VALUE
import com.github.whyrising.recompose.db.appDb
import com.github.whyrising.recompose.events.Event
import com.github.whyrising.recompose.fx.initBuiltinEffectHandlers
import com.github.whyrising.recompose.registrar.register
import com.github.whyrising.recompose.router.EventQueue
import com.github.whyrising.recompose.router.dispatch
import com.github.whyrising.recompose.schemas.Schema
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.q
import com.github.whyrising.y.core.v
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class RouterTest : FreeSpec({
    Dispatchers.setMain(testDispatcher)

    beforeEach {
        register.reset(m())
        appDb.emit(DEFAULT_APP_DB_VALUE)
        EventQueue.qAtom.swap { q() }
        registerDbInjectorCofx()
        injectCofx(Schema.db)
        initBuiltinEffectHandlers()
    }

    "enqueue(event) should add the event to the event queue" {
        runTest {
            val event1 = v(":test-event1", 1)
            val event2 = v(":test-event2", 2)
            EventQueue.enqueue(event1)
            EventQueue.enqueue(event2)

            EventQueue.deferredUntilEvent.await()

            EventQueue.qAtom() shouldBe q<Event>().conj(event1).conj(event2)
        }
    }

    "purge() should set the event queue to empty queue" {
        EventQueue.qAtom.swap { it.conj(v<Any>(":test-event1", 3)) }
        EventQueue.qAtom.swap { it.conj(v<Any>(":test-event2", 4)) }

        EventQueue.purge()

        EventQueue.qAtom() shouldBeSameInstanceAs q<Event>()
    }

    "should process all events in the queue" {
        runTest {
            EventQueue.enqueue(v<Any>(":test-event1", 8))
            EventQueue.enqueue(v<Any>(":test-event2", 9))
            EventQueue.enqueue(v<Any>(":test-event2", 10))

            EventQueue.qAtom() shouldBeSameInstanceAs q<Event>()
        }
    }

    "concurrency test for producer-consumer in EventQueue" {
        appDb.emit(0)
        regEventDb<Any>(":test-event1") { db, _ ->
            runBlocking { delay(100) }
            db as Int + 3
        }
        regEventDb<Any>(":test-event2") { db, _ ->
            runBlocking { delay(1000) }
            db as Int + 5
        }

        var job: Job? = null
        var job1: Job? = null
        runTest {
            job = launch { EventQueue.enqueue(v<Any>(":test-event2")) }
            job1 = launch { EventQueue.enqueue(v<Any>(":test-event1")) }
        }

        job!!.isCompleted.shouldBeTrue()
        job1!!.isCompleted.shouldBeTrue()
        EventQueue.qAtom() shouldBe q<Any>()
        appDb.deref() shouldBe 8
    }

    "dispatch(event)" - {
        "when the event is empty, it should throw an exception" {
            EventQueue.consumerJob.cancel()

            val e = shouldThrowExactly<IllegalArgumentException> {
                dispatch(v())
            }
            val msg = "$TAG: `dispatch` was called with an empty event vector."
            e.message shouldBe msg
        }

        "it should enqueue an event" {
            runTest {
                dispatch(v(":event"))
                advanceUntilIdle()

                EventQueue.qAtom() shouldBe q<Event>().conj(v(":event"))
            }
        }
    }

    "dispatchSync(event)" {
        regEventDb<Any>(":test-event") { _, _ ->
            12
        }

        dispatchSync(v<Any>(":test-event"))

        appDb.deref() shouldBe 12
    }
}) {
    companion object {
        val testDispatcher = StandardTestDispatcher()
    }
}
