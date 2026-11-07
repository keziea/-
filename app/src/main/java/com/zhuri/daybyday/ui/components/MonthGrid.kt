package com.zhuri.daybyday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.util.weekChar
import java.time.LocalDate

/** 星期表头：一 二 三 四 五 六 日 */
@Composable
fun WeekHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        for (i in 1..7) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = weekChar(i),
                    fontSize = 11.sp,
                    color = Zhuri.colors.ink3
                )
            }
        }
    }
}

/**
 * 月视图网格。固定 6 行 × 7 列，周一为首列。
 * [markFor] 返回该日任务数，用于格子下方打点（最多 3 个）。
 */
@Composable
fun MonthGrid(
    year: Int,
    month: Int,
    selected: LocalDate,
    today: LocalDate,
    markFor: (LocalDate) -> DayMark,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showDots: Boolean = true
) {
    val colors = Zhuri.colors
    val firstOfMonth = LocalDate.of(year, month, 1)
    val offset = firstOfMonth.dayOfWeek.value - 1
    val gridStart = firstOfMonth.minusDays(offset.toLong())

    Column(modifier = modifier.fillMaxWidth()) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val date = gridStart.plusDays((row * 7 + col).toLong())
                    val inMonth = date.monthValue == month && date.year == year
                    val isSelected = date == selected
                    val isToday = date == today
                    val mark = markFor(date)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelect(date) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(31.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.ink else colors.surface.copy(alpha = 0f))
                                    .then(
                                        if (!isSelected && isToday) {
                                            Modifier.border(1.dp, colors.accent, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Medium else FontWeight.Normal,
                                    color = when {
                                        isSelected -> colors.onInk
                                        !inMonth -> colors.ink3.copy(alpha = 0.5f)
                                        isToday -> colors.accent
                                        else -> colors.ink
                                    }
                                )
                            }
                            if (showDots) {
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.size(4.dp)) {
                                    if (mark.count > 0) {
                                        Box(
                                            Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (mark.allDone) colors.ink3 else colors.accent
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
