package com.github.whyrising.recompose.example.db

import com.github.whyrising.recompose.cofx.Coeffects
import com.github.whyrising.recompose.cofx.regCofx
import com.github.whyrising.recompose.example.Ids.now
import java.util.Date

data class AppDb(
    val time: Date = Date(),
    val primaryColor: String = "Pink",
    val secondaryColor: String = "Cyan",
)

fun regAllCofx() {
    regCofx(now) { coeffects: Coeffects ->
        coeffects.assoc(now, Date())
    }
}
