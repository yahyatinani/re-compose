package io.github.yahyatinani.recompose.example.subs

import android.graphics.Color.parseColor
import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Yellow
import io.github.yahyatinani.recompose.example.Ids
import io.github.yahyatinani.recompose.example.Ids.formattedTime
import io.github.yahyatinani.recompose.example.Ids.primaryColor
import io.github.yahyatinani.recompose.example.Ids.primaryColorStr
import io.github.yahyatinani.recompose.example.Ids.secondaryColor
import io.github.yahyatinani.recompose.example.Ids.secondaryColorStr
import io.github.yahyatinani.recompose.example.Ids.themeColors
import io.github.yahyatinani.recompose.example.Ids.time
import io.github.yahyatinani.recompose.example.db.AppDb
import io.github.yahyatinani.recompose.regSub
import io.github.yahyatinani.recompose.subs.Query
import io.github.yahyatinani.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val HH_MM_SS = "HH:mm:ss"

private val hexColorRegex = Regex("^#([A-Fa-f0-9]{6})$")

fun toColor(color: String): Color = color
  .lowercase()
  .trim()
  .let {
    when {
      it == "red" -> Red
      it == "blue" -> Color.Blue
      it == "cyan" -> Color.Cyan
      it == "green" -> Color.Green
      it == "yellow" -> Yellow
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
      else -> Color.Gray.copy(alpha = .5f)
    }
  }

fun fib(nth: Long): Long = when {
  nth <= 2 -> 1
  else -> fib(nth - 1) + fib(nth - 2)
}

internal fun heavyComp() {
  fib(40)
}

fun regAllSubs(defaultColors: Colors) {
  regSub<AppDb>(queryId = time) { db, _ ->
    db.time
  }

  val simpleDateFormat = SimpleDateFormat(HH_MM_SS, Locale.getDefault())

  regSub(
    queryId = formattedTime,
    initialValue = "...",
    inputSignal = v(time)
  ) { date: Date, _, _: Query ->
//    heavyComp()
    simpleDateFormat.format(date)
  }

  regSub<AppDb>(queryId = primaryColorStr) { db, _ ->
    db.primaryColor
  }

  regSub<AppDb>(queryId = secondaryColorStr) { db, _ ->
    db.secondaryColor
  }

  regSub(
    queryId = primaryColor,
    initialValue = Red,
    inputSignal = v(primaryColorStr)
  ) { colorName: String, _, _ ->
    toColor(colorName)
  }

  regSub(
    queryId = secondaryColor,
    initialValue = Yellow,
    inputSignal = v(secondaryColorStr)
  ) { colorName: String, _, _ ->
    toColor(colorName)
  }

  regSub(
    queryId = themeColors,
    initialValue = defaultColors,
    v(primaryColor),
    v(secondaryColor)
  ) { (primary, secondary), _, (_, colors) ->
    withContext(Dispatchers.Main) {
      (colors as Colors).copy(
        primary = primary as Color,
        secondary = secondary as Color
      )
    }
  }

  regSub<AppDb>(queryId = Ids.about_dialog) { db, _ ->
    db.showAboutDialog
  }

  regSub<AppDb>(queryId = Ids.info) { db, _ ->
    db.info
  }

  // ** test -------------------------------------------------------------------

  /*  fun info(appDb: AppDb): String = appDb.info
    fun showAboutDialog(appDb: AppDb): Boolean = appDb.showAboutDialog

    regSub(queryId = Ids.info, ::info)

    regSub(queryId = Ids.about_dialog, ::showAboutDialog)

  //  regSub(queryId = Ids.about_dialog, "")

    regSub(
      queryId = secondaryColor,
      initialValue = Yellow,
      v(secondaryColorStr),
      computationFn = ::toColor

        regSub(
      queryId = themeColors,
      initialValue = defaultColors,
      inputSignals = { query ->
        v(
          subscribe(v(primaryColor)),
          subscribe(v(secondaryColor))
        )
      }
    ) { (primary, secondary), _, (_, colors) ->
      withContext(Dispatchers.Main) {
        (colors as Colors).copy(
          primary = primary as Color,
          secondary = secondary as Color
        )
      }
    }
    )*/
}
