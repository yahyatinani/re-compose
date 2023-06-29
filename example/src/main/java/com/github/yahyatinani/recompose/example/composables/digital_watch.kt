package com.github.yahyatinani.recompose.example.composables

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
import com.github.yahyatinani.recompose.example.Ids
import com.github.yahyatinani.recompose.example.events.regAllEvents
import com.github.yahyatinani.recompose.example.initAppDb
import com.github.yahyatinani.recompose.example.subs.regAllSubs
import com.github.yahyatinani.recompose.example.theme.RecomposeTheme
import com.github.yahyatinani.recompose.watch
import io.github.yahyatinani.y.core.v

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
      color = colors.primary
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = watch<String>(v(Ids.formattedTime)),
      modifier = Modifier.align(CenterHorizontally),
      color = colors.secondary,
      style = typography.h1.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 72.sp
      )
    )
  }
}

@Preview(showBackground = true)
@Composable
fun DigitalWatchPreview() {
  initAppDb()
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
  regAllEvents()
  regAllSubs(MaterialTheme.colors)
  RecomposeTheme {
    Surface {
      DigitalWatch()
    }
  }
}
