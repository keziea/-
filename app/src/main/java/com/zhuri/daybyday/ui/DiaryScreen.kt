package com.zhuri.daybyday.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.model.DiaryStats
import com.zhuri.daybyday.model.onDate
import com.zhuri.daybyday.ui.components.CategoryPickerSheet
import com.zhuri.daybyday.ui.components.CategoryPill
import com.zhuri.daybyday.ui.components.EmptyState
import com.zhuri.daybyday.ui.components.IconClose
import com.zhuri.daybyday.ui.components.IconPen
import com.zhuri.daybyday.ui.components.IconPlus
import com.zhuri.daybyday.ui.components.IconSearch
import com.zhuri.daybyday.ui.components.SectionLabel
import com.zhuri.daybyday.ui.components.TextAction
import com.zhuri.daybyday.ui.components.ZChip
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.ui.theme.noteTint
import com.zhuri.daybyday.util.dateShort
import com.zhuri.daybyday.util.dayLabel
import com.zhuri.daybyday.util.monthCn
import com.zhuri.daybyday.util.timeHm
import com.zhuri.daybyday.util.weekdayCn
import java.time.LocalDate

private data class DiaryDay(
    val date: LocalDate,
    val entries: List<DiaryEntry>,
    val showMonth: Boolean
)

private fun buildDays(entries: List<DiaryEntry>): List<DiaryDay> {
    val grouped = entries.groupBy { it.date }.entries.sortedByDescending { it.key }
    var previous: LocalDate? = null
    return grouped.map { (date, list) ->
        val prev = previous
        val showMonth = prev == null ||
            prev.year != date.year ||
            prev.monthValue != date.monthValue
        previous = date
        DiaryDay(
            date = date,
            entries = list.sortedByDescending { it.createdAt },
            showMonth = showMonth
        )
    }
}

@Composable
fun DiaryScreen(
    entries: List<DiaryEntry>,
    categories: List<String>,
    today: LocalDate,
    stats: DiaryStats,
    onOpen: (DiaryEntry) -> Unit,
    onAdd: () -> Unit
) {
    val colors = Zhuri.colors
    var category by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val shown = remember(entries, category, query) {
        val q = query.trim()
        entries.filter { entry ->
            (category == null || entry.category == category) &&
                (q.isEmpty() ||
                    entry.title.contains(q, ignoreCase = true) ||
                    entry.content.contains(q, ignoreCase = true))
        }
    }
    val days = remember(shown) { buildDays(shown) }
    val countsByCategory = remember(entries) { entries.groupingBy { it.category }.eachCount() }
    val wroteToday = remember(entries, today) { entries.any { it.date == today } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 132.dp)
    ) {
        item(key = "diary_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 10.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "笔记",
                        style = Zhuri.type.h1,
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (stats.total > 0) {
                        Text(
                            text = if (stats.currentStreak > 0) {
                                "连续 ${stats.currentStreak} 天"
                            } else {
                                "最长 ${stats.longestStreak} 天"
                            },
                            fontSize = 12.sp,
                            color = colors.ink3
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (stats.isEmpty) {
                        "想法、生活、工作，随手记下"
                    } else {
                        "已写 ${stats.total} 篇 · 在 ${stats.days} 天里留下"
                    },
                    fontSize = 13.sp,
                    color = colors.ink3
                )
            }
        }

        if (entries.isNotEmpty()) {
            item(key = "diary_filters") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 10.dp)
                ) {
                    DiarySearchField(
                        query = query,
                        onQueryChange = { query = it },
                        onClear = { query = "" }
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryPill(
                            text = category ?: "全部分类",
                            onClick = { showPicker = true }
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (query.isBlank() && category == null) {
                                "${entries.size} 篇"
                            } else {
                                "筛出 ${shown.size} 篇"
                            },
                            fontSize = 10.sp,
                            color = colors.ink3
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item(key = "diary_empty") {
                EmptyState(
                    title = "从今天的一句话开始",
                    subtitle = "想法、生活、工作，都值得留下",
                    actionText = "写第一篇",
                    onAction = onAdd,
                    modifier = Modifier.padding(top = 52.dp),
                    illustration = { DiaryMark() }
                )
            }
        } else if (shown.isEmpty()) {
            item(key = "diary_filter_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (query.isNotBlank()) "没有找到「${query.trim()}」" else "这个分类还没有笔记",
                            fontSize = 13.sp,
                            color = colors.ink3
                        )
                        Spacer(Modifier.height(4.dp))
                        TextAction(
                            text = "清除筛选",
                            onClick = {
                                query = ""
                                category = null
                            }
                        )
                    }
                }
            }
        } else {
            item(key = "diary_prompt") {
                DiaryPrompt(
                    title = if (wroteToday) "再写一页" else "写给今天",
                    subtitle = "${dayLabel(today, today)} · ${dateShort(today)}",
                    onClick = onAdd,
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 2.dp)
                )
            }

            items(days, key = { "day_${it.date}" }) { day ->
                DiaryDayBlock(
                    day = day,
                    today = today,
                    onOpen = onOpen,
                    modifier = Modifier.animateItem()
                )
            }

            item(key = "diary_footer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "笔记只留在这台设备上",
                        fontSize = 10.sp,
                        color = colors.ink3.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    if (showPicker) {
        CategoryPickerSheet(
            categories = categories,
            selected = category,
            counts = countsByCategory,
            onSelect = { category = it },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun DiarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val colors = Zhuri.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceAlt)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconSearch(colors.ink3, Modifier.size(14.dp), 1.6.dp)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = Zhuri.type.bodySm.copy(fontSize = 13.sp, color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("搜索标题或正文", fontSize = 13.sp, color = colors.ink3)
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                IconClose(colors.ink3, Modifier.size(10.dp))
            }
        }
    }
}

@Composable
private fun DiaryDayBlock(
    day: DiaryDay,
    today: LocalDate,
    onOpen: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val isToday = day.date == today

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        if (day.showMonth) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthCn(day.date.monthValue),
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = colors.ink
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "${day.date.year}",
                    fontSize = 10.sp,
                    color = colors.ink3,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(colors.line)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(top = if (day.showMonth) 16.dp else 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = Zhuri.type.dateNum,
                    color = if (isToday) colors.note else colors.ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isToday) "今天" else weekdayCn(day.date),
                    fontSize = 9.sp,
                    color = if (isToday) colors.note else colors.ink3
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .weight(1f)
                        .background(
                            if (isToday) colors.note.copy(alpha = 0.28f) else colors.line
                        )
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                day.entries.forEach { entry ->
                    DiaryCard(
                        entry = entry,
                        onClick = { onOpen(entry) },
                        highlight = isToday
                    )
                }
            }
        }
    }
}

@Composable
fun DiaryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    highlight: Boolean = false
) {
    val colors = Zhuri.colors
    val shape = RoundedCornerShape(18.dp)
    val hasTitle = entry.title.isNotBlank()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "diaryPress"
    )

    val meta = buildString {
        timeHm(entry.createdAt)?.let { append(it).append(" · ") }
        append(entry.wordCount).append(" 字")
        if (entry.edited && !compact) append(" · 已编辑")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "打开笔记",
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(2.dp)
                .height(if (hasTitle) 24.dp else 32.dp)
                .clip(CircleShape)
                .background(
                    if (highlight) colors.note.copy(alpha = 0.85f)
                    else colors.note.copy(alpha = 0.30f)
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(noteTint(entry.category))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = entry.category,
                    fontSize = 9.sp,
                    color = colors.ink3
                )
                if (highlight && !compact) {
                    Spacer(Modifier.width(8.dp))
                    Text("今天", fontSize = 9.sp, color = colors.note)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (hasTitle) {
                Text(
                    text = entry.title,
                    fontFamily = FontFamily.Serif,
                    fontSize = if (compact) 17.sp else 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.content.isNotBlank()) Spacer(Modifier.height(6.dp))
            }
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = if (hasTitle) Zhuri.type.bodySm else Zhuri.type.reading,
                    color = if (hasTitle) colors.ink2 else colors.ink,
                    maxLines = when {
                        compact -> if (hasTitle) 2 else 3
                        hasTitle -> 3
                        else -> 5
                    },
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = meta,
                fontSize = 10.sp,
                color = colors.ink3
            )
        }
    }
}

@Composable
fun DiaryPrompt(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceAlt)
            .clickable(onClickLabel = "写笔记", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(colors.note)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink
            )
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 11.sp, color = colors.ink3)
        }
        IconPlus(colors.note, Modifier.size(15.dp), 2.dp)
    }
}

/** 今日页底部：这一天的笔记摘要。 */
@Composable
fun DiaryTodaySection(
    date: LocalDate,
    today: LocalDate,
    entries: List<DiaryEntry>,
    onOpen: (DiaryEntry) -> Unit,
    onAdd: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val dayEntries = remember(entries, date) { entries.onDate(date) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = if (dayEntries.isEmpty()) "笔记" else "笔记 ${dayEntries.size}",
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            if (dayEntries.isNotEmpty()) {
                TextAction(text = "写一条", onClick = onAdd)
            }
        }

        if (dayEntries.isEmpty()) {
            DiaryPrompt(
                title = "给这一天留一句话",
                subtitle = "${dayLabel(date, today)} · ${dateShort(date)}",
                onClick = onAdd,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        } else {
            DiaryCard(
                entry = dayEntries.first(),
                onClick = { onOpen(dayEntries.first()) },
                modifier = Modifier.padding(horizontal = 22.dp),
                compact = true,
                highlight = date == today
            )
            if (dayEntries.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "还有 ${dayEntries.size - 1} 篇",
                        fontSize = 10.sp,
                        color = colors.ink3
                    )
                    Spacer(Modifier.weight(1f))
                    TextAction(text = "查看全部", onClick = onViewAll)
                }
            }
        }
    }
}

@Composable
private fun DiaryMark() {
    val colors = Zhuri.colors
    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.noteSoft)
        )
        IconPen(colors.note, Modifier.size(30.dp), 2.1.dp)
    }
}
