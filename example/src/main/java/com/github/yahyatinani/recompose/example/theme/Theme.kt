package com.github.yahyatinani.recompose.example.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import com.github.whyrising.y.core.v
import com.github.yahyatinani.recompose.example.Ids.themeColors
import com.github.yahyatinani.recompose.watch

private val DarkColorPalette = darkColors(
  primary = Purple200,
  primaryVariant = Purple700,
  secondary = Teal200
)

private val LightColorPalette = lightColors(
  primary = Purple500,
  primaryVariant = Purple700,
  secondary = Teal200

  /* Other default colors to override
  background = Color.White,
  surface = Color.White,
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = Color.Black,
  onSurface = Color.Black,
  */
)

@Composable
fun RecomposeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  colors: Colors = watch(
    v(themeColors, if (darkTheme) DarkColorPalette else LightColorPalette)
  ),
  content: @Composable () -> Unit
) = MaterialTheme(
  colors = colors,
  typography = Typography,
  shapes = Shapes,
  content = content
)
