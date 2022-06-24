package com.github.whyrising.recompose

import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.recompose.subs.ExtractorReaction
import com.github.whyrising.recompose.subs.ReactionBase
import com.github.whyrising.recompose.subs.cacheReaction
import com.github.whyrising.recompose.subs.reactionsCache
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

class CacheReactionTest : FreeSpec({
  Dispatchers.setMain(StandardTestDispatcher())

  beforeEach {
    reactionsCache.clear()
  }

  "cacheReaction() should add the reaction to the cache map" {
    val key = v(v("test"), v())
    val reaction = ExtractorReaction(RAtom(1)) { it.inc() }

    val cached = cacheReaction(key, reaction as ReactionBase<Any, Int>)

    reactionsCache.contains(key = key).shouldBeTrue()
    cached shouldBeSameInstanceAs reaction
  }

  "cacheReaction() should add a dispose fn to the cached reaction" {
    val key = v(v("test"), v())
    val reaction = ExtractorReaction(RAtom(1)) { it.inc() }

    cacheReaction(key, reaction as ReactionBase<Any, Int>)

    reaction.disposeFns().count shouldBeExactly 1
  }

  "dispose function should remove the reaction from the cache" {
    val key = v(v("test"), v())
    val reaction = ExtractorReaction(RAtom(1)) { it.inc() }
    cacheReaction(key, reaction as ReactionBase<Any, Int>)
    val disposeFn = reaction.disposeFns().first()
    reactionsCache.isEmpty().shouldBeFalse()

    disposeFn(reaction)

    reactionsCache.isEmpty().shouldBeTrue()
  }

  "dispose function shouldn't remove if passed the wrong reaction" {
    val key1 = v(v("test1"), v())
    val key2 = v(v("test2"), v())
    val reaction1 = ExtractorReaction(RAtom(1)) { it.inc() }
    val reaction2 = ExtractorReaction(RAtom(1)) { it.inc() }
    cacheReaction(key1, reaction1 as ReactionBase<Any, Int>)
    cacheReaction(key2, reaction2 as ReactionBase<Any, Int>)
    val disposeFn2 = reaction2.disposeFns().first()

    disposeFn2(reaction1)

    reactionsCache.size shouldBeExactly 2
    reactionsCache[key1] shouldBeSameInstanceAs reaction1
    reactionsCache[key2] shouldBeSameInstanceAs reaction2
  }
})
