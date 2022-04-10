package com.github.whyrising.recompose

import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.subs.Reaction
import com.github.whyrising.recompose.subs.deref
import com.github.whyrising.y.inc
import com.github.whyrising.y.l
import com.github.whyrising.y.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class ReactionTest : FreeSpec({
    val dispatcher = StandardTestDispatcher()

    beforeAny {
        Dispatchers.setMain(dispatcher)
    }

    "ctor" - {
        "default values" {
            val defaultVal = 0
            val f = { defaultVal }
            val reaction = Reaction(f)

            reaction.isFresh.shouldBeTrue()
            reaction.id shouldBe "rx${reaction.hashCode()}"
            reaction.state.value shouldBe defaultVal
        }

        "state should be lazy and f called only when state is evaluated" {
            var x = 0
            val f = {
                x = 1
                x
            }

            val reaction = Reaction(f)

            x shouldBe 0 // f not called yet
            reaction.state.value shouldBe 1 // f got called when state evaluated
            x shouldBe 1
        }
    }

    "deref()" {
        val defaultVal = 0
        val f = { defaultVal }
        val reaction = Reaction(f)

        reaction.deref() shouldBe defaultVal
    }

    "reset(value)" {
        val f = { 0 }
        val reaction = Reaction(f)

        reaction.reset(1) shouldBe 1
        reaction.deref() shouldBe 1
    }

    "swap(f)" {
        val f = { 0 }
        val reaction = Reaction(f)

        reaction.swap { 1 } shouldBe 1
        reaction.deref() shouldBe 1
    }

    "swap(arg,f)" {
        val f = { 1 }
        val reaction = Reaction(f)

        reaction.swap(2) { currentVal, arg -> currentVal + arg } shouldBe 3
        reaction.deref() shouldBe 3
    }

    "swap(arg1,arg2,f)" {
        val f = { 1 }
        val reaction = Reaction(f)

        reaction.swap(1, 2) { currentVal: Int, arg1: Int, arg2: Int ->
            currentVal + arg1 + arg2
        } shouldBe 4
        reaction.deref() shouldBe 4
    }

    "addOnDispose(f)" {
        val reaction = Reaction { 1 }
        val f: (Reaction<Int>) -> Unit = { }

        reaction.addOnDispose(f)

        reaction.disposeFns.deref() shouldBe l(f)
    }

    "dispose" - {
        """
        should call all functions in disposeFns atom and cancel the 
        viewModelScope
        """ {
            var isDisposed = false
            val reaction = Reaction { 0 }
            val f: (Reaction<*>) -> Unit = { isDisposed = true }
            reaction.addOnDispose(f)
            reaction.state.value

            reaction.dispose()

            isDisposed.shouldBeTrue()
            reaction.viewModelScope.isActive.shouldBeFalse()
        }

        "when no dispose functions added and scope not active, skip " {
            val reaction = Reaction { 0 }

            reaction.dispose()

            reaction.viewModelScope.isActive.shouldBeFalse()
        }
    }

    "onCleared() should call dispose" {
        var isDisposed = false
        val reaction = Reaction { 0 }
        val f: (Reaction<*>) -> Unit = { isDisposed = true }
        reaction.addOnDispose(f)
        reaction.state.value

        reaction.onCleared()

        isDisposed.shouldBeTrue()
        reaction.viewModelScope.isActive.shouldBeFalse()
    }

    "emit(value)" {
        val reaction = Reaction { 1 }

        reaction.emit(15)

        reaction.deref() shouldBe 15
    }

    "deref(subscriptions) should return a vector of dereferenced reactions" {
        val reaction1 = Reaction { 1 }
        val reaction2 = Reaction { 2 }
        val reaction3 = Reaction { 3 }

        deref(v(reaction1, reaction2, reaction3)) shouldBe v(1, 2, 3)
    }

    "reactTo(node)" {
        val input = Reaction { 0 }
        input.emit(5)
        val node = Reaction { -1 }

        node.reactTo(input, dispatcher) { newInput ->
            inc(newInput)
        }

        node.deref() shouldBe 6
    }

    "reactTo(nodes)" {
        val inputNode1 = Reaction { 0 }
        val inputNode2 = Reaction { 0 }
        inputNode1.emit(3)
        inputNode2.emit(5)
        val node = Reaction { -1 }

        node.reactTo(v(inputNode1, inputNode2), dispatcher) { newInput ->
            val (a, b) = newInput
            inc(a + b)
        }

        node.deref() shouldBe 9
    }
})
