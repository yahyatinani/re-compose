package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReactionsCacheTest : FreeSpec({
  mockkStatic(Log::class)
  every { Log.d(any(), any()) } returns 0
  every { Log.i(any(), any()) } returns 0
  Dispatchers.setMain(StandardTestDispatcher())

  beforeEach {
    queryToReactionCache.value = m()
  }

  "cacheReaction()" - {
    "should call addOnDispose(f) on the cached reaction" {
      val reaction = MockReaction()

      cacheReaction(v(v("test"), v()), reaction as ReactionBase<Any, Int>)

      reaction.addOnDisposeCalled.shouldBeTrue()
    }

    "should add the reaction to the cache map" {
      val key = v("test")
      val reaction = Extraction(RAtom(1)) { (it as Int).inc() }

      val cached = cacheReaction(key, reaction as ReactionBase<Any, Int>)

      queryToReactionCache.value[key].shouldNotBeNull()
      cached shouldBeSameInstanceAs reaction
    }

    "dispose function f should remove the reaction from the cache" {
      val reaction = Extraction(RAtom(1)) { (it as Int).inc() }
      cacheReaction(v("test"), reaction as ReactionBase<Any, Int>)
      val disposeFn = reaction.disposeFns().first()

      disposeFn(reaction)

      queryToReactionCache.value.count shouldBeExactly 0
    }

    "dispose function f shouldn't affect cache if passed different reaction" {
      val key1 = v("test1")
      val key2 = v("test2")
      val reaction1 = Extraction(RAtom(1)) { (it as Int).inc() }
      val reaction2 = Extraction(RAtom(1)) { (it as Int).inc() }
      cacheReaction(key1, reaction1 as ReactionBase<Any, Int>)
      cacheReaction(key2, reaction2 as ReactionBase<Any, Int>)
      val oldCacheValue = queryToReactionCache.value
      val disposeFn2 = reaction2.disposeFns().first()

      disposeFn2(reaction1)

      queryToReactionCache.value shouldBeSameInstanceAs oldCacheValue
      queryToReactionCache.value.count shouldBeExactly 2
      queryToReactionCache.value[key1] shouldBeSameInstanceAs reaction1
      queryToReactionCache.value[key2] shouldBeSameInstanceAs reaction2
    }
  }
}) {
  class MockReaction : ReactionBase<Any?, Any?>() {
    private val _addOnDisposeCalled = atom(false)

    val addOnDisposeCalled: Boolean
      get() = _addOnDisposeCalled.deref()

    override fun deref(): Any? {
      TODO("Not yet implemented")
    }

    override suspend fun collect(collector: FlowCollector<Any?>) {
      TODO("Not yet implemented")
    }

    override val f: Any?
      get() = TODO("Not yet implemented")
    override val reactionScope: CoroutineScope
      get() = TODO("Not yet implemented")
    override val initialValue: Any?
      get() = TODO("Not yet implemented")
    override val signalObserver: Job
      get() = TODO("Not yet implemented")

    override fun addOnDispose(f: (Reaction<*>) -> Unit) {
      _addOnDisposeCalled.reset(true)
    }
  }
}
