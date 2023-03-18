package com.github.whyrising.recompose.subs

import android.util.Log
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.l
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface Disposable {
  fun addOnDispose(f: (Reaction<*>) -> Unit)

  fun dispose(): Boolean
}

abstract class ReactionBase<T, O> : Reaction<O>, Disposable {
  abstract val f: Any?

  abstract val reactionScope: CoroutineScope

  abstract val initialValue: Any?

  // this flag is used to track the last subscriber of this reaction
  internal val isFresh = atom(true)

  internal val disposeFns = atom<ISeq<(Reaction<O>) -> Unit>>(l())

  internal abstract val signalObserver: Job

  internal val _state: MutableStateFlow<Any?> by lazy {
    MutableStateFlow(initialValue).apply {
      subscriptionCount
        .onEach {
          if (!isFresh()) {
            // last subscriber just disappeared => composable left
            // the Composition tree => Reaction is not active.
            if (it == 0) {
              dispose()
            }
          } else if(it > 0) {
            isFresh.reset(false)
          }
        }
        .launchIn(reactionScope)
    }
  }

  override val state: StateFlow<Any?>
    get() = _state

  private val str: String by lazy { "$TAG(${hashCode()}, ${deref()})" }

  override fun addOnDispose(f: (Reaction<*>) -> Unit) {
    disposeFns.swap { it.cons(f) }
  }

  override fun dispose(): Boolean = when {
    _state.subscriptionCount.value != 0 || isFresh() -> false
    else -> {
      var fs: ISeq<(ReactionBase<T, O>) -> Unit>? = disposeFns()
      while (fs != null && fs.count > 0) {
        val f = fs.first()
        f(this)
        fs = fs.next()
      }
      reactionScope.cancel("$this is canceled due to inactivity.")
      true
    }
  }

  override fun toString(): String = str

  protected fun finalize() {
    // FIXME: remove
    Log.i("GCed", toString())
  }

  companion object {
    private const val TAG = "rx"
  }
}
