package com.zhuri.daybyday.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ISO: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

private val MONTH_CN = arrayOf(
    "一月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "十一月", "十二月"
)

private val WEEK_CHAR = "一二三四五六日"

fun LocalDate.toIso(): String = format(ISO)

fun parseIso(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw, ISO)
    } catch (e: Exception) {
        null
    }
}

fun monthCn(month: Int): String = MONTH_CN[(month - 1).coerceIn(0, 11)]

fun weekdayCn(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

/** 1 = 周一 … 7 = 周日 */
fun weekChar(value: Int): String = WEEK_CHAR[(value - 1).coerceIn(0, 6)].toString()

/** 今天 / 明天 / 昨天 / 周三 */
fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "今天"
    date == today.plusDays(1) -> "明天"
    date == today.minusDays(1) -> "昨天"
    else -> weekdayCn(date)
}

fun dateShort(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

fun dateFull(date: LocalDate): String = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

fun doneKey(taskId: String, date: LocalDate): String = "$taskId|${date.toIso()}"

fun formatTimeLabel(time: String?): String = time ?: ""
