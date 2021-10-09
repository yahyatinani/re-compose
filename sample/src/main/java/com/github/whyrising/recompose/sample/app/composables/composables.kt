package com.github.whyrising.recompose.sample.app.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.sample.app.Keys
import com.github.whyrising.recompose.sample.app.init
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.recompose.w
import com.github.whyrising.y.collections.core.v
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun Clock() {
    Text(
        text = subscribe<String>(v(Keys.formattedTime)).w(),
        style = MaterialTheme.typography.h1,
        fontWeight = FontWeight.SemiBold,
        color = subscribe<Color>(
            v(Keys.primaryColor, MaterialTheme.colors.onSurface)
        ).w()
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
                value = subscribe<String>(v(Keys.timeColorName)).w(),
                singleLine = true,
                maxLines = 1,
                onValueChange = {
                    dispatch(v(Keys.timeColorChange, it))
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
                v(Keys.materialThemeColors, MaterialTheme.colors, defaultColor)
            ).w()
        ) {
            val systemUiController = rememberSystemUiController()

            val color = subscribe<Color>(v(Keys.primaryColor, defaultColor)).w()
            val areStatusBarIconsDark = subscribe<Boolean>(
                v(Keys.statusBarDarkIcons, defaultColor)
            ).w()

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
            Spacer(modifier = Modifier.height(16.dp))
            ColorInput()

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                OutlinedTextField(
                    value = subscribe<String>(v(":a")).w(),
                    onValueChange = { dispatch(v(":a", it)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(text = "a") },
                    keyboardOptions = KeyboardOptions
                        .Default
                        .copy(keyboardType = KeyboardType.Number),
                )

                Text(
                    text = "+",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = subscribe<String>(v(":b")).w(),
                    onValueChange = { dispatch(v(":b", it)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(text = "b") },
                    keyboardOptions = KeyboardOptions
                        .Default
                        .copy(keyboardType = KeyboardType.Number),
                )

                Text(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = subscribe<String>(v(":sum-a-b")).w(),
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(text = "sum") },
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun ShowCase() {
    RecomposeTheme {
        init()
        TimeApp()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ShowCase()
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DefaultDarkPreview() {
    ShowCase()
}
