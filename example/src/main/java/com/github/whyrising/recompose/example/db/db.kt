package com.github.whyrising.recompose.example.db

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.example.Ids.now
import java.util.Date

data class AppDbSchema(
    val time: Date
)

val defaultAppDB = AppDbSchema(
    time = Date(),
)

fun regAllCofx() {
    regCofx(now) { coeffects: Coeffects ->
        coeffects.assoc(now, Date())
    }
}
