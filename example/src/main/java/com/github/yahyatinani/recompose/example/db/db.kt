package com.github.yahyatinani.recompose.example.db

import com.github.yahyatinani.recompose.cofx.Coeffects
import com.github.yahyatinani.recompose.cofx.regCofx
import com.github.yahyatinani.recompose.example.Ids.now
import java.util.Date

data class AppDb(
  val time: Date = Date(),
  val primaryColor: String = "Red",
  val secondaryColor: String = "Orange",
  val showAboutDialog: Boolean = false,
  val info: String = "Awesome app"
)

fun regAllCofx() {
  regCofx(now) { coeffects: Coeffects ->
    coeffects.assoc(now, Date())
  }
}
