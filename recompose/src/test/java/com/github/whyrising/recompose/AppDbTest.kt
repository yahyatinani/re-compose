package com.github.whyrising.recompose

import com.github.whyrising.recompose.db.CAtom
import com.github.whyrising.recompose.db.catom
import com.github.whyrising.y.collections.core.get
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull

class AppDbTest : FreeSpec({
    "Catom" - {
        "ctor" {
            val x = 415

            val catom = CAtom(x)
            val watches = catom.atom.watches
            val callback = get(watches, ":ui-reaction")

            catom.atom() shouldBeExactly x
            catom.state.value shouldBeExactly x
            watches.count shouldBeExactly 1
            callback.shouldNotBeNull()
        }

        ":ui-reaction watcher should update state after atom got updated" {
            val n = 14
            val catom = CAtom(415)

            catom.atom.reset(n)

            catom.state.value shouldBeExactly n
        }

        "appDb multithreading" {
            val catom = CAtom(0)

            multiThreadedRun {
                catom.atom.swap { it + 1 }
            }

            catom.atom() shouldBeExactly 100000
            catom.state.value shouldBeExactly 100000
        }

        "deref()" {
            val catom = CAtom(0)

            catom.deref() shouldBeExactly 0
        }

        "invoke()" {
            val catom = CAtom(0)

            catom() shouldBeExactly 0
        }

        "swap()" {
            val catom = CAtom(0)

            catom.swap { it + 1 }

            catom() shouldBeExactly 1
            catom.state.value shouldBeExactly 1
        }

        "catom(x)" {
            val catom: CAtom<Int> = catom(0)

            catom() shouldBeExactly 0
        }
    }
})
