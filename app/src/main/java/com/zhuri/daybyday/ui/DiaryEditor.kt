package com.zhuri.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zhuri.daybyday.model.DiaryEntry
import com.zhuri.daybyday.ui.components.CategoryPickerSheet
import com.zhuri.daybyday.ui.components.CategoryPill
import com.zhuri.daybyday.ui.components.ConfirmDialog
import com.zhuri.daybyday.ui.components.IconClose
import com.zhuri.daybyday.ui.components.IconTrash
import com.zhuri.daybyday.ui.components.TextAction
import com.zhuri.daybyday.ui.components.ZChip
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.ui.theme.noteTint
import com.zhuri.daybyday.util.dateShort
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditor(
    initial: DiaryEntry?,
    initialDate: LocalDate,
    today: LocalDate,
    categories: List<String>,
    onAddCategory: (String) -> Boolean,
    onDeleteCategory: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit
) {
    val colors = Zhuri.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: initialDate) }
    var category by remember { mutableStateOf(initial?.category ?: categories.firstOrNull() ?: "想法") }
    var showCategories by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val canSave = content.isNotBlank()

    fun doSave() {
        if (!canSave) return
        val now = System.currentTimeMillis()
        val entry = DiaryEntry(
            id = initial?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            content = content.trim(),
            date = date,
            category = category,
            createdAt = initial?.createdAt ?: now,
            updatedAt = now
        )
        scope.launch {
            runCatching { sheetState.hide() }
            onSave(entry)
        }
    }

    fun close() {
        scope.launch {
            runCatching { sheetState.hide() }
            onDismiss()
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
                    .padding(top = 10.dp, bottom = 6.dp),
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
                .fillMaxHeight(0.94f)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceAlt)
                        .clickable { close() },
                    contentAlignment = Alignment.Center
                ) {
                    IconClose(colors.ink2, Modifier.size(13.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (initial == null) "写笔记" else "编辑笔记",
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
                        .clickable(enabled = canSave) { doSave() }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "完成",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canSave) colors.onInk else colors.ink3
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(20.dp))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.take(DiaryEntry.MAX_TITLE) },
                    singleLine = true,
                    textStyle = Zhuri.type.h2.copy(
                        color = colors.ink,
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "给这一页起个名字（可选）",
                                    style = Zhuri.type.h2.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 22.sp,
                                        lineHeight = 28.sp
                                    ),
                                    color = colors.ink3.copy(alpha = 0.8f)
                                )
                            }
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.line.copy(alpha = 0.7f))
                )
                Spacer(Modifier.height(14.dp))

                BasicTextField(
                    value = content,
                    onValueChange = { content = it.take(DiaryEntry.MAX_CONTENT) },
                    textStyle = Zhuri.type.reading.copy(color = colors.ink, lineHeight = 26.sp),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp),
                    decorationBox = { inner ->
                        Box {
                            if (content.isEmpty()) {
                                Text(
                                    text = "今天，想记下什么？\n不必完整，真实就好。",
                                    fontSize = 15.sp,
                                    lineHeight = 26.sp,
                                    color = colors.ink3.copy(alpha = 0.85f)
                                )
                            }
                            inner()
                        }
                    }
                )

                Text(
                    text = "${content.count { !it.isWhitespace() }} 字",
                    fontSize = 10.sp,
                    color = colors.ink3,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("归到哪一类", style = Zhuri.type.label, color = colors.ink3)
                    Spacer(Modifier.weight(1f))
                    TextAction(
                        text = "管理分类",
                        onClick = { showCategories = true },
                        fontSize = 11
                    )
                }
                Spacer(Modifier.height(10.dp))
                CategoryPill(
                    text = category,
                    onClick = { showPicker = true }
                )

                Spacer(Modifier.height(20.dp))
                Text("记在哪一天", style = Zhuri.type.label, color = colors.ink3)
                Spacer(Modifier.height(10.dp))

                val recent = (0L..6L).map { today.minusDays(it) }
                val dates = if (date in recent) recent else listOf(date) + recent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dates.forEach { d ->
                        ZChip(
                            text = when (d) {
                                today -> "今天"
                                today.minusDays(1) -> "昨天"
                                else -> dateShort(d)
                            },
                            selected = d == date,
                            onClick = { date = d }
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showPicker) {
        CategoryPickerSheet(
            categories = categories,
            selected = category,
            onSelect = { picked -> if (picked != null) category = picked },
            onManage = {
                showPicker = false
                showCategories = true
            },
            onDismiss = { showPicker = false }
        )
    }

    if (showCategories) {
        CategoryManagerDialog(
            categories = categories,
            onAdd = onAddCategory,
            onDelete = { name ->
                onDeleteCategory(name)
                if (category == name) {
                    category = categories.firstOrNull { it != name } ?: category
                }
            },
            onDismiss = { showCategories = false }
        )
    }

    if (confirmDelete && initial != null) {        ConfirmDialog(
            title = "删除这篇笔记？",
            message = "「${initial.title.ifBlank { initial.content.take(12) }}」将被永久移除，无法撤销。",
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
private fun CategoryManagerDialog(
    categories: List<String>,
    onAdd: (String) -> Boolean,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = Zhuri.colors
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface)
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("管理分类", style = Zhuri.type.h2, color = colors.ink)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceAlt)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    IconClose(colors.ink2, Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "删除分类时，里面的笔记会自动移到第一个分类",
                fontSize = 11.sp,
                color = colors.ink3
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = input,
                    onValueChange = {
                        input = it.take(8)
                        error = null
                    },
                    singleLine = true,
                    textStyle = Zhuri.type.bodySm.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceAlt)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text("新的分类名称", fontSize = 13.sp, color = colors.ink3)
                        }
                        inner()
                    }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(if (input.isBlank()) colors.surfaceAlt else colors.accent)
                        .clickable(enabled = input.isNotBlank()) {
                            if (onAdd(input)) {
                                input = ""
                            } else {
                                error = "名称重复或为空"
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "添加",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (input.isBlank()) colors.ink3 else colors.onInk
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error!!, fontSize = 10.sp, color = colors.accent)
            }

            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                categories.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(noteTint(c))
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(c, fontSize = 15.sp, color = colors.ink, modifier = Modifier.weight(1f))
                        if (categories.size > 1) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onDelete(c) },
                                contentAlignment = Alignment.Center
                            ) {
                                IconTrash(colors.ink3, Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
