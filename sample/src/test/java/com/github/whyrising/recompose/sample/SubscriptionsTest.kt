package com.github.whyrising.recompose.sample

import android.graphics.Color
import android.util.Log
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.toArgb
import com.github.whyrising.recompose.dispatchSync
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.sample.app.Keys
import com.github.whyrising.recompose.sample.app.Keys.formattedTime
import com.github.whyrising.recompose.sample.app.Keys.initializeDb
import com.github.whyrising.recompose.sample.app.Keys.materialThemeColors
import com.github.whyrising.recompose.sample.app.Keys.primaryColor
import com.github.whyrising.recompose.sample.app.Keys.primaryColorName
import com.github.whyrising.recompose.sample.app.Keys.secondaryColorName
import com.github.whyrising.recompose.sample.app.Keys.statusBarDarkIcons
import com.github.whyrising.recompose.sample.app.Keys.time
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.recompose.sample.app.subs.formattedTime
import com.github.whyrising.recompose.sample.app.subs.getPrimaryColorName
import com.github.whyrising.recompose.sample.app.subs.getSecondaryColorName
import com.github.whyrising.recompose.sample.app.subs.getTime
import com.github.whyrising.recompose.sample.app.subs.isLightColor
import com.github.whyrising.recompose.sample.app.subs.primSecondColorReaction
import com.github.whyrising.recompose.sample.app.subs.primaryColorNameReaction
import com.github.whyrising.recompose.sample.app.subs.secondaryColorNameReaction
import com.github.whyrising.recompose.sample.app.subs.secondaryColorReaction
import com.github.whyrising.recompose.sample.app.subs.stringToColor
import com.github.whyrising.recompose.sample.app.subs.themeColors
import com.github.whyrising.recompose.sample.app.subs.timeReaction
import com.github.whyrising.recompose.subs.React
import com.github.whyrising.y.collections.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import java.util.Calendar
import java.util.GregorianCalendar

@ExperimentalCoroutinesApi
class SubscriptionsTest : FreeSpec({
    mockkStatic(Log::class)
    mockkStatic(Color::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Color.parseColor(any()) } returns Blue.toArgb()

    val dispatcher = TestCoroutineDispatcher()
    beforeAny {
        Dispatchers.setMain(dispatcher)
    }

    "getTime(db,query) should return `time` from db" {
        val expectedTime = defaultAppDB.time

        getTime(defaultAppDB, v(time)) shouldBe expectedTime
    }

    "getPrimaryColorName(db,query) should return `primaryColorName` from db" {
        val colorName = defaultAppDB.primaryColor

        getPrimaryColorName(
            defaultAppDB,
            v(primaryColorName)
        ) shouldBe colorName
    }

    "getSecondaryColorName(db,query) should return secondaryColorName from db" {
        val colorName = defaultAppDB.secondaryColor

        getSecondaryColorName(
            defaultAppDB,
            v(secondaryColorName)
        ) shouldBe colorName
    }

    "stringToColor(db,query)" - {
        "should return convert a color name string into a Color" {
            stringToColor("Cyan", v(primaryColor, Blue)) shouldBe Cyan
            stringToColor("cyan", v(primaryColor, Blue)) shouldBe Cyan
        }

        "when color name is identified, return defaultColor from query vector" {
            stringToColor("not-a-color", v(primaryColor, Blue)) shouldBe Blue
        }
    }

    """
        materialThemeColors(colors,query) should return a new theme colors 
        using the given primary and secondary colors
    """ {
        val primary = Blue
        val secondary = Red
        val expectedColors = lightColors(
            primary = primary,
            secondary = secondary
        )

        val themeColors = themeColors(
            v(primary, secondary),
            v(materialThemeColors, lightColors())
        )

        themeColors.primary shouldBe expectedColors.primary
        themeColors.secondary shouldBe expectedColors.secondary
    }

    """
        formattedTime(date,query) should convert the given date to a string of 
        this format `HH:MM:SS`
    """ {
        val date = GregorianCalendar(2021, Calendar.OCTOBER, 16, 1, 13, 56).time

        formattedTime(date, v(formattedTime)) shouldBe "01:13:56"
    }

    "isColorLight(color,query)" {
        isLightColor(Black, v()).shouldBeFalse()
        isLightColor(Magenta, v()).shouldBeFalse()
        isLightColor(White, v()).shouldBeTrue()
        isLightColor(Green, v()).shouldBeTrue()
    }

    fun mockAppDb(mockDb: Any) {
        regEventDb<Any>(id = initializeDb, handler = { _, _ -> mockDb })
        dispatchSync(v(initializeDb))
    }

    "primaryColorNameReaction(query)" {
        mockAppDb(defaultAppDB)
        regSub(primaryColorName, ::getPrimaryColorName)

        val primaryColorName: React<String> = primaryColorNameReaction(v())

        primaryColorName.deref() shouldBe "Pink"
    }

    "secondaryColorNameReaction(query)" {
        mockAppDb(defaultAppDB)
        regSub(secondaryColorName, ::getSecondaryColorName)

        val primaryColorName: React<String> = secondaryColorNameReaction(v())

        primaryColorName.deref() shouldBe "Orange"
    }

    "primSecondColorReaction(query)" {
        mockAppDb(
            defaultAppDB.copy(
                primaryColor = "green",
                secondaryColor = "blue"
            )
        )
        regSub(primaryColorName, ::getPrimaryColorName)
        regSub(secondaryColorName, ::getSecondaryColorName)
        regSub(
            queryId = primaryColor,
            signalsFn = ::primaryColorNameReaction,
            computationFn = ::stringToColor
        )
        regSub(
            queryId = Keys.secondaryColor,
            signalsFn = ::secondaryColorNameReaction,
            computationFn = ::stringToColor
        )

        val (primary, secondary) =
            primSecondColorReaction(v(materialThemeColors, lightColors(), Cyan))

        primary.deref() shouldBe Green
        secondary.deref() shouldBe Blue
    }

    "timeReaction(query)" {
        mockAppDb(defaultAppDB)
        regSub(time, ::getTime)

        val time = timeReaction(v())

        time.deref() shouldBe defaultAppDB.time
    }

    "secondaryColorReaction(query)" - {
        "when colorName is valid, return a reaction of that Color" {
            regSub(secondaryColorName, ::getSecondaryColorName)
            regSub(
                queryId = Keys.secondaryColor,
                signalsFn = ::secondaryColorNameReaction,
                computationFn = ::stringToColor
            )

            val reaction = secondaryColorReaction(v(statusBarDarkIcons, Black))

            reaction.deref() shouldBe Blue
        }

        "when colorName is invalid, return a reaction of that Black" {
            mockAppDb(defaultAppDB.copy(secondaryColor = "invalid-color"))
            regSub(secondaryColorName, ::getSecondaryColorName)
            regSub(
                queryId = Keys.secondaryColor,
                signalsFn = ::secondaryColorNameReaction,
                computationFn = ::stringToColor
            )

            val reaction = secondaryColorReaction(v(statusBarDarkIcons, Black))

            reaction.deref() shouldBe Black
        }
    }
})
