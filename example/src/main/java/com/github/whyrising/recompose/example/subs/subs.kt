package com.github.whyrising.recompose.example.subs

import com.github.whyrising.recompose.example.Ids.formattedTime
import com.github.whyrising.recompose.example.Ids.time
import com.github.whyrising.recompose.example.db.AppDbSchema
import com.github.whyrising.recompose.regSub
import com.github.whyrising.recompose.subs.Query
import com.github.whyrising.recompose.subscribe
import com.github.whyrising.y.v
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val HH_MM_SS = "HH:mm:ss"

fun regAllSubs() {
    regSub<AppDbSchema, Date>(queryId = time) { db, _ ->
        db.time
    }

    regSub<Date, String>(
        queryId = formattedTime,
        signalsFn = { subscribe(v(time)) },
        computationFn = { date: Date, _: Query ->
            val formattedTime = SimpleDateFormat(HH_MM_SS, Locale.getDefault())
            formattedTime.format(date)
        }
    )
}
