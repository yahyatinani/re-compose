package com.github.whyrising.recompose.interceptor

import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.before
import com.github.whyrising.recompose.Keys.coeffects
import com.github.whyrising.recompose.Keys.db
import com.github.whyrising.recompose.Keys.event
import com.github.whyrising.recompose.Keys.originalEvent
import com.github.whyrising.recompose.Keys.queue
import com.github.whyrising.recompose.Keys.stack
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.map.IPersistentMap
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class InterceptorTest : FreeSpec({
    "context(event, interceptors) should return a fresh context" {
        val eventVec = arrayListOf<Any>(":id", 12)
        val interceptors = listOf<IPersistentMap<Keys, Any>>()

        val context = context(eventVec, interceptors)

        context shouldBe m(
            coeffects to m(
                event to eventVec,
                originalEvent to eventVec
            ),
            queue to interceptors
        )
    }

    "changeDirection(context) should put the stack into a new the queue" {
        val context = m<Keys, Any>(
            queue to listOf<Any>(),
            stack to listOf(1, 2, 3)
        )

        val c = changeDirection(context)

        c shouldBe m<Keys, Any>(
            queue to listOf<Any>(1, 2, 3),
            stack to listOf(1, 2, 3)
        )
    }

    "invokeInterceptorFn() should call the interceptor fun based direction" - {
        val context0 = m<Keys, Any>(
            queue to listOf<Any>(),
            stack to listOf(1, 2, 3)
        )

        val f: (IPersistentMap<Keys, Any>) -> IPersistentMap<Keys, Any> =
            { context ->
                val q = (get(context, queue) as List<Any>).plus(1)
                context.assoc(queue, q)
            }

        val addToQAfter = toInterceptor(
            id = ":add-to-queue",
            after = f
        )

        "should call :before and add to the context" {
            val addToQ = toInterceptor(id = ":add-to-queue", before = f)

            val context = invokeInterceptorFn(context0, addToQ, before)

            context shouldBe m<Keys, Any>(
                queue to listOf(1),
                stack to listOf(1, 2, 3)
            )
        }

        "should call :after and add to the context" {
            val context = invokeInterceptorFn(context0, addToQAfter, Keys.after)

            context shouldBe m<Keys, Any>(
                queue to listOf(1),
                stack to listOf(1, 2, 3)
            )
        }

        "when some direction set to default, should return the same context" {
            val context = invokeInterceptorFn(context0, addToQAfter, before)

            context shouldBeSameInstanceAs context0
        }
    }

    "invokeInterceptors(context)" - {
        "should return the same given context when the :queue is empty" {
            val context = m<Keys, Any>(
                queue to l<Any>(),
                stack to l<Any>()
            )

            val newContext = invokeInterceptors(context, before)

            newContext shouldBeSameInstanceAs context
        }

        """
            It should make a new context by invoking all interceptors in :queue
            and stack them in :stack while emptying the queue
        """ {
            val f1: (IPersistentMap<Keys, Any>) -> IPersistentMap<Keys, Any> =
                { context ->
                    context.assoc(db, (get(context, db) as Int).inc())
                }

            val f2: (context: IPersistentMap<Keys, Any>) -> IPersistentMap<Keys, Any> =
                { context ->
                    context.assoc(db, (get(context, db) as Int) + 2)
                }

            val incBy1 = toInterceptor(id = ":incBy1", before = f1)
            val incBy2 = toInterceptor(id = ":incBy2", before = f2)

            val qu = l<Any>(incBy1, incBy2)
            val stck = l<Any>()

            val context = m(
                db to 0,
                queue to qu,
                stack to stck
            )

            val newContext = invokeInterceptors(context, before)

            get(newContext, db) as Int shouldBeExactly 3
            (get(newContext, queue) as List<*>).shouldBeEmpty()
            (get(newContext, stack) as List<*>) shouldContainExactly
                qu.reversed()
        }
    }

    "changeDirection(context) should fill the queue from the stack" {
        val stck = l<Any>(1, 2, 3)

        val context = m(
            queue to l(),
            stack to stck
        )

        val newContext = changeDirection(context)

        (get(newContext, queue) as List<*>) shouldContainExactly stck
        (get(newContext, stack) as List<*>) shouldContainExactly stck
    }
})
