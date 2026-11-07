package com.zhuri.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.model.compareTasks
import com.zhuri.daybyday.ui.components.DateStrip
import com.zhuri.daybyday.ui.components.DayMark
import com.zhuri.daybyday.ui.components.EmptyState
import com.zhuri.daybyday.ui.components.IconChevron
import com.zhuri.daybyday.ui.components.ProgressRing
import com.zhuri.daybyday.ui.components.SectionLabel
import com.zhuri.daybyday.ui.components.TaskRow
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.util.dateShort
import com.zhuri.daybyday.util.dayLabel
import com.zhuri.daybyday.util.doneKey
import com.zhuri.daybyday.util.monthCn
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    tasks: List<Task>,
    done: Set<String>,
    date: LocalDate,
    today: LocalDate,
    diaryEntries: List<DiaryEntry>,
    onSelectDate: (LocalDate) -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onAdd: () -> Unit,
    onOpenDiary: (DiaryEntry) -> Unit,
    onNewDiary: (LocalDate) -> Unit,
    onViewAllDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    var jumpLatched by remember { mutableStateOf(false) }
    var jumpSignal by remember { mutableStateOf(0) }
    // 点击后由程序触发的滚动不能算作用户滑动，否则胶囊会立刻又冒出来
    var suppressLatch by remember { mutableStateOf(false) }
    LaunchedEffect(jumpSignal) {
        if (jumpSignal > 0) {
            suppressLatch = true
            delay(800)
            suppressLatch = false
        }
    }

    val dayTasks = remember(tasks, done, date) {
        tasks.filter { it.occursOn(date) }
            .sortedWith(
                Comparator { a, b ->
                    compareTasks(
                        a,
                        b,
                        doneKey(a.id, date) in done,
                        doneKey(b.id, date) in done
                    )
                }
            )
    }
    val pending = remember(dayTasks, done, date) {
        dayTasks.filter { doneKey(it.id, date) !in done }
    }
    val completed = remember(dayTasks, done, date) {
        dayTasks.filter { doneKey(it.id, date) in done }
    }
    val rate = if (dayTasks.isEmpty()) 0f else completed.size.toFloat() / dayTasks.size

    // 日期条 60 天的状态只算一次，避免每一格在组合里重复过滤全量任务
    val marks = remember(tasks, done, today) {
        val map = HashMap<LocalDate, DayMark>(64)
        for (i in 0 until 60) {
            val d = today.minusDays(14).plusDays(i.toLong())
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        item {
            DateHeader(
                date = date,
                today = today,
                rate = rate,
                completedCount = completed.size,
                totalCount = dayTasks.size,
                showJump = date != today || jumpLatched,
                onBackToToday = {
                    onSelectDate(today)
                    jumpSignal++
                    jumpLatched = false
                }
            )
        }

        item {
            DateStrip(
                selected = date,
                today = today,
                markFor = markFor,
                onSelect = { picked ->
                    jumpLatched = false
                    onSelectDate(picked)
                },
                resetSignal = jumpSignal,
                // 只要滑动过就锁定显示，直到用户点了「回到今天」或主动选了某一天
                onScrollingChange = { scrolling ->
                    if (scrolling && !suppressLatch) jumpLatched = true
                }
            )
        }

        if (dayTasks.isEmpty()) {
            item {
                EmptyState(
                    title = if (date == today) "今天还空着" else "${dateShort(date)}还空着",
                    subtitle = "加一件小事，让这一天有迹可循",
                    actionText = "添加第一件事",
                    onAction = onAdd,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }
        } else {
            if (pending.isNotEmpty()) {
                item(key = "header_pending") {
                    SectionLabel(
                        text = "待办 ${pending.size}",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        if (completed.isEmpty()) {
                            Text(
                                text = "还没开始",
                                fontSize = 11.sp,
                                color = colors.ink3
                            )
                        }
                    }
                }
                items(pending, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        done = false,
                        onClick = { onOpenTask(task) },
                        onToggle = { onToggle(task) },
                        modifier = Modifier
                            .padding(horizontal = 22.dp, vertical = 4.dp)
                            .animateItem()
                    )
                }
            }

            if (completed.isNotEmpty()) {
                item(key = "header_done") {
                    SectionLabel(
                        text = "已完成 ${completed.size}",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        if (pending.isEmpty()) {
                            Text(text = "全部完成", fontSize = 11.sp, color = colors.accent)
                        }
                    }
                }
                items(completed, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        done = true,
                        onClick = { onOpenTask(task) },
                        onToggle = { onToggle(task) },
                        modifier = Modifier
                            .padding(horizontal = 22.dp, vertical = 4.dp)
                            .animateItem()
                    )
                }
            }
        }

        // 未来日期只做安排，不留笔记；笔记是回望，不是预告
        if (!date.isAfter(today)) {
            item(key = "diary_today") {
                DiaryTodaySection(
                    date = date,
                    today = today,
                    entries = diaryEntries,
                    onOpen = onOpenDiary,
                    onAdd = { onNewDiary(date) },
                    onViewAll = onViewAllDiary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    today: LocalDate,
    rate: Float,
    completedCount: Int,
    totalCount: Int,
    showJump: Boolean,
    onBackToToday: () -> Unit
) {
    val colors = Zhuri.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 10.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${date.year} · ${monthCn(date.monthValue)}",
                style = Zhuri.type.label,
                color = colors.ink3
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = Zhuri.type.display,
                    color = colors.ink
                )
                Text(
                    text = "日",
                    fontSize = 15.sp,
                    color = colors.ink2,
                    modifier = Modifier.padding(start = 3.dp, bottom = 12.dp)
                )
                Column(Modifier.padding(start = 12.dp, bottom = 10.dp)) {
                    Text(
                        text = dayLabel(date, today),
                        fontSize = 13.sp,
                        color = colors.ink3
                    )
                    AnimatedVisibility(
                        visible = showJump,
                        enter = fadeIn(tween(140)) + expandHorizontally(tween(140)),
                        exit = fadeOut(tween(100)) + shrinkHorizontally(tween(100))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.accentSoft)
                                .clickable(onClick = onBackToToday)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("回到今天", fontSize = 10.sp, color = colors.accentInk)
                            Spacer(Modifier.width(3.dp))
                            IconChevron(
                                colors.accentInk,
                                direction = 1,
                                modifier = Modifier.size(9.dp),
                                stroke = 1.4.dp
                            )
                        }
                    }
                }
            }
            if (totalCount > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (completedCount == totalCount) {
                        "$totalCount 件全部完成 · 漂亮"
                    } else {
                        "完成 $completedCount / $totalCount 件"
                    },
                    fontSize = 12.sp,
                    color = colors.ink3
                )
            }
        }

        ProgressRing(progress = rate)
    }
}
