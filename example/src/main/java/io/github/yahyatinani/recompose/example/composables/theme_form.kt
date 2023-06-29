package io.github.yahyatinani.recompose.example.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yahyatinani.recompose.dispatchSync
import io.github.yahyatinani.recompose.example.Ids
import io.github.yahyatinani.recompose.example.Ids.secondaryColorStr
import io.github.yahyatinani.recompose.example.Ids.setSecondaryColor
import io.github.yahyatinani.recompose.example.events.regAllEvents
import io.github.yahyatinani.recompose.example.initAppDb
import io.github.yahyatinani.recompose.example.subs.regAllSubs
import io.github.yahyatinani.recompose.example.theme.RecomposeTheme
import io.github.yahyatinani.recompose.watch
import io.github.yahyatinani.y.core.v

@Composable
fun InputThemeForm(modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colors
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    OutlinedTextField(
      value = watch<String>(v(Ids.primaryColorStr)),
      onValueChange = { input -> dispatchSync(v(Ids.setPrimaryColor, input)) },
      placeholder = { Text(text = "Primary Color") },
      singleLine = true,
      maxLines = 1,
      colors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = colors.primary,
        focusedBorderColor = colors.primary
      )
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = watch<String>(v(secondaryColorStr)),
      onValueChange = { input ->
        dispatchSync(v(setSecondaryColor, input))
      },
      placeholder = { Text(text = "Secondary Color") },
      singleLine = true,
      maxLines = 1,
      colors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = colors.secondary,
        focusedBorderColor = colors.secondary
      )
    )
  }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun InputThemeFormPreview() {
  initAppDb()
  regAllEvents()
  regAllSubs(MaterialTheme.colors)
  RecomposeTheme {
    Surface {
      InputThemeForm()
    }
  }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun InputThemeFormDarkPreview() {
  RecomposeTheme {
    Surface {
      InputThemeForm()
    }
  }
}
