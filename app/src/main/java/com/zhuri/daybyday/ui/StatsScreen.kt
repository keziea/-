package com.zhuri.daybyday.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.model.DiaryHeatDay
import com.zhuri.daybyday.model.DiaryStats
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.ui.components.IconPen
import com.zhuri.daybyday.ui.components.ProgressRing
import com.zhuri.daybyday.ui.components.TextAction
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.ui.theme.noteTint
import com.zhuri.daybyday.util.dateShort
import com.zhuri.daybyday.util.doneKey
import com.zhuri.daybyday.util.formatCount
import com.zhuri.daybyday.util.toIso
import com.zhuri.daybyday.util.weekChar
import java.time.LocalDate
import kotlin.math.roundToInt

private data class DayStat(val date: LocalDate, val total: Int, val done: Int) {
    val rate: Float get() = if (total == 0) 0f else done.toFloat() / total
}

private data class StatsData(
    val streak: Int,
    val todayRate: Float,
    val avg30: Float,
    val totalDone: Int,
    val last7: List<DayStat>,
    val heat: List<List<DayStat>>
)

private fun buildStats(tasks: List<Task>, done: Set<String>, today: LocalDate): StatsData {
    // 同一天会被 streak / 30 天 / 热力图重复查询，缓存一次
    val cache = HashMap<LocalDate, DayStat>(128)
    fun statOf(date: LocalDate): DayStat = cache.getOrPut(date) {
        var total = 0
        var completed = 0
        for (task in tasks) {
            if (task.occursOn(date)) {
                total++
                if (doneKey(task.id, date) in done) completed++
            }
        }
        DayStat(date, total, completed)
    }

    // 连续天数：今天没做完则从昨天起算；休息日跳过不计数
    var streak = 0
    val todayStat = statOf(today)
    var cursor = if (todayStat.total > 0 && todayStat.done < todayStat.total) {
        today.minusDays(1)
    } else {
        today
    }
    // 早于所有任务开始日的日子一定是休息日，没必要再回溯
    val earliest = tasks.minOfOrNull { it.startDate } ?: today
    var guard = 0
    while (guard < 3650 && !cursor.isBefore(earliest)) {
        val s = statOf(cursor)
        if (s.total == 0) {
            cursor = cursor.minusDays(1)
            guard++
            continue
        }
        if (s.done >= s.total) {
            streak++
            cursor = cursor.minusDays(1)
            guard++
        } else {
            break
        }
    }

    // 30 天平均：只算有任务的日子
    var sum = 0f
    var counted = 0
    for (i in 0 until 30) {
        val s = statOf(today.minusDays(i.toLong()))
        if (s.total > 0) {
            sum += s.rate
            counted++
        }
    }
    val avg30 = if (counted == 0) 0f else sum / counted

    // 累计完成：近一年
    val cutoff = today.minusDays(365).toIso()
    val totalDone = done.count { it.substringAfter("|") >= cutoff }

    val last7 = (6 downTo 0).map { statOf(today.minusDays(it.toLong())) }

    // 近 8 周热力图：最后一列为本周（周一起）
    val weekEnd = today.plusDays((7 - today.dayOfWeek.value).toLong())
    val heatStart = weekEnd.minusDays(55)
    val heat = (0 until 8).map { col ->
        (0 until 7).map { row -> statOf(heatStart.plusDays((col * 7 + row).toLong())) }
    }

    return StatsData(
        streak = streak,
        todayRate = todayStat.rate,
        avg30 = avg30,
        totalDone = totalDone,
        last7 = last7,
        heat = heat
    )
}

private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"

@Composable
fun StatsScreen(
    tasks: List<Task>,
    done: Set<String>,
    today: LocalDate,
    diaryStats: DiaryStats,
    onOpenDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val stats = remember(tasks, done, today) { buildStats(tasks, done, today) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 132.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 10.dp, bottom = 18.dp)
            ) {
                Text("总结", style = Zhuri.type.h1, color = colors.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "做过的事，写下的字，都算数",
                    fontSize = 13.sp,
                    color = colors.ink3
                )
            }
        }

        item { HeroCard(stats) }
        item { ThreeTiles(stats) }
        item { WeekBarCard(stats.last7) }
        item { HeatCard(stats.heat) }
        item { DiaryStatsCard(diaryStats, today, onOpenDiary) }
    }
}

@Composable
private fun HeroCard(stats: StatsData) {
    val colors = Zhuri.colors
    val heroBg = if (colors.isDark) colors.surfaceAlt else colors.ink
    val heroFg = if (colors.isDark) colors.ink else colors.onInk

    val bigNumber = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
        fontWeight = FontWeight.Normal
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(heroBg)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "连续",
                    style = Zhuri.type.label,
                    color = heroFg.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stats.streak.toString(),
                        style = bigNumber,
                        color = heroFg
                    )
                    Text(
                        text = "天",
                        fontSize = 13.sp,
                        color = heroFg.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (stats.streak > 0) "每天都完成了全部任务" else "从今天开始，先完成一件小事",
                    fontSize = 11.sp,
                    color = heroFg.copy(alpha = 0.5f)
                )
            }
            ProgressRing(
                progress = stats.todayRate,
                diameter = 62.dp,
                track = heroFg.copy(alpha = 0.16f),
                color = colors.accent,
                textColor = heroFg
            )
        }
    }
}

@Composable
private fun ThreeTiles(stats: StatsData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile("今日", percent(stats.todayRate), Modifier.weight(1f))
            StatTile("30 天平均", percent(stats.avg30), Modifier.weight(1f))
            StatTile("累计完成", stats.totalDone.toString(), Modifier.weight(1f), unit = "件")
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    val colors = Zhuri.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.ink3,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
                maxLines = 1
            )
            if (unit != null) {
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = colors.ink3,
                    modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekBarCard(last7: List<DayStat>) {
    val colors = Zhuri.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("近 7 天", style = Zhuri.type.label, color = colors.ink3)
            Spacer(Modifier.weight(1f))
            Text("完成度", fontSize = 10.sp, color = colors.ink3)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                last7.forEach { day ->
                val target = if (day.total == 0) 0f else day.rate.coerceIn(0f, 1f)
                val animated by animateFloatAsState(
                    targetValue = target,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "bar"
                )
                val heightFraction = if (target == 0f) 0.025f else animated.coerceAtLeast(0.06f)
                val barColor = when {
                    day.total == 0 -> colors.line
                    target >= 0.999f -> colors.accent
                    else -> colors.accent.copy(alpha = 0.34f + 0.34f * target)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(76.dp)
                            .width(18.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = weekChar(day.date.dayOfWeek.value),
                        fontSize = 10.sp,
                        color = if (day.date == last7.last().date) colors.ink2 else colors.ink3
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun HeatCard(heat: List<List<DayStat>>) {
    val colors = Zhuri.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("近 8 周", style = Zhuri.type.label, color = colors.ink3)
            Spacer(Modifier.weight(1f))
            Text("每一格是一天", fontSize = 10.sp, color = colors.ink3)
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            heat.forEachIndexed { colIndex, week ->
                if (colIndex > 0) Spacer(Modifier.width(5.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.forEach { day ->
                        val cellColor: Color = when {
                            day.total == 0 -> colors.line
                            day.done == 0 -> colors.accentSoft
                            day.rate >= 0.999f -> colors.accent
                            else -> colors.accent.copy(alpha = 0.22f + 0.55f * day.rate)
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(cellColor)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("少", fontSize = 9.sp, color = colors.ink3)
            Spacer(Modifier.width(5.dp))
            listOf(
                colors.line,
                colors.accentSoft,
                colors.accent.copy(alpha = 0.55f),
                colors.accent
            ).forEach {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(it)
                )
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(2.dp))
            Text("多", fontSize = 9.sp, color = colors.ink3)
        }
    }
}

private val HeatCell = 11.dp
private val HeatGap = 4.dp

@Composable
private fun DiaryStatsCard(stats: DiaryStats, today: LocalDate, onOpen: () -> Unit) {
    val colors = Zhuri.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        if (stats.isEmpty) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.noteSoft),
                    contentAlignment = Alignment.Center
                ) {
                    IconPen(colors.note, Modifier.size(17.dp), 1.8.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "还没有笔记",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ink
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "写下第一句，这里会长出你的写作轨迹",
                        fontSize = 10.sp,
                        color = colors.ink3
                    )
                }
                TextAction(text = "去写", onClick = onOpen)
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("笔记", style = Zhuri.type.label, color = colors.ink3)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stats.total.toString(),
                            style = Zhuri.type.displaySerif.copy(fontSize = 38.sp, lineHeight = 42.sp),
                            color = colors.ink
                        )
                        Text(
                            text = "篇",
                            fontSize = 11.sp,
                            color = colors.ink3,
                            modifier = Modifier.padding(start = 3.dp, bottom = 5.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.note)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "连续 ${stats.currentStreak} 天",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "最长 ${stats.longestStreak} 天",
                        fontSize = 10.sp,
                        color = colors.ink3
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "在 ${stats.days} 天里写下 · 本月 ${stats.thisMonth} 篇 · 共 ${formatCount(stats.words)} 字",
                fontSize = 11.sp,
                color = colors.ink3
            )

            // 分类分布：一条占比条 + 各类篇数，一眼看出记录的重心
            val totalByCategory = stats.byCategory.values.sum()
            if (totalByCategory > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    var first = true
                    stats.byCategory.entries.sortedByDescending { it.value }.forEach { (c, n) ->
                        if (n > 0) {
                            if (!first) Spacer(Modifier.width(2.dp))
                            first = false
                            Box(
                                Modifier
                                    .weight(n.toFloat())
                                    .fillMaxHeight()
                                    .background(noteTint(c))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    stats.byCategory.entries.sortedByDescending { it.value }.forEach { (c, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(noteTint(c))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$c $n",
                                fontSize = 9.sp,
                                color = colors.ink3
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            DiaryHeat(stats.heat, today)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stats.lastDate?.let { "最近 ${dateShort(it)}" } ?: "",
                    fontSize = 10.sp,
                    color = colors.ink3
                )
                Spacer(Modifier.weight(1f))
                TextAction(text = "查看全部", onClick = onOpen)
            }
        }
    }
}

@Composable
private fun DiaryHeat(heat: List<List<DiaryHeatDay>>, today: LocalDate) {
    val colors = Zhuri.colors
    val wroteToday = heat.any { week -> week.any { it.date == today && it.wrote } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(HeatGap)) {
            heat.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(HeatGap)) {
                    week.forEach { day ->
                        val shape = RoundedCornerShape(3.dp)
                        val color = when {
                            day.date.isAfter(today) -> Color.Transparent
                            day.wrote -> colors.note
                            else -> colors.line
                        }
                        Box(
                            modifier = Modifier
                                .size(HeatCell)
                                .clip(shape)
                                .background(color)
                                .then(
                                    if (day.date == today && !day.wrote) {
                                        Modifier.border(
                                            1.dp,
                                            colors.note.copy(alpha = 0.45f),
                                            shape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.width(HeatCell * 12 + HeatGap * 11),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("12 周前", fontSize = 9.sp, color = colors.ink3)
            Text(
                "今天",
                fontSize = 9.sp,
                color = if (wroteToday) colors.note else colors.ink3
            )
        }
    }
}
