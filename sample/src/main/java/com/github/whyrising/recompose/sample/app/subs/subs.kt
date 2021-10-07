package com.github.whyrising.recompose.sample.app.subs

import android.util.Log
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

fun toFloatOrNull(n: String): Float? = try {
    n.toFloat()
} catch (e: NumberFormatException) {
    null
}

fun regSubs() {
    regEventDb<Any>(Keys.initialize) { _, _ ->
        defaultAppDB
    }

    regSub(Keys.time) { AppSchema: AppSchema, _ ->
        AppSchema.time
    }

    regSub(Keys.timeColorName) { AppSchema: AppSchema, _ ->
        AppSchema.timeColor
    }

    regSub(
        Keys.primaryColor,
        { subscribe<String>(v(Keys.timeColorName)) }
    ) { colorStr, (_, defaultColor) ->
        Log.i("MainActivity", "`primaryColor` compFn did run")

        toColor(
            stringColor = colorStr.lowercase(),
            default = defaultColor as Color
        )
    }

    regSub(
        Keys.materialThemeColors,
        { (_, _, defaultColor) ->
            subscribe(v(Keys.primaryColor, defaultColor))
        }
    ) { primaryColor: Color, (_, colors) ->
        Log.i("MainActivity", "`materialThemeColors` compFn did run")

        (colors as Colors).copy(primary = primaryColor)
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
        Log.i("MainActivity", "`statusBarDarkIcons` compFn did run")

        primaryColor.luminance() >= 0.5f
    }

    regSub(":a") { AppSchema: AppSchema, _ ->
        AppSchema.a.trim()
    }

    regSub(":b") { AppSchema: AppSchema, _ ->
        AppSchema.b.trim()
    }

    regSubM(
        ":sum-a-b",
        { v(subscribe<String>(v(":a")), subscribe(v(":b"))) }
    ) { (a, b), _ ->
        val n = toFloatOrNull(a)
        val m = toFloatOrNull(b)

        when {
            n != null && m != null -> "${n + m}"
            else -> ""
        }
    }
}
