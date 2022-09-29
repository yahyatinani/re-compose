package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class ReactionBase<T, O> : Reaction<O>, Disposable {
  val id: String by lazy { str(TAG, hashCode()) }

  internal val disposeFns = atom<ISeq<(ReactionBase<T, O>) -> Unit>>(l())

  abstract val reactionScope: CoroutineScope

  internal abstract val state: MutableStateFlow<T>

  internal abstract fun deref(state: State<T>): O

  override fun addOnDispose(f: (ReactionBase<*, *>) -> Unit) {
    disposeFns.swap { it.cons(f) }
  }

  override fun dispose(): Boolean = when (state.subscriptionCount.value) {
    0 -> {
      var fs: ISeq<(ReactionBase<T, O>) -> Unit>? = disposeFns()
      while (fs != null && fs.count > 0) {
        val f = fs.first()
        f(this)
        fs = fs.next()
      }
      reactionScope.cancel(
        "The reaction `$id` got canceled because it had no subscribers."
      )
      true
    }
    else -> false
  }

  // this flag is used to track the last subscriber of this reaction
  internal val isFresh = atom(true)

  internal fun initState(stateValue: T) = MutableStateFlow(stateValue).apply {
    subscriptionCount
      .onEach { subCount ->
        // last subscriber just disappeared => composable left
        // the Composition tree.
        // Reaction is not used by any.
        if (subCount == 0 && !isFresh()) {
          dispose()
        }
        if (isFresh()) {
          isFresh.reset(false)
        }
      }
      .launchIn(reactionScope)
  }

  companion object {
    const val TAG = "rx"
  }
}
