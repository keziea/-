package com.zhuri.daybyday.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自适应宽度。
 *
 * 内容在手机上铺满；到了平板 / 横屏 / 折叠屏，收窄到 [contentMaxWidth] 并水平居中，
 * 避免任务卡、统计图被拉成一条很长的横线。导航和 FAB 用同一个内容边界对齐。
 */
object ZhuriLayout {
    val contentMaxWidth = 720.dp
    val dialogMaxWidth = 420.dp
    val navMaxWidth = 560.dp

    fun contentInset(screenWidth: Dp): Dp {
        val content = if (screenWidth < contentMaxWidth) screenWidth else contentMaxWidth
        return ((screenWidth - content) / 2).coerceAtLeast(0.dp)
    }
}

/** 居中、限宽的内容容器，所有页面共用。 */
@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ZhuriLayout.contentMaxWidth,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize()
        ) {
            content()
        }
    }
}

/** 自绘对话框在平板上的限宽容器。 */
@Composable
fun AdaptiveDialogFrame(
    maxWidth: Dp = ZhuriLayout.dialogMaxWidth,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
