package com.github.whyrising.recompose.example.composables

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
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.example.Ids.primaryColorStr
import com.github.whyrising.recompose.example.Ids.secondaryColorStr
import com.github.whyrising.recompose.example.Ids.setPrimaryColor
import com.github.whyrising.recompose.example.Ids.setSecondaryColor
import com.github.whyrising.recompose.example.events.regAllEvents
import com.github.whyrising.recompose.example.subs.regAllSubs
import com.github.whyrising.recompose.example.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.recompose.w
import com.github.whyrising.y.core.v

@Composable
fun InputThemeForm(modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colors
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    OutlinedTextField(
      value = subscribe<String>(v(primaryColorStr)).w(),
      onValueChange = { input ->
        dispatch(v(setPrimaryColor, input))
      },
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
      value = subscribe<String>(v(secondaryColorStr)).w(),
      onValueChange = { input ->
        dispatch(v(setSecondaryColor, input))
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
