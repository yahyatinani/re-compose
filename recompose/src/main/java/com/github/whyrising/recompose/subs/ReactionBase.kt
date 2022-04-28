package com.github.whyrising.recompose.subs

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.whyrising.y.collections.seq.ISeq
import com.github.whyrising.y.concurrency.atom
import com.github.whyrising.y.l
import com.github.whyrising.y.str
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class ReactionBase<T, O> : ViewModel(), Reaction<O>, Disposable {
    // TODO: Maybe replace Flow with an atom or something because collecting
    //  takes time and the concurrency test fail.
    internal abstract val state: MutableStateFlow<T>

    internal abstract fun deref(state: State<T>): O

    internal fun initState(t: T): MutableStateFlow<T> = MutableStateFlow(t)
        .apply {
            subscriptionCount
                .onEach { subCount ->
                    // TODO: refactor this to ifs without else
                    when {
                        // last subscriber just disappeared => composable left
                        // the Composition tree.
                        subCount == 0 && !isFresh.deref() -> onCleared()
                        else -> isFresh.swap { false }
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
