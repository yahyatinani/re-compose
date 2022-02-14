package com.github.whyrising.recompose.example.home

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
import com.github.whyrising.recompose.example.ui.theme.RecomposeTheme

@Composable
fun ThemeForm(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = { input -> },
            placeholder = { Text(text = "Primary Color") },
            singleLine = true,
            maxLines = 1,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.primary,
                focusedBorderColor = MaterialTheme.colors.primary,
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { input -> },
            placeholder = { Text(text = "Secondary Color") },
            singleLine = true,
            maxLines = 1,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.secondary,
                focusedBorderColor = MaterialTheme.colors.secondary,
            )
        )
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun LightPreview() {
    RecomposeTheme {
        Surface {
            ThemeForm()
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkPreview() {
    RecomposeTheme {
        Surface {
            ThemeForm()
        }
    }
}
