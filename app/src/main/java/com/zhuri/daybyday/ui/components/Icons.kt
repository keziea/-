package com.zhuri.daybyday.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全部图标自绘，不引图标库，保证 APK 体积与线条风格统一。
 * 约定：画布为正方形，线宽默认 1.8dp，端点圆头。
 */

@Composable
fun IconPlus(color: Color, modifier: Modifier = Modifier, stroke: Dp = 2.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val c = Offset(s / 2f, s / 2f)
        val r = s * 0.30f
        drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), sw, StrokeCap.Round)
        drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), sw, StrokeCap.Round)
    }
}

@Composable
fun IconCheck(color: Color, modifier: Modifier = Modifier, stroke: Dp = 2.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val path = Path().apply {
            moveTo(s * 0.20f, s * 0.53f)
            lineTo(s * 0.42f, s * 0.74f)
            lineTo(s * 0.80f, s * 0.27f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconClose(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val a = s * 0.28f
        val b = s * 0.72f
        drawLine(color, Offset(a, a), Offset(b, b), sw, StrokeCap.Round)
        drawLine(color, Offset(b, a), Offset(a, b), sw, StrokeCap.Round)
    }
}

/** direction: -1 左, 1 右 */
@Composable
fun IconChevron(
    color: Color,
    direction: Int,
    modifier: Modifier = Modifier,
    stroke: Dp = 1.8.dp
) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val x0 = if (direction < 0) s * 0.60f else s * 0.40f
        val x1 = if (direction < 0) s * 0.38f else s * 0.62f
        val path = Path().apply {
            moveTo(x0, s * 0.22f)
            lineTo(x1, s * 0.50f)
            lineTo(x0, s * 0.78f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** direction: 1 上, -1 下 */
@Composable
fun IconCaret(
    color: Color,
    direction: Int,
    modifier: Modifier = Modifier,
    stroke: Dp = 1.8.dp
) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val y0 = if (direction > 0) s * 0.62f else s * 0.38f
        val y1 = if (direction > 0) s * 0.38f else s * 0.62f
        val path = Path().apply {
            moveTo(s * 0.24f, y0)
            lineTo(s * 0.50f, y1)
            lineTo(s * 0.76f, y0)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 今日：圆圈 + 勾 */
@Composable
fun IconToday(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val inset = sw / 2f
        drawCircle(
            color = color,
            radius = s / 2f - inset,
            center = Offset(s / 2f, s / 2f),
            style = Stroke(sw)
        )
        val path = Path().apply {
            moveTo(s * 0.33f, s * 0.52f)
            lineTo(s * 0.45f, s * 0.64f)
            lineTo(s * 0.68f, s * 0.38f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 日历：圆角框 + 顶部横线 + 六个点 */
@Composable
fun IconCalendar(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val o = sw / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(o, s * 0.10f + o),
            size = Size(s - sw, s * 0.82f - sw),
            cornerRadius = CornerRadius(s * 0.16f),
            style = Stroke(sw)
        )
        drawLine(
            color,
            Offset(o, s * 0.38f),
            Offset(s - o, s * 0.38f),
            sw * 0.85f,
            StrokeCap.Round
        )
        val dotR = s * 0.045f
        val xs = floatArrayOf(s * 0.32f, s * 0.50f, s * 0.68f)
        val ys = floatArrayOf(s * 0.56f, s * 0.72f)
        ys.forEach { y ->
            xs.forEach { x ->
                drawCircle(color, dotR, Offset(x, y))
            }
        }
    }
}

/** 总结：三根柱子 + 基线 */
@Composable
fun IconStats(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val baseY = s * 0.84f
        drawLine(color, Offset(s * 0.14f, baseY), Offset(s * 0.86f, baseY), sw, StrokeCap.Round)
        val w = s * 0.14f
        val heights = floatArrayOf(0.26f, 0.46f, 0.66f)
        heights.forEachIndexed { i, h ->
            val x = s * 0.22f + i * s * 0.22f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, baseY - s * h),
                size = Size(w, s * h),
                cornerRadius = CornerRadius(w / 2f, w / 2f)
            )
        }
    }
}

/** 时钟：圆 + 两根指针 */
@Composable
fun IconClock(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.7.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val o = sw / 2f
        drawCircle(color, s / 2f - o, Offset(s / 2f, s / 2f), style = Stroke(sw))
        drawLine(
            color,
            Offset(s / 2f, s / 2f),
            Offset(s / 2f, s * 0.28f),
            sw,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(s / 2f, s / 2f),
            Offset(s * 0.68f, s * 0.60f),
            sw,
            StrokeCap.Round
        )
    }
}

/** 笔记：圆角书页 + 两条短横线 */
@Composable
fun IconDiary(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val o = sw / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.18f + o, s * 0.10f + o),
            size = Size(s * 0.64f - sw, s * 0.80f - sw),
            cornerRadius = CornerRadius(s * 0.12f),
            style = Stroke(sw)
        )
        drawLine(color, Offset(s * 0.34f, s * 0.38f), Offset(s * 0.66f, s * 0.38f), sw, StrokeCap.Round)
        drawLine(color, Offset(s * 0.34f, s * 0.58f), Offset(s * 0.58f, s * 0.58f), sw, StrokeCap.Round)
    }
}

/** 笔：斜向笔身 + 笔尖分隔线 */
@Composable
fun IconPen(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        val body = Path().apply {
            moveTo(s * 0.20f, s * 0.80f)
            lineTo(s * 0.27f, s * 0.56f)
            lineTo(s * 0.60f, s * 0.23f)
            lineTo(s * 0.77f, s * 0.40f)
            lineTo(s * 0.44f, s * 0.73f)
            close()
        }
        drawPath(
            path = body,
            color = color,
            style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color,
            Offset(s * 0.27f, s * 0.56f),
            Offset(s * 0.44f, s * 0.73f),
            sw * 0.9f,
            StrokeCap.Round
        )
    }
}

/** 搜索：圆 + 手柄 */
@Composable
fun IconSearch(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.8.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        drawCircle(
            color = color,
            radius = s * 0.30f,
            center = Offset(s * 0.44f, s * 0.44f),
            style = Stroke(sw)
        )
        drawLine(
            color,
            Offset(s * 0.65f, s * 0.65f),
            Offset(s * 0.84f, s * 0.84f),
            sw,
            StrokeCap.Round
        )
    }
}

/** 垃圾桶：盖 + 桶身 + 一条竖线 */
@Composable
fun IconTrash(color: Color, modifier: Modifier = Modifier, stroke: Dp = 1.6.dp) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = stroke.toPx()
        drawLine(color, Offset(s * 0.16f, s * 0.28f), Offset(s * 0.84f, s * 0.28f), sw, StrokeCap.Round)
        drawLine(color, Offset(s * 0.40f, s * 0.20f), Offset(s * 0.60f, s * 0.20f), sw, StrokeCap.Round)
        val path = Path().apply {
            moveTo(s * 0.26f, s * 0.32f)
            lineTo(s * 0.31f, s * 0.82f)
            lineTo(s * 0.69f, s * 0.82f)
            lineTo(s * 0.74f, s * 0.32f)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(s * 0.50f, s * 0.42f), Offset(s * 0.50f, s * 0.70f), sw * 0.8f, StrokeCap.Round)
    }
}
