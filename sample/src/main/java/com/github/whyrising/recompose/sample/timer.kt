package com.github.whyrising.recompose.sample

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.whyrising.recompose.Keys
import com.github.whyrising.recompose.Keys.db
import com.github.whyrising.recompose.cofx.injectCofx
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regEventFx
import com.github.whyrising.recompose.regFx
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.sample.Keys.formattedTime
import com.github.whyrising.recompose.sample.Keys.materialThemeColors
import com.github.whyrising.recompose.sample.Keys.now
import com.github.whyrising.recompose.sample.Keys.primaryColor
import com.github.whyrising.recompose.sample.Keys.startTicks
import com.github.whyrising.recompose.sample.Keys.statusBarDarkIcons
import com.github.whyrising.recompose.sample.Keys.time
import com.github.whyrising.recompose.sample.Keys.timeColorChange
import com.github.whyrising.recompose.sample.Keys.timeColorName
import com.github.whyrising.recompose.sample.Keys.timer
import com.github.whyrising.recompose.sample.Keys.timeticker
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.sample.util.toColor
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.recompose.watch
import com.github.whyrising.y.collections.concretions.vector.PersistentVector
import com.github.whyrising.y.collections.core.get
import com.github.whyrising.y.collections.core.m
import com.github.whyrising.y.collections.core.v
import com.github.whyrising.y.collections.map.IPersistentMap
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val HH_MM_SS = "HH:mm:ss"

fun reg(scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)) {
    regFx(timeticker) {
        scope.launch(Dispatchers.Default) {
            while (true) {
                dispatch(v(timer))
                delay(1000)
            }
        }
    }

    regEventFx(startTicks) { _, _ ->
        m(Keys.fx to v(v(timeticker, null)))
    }

    regCofx(now) {
        it.assoc(now, Date())
    }

    regEventFx(
        id = timer,
        interceptors = v(injectCofx(now))
    ) { cofx: IPersistentMap<Any, Any>, _ ->
        val db = get(cofx, db) as AppSchema

        m(Keys.db to db.copy(time = get(cofx, now) as Date))
    }

    regEventDb<AppSchema>(timeColorChange) { db, (_, color) ->
        db.copy(timeColor = (color as String))
    }

    regSub(time) { db: AppSchema, _ ->
        db.time
    }

    regSub(timeColorName) { db: AppSchema, _ ->
        db.timeColor
    }

    regSub(
        primaryColor,
        { subscribe<String>(v(timeColorName)) }
    ) { colorStr, (_, defaultColor) ->
        Log.i("MainActivity", "`primaryColor` compFn did run")

        toColor(
            stringColor = colorStr.lowercase(),
            default = defaultColor as Color
        )
    }

    regSub(
        materialThemeColors,
        { (_, _, defaultColor) ->
            subscribe(v(primaryColor, defaultColor))
        }
    ) { primaryColor: Color, (_, colors) ->
        Log.i("MainActivity", "`materialThemeColors` compFn did run")

        (colors as Colors).copy(primary = primaryColor)
    }

    val simpleDateFormat = SimpleDateFormat(HH_MM_SS, Locale.getDefault())
    regSub(
        formattedTime,
        { subscribe(v(time)) }
    ) { date: Date, _ ->
        simpleDateFormat.format(date)
    }

    regSub(
        statusBarDarkIcons,
        { (_, defaultColor): PersistentVector<Any> ->
            subscribe(v(primaryColor, defaultColor))
        }
    ) { primaryColor: Color, _ ->
        Log.i("MainActivity", "`statusBarDarkIcons` compFn did run")

        primaryColor.luminance() >= 0.5f
    }
}

@Composable
fun Clock() {
    Text(
        text = subscribe<String>(v(formattedTime)).watch(),
        style = MaterialTheme.typography.h1,
        fontWeight = FontWeight.SemiBold,
        color = subscribe<Color>(
            v(primaryColor, MaterialTheme.colors.onSurface)
        ).watch()
    )
}

@Composable
fun ColorInput() {
    Surface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Primary color:",
                style = MaterialTheme.typography.h5
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = subscribe<String>(v(timeColorName)).watch(),
                singleLine = true,
                maxLines = 1,
                onValueChange = {
                    dispatch(v(timeColorChange, it))
                }
            )
        }
    }
}

@Composable
fun TimeApp() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val defaultColor = MaterialTheme.colors.onSurface
        MaterialTheme(
            colors = subscribe<Colors>(
                v(materialThemeColors, MaterialTheme.colors, defaultColor)
            ).watch()
        ) {
            val systemUiController = rememberSystemUiController()

            val color = subscribe<Color>(v(primaryColor, defaultColor)).watch()
            val areStatusBarIconsDark = subscribe<Boolean>(
                v(statusBarDarkIcons, defaultColor)
            ).watch()

            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = color,
                    darkIcons = areStatusBarIconsDark
                )
            }

            Text(
                text = "Local time:",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colors.primary
            )
            Clock()
            ColorInput()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RecomposeTheme {
        initialize()
        reg()
        TimeApp()
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun DefaultDarkPreview() {
    RecomposeTheme {
        initialize()
        reg()
        TimeApp()
    }
}
