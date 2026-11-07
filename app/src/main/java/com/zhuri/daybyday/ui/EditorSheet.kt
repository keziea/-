package com.zhuri.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zhuri.daybyday.model.Repeat
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.ui.components.ConfirmDialog
import com.zhuri.daybyday.ui.components.DayMark
import com.zhuri.daybyday.ui.components.IconCaret
import com.zhuri.daybyday.ui.components.IconChevron
import com.zhuri.daybyday.ui.components.IconClose
import com.zhuri.daybyday.ui.components.IconTrash
import com.zhuri.daybyday.ui.components.MonthGrid
import com.zhuri.daybyday.ui.components.WeekHeader
import com.zhuri.daybyday.ui.components.ZChip
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.util.dateShort
import com.zhuri.daybyday.util.weekChar
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(
    initial: Task?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val colors = Zhuri.colors
    val today = remember { LocalDate.now() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.startDate ?: initialDate) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: Repeat.NONE) }
    var weekDays by remember {
        mutableStateOf(
            (initial?.weekDays ?: emptySet()).ifEmpty { setOf(date.dayOfWeek.value) }
        )
    }
    var monthDay by remember { mutableStateOf(initial?.monthDay ?: date.dayOfMonth) }
    var time by remember { mutableStateOf(initial?.time) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = title.trim().isNotEmpty()

    // 新建任务时自动聚焦标题并唤起键盘；编辑已有任务不抢焦点
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initial == null) {
            delay(280)
            runCatching { titleFocus.requestFocus() }
        }
    }

    // 主按钮放在抽屉顶部，键盘弹出时也始终可见
    val doSave: () -> Unit = {
        val task = Task(
            id = initial?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            note = note.trim(),
            startDate = date,
            repeat = repeat,
            weekDays = if (repeat == Repeat.WEEKLY) {
                weekDays.ifEmpty { setOf(date.dayOfWeek.value) }
            } else {
                emptySet()
            },
            monthDay = monthDay,
            time = time,
            createdAt = initial?.createdAt ?: System.currentTimeMillis()
        )
        scope.launch {
            runCatching { sheetState.hide() }
            onSave(task)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.ink,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.line)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (initial == null) "新任务" else "编辑任务",
                    style = Zhuri.type.label,
                    color = colors.ink3
                )
                Spacer(Modifier.weight(1f))
                if (initial != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceAlt)
                            .clickable { confirmDelete = true },
                        contentAlignment = Alignment.Center
                    ) {
                        IconTrash(colors.accent, Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(if (canSave) colors.accent else colors.surfaceAlt)
                        .clickable(enabled = canSave, onClick = doSave)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (initial == null) "添加" else "保存",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canSave) colors.onInk else colors.ink3
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            // 标题
            BasicTextField(
                value = title,
                onValueChange = { if (it.length <= Task.MAX_TITLE) title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
                textStyle = Zhuri.type.h2.copy(color = colors.ink),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = "要做点什么？",
                                style = Zhuri.type.h2,
                                color = colors.ink3.copy(alpha = 0.75f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            // 详情
            BasicTextField(
                value = note,
                onValueChange = { if (it.length <= Task.MAX_NOTE) note = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = Zhuri.type.bodySm.copy(color = colors.ink2),
                minLines = 1,
                maxLines = 4,
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { inner ->
                    Box {
                        if (note.isEmpty()) {
                            Text(
                                text = "添加详情（可选）",
                                style = Zhuri.type.bodySm,
                                color = colors.ink3.copy(alpha = 0.85f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line)
            )

            // 日期
            FieldBlock(label = "日期") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quick = listOf(
                        today to "今天",
                        today.plusDays(1) to "明天",
                        today.plusDays(2) to "后天"
                    )
                    quick.forEach { (d, label) ->
                        ZChip(
                            text = label,
                            selected = date == d,
                            onClick = { date = d }
                        )
                    }
                    val isCustom = date != today && date != today.plusDays(1) && date != today.plusDays(2)
                    ZChip(
                        text = if (isCustom) dateShort(date) else "选择日期",
                        selected = isCustom,
                        onClick = { showDatePicker = true }
                    )
                }
            }

            // 重复
            FieldBlock(label = "重复") {
                val options = listOf(
                    Repeat.NONE to "不重复",
                    Repeat.DAILY to "每天",
                    Repeat.WEEKLY to "每周",
                    Repeat.MONTHLY to "每月"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.surfaceAlt)
                        .padding(4.dp)
                ) {
                    options.forEach { (value, label) ->
                        val selected = repeat == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) colors.ink else Color.Transparent)
                                .clickable { repeat = value }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                color = if (selected) colors.onInk else colors.ink2
                            )
                        }
                    }
                }

                if (repeat == Repeat.WEEKLY) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 1..7) {
                            val on = i in weekDays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .background(if (on) colors.accent else colors.surfaceAlt)
                                    .clickable {
                                        weekDays = if (on) weekDays - i else weekDays + i
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = weekChar(i),
                                    fontSize = 12.sp,
                                    color = if (on) colors.onInk else colors.ink2
                                )
                            }
                        }
                    }
                }

                if (repeat == Repeat.MONTHLY) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("每月", fontSize = 13.sp, color = colors.ink2)
                        Spacer(Modifier.width(12.dp))
                        StepButton("−") { monthDay = if (monthDay <= 1) 31 else monthDay - 1 }
                        Box(
                            modifier = Modifier.width(66.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$monthDay 日",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink
                            )
                        }
                        StepButton("+") { monthDay = if (monthDay >= 31) 1 else monthDay + 1 }
                        Spacer(Modifier.width(10.dp))
                        Text("遇短月自动落到月末", fontSize = 10.sp, color = colors.ink3)
                    }
                }
            }

            // 时间
            FieldBlock(label = "时间") {
                val presets = listOf("08:00", "12:00", "18:00", "21:00")
                val custom = time != null && time !in presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZChip(text = "不设", selected = time == null, onClick = { time = null })
                    // 自定义时间排在最前，保证选中态永远可见
                    if (custom) {
                        ZChip(
                            text = time ?: "",
                            selected = true,
                            onClick = { showTimePicker = true }
                        )
                    }
                    presets.forEach { p ->
                        ZChip(text = p, selected = time == p, onClick = { time = p })
                    }
                    if (!custom) {
                        ZChip(
                            text = "自定义",
                            selected = false,
                            onClick = { showTimePicker = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        DatePickDialog(
            initial = date,
            today = today,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                date = it
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        TimePickDialog(
            initial = time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = it
                showTimePicker = false
            }
        )
    }

    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = "删除这件事？",
            message = "「${initial.title}」以及它在日历上的完成记录都会被移除，无法撤销。",
            confirmText = "删除",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    runCatching { sheetState.hide() }
                    onDelete(initial)
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun FieldBlock(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Text(text = label, style = Zhuri.type.label, color = Zhuri.colors.ink3)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    val colors = Zhuri.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(colors.surfaceAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, fontSize = 16.sp, color = colors.ink2)
    }
}

@Composable
private fun DatePickDialog(
    initial: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val colors = Zhuri.colors
    var cursor by remember { mutableStateOf(YearMonth.from(initial)) }
    var picked by remember { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AdaptiveDialogFrame(maxWidth = 420.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surface)
                    .padding(20.dp)
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("选择日期", style = Zhuri.type.h2, color = colors.ink)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    IconClose(colors.ink3, Modifier.size(13.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                DialogNavButton(-1) { cursor = cursor.minusMonths(1) }
                Text(
                    text = "${cursor.year}年${cursor.monthValue}月",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                DialogNavButton(1) { cursor = cursor.plusMonths(1) }
            }

            Spacer(Modifier.height(12.dp))
            WeekHeader()
            Spacer(Modifier.height(4.dp))

            MonthGrid(
                year = cursor.year,
                month = cursor.monthValue,
                selected = picked,
                today = today,
                markFor = { DayMark() },
                onSelect = { picked = it },
                showDots = false
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.ink2)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.accent)
                        .clickable { onConfirm(picked) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        "确定",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onInk
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun DialogNavButton(direction: Int, onClick: () -> Unit) {
    val colors = Zhuri.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(colors.surfaceAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconChevron(colors.ink2, direction, Modifier.size(14.dp))
    }
}

@Composable
private fun TimePickDialog(
    initial: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val colors = Zhuri.colors
    var hour by remember {
        mutableStateOf(initial?.substringBefore(":")?.toIntOrNull()?.coerceIn(0, 23) ?: 9)
    }
    var minute by remember {
        val m = initial?.substringAfter(":")?.toIntOrNull() ?: 0
        mutableStateOf(((m / 5) * 5).coerceIn(0, 55))
    }

    val numberStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Normal
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AdaptiveDialogFrame(maxWidth = 380.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surface)
                    .padding(22.dp)
            ) {
                Text("选择时间", style = Zhuri.type.h2, color = colors.ink)
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberColumn(
                    value = hour,
                    style = numberStyle,
                    onDelta = { hour = (hour + it + 24) % 24 }
                )
                Text(
                    text = ":",
                    style = numberStyle,
                    color = colors.ink3,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                NumberColumn(
                    value = minute,
                    style = numberStyle,
                    onDelta = { minute = (minute + it * 5 + 60) % 60 }
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onConfirm(null) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("不设时间", fontSize = 13.sp, color = colors.ink2)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("取消", fontSize = 13.sp, color = colors.ink2)
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.accent)
                        .clickable {
                            onConfirm(
                                hour.toString().padStart(2, '0') + ":" +
                                    minute.toString().padStart(2, '0')
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        "确定",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onInk
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun NumberColumn(value: Int, style: TextStyle, onDelta: (Int) -> Unit) {
    val colors = Zhuri.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { onDelta(1) },
            contentAlignment = Alignment.Center
        ) {
            IconCaret(colors.ink3, 1, Modifier.size(14.dp))
        }
        Text(text = value.toString().padStart(2, '0'), style = style, color = colors.ink)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { onDelta(-1) },
            contentAlignment = Alignment.Center
        ) {
            IconCaret(colors.ink3, -1, Modifier.size(14.dp))
        }
    }
}
