package com.zhuri.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.model.compareTasks
import com.zhuri.daybyday.ui.components.DateStrip
import com.zhuri.daybyday.ui.components.DayMark
import com.zhuri.daybyday.ui.components.EmptyState
import com.zhuri.daybyday.ui.components.IconChevron
import com.zhuri.daybyday.ui.components.ProgressRing
import com.zhuri.daybyday.ui.components.SectionLabel
import com.zhuri.daybyday.ui.components.TextAction
import com.zhuri.daybyday.ui.components.TaskRow
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.util.dateShort
import com.zhuri.daybyday.util.dayLabel
import com.zhuri.daybyday.util.doneKey
import com.zhuri.daybyday.util.monthCn
import java.time.LocalDate

@Composable
fun TodayScreen(
    tasks: List<Task>,
    done: Set<String>,
    date: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    var jumpSignal by remember { mutableStateOf(0) }
    // 由日期条位置驱动：滑偏才显示，拨回原位自动隐藏
    var stripDisplaced by remember { mutableStateOf(false) }

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
                showJump = stripDisplaced,
                onBackToToday = {
                    onSelectDate(today)
                    jumpSignal++
                }
            )
        }

        item {
            DateStrip(
                selected = date,
                today = today,
                markFor = markFor,
                onSelect = onSelectDate,
                resetSignal = jumpSignal,
                onDisplacedChange = { stripDisplaced = it }
            )
        }

        item(key = "header_schedule") {
            SectionLabel(
                text = "日程",
                dotColor = colors.accent,
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .padding(top = 18.dp, bottom = 2.dp)
            ) {
                Text(
                    text = if (dayTasks.isEmpty()) "还没安排" else "完成 ${completed.size} / ${dayTasks.size}",
                    fontSize = 10.sp,
                    color = colors.ink3
                )
                Spacer(Modifier.width(10.dp))
                TextAction(text = "添加", onClick = onAdd, color = colors.accent)
            }
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
                            .animateItem(placementSpec = tween(180))
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
                            .animateItem(placementSpec = tween(180))
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    today: LocalDate,
    rate: Float,
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
                    // 固定高度槽位：胶囊出现时不再改变头部高度，避免滑动一开始抖一下
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .padding(top = 3.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showJump,
                            enter = fadeIn(tween(120)),
                            exit = fadeOut(tween(90))
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.accentSoft)
                                    .clickable(onClick = onBackToToday)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
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
            }
        }

        ProgressRing(progress = rate)
    }
}
