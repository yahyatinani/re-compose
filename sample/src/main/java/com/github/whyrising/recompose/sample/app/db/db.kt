package com.github.whyrising.recompose.sample.app.db

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.sample.app.Keys
import java.util.Date

data class AppSchema(
    val time: Date,
    val primaryColor: String,
    val secondaryColor: String
)

val defaultAppDB = AppSchema(
    time = Date(),
    primaryColor = "Pink",
    secondaryColor = "Orange",
)

fun regCofxs() {
    regCofx(Keys.now) { coeffects: Coeffects ->
        coeffects.assoc(Keys.now, Date())
    }
}
