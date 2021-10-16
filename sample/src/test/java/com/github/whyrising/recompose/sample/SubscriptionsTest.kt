package com.github.whyrising.recompose.sample

import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import com.github.whyrising.recompose.sample.app.Keys.formattedTime
import com.github.whyrising.recompose.sample.app.Keys.materialThemeColors
import com.github.whyrising.recompose.sample.app.Keys.primaryColor
import com.github.whyrising.recompose.sample.app.Keys.primaryColorName
import com.github.whyrising.recompose.sample.app.Keys.secondaryColorName
import com.github.whyrising.recompose.sample.app.Keys.time
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.recompose.sample.app.subs.formattedTime
import com.github.whyrising.recompose.sample.app.subs.getPrimaryColorName
import com.github.whyrising.recompose.sample.app.subs.getSecondaryColorName
import com.github.whyrising.recompose.sample.app.subs.getTime
import com.github.whyrising.recompose.sample.app.subs.isLightColor
import com.github.whyrising.recompose.sample.app.subs.stringToColor
import com.github.whyrising.recompose.sample.app.subs.themeColors
import com.github.whyrising.y.collections.core.v
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.GregorianCalendar

class SubscriptionsTest : FreeSpec({
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
})
