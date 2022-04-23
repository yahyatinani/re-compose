package com.github.whyrising.recompose.example.composables

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.whyrising.recompose.example.Ids.formattedTime
import com.github.whyrising.recompose.example.events.regAllEvents
import com.github.whyrising.recompose.example.subs.regAllSubs
import com.github.whyrising.recompose.example.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.recompose.w
import com.github.whyrising.y.v

@Composable
fun DigitalWatch(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colors
    val typography = MaterialTheme.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Local time:",
            modifier = Modifier.align(CenterHorizontally),
            style = typography.h3.copy(fontWeight = FontWeight.Light),
            color = colors.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subscribe<String>(v(formattedTime)).w(),
            modifier = Modifier.align(CenterHorizontally),
            color = colors.secondary,
            style = typography.h1.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 80.sp,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DigitalWatchPreview() {
    regAllEvents()
    regAllSubs(MaterialTheme.colors)
    RecomposeTheme {
        Surface {
            DigitalWatch()
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DigitalWatchDarkPreview() {
    RecomposeTheme {
        Surface {
            DigitalWatch()
        }
    }
}
