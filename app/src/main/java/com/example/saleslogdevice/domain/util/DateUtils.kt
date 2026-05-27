package com.example.saleslogdevice.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).apply {
        timeZone = TimeZone.getDefault()
    }
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN).apply {
        timeZone = TimeZone.getDefault()
    }
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).apply {
        timeZone = TimeZone.getDefault()
    }

    fun today(): String = dateFormat.format(Date())

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    /** 終了 - 開始 を分単位で返す。負なら 0。 */
    fun diffMinutes(startMillis: Long, endMillis: Long): Int =
        ((endMillis - startMillis).coerceAtLeast(0L) / 60_000L).toInt()

    /** 分を "H時間M分" or "M分" に整形。 */
    fun humanDuration(minutes: Int): String =
        if (minutes >= 60) "${minutes / 60}時間${minutes % 60}分" else "${minutes}分"
}
