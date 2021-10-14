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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.sample.app.Keys
import com.github.whyrising.recompose.sample.app.Keys.secondaryColor
import com.github.whyrising.recompose.sample.app.Keys.statusBarDarkIcons
import com.github.whyrising.recompose.sample.app.db.AppSchema
import com.github.whyrising.recompose.sample.app.init
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.recompose.w
import com.github.whyrising.y.collections.core.v
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.util.Date

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
fun ColorInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Surface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.h5,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = value,
                singleLine = true,
                maxLines = 1,
                placeholder = { Text(text = "Color") },
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
fun UpdateStatusBarColor(defaultColor: Color) {
    val systemUiController = rememberSystemUiController()

    val color = subscribe<Color>(v(secondaryColor, defaultColor)).w()
    val darkIcons = subscribe<Boolean>(v(statusBarDarkIcons, defaultColor)).w()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = color,
            darkIcons = darkIcons
        )
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
            UpdateStatusBarColor(defaultColor = defaultColor)

            Text(
                text = "Local time:",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colors.primary
            )

            Clock()

            Spacer(modifier = Modifier.height(16.dp))

            ColorInput(
                label = "Primary color:",
                value = subscribe<String>(v(Keys.primaryColorName)).w()
            ) {
                dispatch(v(Keys.primaryColorChange, it))
            }

            Spacer(modifier = Modifier.height(8.dp))

            ColorInput(
                label = "Secondary color:",
                value = subscribe<String>(v(Keys.secondaryColorName)).w()
            ) {
                dispatch(v(Keys.secondaryColorChange, it))
            }
        }
    }
}

@Composable
fun AppPreview() {
    init(
        initAppDb = AppSchema(
            time = Date(),
            primaryColor = "",
            secondaryColor = "",
        )
    )
    RecomposeTheme {
        TimeApp()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppPreview()
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DefaultDarkPreview() {
    AppPreview()
}
