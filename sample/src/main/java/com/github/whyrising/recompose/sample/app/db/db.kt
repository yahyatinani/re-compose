package com.github.whyrising.recompose.sample.app.db

import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.sample.app.Keys
import java.util.Date

data class AppSchema(
    val time: Date,
    val timeColor: String,
    val a: String = "",
    val b: String = "",
)

val defaultAppDB = AppSchema(
    time = Date(),
    timeColor = "Pink",
)

fun regCofxs() {
    regCofx(Keys.now) {
        it.assoc(Keys.now, Date())
    }
}
