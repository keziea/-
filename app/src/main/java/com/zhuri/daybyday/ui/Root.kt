package com.zhuri.daybyday.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhuri.daybyday.data.TaskRepository
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.model.Achievement
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.model.currentStreak
import com.zhuri.daybyday.model.buildDiaryStats
import com.zhuri.daybyday.ui.components.IconCalendar
import com.zhuri.daybyday.ui.components.IconDiary
import com.zhuri.daybyday.ui.components.IconPlus
import com.zhuri.daybyday.ui.components.IconStats
import com.zhuri.daybyday.ui.components.AllDoneCelebration
import com.zhuri.daybyday.ui.components.IconToday
import com.zhuri.daybyday.ui.components.TrophyCelebration
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.util.doneKey
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TAB_LABELS = listOf("今日", "日历", "笔记", "总结")

@Composable
fun Root(repo: TaskRepository) {
    val colors = Zhuri.colors
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentInset = ZhuriLayout.contentInset(screenWidth)
    val navMaxWidth = ZhuriLayout.navMaxWidth
    val haptic = LocalHapticFeedback.current
    val tasks by repo.tasks.collectAsStateWithLifecycle()
    val done by repo.done.collectAsStateWithLifecycle()
    val entries by repo.entries.collectAsStateWithLifecycle()
    val categories by repo.categories.collectAsStateWithLifecycle()
    val unlockedAchievements by repo.achievements.collectAsStateWithLifecycle()

    var today by remember { mutableStateOf(LocalDate.now()) }
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(LocalDate.now()) }

    var editorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editorDate by remember { mutableStateOf(LocalDate.now()) }

    var diaryEditorOpen by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var diaryDate by remember { mutableStateOf(LocalDate.now()) }

    val diaryStats = remember(entries, today) { buildDiaryStats(entries, today) }
    var streak by remember { mutableStateOf(0) }
    val tasksOnSelected = remember(tasks, selected) { tasks.filter { it.occursOn(selected) } }

    var allDonePulse by remember { mutableStateOf(0) }
    var unlockedTrophy by remember { mutableStateOf<Achievement?>(null) }
    var achievementsBootstrapped by remember { mutableStateOf(false) }

    // 升级后首次启动：按已有连续天数静默补齐奖杯；后续跨过门槛才庆祝
    LaunchedEffect(tasks, done, today) {
        // 连续天数可能要用一年多的数据，放后台算，避免勾选任务时掉帧
        val current = withContext(Dispatchers.Default) { currentStreak(tasks, done, today) }
        streak = current
        val newly = repo.unlockAchievements(current)
        if (achievementsBootstrapped && newly.isNotEmpty()) {
            allDonePulse = 0
            unlockedTrophy = newly.last()
        }
        achievementsBootstrapped = true
    }

    fun toggleTask(task: Task) {
        val wasAllDone = tasksOnSelected.isNotEmpty() &&
            tasksOnSelected.all { doneKey(it.id, selected) in done }
        val becomesAllDone = tasksOnSelected.isNotEmpty() &&
            doneKey(task.id, selected) !in done &&
            tasksOnSelected.all { it.id == task.id || doneKey(it.id, selected) in done }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        repo.toggle(task.id, selected)
        if (selected == today && !wasAllDone && becomesAllDone) {
            allDonePulse++
        }
    }
    // 空白的今天已有「添加第一件事」按钮，不再叠一个 FAB

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

    fun openNewDiary(date: LocalDate) {
        editingEntry = null
        diaryDate = if (date.isAfter(today)) today else date
        diaryEditorOpen = true
    }

    fun openDiary(entry: DiaryEntry) {
        editingEntry = entry
        diaryDate = entry.date
        diaryEditorOpen = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
    ) {
        AdaptiveContent {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(tween(190)).togetherWith(fadeOut(tween(130)))
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
                    onToggle = { toggleTask(it) },
                    onOpenTask = { openTask(it) },
                    onAdd = { openNew() }
                )

                1 -> CalendarScreen(
                    tasks = tasks,
                    done = done,
                    entries = entries,
                    categories = categories,
                    selected = selected,
                    today = today,
                    onSelect = { selected = it },
                    onToggle = { toggleTask(it) },
                    onOpenTask = { openTask(it) },
                    onOpenDiary = { openDiary(it) }
                )

                2 -> AdaptiveContent(maxWidth = 560.dp) {
                    DiaryScreen(
                        entries = entries,
                        categories = categories,
                        today = today,
                        stats = diaryStats,
                        onOpen = { openDiary(it) },
                        onAdd = { openNewDiary(today) }
                    )
                }

                else -> StatsScreen(
                    tasks = tasks,
                    done = done,
                    today = today,
                    diaryStats = diaryStats,
                    categories = categories,
                    unlockedAchievements = unlockedAchievements,
                    streak = streak,
                    onOpenDiary = { tab = 2 }
                )
            }
            }
        }

        // 笔记页顶部已有「写下今天」入口；空白的今天用空状态按钮，都不叠 FAB
        // 今日页的添加按钮已下沉到「日程」和「笔记」各自的标题里，不再用全局加号
        if (tab == 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = contentInset + 22.dp, bottom = 92.dp)
            ) {
                AddFab(onClick = { openNew() })
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
                .padding(horizontal = 22.dp)
        ) {
            NavPill(
                current = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .widthIn(max = navMaxWidth)
                    .fillMaxWidth()
            )
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

    if (diaryEditorOpen) {
        DiaryEditor(
            initial = editingEntry,
            initialDate = diaryDate,
            today = today,
            categories = categories,
            onAddCategory = { repo.addCategory(it) },
            onDeleteCategory = { repo.deleteCategory(it) },
            onDismiss = { diaryEditorOpen = false },
            onSave = { entry ->
                if (editingEntry == null) repo.addEntry(entry) else repo.updateEntry(entry)
                diaryEditorOpen = false
            },
            onDelete = { entry ->
                repo.deleteEntry(entry.id)
                diaryEditorOpen = false
            }
        )
    }

    // 庆祝层：奖杯优先于「全部完成」，都用轻量遮罩，不跳页面
    unlockedTrophy?.let { achievement ->
        key(achievement.id) {
            TrophyCelebration(achievement) { unlockedTrophy = null }
        }
    }
    if (allDonePulse > 0 && unlockedTrophy == null) {
        key(allDonePulse) {
            AllDoneCelebration { allDonePulse = 0 }
        }
    }
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    val colors = Zhuri.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "fabScale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x33000000)
            )
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "添加任务",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        IconPlus(colors.onInk, Modifier.size(22.dp), 2.6.dp)
    }
}

@Composable
private fun NavPill(
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    Row(
        modifier = modifier
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
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TAB_LABELS.forEachIndexed { index, label ->
            NavItem(
                index = index,
                label = label,
                selected = current == index,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    index: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = Zhuri.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "navScale"
    )
    val background by animateColorAsState(
        targetValue = if (selected) colors.ink else Color.Transparent,
        animationSpec = tween(200),
        label = "navBg"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) colors.onInk else colors.ink3,
        animationSpec = tween(180),
        label = "navTint"
    )

    Row(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = label,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (index) {
            0 -> IconToday(tint, Modifier.size(16.dp), 1.7.dp)
            1 -> IconCalendar(tint, Modifier.size(16.dp), 1.7.dp)
            2 -> IconDiary(tint, Modifier.size(16.dp), 1.7.dp)
            else -> IconStats(tint, Modifier.size(16.dp), 1.7.dp)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false
        )
    }
}
