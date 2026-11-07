package com.zhuri.daybyday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.ui.theme.Zhuri
import com.zhuri.daybyday.util.weekChar
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

/** 某一天的任务标记：数量 + 是否全部完成 */
data class DayMark(val count: Int = 0, val allDone: Boolean = false)

private const val PAST_DAYS = 14
private const val FUTURE_DAYS = 45

/**
 * 横向日期条。范围：今天 -14 … 今天 +45，进入时自动居中到今天附近。
 */
@Composable
fun DateStrip(
    selected: LocalDate,
    today: LocalDate,
    markFor: (LocalDate) -> DayMark,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    resetSignal: Int = 0,
    onScrollingChange: (Boolean) -> Unit = {}
) {
    val colors = Zhuri.colors
    val total = PAST_DAYS + FUTURE_DAYS + 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (PAST_DAYS - 4).coerceAtLeast(0))

    // 选中日期变化时（包括点「回到今天」），把这一天带回视野
    val selectedIndex = remember(selected, today) {
        (PAST_DAYS + ChronoUnit.DAYS.between(today, selected)).toInt()
    }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in 0 until total) {
            val target = (selectedIndex - 4).coerceIn(0, (total - 1).coerceAtLeast(0))
            if (abs(target - listState.firstVisibleItemIndex) > 8) {
                listState.scrollToItem(target)
            } else {
                listState.animateScrollToItem(target)
            }
        }
    }

    // 用户只是滑动、没有选中任何日期时，也要能一键回到今天
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> onScrollingChange(scrolling) }
    }

    // 点击「回到今天」但选中日已经是今天时，靠这个信号强制把日期条滚回去
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            val target = (PAST_DAYS - 4).coerceIn(0, total - 1)
            listState.animateScrollToItem(target)
        }
    }

    LazyRow(
        modifier = modifier.padding(horizontal = 22.dp),
        state = listState,
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(total) { index ->
            val date = today.minusDays(PAST_DAYS.toLong()).plusDays(index.toLong())
            val isSelected = date == selected
            val isToday = date == today
            val mark = markFor(date)

            val shape = RoundedCornerShape(18.dp)
            val background by animateColorAsState(
                targetValue = if (isSelected) colors.ink else Color.Transparent,
                animationSpec = tween(180),
                label = "dateBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (!isSelected && isToday) {
                    colors.accent.copy(alpha = 0.42f)
                } else if (isSelected) {
                    colors.ink
                } else {
                    Color.Transparent
                },
                animationSpec = tween(180),
                label = "dateBorder"
            )

            Column(
                modifier = Modifier
                    .width(40.dp)
                    .height(64.dp)
                    .clip(shape)
                    .background(background)
                    .border(1.dp, borderColor, shape)
                    .clickable { onSelect(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isToday) "今" else weekChar(date.dayOfWeek.value),
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colors.onInk.copy(alpha = 0.72f) else colors.ink3
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) colors.onInk else colors.ink
                )
                Spacer(Modifier.height(5.dp))
                Box(Modifier.height(4.dp)) {
                    if (mark.count > 0) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> colors.onInk
                                        mark.allDone -> colors.ink3
                                        else -> colors.accent
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
