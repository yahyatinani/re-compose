package com.github.whyrising.recompose

import androidx.lifecycle.viewModelScope
import com.github.whyrising.recompose.db.RAtom
import com.github.whyrising.recompose.subs.ComputationReaction
import com.github.whyrising.recompose.subs.ExtractorReaction
import com.github.whyrising.recompose.subs.ReactionBase
import com.github.whyrising.recompose.subs.deref
import com.github.whyrising.recompose.subs.inputsKey
import com.github.whyrising.recompose.subs.stateKey
import com.github.whyrising.y.collections.vector.IPersistentVector
import com.github.whyrising.y.get
import com.github.whyrising.y.inc
import com.github.whyrising.y.l
import com.github.whyrising.y.m
import com.github.whyrising.y.v
import io.kotest.assertions.timing.continually
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds

class ComputationReactionTest : FreeSpec({
    val testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)

    "ctor" - {
        "default values" {
            val defaultVal = 0
            val f = { _: IPersistentVector<Int> -> defaultVal }
            val reaction = ComputationReaction(v(), testDispatcher, null, f)

            reaction.isFresh.deref().shouldBeTrue()
            reaction.id shouldBe "rx${reaction.hashCode()}"
            reaction.state.value shouldBe m(
                stateKey to defaultVal,
                "input" to v<Int>()
            )
        }

        "when initial is null, value should be calculated through f" {
            val f = { _: IPersistentVector<Int> -> -1 }
            val reaction = ComputationReaction(v(), testDispatcher, null, f)

            reaction.deref() shouldBeExactly -1
        }

        "when initial is not null, value should be calculated through f" {
            val initial = 0
            val f = { _: IPersistentVector<Int> -> -1 }
            val reaction = ComputationReaction(v(), testDispatcher, initial, f)

            reaction.deref() shouldBeExactly initial
        }

        "when reaction is first created, state should be lazy" {
            var x = 0
            val f = { _: IPersistentVector<Int> ->
                x = 1
                x
            }

            val reaction = ComputationReaction(v(), testDispatcher, null, f)

            x shouldBe 0 // f not called yet
            reaction.state.value[stateKey] shouldBe 1
            x shouldBe 1
        }
    }

    "deref()" {
        val defaultVal = 0
        val f = { _: IPersistentVector<Int> -> defaultVal }
        val reaction = ComputationReaction(v(), testDispatcher, null, f)

        reaction.deref() shouldBe defaultVal
    }

    "addOnDispose(f)" {
        val reaction =
            ComputationReaction(
                v(),
                testDispatcher,
                null
            ) { _: IPersistentVector<Int> -> 1 }
        val f: (ReactionBase<*, *>) -> Unit = { }

        reaction.addOnDispose(f)

        reaction.disposeFns.deref() shouldBe l(f)
    }

    "dispose()" - {
        """
        should call all functions in disposeFns atom and cancel the
        viewModelScope
        """ {
            var isDisposed = false
            val reaction =
                ComputationReaction<Int, Int>(v(), testDispatcher, null) { 0 }
            val f: (ReactionBase<*, *>) -> Unit = { isDisposed = true }
            reaction.addOnDispose(f)
            reaction.state.value

            reaction.dispose()

            isDisposed.shouldBeTrue()
            reaction.viewModelScope.isActive.shouldBeFalse()
        }

        "when no dispose functions added and scope not active, skip " {
            val reaction =
                ComputationReaction<Int, Int>(v(), testDispatcher, null) { 0 }

            reaction.dispose()

            reaction.viewModelScope.isActive.shouldBeFalse()
        }
    }

    "onCleared() should call dispose" {
        var isDisposed = false
        val reaction =
            ComputationReaction<Int, Int>(v(), testDispatcher, null) { 0 }
        val f: (ReactionBase<*, *>) -> Unit = { isDisposed = true }
        reaction.addOnDispose(f)
        reaction.state.value

        reaction.onCleared()

        isDisposed.shouldBeTrue()
        reaction.viewModelScope.isActive.shouldBeFalse()
    }

    "recompute()" - {
        "should recompute the reaction using the new arg" {
            runTest {
                val input1 = ExtractorReaction(RAtom(1)) { it }
                val input2 = ExtractorReaction(RAtom(2)) { it }
                val reaction = ComputationReaction(
                    inputSignals = v(input1, input2),
                    context = testDispatcher,
                    initial = null
                ) { (a, b) ->
                    a.inc() + b.inc()
                }

                reaction.deref() shouldBeExactly 5

                reaction.recompute(-5, 0)

                reaction.deref() shouldBe -1
                reaction.state.value shouldBe m(
                    stateKey to -1,
                    inputsKey to v(-5, 2)
                )
            }
        }

        "when the same arg passed it should not recompute" {
            val r1 =
                ComputationReaction<Int, Int>(v(), testDispatcher, null) { 1 }
            val r2 =
                ComputationReaction<Int, Int>(v(), testDispatcher, null) { 2 }
            val reaction =
                ComputationReaction(v(r1, r2), testDispatcher, null) { (a, b) ->
                    a.inc() + b.inc()
                }
            reaction.deref() shouldBe 5

            reaction.recompute(1, 0)

            reaction.deref() shouldBe 5
        }
    }

    "deref(subscriptions) should return a vector of dereferenced reactions" {
        val reaction1 =
            ComputationReaction<Int, Int>(v(), testDispatcher, null) { 1 }
        val reaction2 =
            ComputationReaction<Int, Int>(v(), testDispatcher, null) { 2 }
        val reaction3 =
            ComputationReaction<Int, Int>(v(), testDispatcher, null) { 3 }

        deref(v(reaction1, reaction2, reaction3)) shouldBe v(1, 2, 3)
    }

    "input signals" - {
        "one input signal" {
            val input = ExtractorReaction(RAtom(0)) { 0 }
            val r =
                ComputationReaction(v(input), testDispatcher, null) { args ->
                    inc(args[0])
                }
            input.state.emit(5)

            r.deref() shouldBe 6
        }

        "multiple input signals" {
            val input1 = ExtractorReaction(RAtom(0)) { 0 }
            val input2 = ExtractorReaction(RAtom(0)) { 0 }
            val node = ComputationReaction(
                inputSignals = v(input1, input2),
                context = testDispatcher,
                initial = null
            ) { (a, b) ->
                inc(a + b)
            }

            input1.state.emit(3)
            input2.state.emit(5)

            node.deref() shouldBe 9
        }

        "parallel incoming input signals" {
            continually(duration = 60.seconds) {
                runTest {
                    val input1 = ExtractorReaction(RAtom(0)) { 0 }
                    val input2 = ExtractorReaction(RAtom(0)) { 0 }
                    val r = ComputationReaction(
                        inputSignals = v(input1, input2),
                        context = StandardTestDispatcher(),
                        initial = -1
                    ) { (a, b) ->
                        inc(a + b)
                    }

                    multiThreadedRun(100, 100, StandardTestDispatcher()) {
                        input1.state.update { it.inc() }
                    }
                    multiThreadedRun(100, 100, StandardTestDispatcher()) {
                        input2.state.update { it.inc() }
                    }
                    advanceUntilIdle()

                    input1.deref() shouldBeExactly 10000
                    input2.deref() shouldBeExactly 10000
                    r.deref() shouldBeExactly 20001
                }
            }
        }
    }
})
