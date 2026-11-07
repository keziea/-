package com.zhuri.daybyday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.ui.theme.noteTint

/**
 * 分类选择器。取代横向滚动的胶囊行：分类再多也只是一列，点一下就选中。
 * counts 为空时不显示数量（写作页用）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<String>,
    selected: String?,
    counts: Map<String, Int>? = null,
    onSelect: (String?) -> Unit,
    onManage: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = Zhuri.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("选择分类", style = Zhuri.type.h2, color = colors.ink)
                Spacer(Modifier.weight(1f))
                if (onManage != null) {
                    TextAction(text = "管理分类", onClick = onManage)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable {
                            onSelect(null)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "全部笔记",
                        fontSize = 15.sp,
                        fontWeight = if (selected == null) FontWeight.Medium else FontWeight.Normal,
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (counts != null) {
                        Text("${counts.values.sum()}", fontSize = 11.sp, color = colors.ink3)
                        Spacer(Modifier.width(10.dp))
                    }
                    if (selected == null) IconCheck(colors.accent, Modifier.size(14.dp), 2.dp)
                }

                categories.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .clickable {
                                onSelect(c)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(noteTint(c))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = c,
                            fontSize = 15.sp,
                            fontWeight = if (selected == c) FontWeight.Medium else FontWeight.Normal,
                            color = colors.ink,
                            modifier = Modifier.weight(1f)
                        )
                        if (counts != null) {
                            Text(
                                "${counts[c] ?: 0}",
                                fontSize = 11.sp,
                                color = colors.ink3
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        if (selected == c) IconCheck(colors.accent, Modifier.size(14.dp), 2.dp)
                    }
                }
            }
        }
    }
}

/** 分类选择入口：当前分类 + 数量 + 下拉箭头，替代横向滚动的胶囊 */
@Composable
fun CategoryPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = Zhuri.colors
    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(colors.surfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = text, fontSize = 12.sp, color = colors.ink2)
        Spacer(Modifier.width(4.dp))
        IconCaret(colors.ink3, direction = -1, modifier = Modifier.size(9.dp), stroke = 1.4.dp)
    }
}
