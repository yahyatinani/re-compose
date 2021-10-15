package com.github.whyrising.recompose.sample.app.subs

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.regSubM
import com.github.whyrising.recompose.sample.app.Keys
import com.github.whyrising.recompose.sample.app.db.AppSchema
import com.github.whyrising.recompose.sample.app.db.defaultAppDB
import com.github.whyrising.recompose.sample.app.fx.HH_MM_SS
import com.github.whyrising.recompose.sample.util.toColor
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.vector.IPersistentVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun regSubs() {
    regEventDb<Any>(Keys.initializeDb) { _, _ ->
        defaultAppDB
    }

    regSub(Keys.time) { AppSchema: AppSchema, _ ->
        AppSchema.time
    }

    regSub(Keys.primaryColorName) { AppSchema: AppSchema, _ ->
        AppSchema.primaryColor
    }

    regSub(Keys.secondaryColorName) { AppSchema: AppSchema, _ ->
        AppSchema.secondaryColor
    }

    regSub(
        Keys.primaryColor,
        { subscribe<String>(v(Keys.primaryColorName)) }
    ) { colorStr, (_, defaultColor) ->
        toColor(
            stringColor = colorStr.lowercase(),
            default = defaultColor as Color
        )
    }

    regSub(
        Keys.secondaryColor,
        { subscribe<String>(v(Keys.secondaryColorName)) }
    ) { colorStr, (_, defaultColor) ->
        toColor(
            stringColor = colorStr.lowercase(),
            default = defaultColor as Color
        )
    }

    regSubM<Color, Colors>(
        Keys.materialThemeColors,
        { (_, _, defaultColor) ->
            v(
                subscribe(v(Keys.primaryColor, defaultColor)),
                subscribe(v(Keys.secondaryColor, defaultColor))
            )
        }
    ) { (primaryColor, secondaryColor), (_, colors) ->
        (colors as Colors).copy(
            primary = primaryColor,
            secondary = secondaryColor
        )
    }

    val simpleDateFormat = SimpleDateFormat(HH_MM_SS, Locale.getDefault())
    regSub(
        Keys.formattedTime,
        { subscribe(v(Keys.time)) }
    ) { date: Date, _ ->
        simpleDateFormat.format(date)
    }

    regSub(
        Keys.statusBarDarkIcons,
        { (_, defaultColor): IPersistentVector<Any> ->
            subscribe(v(Keys.primaryColor, defaultColor))
        }
    ) { primaryColor: Color, _ ->
        primaryColor.luminance() >= 0.5f
    }
}
