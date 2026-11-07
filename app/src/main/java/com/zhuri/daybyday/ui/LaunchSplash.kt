package com.zhuri.daybyday.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.ui.theme.Zhuri
import kotlin.math.cos
import kotlin.math.sin

private const val DURATION_MS = 620

private val CREAM = Color(0xFFFDF6EE)
private val GRADIENT_TOP = Color(0xFFF4825A)
private val GRADIENT_BOTTOM = Color(0xFFCC4526)

private fun easeOutCubic(raw: Float): Float {
    val t = raw.coerceIn(0f, 1f)
    val inv = 1f - t
    return 1f - inv * inv * inv
}

/**
 * 启动动画：系统启动画面之后接管，太阳从地平线升起 + 标题淡入，然后整体淡出。
 * 规格见 docs/03 第 9.3 节。总时长 560ms，动画期间主界面已完成首帧，淡出后立即可用。
 */
@Composable
fun LaunchSplash(onFinish: () -> Unit) {
    val colors = Zhuri.colors
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        onFinish()
    }

    val p = progress.value
    val sunT = easeOutCubic((p - 0.00f) / 0.42f)
    val lineT = easeOutCubic((p - 0.08f) / 0.38f)
    val textT = easeOutCubic((p - 0.28f) / 0.22f)
    val fadeT = ((p - 0.84f) / 0.16f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - fadeT }
            .background(colors.paper)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val s = 1f + 0.05f * fadeT
                scaleX = s
                scaleY = s
            }
        ) {
            MarkCard(sunT = sunT, lineT = lineT)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "逐日",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = colors.ink.copy(alpha = textT * 0.85f),
                modifier = Modifier.graphicsLayer { translationY = (1f - textT) * 20f }
            )
        }
    }
}

@Composable
private fun MarkCard(sunT: Float, lineT: Float) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .size(100.dp)
            .shadow(22.dp, shape, clip = false, ambientColor = Color(0x2ECC4526), spotColor = Color(0x33CC4526))
            .clip(shape)
            .background(Brush.linearGradient(listOf(GRADIENT_TOP, GRADIENT_BOTTOM)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 108f
            val cx = 54f * unit
            val rise = (1f - sunT) * 20f * unit
            val cy = 47f * unit + rise
            val markAlpha = sunT.coerceAtLeast(0.02f)

            // 光晕
            drawCircle(
                color = CREAM.copy(alpha = 0.09f * sunT),
                radius = 24.5f * unit,
                center = Offset(cx, cy)
            )
            // 三道光芒
            intArrayOf(-90, -138, -42).forEach { angle ->
                val rad = Math.toRadians(angle.toDouble())
                val dx = cos(rad).toFloat()
                val dy = sin(rad).toFloat()
                drawLine(
                    color = CREAM.copy(alpha = markAlpha),
                    start = Offset(cx + dx * 18f * unit, cy + dy * 18f * unit),
                    end = Offset(cx + dx * 24f * unit, cy + dy * 24f * unit),
                    strokeWidth = 5.2f * unit,
                    cap = StrokeCap.Round
                )
            }
            // 太阳
            drawCircle(
                color = CREAM.copy(alpha = markAlpha),
                radius = 14.5f * unit,
                center = Offset(cx, cy)
            )
            // 地平线：从中心向两端生长
            if (lineT > 0.01f) {
                val halfW = 24f * lineT * unit
                drawRoundRect(
                    color = CREAM,
                    topLeft = Offset(cx - halfW, 68.8f * unit),
                    size = Size(halfW * 2f, 6.6f * unit),
                    cornerRadius = CornerRadius(3.3f * unit)
                )
            }
        }
    }
}
