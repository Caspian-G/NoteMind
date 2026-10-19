package com.caspian.notemind.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
private val monthDayFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
private val yearFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

/** 列表里的时间戳，当天只显示时分，同年只显示月日，跨年显示完整日期。 */
fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    return when {
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) ->
            "今天 " + timeFormat.format(Date(millis))

        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
            monthDayFormat.format(Date(millis))

        else -> yearFormat.format(Date(millis))
    }
}
