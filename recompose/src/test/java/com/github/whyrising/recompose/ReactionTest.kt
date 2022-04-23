package com.github.whyrising.recompose

import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.recompose.subs.deref
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.inc
import com.github.whyrising.y.l
import com.github.whyrising.y.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ReactionTest : FreeSpec({
    val context = UnconfinedTestDispatcher()

    beforeAny {
        Dispatchers.setMain(context)
    }

    "ctor" - {
        "default values" {
            val defaultVal = 0
            val f = { _: IPersistentVector<Int> -> defaultVal }
            val reaction = Reaction(v(), context, null, f)

            reaction.isFresh.deref().shouldBeTrue()
            reaction.id shouldBe "rx${reaction.hashCode()}"
            reaction.state.value shouldBe defaultVal
        }

        "when initial is null, value should be calculated through f" {
            val f = { _: IPersistentVector<Int> -> -1 }
            val reaction = Reaction(v(), context, null, f)

            reaction.deref() shouldBeExactly -1
        }

        "when initial is not null, value should be calculated through f" {
            val initial = 0
            val f = { _: IPersistentVector<Int> -> -1 }
            val reaction = Reaction(v(), context, initial, f)

            reaction.deref() shouldBeExactly initial
        }

        "when reaction is first created, state should be lazy" {
            var x = 0
            val f = { _: IPersistentVector<Int> ->
                x = 1
                x
            }

            val reaction = Reaction(v(), context, null, f)

            x shouldBe 0 // f not called yet
            reaction.state.value shouldBe 1 // f got called when state evaluated
            x shouldBe 1
        }
    }

    "deref()" {
        val defaultVal = 0
        val f = { _: IPersistentVector<Int> -> defaultVal }
        val reaction = Reaction(v(), context, null, f)

        reaction.deref() shouldBe defaultVal
    }

    "reset(value)" {
        val f = { _: IPersistentVector<Int> -> 0 }
        val reaction = Reaction(v(), context, null, f)

        reaction.reset(1) shouldBe 1
        reaction.deref() shouldBe 1
    }

    "swap(f)" {
        val f = { _: IPersistentVector<Int> -> 0 }
        val reaction = Reaction(v(), context, null, f)

        reaction.swap { 1 } shouldBe 1
        reaction.deref() shouldBe 1
    }

    "swap(arg,f)" {
        val f = { _: IPersistentVector<Int> -> 1 }
        val reaction = Reaction(v(), context, null, f)

        reaction.swap(2) { currentVal, arg -> currentVal + arg } shouldBe 3
        reaction.deref() shouldBe 3
    }

    "swap(arg1,arg2,f)" {
        val f = { _: IPersistentVector<Int> -> 1 }
        val reaction = Reaction(v(), context, null, f)

        reaction.swap(1, 2) { currentVal: Int, arg1: Int, arg2: Int ->
            currentVal + arg1 + arg2
        } shouldBe 4
        reaction.deref() shouldBe 4
    }

    "addOnDispose(f)" {
        val reaction =
            Reaction(v(), context, null) { _: IPersistentVector<Int> -> 1 }
        val f: (Reaction<Int, Int>) -> Unit = { }

        reaction.addOnDispose(f)

        reaction.disposeFns.deref() shouldBe l(f)
    }

    "dispose()" - {
        """
        should call all functions in disposeFns atom and cancel the
        viewModelScope
        """ {
            var isDisposed = false
            val reaction = Reaction<Int, Int>(v(), context, null) { 0 }
            val f: (Reaction<*, *>) -> Unit = { isDisposed = true }
            reaction.addOnDispose(f)
            reaction.state.value

            reaction.dispose()

            isDisposed.shouldBeTrue()
            reaction.viewModelScope.isActive.shouldBeFalse()
        }

        "when no dispose functions added and scope not active, skip " {
            val reaction = Reaction<Int, Int>(v(), context, null) { 0 }

            reaction.dispose()

            reaction.viewModelScope.isActive.shouldBeFalse()
        }
    }

    "onCleared() should call dispose" {
        var isDisposed = false
        val reaction = Reaction<Int, Int>(v(), context, null) { 0 }
        val f: (Reaction<*, *>) -> Unit = { isDisposed = true }
        reaction.addOnDispose(f)
        reaction.state.value

        reaction.onCleared()

        isDisposed.shouldBeTrue()
        reaction.viewModelScope.isActive.shouldBeFalse()
    }

    "emit(value)" {
        val reaction = Reaction<Int, Int>(v(), context, null) { 1 }

        reaction.emit(15)

        reaction.deref() shouldBe 15
    }

    "deref(subscriptions) should return a vector of dereferenced reactions" {
        val reaction1 = Reaction<Int, Int>(v(), context, null) { 1 }
        val reaction2 = Reaction<Int, Int>(v(), context, null) { 2 }
        val reaction3 = Reaction<Int, Int>(v(), context, null) { 3 }

        deref(v(reaction1, reaction2, reaction3)) shouldBe v(1, 2, 3)
    }

    "input signals" - {
        "one input signal" {
            val input = Reaction<Int, Int>(v(), context, null) { 0 }
            val node = Reaction(v(input), context, null) { signalsValues ->
                inc(signalsValues[0])
            }
            input.emit(5)

            node.deref() shouldBe 6
        }

        "multiple input signals" {
            val input1 = Reaction<Int, Int>(v(), context, null) { 0 }
            val input2 = Reaction<Int, Int>(v(), context, null) { 0 }
            val node = Reaction(v(input1, input2), context, null) { (a, b) ->
                inc(a + b)
            }

            input1.emit(3)
            input2.emit(5)

            node.deref() shouldBe 9
        }

        "concurrency" {
            runTest {
                val input1 = Reaction<Int, Int>(v(), context, null) { 0 }
                val input2 = Reaction<Int, Int>(v(), context, null) { 0 }
                val r = Reaction(v(input1, input2), context, null) { (a, b) ->
                    inc(a + b)
                }

                multiThreadedRun(100, 100) {
                    input1.emit(input1.deref().inc())
                }
                multiThreadedRun(100, 100) {
                    input2.emit(input2.deref().inc())
                }

                r.deref() shouldBe 20001
            }
        }
    }
})
