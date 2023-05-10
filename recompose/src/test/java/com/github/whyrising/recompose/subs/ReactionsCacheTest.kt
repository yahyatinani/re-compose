package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.get
import com.github.whyrising.y.core.m
import com.github.whyrising.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReactionsCacheTest : FreeSpec({
  mockkStatic(Log::class)
  every { Log.d(any(), any()) } returns 0
  every { Log.i(any(), any()) } returns 0
  Dispatchers.setMain(StandardTestDispatcher())

  beforeEach { queryToReactionCache.value = m() }

  "cacheReaction()" - {
    "should call addOnDispose(f) on the cached reaction" {
      val reaction = MockReaction()

      cacheReaction(v(v("test"), v()), reaction as ReactionBase<*, *>)

      reaction.addOnDisposeCalled.shouldBeTrue()
    }

    "should add the reaction to the cache map" {
      val key = v("test")
      val reaction = Extraction(atom(1)) { (it as Int).inc() }

      val cached = cacheReaction(key, reaction)

      queryToReactionCache.value[key].shouldNotBeNull()
      cached shouldBeSameInstanceAs reaction
    }
  }
}) {
  class MockReaction : ReactionBase<Any?, Any?>("mock") {
    private val _addOnDisposeCalled = atom(false)

    val addOnDisposeCalled: Boolean
      get() = _addOnDisposeCalled.deref()

    override fun deref(): Any? {
      TODO("Not yet implemented")
    }

    override suspend fun collect(collector: FlowCollector<Any?>) {
      TODO("Not yet implemented")
    }

    override val f: Any
      get() = TODO("Not yet implemented")
    override val reactionScope: CoroutineScope
      get() = TODO("Not yet implemented")
    override val initialValue: Any
      get() = TODO("Not yet implemented")

    override val state: StateFlow<Any?>
      get() = TODO("Not yet implemented")

    override val signalObserver: Job
      get() = TODO("Not yet implemented")

    override fun addOnDispose(f: (Reaction<*>) -> Unit) {
      _addOnDisposeCalled.reset(true)
    }
  }
}
