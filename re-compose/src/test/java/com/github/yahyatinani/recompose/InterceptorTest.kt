package com.github.yahyatinani.recompose

import com.github.whyrising.y.core.assocIn
import com.github.whyrising.y.core.collections.IPersistentMap
import com.github.whyrising.y.core.collections.IPersistentVector
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.collections.PersistentList
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import com.github.yahyatinani.recompose.cofx.Coeffects
import com.github.yahyatinani.recompose.ids.InterceptSpec
import com.github.yahyatinani.recompose.ids.InterceptSpec.after
import com.github.yahyatinani.recompose.ids.InterceptSpec.after_async
import com.github.yahyatinani.recompose.ids.InterceptSpec.before
import com.github.yahyatinani.recompose.ids.InterceptSpec.id
import com.github.yahyatinani.recompose.ids.coeffects.event
import com.github.yahyatinani.recompose.ids.coeffects.originalEvent
import com.github.yahyatinani.recompose.ids.context
import com.github.yahyatinani.recompose.ids.context.coeffects
import com.github.yahyatinani.recompose.ids.context.queue
import com.github.yahyatinani.recompose.ids.context.stack
import com.github.yahyatinani.recompose.ids.recompose.db
import com.github.yahyatinani.recompose.interceptor.Context
import com.github.yahyatinani.recompose.interceptor.Interceptor
import com.github.yahyatinani.recompose.interceptor.InterceptorFn
import com.github.yahyatinani.recompose.interceptor.assocCofx
import com.github.yahyatinani.recompose.interceptor.changeDirection
import com.github.yahyatinani.recompose.interceptor.context
import com.github.yahyatinani.recompose.interceptor.defaultInterceptorAsyncFn
import com.github.yahyatinani.recompose.interceptor.defaultInterceptorFn
import com.github.yahyatinani.recompose.interceptor.enqueue
import com.github.yahyatinani.recompose.interceptor.execute
import com.github.yahyatinani.recompose.interceptor.invokeInterceptorFn
import com.github.yahyatinani.recompose.interceptor.invokeInterceptors
import com.github.yahyatinani.recompose.interceptor.toInterceptor
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

@Suppress("UNCHECKED_CAST")
class InterceptorTest : FreeSpec({
  "toInterceptor() should return a map interceptor" - {
    "return a map with default functions" {
      val expectedInterceptor = m(
        id to ":test",
        before to defaultInterceptorFn,
        after to defaultInterceptorFn,
        after_async to defaultInterceptorAsyncFn
      )

      val toInterceptor = toInterceptor(":test")

      toInterceptor shouldBe expectedInterceptor
    }

    "return a map of the passed values" {
      val f1: InterceptorFn = { context: Context -> context }
      val f2: InterceptorFn = { context: Context -> context }

      val interceptor = toInterceptor(
        id = ":test",
        before = f1,
        after = f2
      )

      interceptor shouldBe m(
        id to ":test",
        before to f1,
        after to f2,
        after_async to defaultInterceptorAsyncFn
      )
    }
  }

  "assocCofx() should inject key/val pair into `coeffects` into context" {
    assocCofx(m(), event, 14) shouldBe m(coeffects to m(event to 14))
    assocCofx(m(stack to l<Any>()), event, 14) shouldBe m(
      stack to l<Any>(),
      coeffects to m(event to 14)
    )
  }

  "enqueue()" - {
    "when interceptors is null, use an empty vec instead" {
      enqueue(m(), null) shouldBe m(queue to l<Any>())
    }

    "should inject key/val pair of queue/interceptors" {
      val interceptors: ISeq<Interceptor> = l()
      enqueue(m(), interceptors) shouldBe m(queue to interceptors)
    }
  }

  "context(event, interceptors) should return a fresh context" {
    val eventVec = v<Any>(":id", 12)
    val interceptors = l<IPersistentMap<InterceptSpec, Any>>()

    val context = context(eventVec, interceptors)

    context shouldBe m(
      coeffects to m(
        event to eventVec,
        originalEvent to eventVec
      ),
      queue to interceptors
    )
  }

  "invokeInterceptorFn() should call the interceptor fun based direction" - {
    val context0 = m<context, Any>(
      queue to v<Any>(),
      stack to v(1, 2, 3)
    )

    val f: (
      IPersistentMap<context, Any>
    ) -> IPersistentMap<context, Any> = { context ->
      val q = (context[queue] as IPersistentVector<Any>).conj(1)
      context.assoc(queue, q)
    }

    val g: (
      IPersistentMap<context, Any>
    ) -> IPersistentMap<context, Any> = { context ->
      val q = (context[queue] as IPersistentVector<Any>).plus(1)
      context.assoc(queue, q)
    }

    val addToQAfter = toInterceptor(
      id = ":add-to-queue",
      after = f
    )

    "should call :before and add to the context" {
      val addToQ = toInterceptor(id = ":add-to-queue", before = g)

      val context = invokeInterceptorFn(context0, addToQ, before)

      context shouldBe m<context, Any>(
        queue to v(1),
        stack to v(1, 2, 3)
      )
    }

    "should call :after and add to the context" {
      val context =
        invokeInterceptorFn(context0, addToQAfter, after)

      context shouldBe m<context, Any>(
        queue to v(1),
        stack to v(1, 2, 3)
      )
    }

    "when some direction set to default, should return the same context" {
      val context = invokeInterceptorFn(context0, addToQAfter, before)

      context shouldBeSameInstanceAs context0
    }
  }

  "invokeInterceptors(context)" - {
    "should return the same given context when the :queue is empty" {
      val context = m<context, Any>(
        queue to l<Any>(),
        stack to l<Any>()
      )

      val newContext = invokeInterceptors(context, before)

      newContext shouldBeSameInstanceAs context
    }

    """
            It should make a new context by invoking all interceptors in :queue
            and stack then in :stack while emptying the queue
        """ {
      val f1: (
        IPersistentMap<context, Any>
      ) -> IPersistentMap<context, Any> = { context ->
        val i = (context[coeffects] as Coeffects)[db] as Int
        context.assoc(coeffects, m(db to i.inc()))
      }
      val f2: (
        IPersistentMap<context, Any>
      ) -> IPersistentMap<context, Any> = { context ->
        val i = (context[coeffects] as Coeffects)[db] as Int

        context.assoc(coeffects, m(db to i + 2))
      }
      val incBy1 = toInterceptor(id = ":incBy1", before = f1)
      val incBy2 = toInterceptor(id = ":incBy2", before = f2)
      val qu = l<Any>(incBy1, incBy2)
      val stck = l<Any>()
      val context: Context = m(
        coeffects to m(db to 0),
        queue to qu,
        stack to stck
      )

      val newContext = invokeInterceptors(context, before)

      newContext[coeffects] shouldBe m(db to 3)
      (newContext[queue] as PersistentList<*>).shouldBeEmpty()
      (newContext[stack] as PersistentList<*>) shouldContainExactly
        qu.reversed()
    }
  }

  "changeDirection(context)" - {
    "should put the stack into a new the queue" {
      val context = m<context, Any>(
        queue to l<Any>(),
        stack to l(1, 2, 3)
      )
      val c = changeDirection(context)

      c shouldBe m<context, Any>(
        queue to l<Any>(1, 2, 3),
        stack to l(1, 2, 3)
      )
    }

    "should fill the queue from the stack" {
      val s = l<Any>(1, 2, 3)
      val context = m(
        queue to l(),
        stack to s
      )

      val newContext = changeDirection(context)

      (newContext[queue] as PersistentList<*>) shouldContainExactly s
      (newContext[stack] as PersistentList<*>) shouldContainExactly s
    }
  }

  """
        execute() should build context and run :before interceptors in the given
        order, then :after interceptors in reversed order
    """ {
    val eventV = v(":test")
    val interceptor1 = toInterceptor(
      ":i1",
      before = { context ->
        assocIn(context, l(coeffects, db), 3) as Context
      }
    )
    val interceptor2 = toInterceptor(
      ":i2",
      after = { context: Context ->
        val value = (context[coeffects] as Coeffects)[db] as Int * 4
        assocIn(context, l(coeffects, db), value) as Context
      }
    )

    val context = execute(eventV, l(interceptor1, interceptor2))

    context shouldBe m(
      coeffects to m(event to eventV, originalEvent to eventV, db to 12),
      queue to l<Any>(),
      stack to l(interceptor1, interceptor2, interceptor2, interceptor1)
    )
  }
})
