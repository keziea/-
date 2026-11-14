package com.zhuri.daybyday.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.model.DiaryHeatDay
import com.zhuri.daybyday.model.AchievementCatalog
import com.zhuri.daybyday.model.DiaryStats
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.ui.components.IconPen
import com.zhuri.daybyday.ui.components.ProgressRing
import com.zhuri.daybyday.ui.components.IconChevron
import com.zhuri.daybyday.ui.components.TextAction
import com.zhuri.daybyday.ui.components.TrophyRow
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.ui.theme.noteTint
import com.zhuri.daybyday.util.dateShort
import com.zhuri.daybyday.util.doneKey
import com.zhuri.daybyday.util.formatCount
import com.zhuri.daybyday.util.toIso
import com.zhuri.daybyday.util.weekChar
import android.widget.Toast
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class MissedOccurrence(val task: Task, val date: LocalDate)

private data class DayStat(val date: LocalDate, val total: Int, val done: Int) {
    val rate: Float get() = if (total == 0) 0f else done.toFloat() / total
}

private data class StatsData(
    val streak: Int,
    val todayRate: Float,
    val avg30: Float,
    val totalDone: Int,
    val last7: List<DayStat>,
    val heat: List<List<DayStat>>,
    val weekRate: Float,
    val prevWeekRate: Float
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

    // 本周 / 上周完成率：给人一个「现在状态如何」的直观判断
    fun computeWeekRate(weeksAgo: Long): Float {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong()).minusDays(weeksAgo * 7)
        var sumRate = 0f
        var n = 0
        for (i in 0 until 7) {
            val s = statOf(monday.plusDays(i.toLong()))
            if (s.total > 0) {
                sumRate += s.rate
                n++
            }
        }
        return if (n == 0) 0f else sumRate / n
    }
    val weekRate = computeWeekRate(0)
    val prevWeekRate = computeWeekRate(1)

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
        heat = heat,
        weekRate = weekRate,
        prevWeekRate = prevWeekRate
    )
}

private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"

@Composable
fun StatsScreen(
    tasks: List<Task>,
    done: Set<String>,
    today: LocalDate,
    diaryStats: DiaryStats,
    categories: List<String>,
    unlockedAchievements: Set<String>,
    streak: Int,
    onMarkDone: (Task, LocalDate) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onOpenDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val stats = remember(tasks, done, today) { buildStats(tasks, done, today) }
    val recentMissed = remember(tasks, done, today) {
        val list = ArrayList<MissedOccurrence>()
        for (offset in 1..7) {
            val day = today.minusDays(offset.toLong())
            tasks.forEach { task ->
                if (task.occursOn(day) && doneKey(task.id, day) !in done) {
                    list.add(MissedOccurrence(task, day))
                }
            }
        }
        list.sortedWith(
            compareByDescending<MissedOccurrence> { it.date }
                .thenBy { it.task.title }
        )
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) { onExportJson() }
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(text.toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) "备份已导出" else "导出失败，请换个位置再试",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (text.isNullOrBlank()) {
                    Toast.makeText(context, "读取备份失败", Toast.LENGTH_SHORT).show()
                } else {
                    pendingImport = text
                }
            }
        }
    }
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    val versionCode = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString()
        }.getOrNull() ?: "—"
    }

    var section by remember { mutableStateOf<StatsSection?>(null) }
    val overviewState = rememberLazyListState()
    BackHandler(enabled = section != null) { section = null }
    val exportAction: () -> Unit = {
        exportLauncher.launch("zhuri-backup-${LocalDate.now()}.json")
    }
    val importAction: () -> Unit = {
        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }

    if (section != null) {
        StatsDetailPage(
            section = section!!,
            onBack = { section = null },
            versionName = versionName,
            versionCode = versionCode,
            tasks = tasks,
            diaryStats = diaryStats,
            categories = categories,
            today = today,
            unlockedAchievements = unlockedAchievements,
            streak = streak,
            onOpenDiary = onOpenDiary,
            onExport = exportAction,
            onImport = importAction,
            modifier = modifier
        )
    } else {
    LazyColumn(
        state = overviewState,
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "v$versionName ($versionCode) · 纯本地运行",
                    fontSize = 10.sp,
                    color = colors.ink3.copy(alpha = 0.85f)
                )
            }
        }

        item { HeroCard(stats) }
        item { ThreeTiles(stats) }
        item { WeekBarCard(stats) }
        item { HeatCard(stats.heat) }
        item { MissedCard(recentMissed, onMarkDone) }
        item {
            MoreGroup(
                versionName = versionName,
                versionCode = versionCode,
                unlockedCount = unlockedAchievements.size,
                noteCount = diaryStats.total,
                wordCount = diaryStats.words,
                onOpen = { section = it }
            )
        }
    }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("恢复这份备份？") },
            text = {
                Text("会用备份中的日程、笔记、分类和奖杯覆盖当前全部数据，且无法撤销。")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { onImportJson(text) }
                        Toast.makeText(
                            context,
                            if (ok) "备份已恢复" else "这不是有效的逐日备份",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text("覆盖恢复") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            }
        )
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
private fun WeekBarCard(stats: StatsData) {
    val colors = Zhuri.colors
    val last7 = stats.last7
    val delta = stats.weekRate - stats.prevWeekRate
    val deltaText = when {
        stats.prevWeekRate <= 0f && stats.weekRate <= 0f -> ""
        delta > 0.005f -> "较上周 +${percent(delta)}"
        delta < -0.005f -> "较上周 −${percent(-delta)}"
        else -> "与上周持平"
    }

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
            Text(
                text = if (stats.weekRate > 0f) {
                    "本周 ${percent(stats.weekRate)}" + if (deltaText.isNotEmpty()) " · $deltaText" else ""
                } else {
                    "完成度"
                },
                fontSize = 10.sp,
                color = if (delta > 0.005f) colors.accent else colors.ink3
            )
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
private fun MissedCard(
    items: List<MissedOccurrence>,
    onMarkDone: (Task, LocalDate) -> Unit
) {
    val colors = Zhuri.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("近 7 天未完成", style = Zhuri.type.label, color = colors.ink3)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (items.isEmpty()) "0 件" else "${items.size} 件",
                fontSize = 11.sp,
                color = if (items.isEmpty()) colors.ink3 else colors.accent
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (items.isEmpty()) {
                "没有落下的事，保持住"
            } else {
                "漏掉的可以补记；补完就重新算作成功"
            },
            fontSize = 11.sp,
            color = colors.ink3
        )
        if (items.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            items.take(6).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.task.title,
                            fontSize = 13.sp,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${item.date.monthValue}月${item.date.dayOfMonth}日 · 未完成",
                            fontSize = 9.sp,
                            color = colors.ink3
                        )
                    }
                    TextAction(
                        text = "补记",
                        onClick = { onMarkDone(item.task, item.date) }
                    )
                }
            }
            if (items.size > 6) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "还有 ${items.size - 6} 件，可在日历里逐天补记",
                    fontSize = 9.sp,
                    color = colors.ink3
                )
            }
        }
    }
}

private enum class StatsSection(val title: String) {
    TROPHY("奖杯"),
    WRITING("写作"),
    BACKUP("数据与备份"),
    ABOUT("关于逐日")
}

@Composable
private fun MoreGroup(
    versionName: String,
    versionCode: String,
    unlockedCount: Int,
    noteCount: Int,
    wordCount: Int,
    onOpen: (StatsSection) -> Unit
) {
    val colors = Zhuri.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
    ) {
        Text(
            "更多",
            style = Zhuri.type.label,
            color = colors.ink3,
            modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 4.dp)
        )
        MoreRow(
            title = "奖杯",
            summary = "已解锁 $unlockedCount / 10",
            onClick = { onOpen(StatsSection.TROPHY) }
        )
        MoreDivider()
        MoreRow(
            title = "写作统计",
            summary = "$noteCount 篇 · $wordCount 字",
            onClick = { onOpen(StatsSection.WRITING) }
        )
        MoreDivider()
        MoreRow(
            title = "数据与备份",
            summary = "导出或恢复完整数据",
            onClick = { onOpen(StatsSection.BACKUP) }
        )
        MoreDivider()
        MoreRow(
            title = "关于逐日",
            summary = "v$versionName ($versionCode)",
            onClick = { onOpen(StatsSection.ABOUT) }
        )
    }
}

@Composable
private fun MoreRow(title: String, summary: String, onClick: () -> Unit) {
    val colors = Zhuri.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(summary, fontSize = 9.sp, color = colors.ink3)
        }
        IconChevron(
            colors.ink3,
            direction = 1,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun MoreDivider() {
    val colors = Zhuri.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(colors.line.copy(alpha = 0.6f))
    )
}

@Composable
private fun StatsDetailPage(
    section: StatsSection,
    onBack: () -> Unit,
    versionName: String,
    versionCode: String,
    tasks: List<Task>,
    diaryStats: DiaryStats,
    categories: List<String>,
    today: LocalDate,
    unlockedAchievements: Set<String>,
    streak: Int,
    onOpenDiary: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 10.dp)
                .clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconChevron(
                colors.ink2,
                direction = -1,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text("总结", fontSize = 12.sp, color = colors.ink2)
        }
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 132.dp)
        ) {
            when (section) {
                StatsSection.TROPHY -> item {
                    TrophyCard(unlockedAchievements, streak)
                }
                StatsSection.WRITING -> item {
                    DiaryStatsCard(diaryStats, categories, today, onOpenDiary)
                }
                StatsSection.BACKUP -> item {
                    BackupCard(onExport, onImport)
                }
                StatsSection.ABOUT -> item {
                    AboutCard(
                        versionName = versionName,
                        versionCode = versionCode,
                        taskCount = tasks.size,
                        noteCount = diaryStats.total,
                        trophyCount = unlockedAchievements.size
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupCard(onExport: () -> Unit, onImport: () -> Unit) {
    val colors = Zhuri.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("数据与备份", style = Zhuri.type.label, color = colors.ink3)
            Spacer(Modifier.weight(1f))
            Text("建议每月一次", fontSize = 9.sp, color = colors.ink3)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "换机或卸载前，先留一份完整备份",
            fontSize = 11.sp,
            color = colors.ink3
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable(onClick = onExport),
                contentAlignment = Alignment.Center
            ) {
                Text("导出备份", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onInk)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceAlt)
                    .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                    .clickable(onClick = onImport),
                contentAlignment = Alignment.Center
            ) {
                Text("恢复备份", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.ink)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "备份包含日程、笔记、分类和奖杯；恢复会覆盖当前全部数据。",
            fontSize = 9.sp,
            color = colors.ink3
        )
    }
}

@Composable
private fun AboutCard(
    versionName: String,
    versionCode: String,
    taskCount: Int,
    noteCount: Int,
    trophyCount: Int
) {
    val colors = Zhuri.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("关于逐日", style = Zhuri.type.label, color = colors.ink3)
            Spacer(Modifier.weight(1f))
            Text("v$versionName ($versionCode)", fontSize = 11.sp, color = colors.ink2)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "纯本地 · 无广告 · 无账号 · 无网络权限",
            fontSize = 11.sp,
            color = colors.ink3
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            AboutStat("日程", "$taskCount", Modifier.weight(1f))
            AboutStat("笔记", "$noteCount", Modifier.weight(1f))
            AboutStat("奖杯", "$trophyCount / 10", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "所有数据只保存在这台设备上。覆盖安装会保留，卸载会清空。",
            fontSize = 10.sp,
            color = colors.ink3
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "逐日 · Day by Day",
            fontSize = 9.sp,
            color = colors.ink3.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun AboutStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = Zhuri.colors
    Column(modifier = modifier) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.ink)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = colors.ink3)
    }
}

@Composable
private fun TrophyCard(unlocked: Set<String>, streak: Int) {
    val colors = Zhuri.colors
    val next = AchievementCatalog.firstOrNull { it.id !in unlocked }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("奖杯", style = Zhuri.type.label, color = colors.ink3)
            Spacer(Modifier.weight(1f))
            Text(
                "已解锁 ${unlocked.size} / ${AchievementCatalog.size}",
                fontSize = 10.sp,
                color = colors.ink3
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "连续完成日程，奖杯会一座座亮起来",
            fontSize = 11.sp,
            color = colors.ink3
        )
        Spacer(Modifier.height(14.dp))
        TrophyRow(unlocked = unlocked)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (next == null) {
                "十座奖杯全部点亮 · 你已经在很前面了"
            } else {
                "当前连续 $streak 天 · 下一座「${next.name}」还差 ${(next.days - streak).coerceAtLeast(0)} 天"
            },
            fontSize = 10.sp,
            color = if (next == null) colors.accent else colors.ink3
        )
    }
}

@Composable
private fun DiaryStatsCard(
    stats: DiaryStats,
    categories: List<String>,
    today: LocalDate,
    onOpen: () -> Unit
) {
    val colors = Zhuri.colors
    var showAll by remember { mutableStateOf(false) }

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
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "共 ${categories.size} 个分类 · ${stats.byCategory.size} 个已有笔记",
                    fontSize = 11.sp,
                    color = colors.ink3
                )
                Spacer(Modifier.weight(1f))
                TextAction(
                    text = if (showAll) "只看已有" else "显示全部",
                    onClick = { showAll = !showAll }
                )
            }

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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ordered = if (showAll) {
                        categories.map { it to (stats.byCategory[it] ?: 0) }
                    } else {
                        stats.byCategory.entries.sortedByDescending { it.value }.map { it.key to it.value }
                    }
                    ordered.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (c, n) ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (n > 0) noteTint(c)
                                                else colors.ink3.copy(alpha = 0.30f)
                                            )
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$c $n",
                                        fontSize = 9.sp,
                                        color = if (n > 0) colors.ink3 else colors.ink3.copy(alpha = 0.6f),
                                        maxLines = 1
                                    )
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("近 7 天", style = Zhuri.type.label, color = colors.ink3)
                Spacer(Modifier.weight(1f))
                Text("每天写了几篇", fontSize = 9.sp, color = colors.ink3)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                stats.last7.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (day.count > 0) "${day.count}" else "·",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (day.count > 0) colors.note else colors.ink3.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = weekChar(day.date.dayOfWeek.value),
                            fontSize = 9.sp,
                            color = if (day.date == today) colors.ink2 else colors.ink3
                        )
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
