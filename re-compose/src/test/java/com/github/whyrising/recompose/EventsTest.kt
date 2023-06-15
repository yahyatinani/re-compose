package com.github.whyrising.recompose

import com.github.whyrising.recompose.events.flatten
import com.github.whyrising.y.core.collections.LazySeq
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.reflection.shouldBeSubtypeOf
import io.kotest.matchers.shouldBe

class EventsTest : FreeSpec({
  "flatten(interceptors)" {
    val items = v(1, 2, v("3", false))

    val r = flatten(items)

    r.first() shouldBe 1
    r.rest().first() shouldBe 2
    r.rest().rest().first() shouldBe "3"
    r.rest().rest().rest().first() shouldBe false
    r::class.shouldBeSubtypeOf<LazySeq<*>>()
  }
})
