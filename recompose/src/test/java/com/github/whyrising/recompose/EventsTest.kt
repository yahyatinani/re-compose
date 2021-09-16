package com.github.whyrising.recompose

import com.github.whyrising.recompose.events.flatten
import com.github.whyrising.y.collections.concretions.list.PersistentList
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.reflection.shouldBeSubtypeOf

class EventsTest : FreeSpec({
    "flatten(interceptors)" {
        val items = v(1, 2, v("3", false))

        val r = flatten(items as PersistentVector<Any>)

        r shouldContainExactly v(1, 2, "3", false) as PersistentVector
        r::class.shouldBeSubtypeOf<PersistentList<*>>()
    }
})
