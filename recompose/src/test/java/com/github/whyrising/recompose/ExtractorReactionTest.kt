package com.github.whyrising.recompose

import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.recompose.subs.ExtractorReaction
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ExtractorReactionTest : FreeSpec({
  val dispatcher = StandardTestDispatcher()
  Dispatchers.setMain(dispatcher)

  "ctor" {
    val reaction = ExtractorReaction(RAtom(1)) { it.inc() }

    reaction.deref() shouldBeExactly 2
  }

  "recompute()" {
    val reaction = ExtractorReaction(RAtom(1)) { it.inc() }

    reaction.recompute(3)

    reaction.deref() shouldBeExactly 4
  }

  "collect()" {
    runTest {
      val r = ExtractorReaction(RAtom(1)) { it.inc() }
      val reaction = ExtractorReaction(r) { it.inc() }

      launch { r.state.emit(4) }
      advanceUntilIdle()

      reaction.deref() shouldBeExactly 5
    }
  }

  "deref()" {
    val reaction = ExtractorReaction(RAtom(1)) { it.inc() }

    reaction.deref() shouldBeExactly 2
  }
})
