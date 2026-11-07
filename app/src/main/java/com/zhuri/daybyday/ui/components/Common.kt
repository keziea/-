package com.zhuri.daybyday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zhuri.daybyday.ui.theme.Zhuri
import kotlin.math.roundToInt

/** 完成度环。尺寸 / 线宽严格按 docs/03 6.2。 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 58.dp,
    strokeWidth: Dp = 5.dp,
    track: Color = Zhuri.colors.line,
    color: Color = Zhuri.colors.accent,
    textColor: Color = Zhuri.colors.ink,
    showText: Boolean = true
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ring"
    )
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val sw = strokeWidth.toPx()
            val d = diameter.toPx() - sw
            val topLeft = Offset((diameter.toPx() - d) / 2f, (diameter.toPx() - d) / 2f)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = sw)
            )
            if (animated > 0.002f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(d, d),
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }
        if (showText) {
            Text(
                text = "${(animated * 100).roundToInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/** 任务左边的圆圈勾选。视觉 23dp，点击热区 40dp。 */
@Composable
fun CheckCircle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val background by animateColorAsState(
        targetValue = if (checked) colors.accent else Color.Transparent,
        animationSpec = tween(180),
        label = "checkBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.ink3,
        animationSpec = tween(180),
        label = "checkBorder"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(150),
        label = "checkAlpha"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 620f),
        label = "checkScale"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(23.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.6.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checkAlpha > 0.01f) {
                IconCheck(
                    color = colors.onInk,
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                        .alpha(checkAlpha),
                    stroke = 2.6.dp
                )
            }
        }
    }
}

/** 通用胶囊选项 */
@Composable
fun ZChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val colors = Zhuri.colors
    val background = when {
        selected && accent -> colors.accent
        selected -> colors.ink
        else -> colors.surfaceAlt
    }
    val contentColor = when {
        selected -> colors.onInk
        else -> colors.ink2
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor
        )
    }
}

/** 分组小标题：待办 3 / 已完成 2 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = Zhuri.type.label,
            color = Zhuri.colors.ink3
        )
        Spacer(Modifier.width(8.dp))
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/** 一枚安静的文字按钮，外套 44dp 点击热区。 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Zhuri.colors.accent,
    fontSize: Int = 11
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/** 空状态：自绘插画 + 两句文案 + 文字按钮。[illustration] 可替换插画。 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null
) {
    val colors = Zhuri.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            illustration()
        } else {
            Canvas(Modifier.size(84.dp)) {
                val s = size.minDimension
                val c = Offset(s / 2f, s / 2f)
                drawCircle(colors.accentSoft, radius = s * 0.42f, center = c)
                val path = Path().apply {
                    moveTo(s * 0.34f, s * 0.52f)
                    lineTo(s * 0.46f, s * 0.64f)
                    lineTo(s * 0.68f, s * 0.38f)
                }
                drawPath(
                    path = path,
                    color = colors.accent,
                    style = Stroke(width = s * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawCircle(colors.accent, radius = s * 0.022f, center = Offset(s * 0.74f, s * 0.26f))
                drawCircle(colors.accent, radius = s * 0.014f, center = Offset(s * 0.24f, s * 0.34f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = colors.ink3,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onAction)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = actionText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        }
    }
}

/** 二次确认，风格与整体一致（不用系统 AlertDialog） */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = Zhuri.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.surface)
                .padding(22.dp)
        ) {
            Text(title, style = Zhuri.type.h2, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 14.sp, lineHeight = 20.sp, color = colors.ink2)
            Spacer(Modifier.height(20.dp))
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
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(confirmText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.onInk)
                }
            }
        }
    }
}
