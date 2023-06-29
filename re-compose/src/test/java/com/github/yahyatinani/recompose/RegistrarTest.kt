package com.github.yahyatinani.recompose

import com.github.yahyatinani.recompose.registrar.Kinds.Cofx
import com.github.yahyatinani.recompose.registrar.Kinds.Event
import com.github.yahyatinani.recompose.registrar.Kinds.Fx
import com.github.yahyatinani.recompose.registrar.Kinds.Sub
import com.github.yahyatinani.recompose.registrar.clearHandlers
import com.github.yahyatinani.recompose.registrar.getHandler
import com.github.yahyatinani.recompose.registrar.kindIdHandler
import com.github.yahyatinani.recompose.registrar.registerHandler
import io.github.yahyatinani.y.core.collections.IPersistentVector
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import com.github.yahyatinani.recompose.registrar.kindIdHandler as myRegister

class RegistrarTest : FreeSpec({
  afterTest {
    myRegister.reset(m())
  }

  "registerHandler()/getHandler(kind)" - {
    "Fx kind" {
      val id = ":fx"
      val handlerFn = { _: Any -> }
      val hFn = registerHandler(id, Fx, handlerFn)

      val handler = getHandler(Fx, id)

      handler shouldBeSameInstanceAs handlerFn
      hFn shouldBeSameInstanceAs handlerFn
    }

    "Event kind" {
      val id = ":event"
      val interceptors = v<Any>()
      registerHandler(id, Event, interceptors)

      val handler = getHandler(Event, id)

      handler shouldBeSameInstanceAs interceptors
    }

    "Cofx kind" {
      val id = ":cofx"
      val handlerFn: (Any) -> Any = { _ -> }
      registerHandler(id, Cofx, handlerFn)

      val handler = getHandler(Cofx, id)

      myRegister.deref().count shouldBeExactly 1
      handler shouldBeSameInstanceAs handlerFn
    }

    "Sub kind" {
      val id = ":sub"
      val handlerFn: (Any, IPersistentVector<Any>) -> Any = { _, _ -> }
      registerHandler(id, Sub, handlerFn)

      val handler = getHandler(Sub, id)

      handler shouldBeSameInstanceAs handlerFn
    }

    "two kinds of handlers with same id should register separately" {
      val id = ":id"
      val handlerFn1 = { _: Any -> }
      val handlerFn2 = { _: Any -> }
      val handlerFn3 = { _: Any -> }
      val handlerFn4 = { _: Any -> }
      registerHandler(id, Fx, handlerFn1)
      registerHandler(id, Cofx, handlerFn2)
      registerHandler(id, Event, handlerFn3)
      registerHandler(id, Sub, handlerFn4)

      val handler1 = getHandler(Fx, id)
      val handler2 = getHandler(Cofx, id)
      val handler3 = getHandler(Event, id)
      val handler4 = getHandler(Sub, id)

      handler1 shouldBeSameInstanceAs handlerFn1
      handler2 shouldBeSameInstanceAs handlerFn2
      handler3 shouldBeSameInstanceAs handlerFn3
      handler4 shouldBeSameInstanceAs handlerFn4

      handler1 shouldNotBeSameInstanceAs handler2
      handler2 shouldNotBeSameInstanceAs handler3
      handler3 shouldNotBeSameInstanceAs handler4
      handler4 shouldNotBeSameInstanceAs handler1
    }
  }

  "clearHandlers" - {
    "clearHandlers() should clear all event handlers." {
      registerHandler(":event1", Event, {})
      registerHandler(":event2", Event, {})

      clearHandlers()

      kindIdHandler() shouldBe m<Any, Any>()
    }

    "clearHandlers(kind) should clear all event handlers of given kind." {
      registerHandler(":event1", Event, {})
      registerHandler(":event2", Event, {})
      val subHandler = {}
      registerHandler(":sub", Sub, subHandler)

      clearHandlers(Event)

      kindIdHandler() shouldBe m(Sub to m(":sub" to subHandler))
    }

    "clearHandlers(kind) should clear all event handlers of given kind." {
      registerHandler(":event1", Event, {})
      val eventHandler = {}
      registerHandler(":event2", Event, eventHandler)
      val subHandler = {}
      registerHandler(":sub", Sub, subHandler)

      clearHandlers(Event, ":event1")
      clearHandlers(Event, ":not-found-event")

      kindIdHandler() shouldBe m(
        Sub to m(":sub" to subHandler),
        Event to m(":event2" to eventHandler)
      )
    }
  }
})
