package com.github.whyrising.recompose.sample.util

import android.graphics.Color.parseColor
import androidx.compose.ui.graphics.Color

private val hexColorRegex = Regex("^#([A-Fa-f0-9]{6})$")

fun toColor(stringColor: String, default: Color = Color.Black): Color = when {
    stringColor == "red" -> Color.Red
    stringColor == "blue" -> Color.Blue
    stringColor == "cyan" -> Color.Cyan
    stringColor == "green" -> Color.Green
    stringColor == "yellow" -> Color.Yellow
    stringColor == "magenta" -> Color.Magenta
    stringColor == "darkGray" -> Color.DarkGray
    stringColor == "orange" -> Color(parseColor("#ffbf00"))
    stringColor == "purple" -> Color(parseColor("#6A0DAD"))
    stringColor == "pink" -> Color(parseColor("#FBA0E3"))
    hexColorRegex.matches(stringColor) -> Color(parseColor(stringColor))
    else -> default
}
