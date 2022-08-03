package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.core.collections.ISeq
import com.github.whyrising.y.core.l
import com.github.whyrising.y.core.str
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class ReactionBase<T, O> : ViewModel(), Reaction<O>, Disposable {
  internal abstract val state: MutableStateFlow<T>

  internal abstract fun deref(state: State<T>): O

  internal fun initState(t: T): MutableStateFlow<T> = MutableStateFlow(t)
    .apply {
      subscriptionCount
        .onEach { subCount ->
          // last subscriber just disappeared => composable left
          // the Composition tree.
          // Reaction is not used by any.
          if (subCount == 0 && !isFresh()) {
            onCleared()
          }

          if (isFresh()) {
            isFresh.reset(false)
          }
        }
        .launchIn(viewModelScope)
    }

  internal val disposeFns = atom<ISeq<(ReactionBase<T, O>) -> Unit>>(l())

  // this flag is used to track the last subscriber of this reaction
  internal var isFresh = atom(true)
  val id: String by lazy { str("rx", hashCode()) }

  override fun addOnDispose(f: (ReactionBase<*, *>) -> Unit) {
    disposeFns.swap { it.cons(f) }
  }

  override fun dispose() {
    var fs: ISeq<(ReactionBase<T, O>) -> Unit>? = disposeFns()
    while (fs != null && fs.count > 0) {
      val f = fs.first()
      f(this)
      fs = fs.next()
    }

    viewModelScope.cancel("This reaction `$id` just got canceled.")
  }

  public override fun onCleared() {
    super.onCleared()

    dispose()
  }
}
