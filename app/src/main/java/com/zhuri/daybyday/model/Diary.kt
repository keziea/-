package com.zhuri.daybyday.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/** 内置分类；用户可以在此基础上增删，分类名直接入库。 */
val DefaultNoteCategories: List<String> = listOf("想法", "生活", "工作", "学习", "其他")
const val DefaultNoteCategory: String = "想法"

/** 兼容 v1.4 之前存的是枚举名（IDEA/LIFE…） */
private val LegacyCategoryNames = mapOf(
    "IDEA" to "想法",
    "LIFE" to "生活",
    "WORK" to "工作",
    "STUDY" to "学习",
    "OTHER" to "其他"
)

fun normalizeCategory(raw: String): String =
    LegacyCategoryNames[raw] ?: raw.trim().ifBlank { DefaultNoteCategory }

/** 笔记：同一天允许多篇，正文必填，标题可空。 */
@Immutable
data class DiaryEntry(
    val id: String,
    val title: String = "",
    val content: String,
    val date: LocalDate,
    val category: String = DefaultNoteCategory,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    /** 正文字数，不计空白；标题不计入，避免「起了个长名字」虚增统计。 */
    val wordCount: Int get() = content.count { !it.isWhitespace() }

    val edited: Boolean get() = updatedAt > createdAt + 1_000L

    companion object {
        const val MAX_TITLE = 40
        const val MAX_CONTENT = 5000
    }
}

@Immutable
data class DiaryHeatDay(
    val date: LocalDate,
    val wrote: Boolean
)

@Immutable
data class DiaryStats(
    val total: Int = 0,
    val days: Int = 0,
    val words: Int = 0,
    val thisMonth: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastDate: LocalDate? = null,
    val heat: List<List<DiaryHeatDay>> = emptyList(),
    val byCategory: Map<String, Int> = emptyMap()
) {
    val isEmpty: Boolean get() = total == 0
}

/**
 * 笔记统计。与任务统计完全分开：
 * - 连续 = 从今天（今天还没写则从昨天）往前，每天至少一篇的连续天数；
 * - 最长 = 历史上最长的连续写作天数；
 * - 热力图 = 以今天所在周为最后一列的 12 周 × 7 天。
 */
fun buildDiaryStats(entries: List<DiaryEntry>, today: LocalDate): DiaryStats {
    val weekEnd = today.plusDays((7 - today.dayOfWeek.value).toLong())
    val heatStart = weekEnd.minusDays(83)

    if (entries.isEmpty()) {
        val heat = (0 until 12).map { col ->
            (0 until 7).map { row ->
                DiaryHeatDay(heatStart.plusDays((col * 7 + row).toLong()), false)
            }
        }
        return DiaryStats(heat = heat)
    }

    val dates = HashSet<LocalDate>(entries.size)
    var words = 0
    var thisMonth = 0
    var last: LocalDate? = null
    val byCategory = HashMap<String, Int>()

    entries.forEach { entry ->
        dates.add(entry.date)
        words += entry.wordCount
        if (entry.date.year == today.year && entry.date.monthValue == today.monthValue) thisMonth++
        if (last == null || entry.date.isAfter(last)) last = entry.date
        byCategory[entry.category] = (byCategory[entry.category] ?: 0) + 1
    }

    var cursor = if (today in dates) today else today.minusDays(1)
    var current = 0
    while (cursor in dates) {
        current++
        cursor = cursor.minusDays(1)
    }

    val sorted = dates.sorted()
    var longest = 1
    var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
        if (run > longest) longest = run
    }

    val heat = (0 until 12).map { col ->
        (0 until 7).map { row ->
            val date = heatStart.plusDays((col * 7 + row).toLong())
            DiaryHeatDay(date, date in dates)
        }
    }

    return DiaryStats(
        total = entries.size,
        days = dates.size,
        words = words,
        thisMonth = thisMonth,
        currentStreak = current,
        longestStreak = longest,
        lastDate = last,
        heat = heat,
        byCategory = byCategory
    )
}

/** 某一天的笔记，新的在上。 */
fun List<DiaryEntry>.onDate(date: LocalDate): List<DiaryEntry> =
    filter { it.date == date }.sortedByDescending { it.createdAt }
