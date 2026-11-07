package com.zhuri.daybyday.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhuri.daybyday.data.TaskRepository
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.ui.components.IconCalendar
import com.zhuri.daybyday.ui.components.IconPlus
import com.zhuri.daybyday.ui.components.IconStats
import com.zhuri.daybyday.ui.components.IconToday
import com.zhuri.daybyday.ui.theme.Zhuri
import java.time.LocalDate

private val TAB_LABELS = listOf("今日", "日历", "总结")

@Composable
fun Root(repo: TaskRepository) {
    val colors = Zhuri.colors
    val haptic = LocalHapticFeedback.current
    val tasks by repo.tasks.collectAsStateWithLifecycle()
    val done by repo.done.collectAsStateWithLifecycle()

    var today by remember { mutableStateOf(LocalDate.now()) }
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(LocalDate.now()) }

    var editorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editorDate by remember { mutableStateOf(LocalDate.now()) }

    // 跨天回到前台时刷新「今天」
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = LocalDate.now()
                if (now != today) {
                    if (selected == today) selected = now
                    today = now
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openNew() {
        editingTask = null
        editorDate = selected
        editorOpen = true
    }

    fun openTask(task: Task) {
        editingTask = task
        editorDate = task.startDate
        editorOpen = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
    ) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 40 })
                    .togetherWith(fadeOut(tween(120)))
            },
            label = "tab",
            modifier = Modifier.fillMaxSize()
        ) { current ->
            when (current) {
                0 -> TodayScreen(
                    tasks = tasks,
                    done = done,
                    date = selected,
                    today = today,
                    onSelectDate = { selected = it },
                    onToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        repo.toggle(it.id, selected)
                    },
                    onOpenTask = { openTask(it) },
                    onAdd = { openNew() }
                )

                1 -> CalendarScreen(
                    tasks = tasks,
                    done = done,
                    selected = selected,
                    today = today,
                    onSelect = { selected = it },
                    onToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        repo.toggle(it.id, selected)
                    },
                    onOpenTask = { openTask(it) }
                )

                else -> StatsScreen(
                    tasks = tasks,
                    done = done,
                    today = today
                )
            }
        }

        if (tab != 2) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 22.dp, bottom = 92.dp)
            ) {
                AddFab(onClick = { openNew() })
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        ) {
            NavPill(current = tab, onSelect = { tab = it })
        }
    }

    if (editorOpen) {
        EditorSheet(
            initial = editingTask,
            initialDate = editorDate,
            onDismiss = { editorOpen = false },
            onSave = { task ->
                if (editingTask == null) {
                    repo.add(task)
                    selected = task.startDate
                } else {
                    repo.update(task)
                }
                editorOpen = false
            },
            onDelete = { task ->
                repo.delete(task.id)
                editorOpen = false
            }
        )
    }
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    val colors = Zhuri.colors
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x33000000)
            )
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconPlus(colors.onInk, Modifier.size(22.dp), 2.6.dp)
    }
}

@Composable
private fun NavPill(current: Int, onSelect: (Int) -> Unit) {
    val colors = Zhuri.colors
    Row(
        modifier = Modifier
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color(0x14000000),
                spotColor = Color(0x1E000000)
            )
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.line, CircleShape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TAB_LABELS.forEachIndexed { index, label ->
            val selected = current == index
            val tint = if (selected) colors.onInk else colors.ink3
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) colors.ink else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (index) {
                    0 -> IconToday(tint, Modifier.size(17.dp), 1.7.dp)
                    1 -> IconCalendar(tint, Modifier.size(17.dp), 1.7.dp)
                    else -> IconStats(tint, Modifier.size(17.dp), 1.7.dp)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = tint
                )
            }
        }
    }
}
