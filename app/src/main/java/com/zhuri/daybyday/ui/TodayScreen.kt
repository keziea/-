package com.zhuri.daybyday.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.model.compareTasks
import com.zhuri.daybyday.ui.components.DateStrip
import com.zhuri.daybyday.ui.components.DayMark
import com.zhuri.daybyday.ui.components.EmptyState
import com.zhuri.daybyday.ui.components.ProgressRing
import com.zhuri.daybyday.ui.components.SectionLabel
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
    val pending = dayTasks.filter { doneKey(it.id, date) !in done }
    val completed = dayTasks.filter { doneKey(it.id, date) in done }
    val rate = if (dayTasks.isEmpty()) 0f else completed.size.toFloat() / dayTasks.size

    val markFor: (LocalDate) -> DayMark = { d ->
        val list = tasks.filter { it.occursOn(d) }
        if (list.isEmpty()) {
            DayMark()
        } else {
            val doneCount = list.count { doneKey(it.id, d) in done }
            DayMark(count = list.size, allDone = doneCount == list.size)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 132.dp)
    ) {
        item {
            DateHeader(
                date = date,
                today = today,
                rate = rate,
                completedCount = completed.size,
                totalCount = dayTasks.size,
                onBackToToday = { onSelectDate(today) }
            )
        }

        item {
            DateStrip(
                selected = date,
                today = today,
                markFor = markFor,
                onSelect = onSelectDate
            )
        }

        if (dayTasks.isEmpty()) {
            item {
                EmptyState(
                    title = if (date == today) "今天还空着" else "${dateShort(date)}还空着",
                    subtitle = "加一件小事，让这一天有迹可循",
                    actionText = "添加第一件事",
                    onAction = onAdd,
                    modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
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
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    today: LocalDate,
    rate: Float,
    completedCount: Int,
    totalCount: Int,
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
                    if (date != today) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "回到今天",
                            fontSize = 11.sp,
                            color = colors.accent,
                            modifier = Modifier.clickable(onClick = onBackToToday)
                        )
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
