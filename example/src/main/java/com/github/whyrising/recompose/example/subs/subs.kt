package com.github.whyrising.recompose.example.subs

import android.graphics.Color.parseColor
import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
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
      it == "orange" -> Color(parseColor("#ffbf00"))
      it == "purple" -> Color(parseColor("#6A0DAD"))
      it == "pink" -> Color(parseColor("#FBA0E3"))
      hexColorRegex.matches(color) -> Color(parseColor(color))
      else -> default
    }
  }

fun fib(nth: Long): Long = when {
  nth <= 2 -> 1
  else -> fib(nth - 1) + fib(nth - 2)
}

internal fun heavyComp() {
  fib(37)
}

fun regAllSubs(defaultColors: Colors) {
  regSub<AppDb, Date>(queryId = time) { db, _ ->
    db.time
  }

  regSub<Date, String>(
    queryId = formattedTime,
//        context = Dispatchers.Default,
//        placeholder = "...",
    signalsFn = { subscribe(v(time)) },
  ) { date: Date, _: Query ->
    val formattedTime = SimpleDateFormat(HH_MM_SS, Locale.getDefault())
//        heavyComp()
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
    signalsFn = { subscribe<String>(v(primaryColorStr)) }
  ) { colorName, _ ->
    toColor(colorName)
  }

  regSub(
    queryId = secondaryColor,
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
    }
  ) { (primary, secondary), (_, colors) ->
    (colors as Colors).copy(
      primary = primary as Color,
      secondary = secondary as Color
    )
  }
}
