package com.github.yahyatinani.recompose.subs

import io.github.yahyatinani.y.concurrency.atom
import io.github.yahyatinani.y.core.collections.ISeq
import io.github.yahyatinani.y.core.l
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

abstract class ReactionBase<T, O>(override val id: Any) : Reaction<O> {
  internal val disposeFns = atom<ISeq<(Reaction<O>) -> Unit>>(
    l({
      isDisposed.reset(true)
      reactionScope.cancel("$this is canceled due to inactivity.")
      viewSubCount.removeWatch(id)
    })
  )

  /** This flag is used to track the last subscriber of this reaction. */
  internal val isFresh = atom(true)

  abstract val reactionScope: CoroutineScope

  val isDisposed = atom(false)

  private val viewSubCount = atom(0).apply {
    addWatch(id) { _, _, _, new ->
      if (isNotActive(subCount = new)) {
        reactionScope.launch {
          delay(5000) // wait for the view maybe it's a screen rotation?
          dispose()
        }
      }
    }
  }

  @Suppress("PropertyName")
  internal val _state: Lazy<MutableStateFlow<Any?>> = lazy {
    MutableStateFlow(initialValue).apply {
      subscriptionCount
        .transform<Int, Unit> {
          if (it > 0 && isFresh.deref()) { // first subscriber.
            isFresh.reset(false)
          } else if (it == 0 && !isFresh.deref()) { // last sub disappeared.
            delay(5000) // wait for the view maybe it's a screen rotation?
            dispose()
          }
        }
        .launchIn(reactionScope)
    }
  }

  abstract val state: StateFlow<Any?>

  private fun isNotActive(subCount: Int = viewSubCount.deref()) =
    subCount == 0 &&
      (!_state.isInitialized() || _state.value.subscriptionCount.value == 0)

  internal fun decUiSubCount() {
    viewSubCount.swap { if (it < 0) 0 else it.dec() }
  }

  internal fun incUiSubCount() {
    viewSubCount.swap { it.inc() }
  }

  override fun addOnDispose(f: (Reaction<*>) -> Unit) {
    disposeFns.swap { it.cons(f) }
  }

  override fun dispose(): Boolean = when {
    !isDisposed.deref() && isNotActive() -> {
      var fs: ISeq<(ReactionBase<T, O>) -> Unit>? = disposeFns.deref()
      while (fs != null && fs.count > 0) {
        val f = fs.first()
        f(this)
        fs = fs.next()
      }
      true
    }

    else -> false
  }

  abstract val category: Char

  private val str: String by lazy { "$TAG$category($id)" }

  override fun toString(): String = str

  // FIXME: comment this out.
  /* protected fun finalize() {
     Log.d(TAG, "GCed: $id")
   }*/

  companion object {
    internal const val TAG = "rx"
  }
}
