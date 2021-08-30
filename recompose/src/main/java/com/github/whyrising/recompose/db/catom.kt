package com.github.whyrising.recompose.db

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.whyrising.y.concurrency.Atom
import com.github.whyrising.y.concurrency.IAtom
import com.github.whyrising.y.concurrency.IDeref
import com.github.whyrising.y.concurrency.IRef
import com.github.whyrising.y.concurrency.atom

/**
 * A Compose atom implementation using MutableState<T>.
 */
class CAtom<T>(x: T) : IDeref<T>, IAtom<T> {
    internal val atom: Atom<T> = atom(x)

    internal val state: MutableState<T> = mutableStateOf(x)

    init {
        atom.addWatch(":ui-reaction") { key: Any, _: IRef<T>, _: T, new: T ->
            state.value = new

            key
        }
    }

    override fun deref(): T = state.value

    operator fun invoke(): T = deref()

    override fun reset(newValue: T): T = atom.reset(newValue)

    override fun swap(f: (currentVal: T) -> T): T = atom.swap(f)

    override fun <A> swap(arg: A, f: (currentVal: T, arg: A) -> T): T =
        atom.swap(arg, f)

    override fun <A1, A2> swap(
        arg1: A1,
        arg2: A2,
        f: (currentVal: T, arg1: A1, arg2: A2) -> T
    ): T = atom.swap(arg1, arg2, f)
}

fun <T> catom(x: T): CAtom<T> = CAtom(x)
