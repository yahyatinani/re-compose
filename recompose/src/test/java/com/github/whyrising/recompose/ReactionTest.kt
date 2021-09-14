package com.github.whyrising.recompose

import com.github.whyrising.recompose.subs.Reaction
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ReactionTest : FreeSpec({
    "id" {
        val f = { 0 }
        val r1 = Reaction(f)

        r1.id shouldBe "rx${r1.hashCode()}"
    }
})
