package com.zhuri.daybyday.model

import androidx.compose.runtime.Immutable
import com.zhuri.daybyday.util.weekChar
import java.time.LocalDate

enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY }

/** 某条任务在某一天的结果：已完成 / 已过期未完成 / 还未到 */
enum class TaskOutcome { SUCCESS, FAILED, PENDING }

fun outcomeOf(task: Task, date: LocalDate, today: LocalDate, done: Set<String>): TaskOutcome {
    val key = com.zhuri.daybyday.util.doneKey(task.id, date)
    return when {
        key in done -> TaskOutcome.SUCCESS
        date.isBefore(today) -> TaskOutcome.FAILED
        else -> TaskOutcome.PENDING
    }
}

/**
 * 一条待办定义。重复任务不是「生成 N 条实例」，而是同一份定义 + 完成记录，见 docs/04。
 */
@Immutable
data class Task(
    val id: String,
    val title: String,
    val note: String = "",
    val startDate: LocalDate,
    val repeat: Repeat = Repeat.NONE,
    val weekDays: Set<Int> = emptySet(),
    val monthDay: Int = 1,
    val time: String? = null,
    val createdAt: Long = 0L
) {

    /** 该任务在 [date] 这天是否需要做。规则见 docs/02 第 2 节。 */
    fun occursOn(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        return when (repeat) {
            Repeat.NONE -> date == startDate
            Repeat.DAILY -> true
            Repeat.WEEKLY -> date.dayOfWeek.value in weekDays
            Repeat.MONTHLY -> date.dayOfMonth == monthDay.coerceAtMost(date.lengthOfMonth())
        }
    }

    /** 列表里显示的重复说明，NONE 返回空串 */
    fun repeatLabel(): String = when (repeat) {
        Repeat.NONE -> ""
        Repeat.DAILY -> "每天"
        Repeat.WEEKLY -> when {
            weekDays.isEmpty() -> "每周"
            weekDays.size == 7 -> "每天"
            else -> "每周" + weekDays.sorted().joinToString("") { weekChar(it) }
        }
        Repeat.MONTHLY -> "每月 $monthDay 日"
    }

    /** 同时显示时间与重复时用「·」连接 */
    fun metaLabel(): String {
        val parts = ArrayList<String>(2)
        if (!time.isNullOrBlank()) parts.add(time)
        val repeatText = repeatLabel()
        if (repeatText.isNotEmpty()) parts.add(repeatText)
        return parts.joinToString(" · ")
    }

    companion object {
        const val MAX_TITLE = 60
        const val MAX_NOTE = 300
    }
}

/** 列表排序：未完成在前；有时间的在前且按时间升序；再按创建时间 */
fun compareTasks(a: Task, b: Task, aDone: Boolean, bDone: Boolean): Int {
    if (aDone != bDone) return if (aDone) 1 else -1
    val at = a.time
    val bt = b.time
    if (at != null && bt != null) {
        val c = at.compareTo(bt)
        if (c != 0) return c
    } else if (at != null) {
        return -1
    } else if (bt != null) {
        return 1
    }
    return a.createdAt.compareTo(b.createdAt)
}


/**
 * 当前连续完成天数，口径与总结页一致：
 * 今天没做完就从昨天起算，休息日跳过不计数，遇到未完成的一天即中断。
 */
fun currentStreak(tasks: List<Task>, done: Set<String>, today: LocalDate): Int {
    if (tasks.isEmpty()) return 0
    fun statOf(date: LocalDate): Pair<Int, Int> {
        var total = 0
        var completed = 0
        for (task in tasks) {
            if (task.occursOn(date)) {
                total++
                if (com.zhuri.daybyday.util.doneKey(task.id, date) in done) completed++
            }
        }
        return total to completed
    }

    val todayStat = statOf(today)
    var cursor = if (todayStat.first > 0 && todayStat.second < todayStat.first) {
        today.minusDays(1)
    } else {
        today
    }
    val earliest = tasks.minOfOrNull { it.startDate } ?: today
    var streak = 0
    var guard = 0
    while (guard < 3650 && !cursor.isBefore(earliest)) {
        val (total, completed) = statOf(cursor)
        if (total == 0) {
            cursor = cursor.minusDays(1)
            guard++
            continue
        }
        if (completed >= total) {
            streak++
            cursor = cursor.minusDays(1)
            guard++
        } else {
            break
        }
    }
    return streak
}
