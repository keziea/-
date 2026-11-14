package com.zhuri.daybyday.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.model.compareTasks
import com.zhuri.daybyday.ui.components.DayMark
import com.zhuri.daybyday.ui.components.IconChevron
import com.zhuri.daybyday.ui.components.MonthGrid
import com.zhuri.daybyday.ui.components.TaskRow
import com.zhuri.daybyday.ui.components.WeekHeader
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.ui.theme.noteTint
import com.zhuri.daybyday.util.dateFull
import com.zhuri.daybyday.util.doneKey
import com.zhuri.daybyday.util.monthCn
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    done: Set<String>,
    entries: List<DiaryEntry>,
    categories: List<String>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenDiary: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    var cursor by remember { mutableStateOf(YearMonth.from(selected)) }
    val todayMonth = remember(today) { YearMonth.from(today) }
    val showToday = cursor != todayMonth || selected != today

    // 42 个格子的状态一次算好，翻月 / 勾选时不再逐格过滤任务
    val marks = remember(tasks, done, cursor) {
        val first = LocalDate.of(cursor.year, cursor.monthValue, 1)
        val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
        val map = HashMap<LocalDate, DayMark>(48)
        for (i in 0 until 42) {
            val d = gridStart.plusDays(i.toLong())
            val list = tasks.filter { it.occursOn(d) }
            map[d] = if (list.isEmpty()) {
                DayMark()
            } else {
                val doneCount = list.count { doneKey(it.id, d) in done }
                DayMark(count = list.size, allDone = doneCount == list.size)
            }
        }
        map
    }
    val markFor = remember(marks) {
        { d: LocalDate -> marks[d] ?: DayMark() }
    }

    val dayTasks = remember(tasks, done, selected) {
        tasks.filter { it.occursOn(selected) }
            .sortedWith(
                Comparator { a, b ->
                    compareTasks(
                        a,
                        b,
                        doneKey(a.id, selected) in done,
                        doneKey(b.id, selected) in done
                    )
                }
            )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 月份标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cursor.year}年",
                    style = Zhuri.type.label,
                    color = colors.ink3
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = monthCn(cursor.monthValue),
                    style = Zhuri.type.h1,
                    color = colors.ink,
                    modifier = Modifier.clickable {
                        cursor = todayMonth
                        onSelect(today)
                    }
                )
            }

            AnimatedVisibility(
                visible = showToday,
                enter = fadeIn(tween(160)) +
                    slideInHorizontally(tween(200)) { it / 3 } +
                    expandHorizontally(tween(200), expandFrom = Alignment.End),
                exit = fadeOut(tween(120)) +
                    shrinkHorizontally(tween(160), shrinkTowards = Alignment.End)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accentSoft)
                        .clickable(
                            onClickLabel = "回到今天",
                            onClick = {
                                cursor = todayMonth
                                onSelect(today)
                            }
                        )
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "回到今天",
                        fontSize = 10.sp,
                        color = colors.accentInk,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(2.dp))
                    IconChevron(
                        colors.accentInk,
                        direction = 1,
                        modifier = Modifier.size(9.dp),
                        stroke = 1.4.dp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CircleIconButton(direction = -1) {
                cursor = cursor.minusMonths(1)
            }
            Spacer(Modifier.width(8.dp))
            CircleIconButton(direction = 1) {
                cursor = cursor.plusMonths(1)
            }
        }

        WeekHeader(modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = cursor,
            transitionSpec = {
                val forward = targetState > initialState
                (
                    fadeIn(tween(180)) +
                        slideInHorizontally(tween(220)) { if (forward) it / 8 else -it / 8 }
                    ).togetherWith(fadeOut(tween(120)))
            },
            label = "month"
        ) { month ->
            MonthGrid(
                year = month.year,
                month = month.monthValue,
                selected = selected,
                today = today,
                markFor = markFor,
                onSelect = { d ->
                    cursor = YearMonth.from(d)
                    onSelect(d)
                },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        val dayNotes = entries.filter { it.date == selected }.sortedByDescending { it.createdAt }
        val doneCount = dayTasks.count { doneKey(it.id, selected) in done }
        var tasksExpanded by remember(selected) {
            mutableStateOf(dayTasks.isNotEmpty() && doneCount < dayTasks.size)
        }
        var notesExpanded by remember(selected) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 150.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateFull(selected), fontSize = 13.sp, color = colors.ink2)
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (dayTasks.isEmpty() && dayNotes.isEmpty()) "" else {
                        "${dayTasks.size} 日程 · ${dayNotes.size} 笔记"
                    },
                    fontSize = 11.sp,
                    color = colors.ink3
                )
            }

            if (dayTasks.isEmpty() && dayNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("这天还没有安排", fontSize = 14.sp, color = colors.ink2)
                        Spacer(Modifier.height(4.dp))
                        Text("点右下角添加一件事", fontSize = 12.sp, color = colors.ink3)
                    }
                }
            } else {
                if (dayTasks.isNotEmpty()) {
                    DayScheduleCard(
                        tasks = dayTasks,
                        done = done,
                        date = selected,
                        today = today,
                        expanded = tasksExpanded,
                        onToggleExpand = { tasksExpanded = !tasksExpanded },
                        onToggle = onToggle,
                        onOpenTask = onOpenTask
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (dayNotes.isNotEmpty()) {
                    DayNotesCard(
                        notes = dayNotes,
                        expanded = notesExpanded,
                        onToggleExpand = { notesExpanded = !notesExpanded },
                        onOpen = onOpenDiary
                    )
                }
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    tasks: List<Task>,
    done: Set<String>,
    date: LocalDate,
    today: LocalDate,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit
) {
    val colors = Zhuri.colors
    val completed = tasks.count { doneKey(it.id, date) in done }
    val failed = if (date.isBefore(today)) tasks.size - completed else 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
            Spacer(Modifier.width(8.dp))
            Text("日程", style = Zhuri.type.label, color = colors.ink2)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (date.isBefore(today)) {
                    "成功 $completed · 未完成 $failed"
                } else {
                    "完成 $completed / ${tasks.size}"
                },
                fontSize = 11.sp,
                color = when {
                    date.isBefore(today) && failed > 0 -> colors.accent
                    completed == tasks.size -> colors.accent
                    else -> colors.ink3
                }
            )
            Spacer(Modifier.width(8.dp))
            IconChevron(
                color = colors.ink3,
                direction = if (expanded) 1 else -1,
                modifier = Modifier.size(12.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 8.dp)) {
                tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        done = doneKey(task.id, date) in done,
                        outcome = com.zhuri.daybyday.model.outcomeOf(task, date, today, done),
                        onClick = { onOpenTask(task) },
                        onToggle = { onToggle(task) },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayNotesCard(
    notes: List<DiaryEntry>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpen: (DiaryEntry) -> Unit
) {
    val colors = Zhuri.colors
    val byCategory = notes.groupingBy { it.category }.eachCount()
    val summary = byCategory.entries.sortedByDescending { it.value }
        .joinToString(" · ") { "${it.key} ${it.value}" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.note)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("笔记", style = Zhuri.type.label, color = colors.ink2)
                    Spacer(Modifier.width(8.dp))
                    Text("${notes.size} 篇", fontSize = 11.sp, color = colors.ink3)
                }
                if (!expanded) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = summary,
                        fontSize = 10.sp,
                        color = colors.ink3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconChevron(
                color = colors.ink3,
                direction = if (expanded) 1 else -1,
                modifier = Modifier.size(12.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 10.dp)) {
                notes.forEach { note ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(note) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(noteTint(note.category))
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = note.title.ifBlank { note.content.take(18) },
                                fontSize = 14.sp,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(note.category, fontSize = 9.sp, color = colors.ink3)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = note.content.replace("\n", " ").take(50),
                            fontSize = 11.sp,
                            color = colors.ink3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(direction: Int, onClick: () -> Unit) {
    val colors = Zhuri.colors
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconChevron(color = colors.ink2, direction = direction, modifier = Modifier.size(16.dp))
    }
}
