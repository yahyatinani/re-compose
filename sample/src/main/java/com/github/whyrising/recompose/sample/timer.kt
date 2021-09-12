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
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regEventFx
import com.github.whyrising.recompose.regFx
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.sample.Keys.formattedTime
import com.github.whyrising.recompose.sample.Keys.materialThemeColors
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
import com.github.whyrising.y.collections.core.l
import com.github.whyrising.y.collections.core.m
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val HH_MM_SS = "HH:mm:ss"

fun reg(lifecycleScope: CoroutineScope) {
    regEventDb<AppSchema>(timer) { db, (_, newTime) ->
        db.copy(time = newTime as Date)
    }

    regEventDb<AppSchema>(timeColorChange) { db, (_, color) ->
        db.copy(timeColor = (color as String))
    }

    regEventFx(startTicks) { _, _ ->
        m(Keys.fx to l(l(timeticker, null)))
    }

    regFx(timeticker) {
        lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                dispatch(event(timer, Date()))
                delay(1000)
            }
        }
    }

    regSub(time) { db: AppSchema, _ ->
        db.time
    }

    regSub(timeColorName) { db: AppSchema, _ ->
        db.timeColor
    }

    regSub(
        primaryColor,
        { subscribe<String>(l(timeColorName)) }
    ) { colorStr, (_, defaultColor) ->
        Log.i("MainActivity", "`primaryColor` compFn did run")

        toColor(
            stringColor = colorStr.replaceFirstChar { it.uppercase() },
            default = defaultColor as Color
        )
    }

    regSub(
        materialThemeColors,
        { (_, _, defaultColor): List<Any> ->
            subscribe(event(primaryColor, defaultColor))
        }
    ) { primaryColor: Color, (_, colors): List<Any> ->
        Log.i("MainActivity", "`materialThemeColors` compFn did run")

        (colors as Colors).copy(primary = primaryColor)
    }

    val simpleDateFormat = SimpleDateFormat(HH_MM_SS, Locale.getDefault())
    regSub(
        formattedTime,
        { subscribe(event(time)) }
    ) { date: Date, _ ->
        simpleDateFormat.format(date)
    }

    regSub(
        statusBarDarkIcons,
        { (_, defaultColor): List<Any> ->
            subscribe(event(primaryColor, defaultColor))
        }
    ) { primaryColor: Color, _ ->
        Log.i("MainActivity", "`statusBarDarkIcons` compFn did run")

        primaryColor.luminance() >= 0.5f
    }
}

@Composable
fun Clock() {
    Text(
        text = subscribe<String>(l(formattedTime)).deref(),
        style = MaterialTheme.typography.h1,
        fontWeight = FontWeight.SemiBold,
        color = subscribe<Color>(
            l(primaryColor, MaterialTheme.colors.onSurface)
        ).deref()
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
                value = subscribe<String>(l(timeColorName)).deref(),
                singleLine = true,
                maxLines = 1,
                onValueChange = {
                    dispatch(event(timeColorChange, it))
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
                l(materialThemeColors, MaterialTheme.colors, defaultColor)
            ).deref()
        ) {
            val systemUiController = rememberSystemUiController()

            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = subscribe<Color>(
                        l(primaryColor, defaultColor)
                    ).deref(),
                    darkIcons = subscribe<Boolean>(
                        l(statusBarDarkIcons, defaultColor)
                    ).deref()
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
        reg(CoroutineScope(Dispatchers.Main))
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
        reg(CoroutineScope(Dispatchers.Main))
        TimeApp()
    }
}
