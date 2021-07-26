package com.github.whyrising.recompose.events

import com.github.whyrising.y.concretions.list.PersistentList
import com.github.whyrising.y.concretions.list.l
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.reflection.shouldBeSubtypeOf

class EventsTest : FreeSpec({
    "flatten(interceptors)" {
        val items = arrayListOf(1, 2, arrayListOf("3", false))

        val r = flatten(items)

        r shouldContainExactly l(1, 2, "3", false)
        r::class.shouldBeSubtypeOf<PersistentList<*>>()
    }
})
