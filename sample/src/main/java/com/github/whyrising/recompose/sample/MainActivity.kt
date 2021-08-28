package com.github.whyrising.recompose.sample

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color.parseColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.whyrising.recompose.Framework
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.regEventDb
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.sample.Keys.time
import com.github.whyrising.recompose.sample.Keys.timeColor
import com.github.whyrising.recompose.sample.Keys.timeColorChange
import com.github.whyrising.recompose.sample.Keys.timeColorName
import com.github.whyrising.recompose.sample.Keys.timeFormat
import com.github.whyrising.recompose.sample.Keys.timer
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.subscribe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.schedule

private fun reg() {
    regEventDb<AppSchema>(timer) { db, (_, newTime) ->
        db.copy(time = newTime as Date)
    }

    regEventDb<AppSchema>(timeColorChange) { db, (_, newColor) ->
        db.copy(timeColor = newColor as String)
    }

    regSub(time) { db: AppSchema, _ ->
        db.time
    }

    regSub(timeColorName) { db: AppSchema, _ ->
        db.timeColor
    }

    regSub(
        timeColor,
        inputFn = { subscribe<String>(event(timeColorName)) }
    ) { input, _ ->
        val regex = Regex("^#([A-Fa-f0-9]{6})$")
        val colorStr = input as String
        when {
            colorStr == "Red" -> Color.Red
            colorStr == "Blue" -> Color.Blue
            colorStr == "Cyan" -> Color.Cyan
            colorStr == "Green" -> Color.Green
            colorStr == "Yellow" -> Color.Yellow
            colorStr == "Magenta" -> Color.Magenta
            colorStr == "DarkGray" -> Color.DarkGray
            colorStr == "Orange" -> Color(parseColor("#ffbf00"))
            regex.matches(colorStr) -> Color(parseColor(colorStr))
            else -> Color.Black
        }
    }

    regSub(
        timeFormat,
        inputFn = { subscribe<String>(event(time)) }
    ) { input, _ ->
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(input)
    }
}

@Composable
fun Clock() {
    Text(
        text = subscribe<String>(event(timeFormat)),
        style = MaterialTheme.typography.h1,
        fontWeight = FontWeight.SemiBold,
        color = subscribe(event(timeColor))
    )
}

@Composable
fun ColorInput() {
    Surface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Time color:",
                style = MaterialTheme.typography.h5,
            )

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = subscribe<String>(event(timeColorName)),
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

class MainActivity : ComponentActivity() {

    private val framework = Framework()

    override fun onDestroy() {
        super.onDestroy()

        framework.halt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reg()

        setContent {
            LaunchedEffect(true) {
                Timer().schedule(0, 1000) {
                    dispatch(event(timer, Date()))
                }
            }

            RecomposeTheme {
                Surface {
                    TimeApp()
                }
            }
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
