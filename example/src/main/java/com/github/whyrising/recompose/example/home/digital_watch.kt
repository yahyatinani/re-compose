package com.github.whyrising.recompose.example.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.github.whyrising.recompose.example.ui.theme.RecomposeTheme

@Composable
fun DigitalWatch(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colors.primary
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
            color = color,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "20:15:45",
            modifier = Modifier.align(CenterHorizontally),
            style = typography.h1.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 80.sp,
            ),
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RecomposeTheme {
        Surface {
            DigitalWatch()
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DefaultDarkPreview() {
    RecomposeTheme {
        Surface {
            DigitalWatch()
        }
    }
}
