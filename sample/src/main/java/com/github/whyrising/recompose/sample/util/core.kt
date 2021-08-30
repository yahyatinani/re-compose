package com.github.whyrising.recompose.sample.util

import android.graphics.Color.parseColor
import androidx.compose.ui.graphics.Color

private val hexColorRegex = Regex("^#([A-Fa-f0-9]{6})$")

fun toColor(stringColor: String): Color = when {
    stringColor == "Red" -> Color.Red
    stringColor == "Blue" -> Color.Blue
    stringColor == "Cyan" -> Color.Cyan
    stringColor == "Green" -> Color.Green
    stringColor == "Yellow" -> Color.Yellow
    stringColor == "Magenta" -> Color.Magenta
    stringColor == "DarkGray" -> Color.DarkGray
    stringColor == "Orange" -> Color(parseColor("#ffbf00"))
    hexColorRegex.matches(stringColor) -> Color(parseColor(stringColor))
    else -> Color.Black
}