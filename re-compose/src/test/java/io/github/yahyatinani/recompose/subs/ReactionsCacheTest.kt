package io.github.yahyatinani.recompose.subs

import android.util.Log
import io.github.yahyatinani.y.concurrency.atom
import io.github.yahyatinani.y.core.get
import io.github.yahyatinani.y.core.m
import io.github.yahyatinani.y.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

  beforeEach { reactionsCache.reset(m()) }

  "cacheReaction()" - {
    "should call addOnDispose(f) on the cached reaction" {
      val reaction = MockReaction()

      cacheReaction(v(v("test"), v()), reaction as ReactionBase<*, *>)

      reaction.addOnDisposeCalled.shouldBeTrue()
    }

    "should add the reaction to the cache map" {
      val key = v("test")
      val reaction = Extraction(atom(1), id = "id") { (it as Int).inc() }

      val cached = cacheReaction(key, reaction)

      reactionsCache.deref()[key].shouldNotBeNull()
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

    override fun addOnDispose(f: (Reaction<*>) -> Unit) {
      _addOnDisposeCalled.reset(true)
    }

    override val category: Char
      get() = TODO("Not yet implemented")
  }
}
