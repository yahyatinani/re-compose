package com.github.whyrising.recompose.example.subs

import android.graphics.Color.parseColor
import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import com.github.whyrising.recompose.example.Ids
import com.github.whyrising.recompose.example.Ids.formattedTime
import com.github.whyrising.recompose.example.Ids.primaryColor
import com.github.whyrising.recompose.example.Ids.primaryColorStr
import com.github.whyrising.recompose.example.Ids.secondaryColor
import com.github.whyrising.recompose.example.Ids.secondaryColorStr
import com.github.whyrising.recompose.example.Ids.themeColors
import com.github.whyrising.recompose.example.Ids.time
import com.github.whyrising.recompose.example.db.AppDb
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.regSubM
import com.github.whyrising.recompose.subs.Query
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val HH_MM_SS = "HH:mm:ss"

private val hexColorRegex = Regex("^#([A-Fa-f0-9]{6})$")

fun toColor(color: String, default: Color = Color.Gray): Color = color
  .lowercase()
  .trim()
  .let {
    when {
      it == "red" -> Color.Red
      it == "blue" -> Color.Blue
      it == "cyan" -> Color.Cyan
      it == "green" -> Color.Green
      it == "yellow" -> Color.Yellow
      it == "magenta" -> Color.Magenta
      it == "darkGray" -> Color.DarkGray
      it == "black" -> Color.Black
      it == "orange" -> Color(parseColor("#ff8c00"))
      it == "teal" -> Color(parseColor("#008080"))
      it == "golden" -> Color(parseColor("#ffd700"))
      it == "purple" -> Color(parseColor("#6A0DAD"))
      it == "pink" -> Color(parseColor("#FBA0E3"))
      it == "brown" -> Color(parseColor("#8b4513"))
      hexColorRegex.matches(color) -> Color(parseColor(color))
      else -> default
    }
  }

fun fib(nth: Long): Long = when {
  nth <= 2 -> 1
  else -> fib(nth - 1) + fib(nth - 2)
}

internal fun heavyComp() {
//  fib(40)
}

fun regAllSubs(defaultColors: Colors) {
  regSub<AppDb, Date>(queryId = time) { db, _ ->
    db.time
  }

  regSub(
    queryId = formattedTime,
    initialValue = "...",
    signalsFn = { subscribe(v(time)) }
  ) { date: Date, _: Query ->
    val formattedTime = SimpleDateFormat(HH_MM_SS, Locale.getDefault())
//    heavyComp()
    formattedTime.format(date)
  }

  regSub<AppDb, String>(queryId = primaryColorStr) { db, _ ->
    db.primaryColor
  }

  regSub<AppDb, String>(queryId = secondaryColorStr) { db, _ ->
    db.secondaryColor
  }

  regSub(
    queryId = primaryColor,
    initialValue = Color.Red,
    signalsFn = { subscribe<String>(v(primaryColorStr)) }
  ) { colorName, _ ->
    toColor(colorName)
  }

  regSub(
    queryId = secondaryColor,
    initialValue = Color.Yellow,
    signalsFn = { subscribe<String>(v(secondaryColorStr)) }
  ) { colorName, _ ->
    toColor(colorName)
  }

  regSubM(
    queryId = themeColors,
    signalsFn = {
      v(
        subscribe(v(primaryColor)),
        subscribe(v(secondaryColor))
      )
    },
    initialValue = defaultColors
  ) { (primary, secondary), (_, colors) ->
    if (primary == null || secondary == null) colors
    else withContext(Dispatchers.Main.immediate) {
      (colors as Colors).copy(
        primary = primary as Color,
        secondary = secondary as Color
      )
    }
  }

  regSub<AppDb, Boolean>(queryId = Ids.about_dialog) { db, _ ->
    db.showAboutDialog
  }

  regSub<AppDb, String>(queryId = Ids.info) { db, _ ->
    db.info
  }
}
