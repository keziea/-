package com.zhuri.daybyday.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhuri.daybyday.model.Achievement
import com.zhuri.daybyday.model.TrophyTier
import com.zhuri.daybyday.ui.theme.Zhuri
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private data class TrophyPalette(val light: Color, val base: Color, val dark: Color, val name: String)

private fun paletteOf(tier: TrophyTier): TrophyPalette = when (tier) {
    TrophyTier.BRONZE -> TrophyPalette(Color(0xFFF0C49B), Color(0xFFB9723F), Color(0xFF7C4423), "铜")
    TrophyTier.SILVER -> TrophyPalette(Color(0xFFF7FAFC), Color(0xFFA9B7C4), Color(0xFF5E6C79), "银")
    TrophyTier.GOLD -> TrophyPalette(Color(0xFFFFE3A3), Color(0xFFC99A45), Color(0xFF8C6122), "金")
    TrophyTier.JADE -> TrophyPalette(Color(0xFFD3E8DA), Color(0xFF759B84), Color(0xFF456353), "玉")
    TrophyTier.PLATINUM -> TrophyPalette(Color(0xFFF2F8FA), Color(0xFF9FB6C0), Color(0xFF557079), "铂")
}

/**
 * 自绘奖杯。unlocked=false 时为灰色剪影；unlocked=true 时使用材质渐变。
 */
@Composable
fun TrophyBadge(
    tier: TrophyTier,
    unlocked: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    val palette = paletteOf(tier)
    // 只有真正需要动效的那一座才创建无限动画；奖杯墙里 10 座静止杯不产生任何每帧重绘
    val pulse: Float = if (animated && unlocked) {
        val transition = rememberInfiniteTransition(label = "trophy")
        val value by transition.animateFloat(
            initialValue = 0.985f,
            targetValue = 1.015f,
            animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
            label = "trophyPulse"
        )
        value
    } else {
        1f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (animated && unlocked) {
                    scaleX = pulse
                    scaleY = pulse
                }
            }
    ) {
        val s = this.size.minDimension
        val lockedBase = Color(0xFFD8D2C8)
        val lockedDark = Color(0xFFBDB6AA)

        val brush = if (unlocked) {
            Brush.verticalGradient(
                listOf(palette.light, palette.base, palette.dark),
                startY = s * 0.15f,
                endY = s * 0.82f
            )
        } else {
            Brush.verticalGradient(listOf(lockedBase, lockedDark), startY = s * 0.15f, endY = s * 0.82f)
        }
        val edgeColor = if (unlocked) palette.dark else lockedDark

        // 杯身
        val cup = Path().apply {
            moveTo(s * 0.31f, s * 0.20f)
            lineTo(s * 0.69f, s * 0.20f)
            cubicTo(s * 0.69f, s * 0.45f, s * 0.62f, s * 0.58f, s * 0.50f, s * 0.58f)
            cubicTo(s * 0.38f, s * 0.58f, s * 0.31f, s * 0.45f, s * 0.31f, s * 0.20f)
            close()
        }
        drawPath(cup, brush)
        drawPath(cup, edgeColor.copy(alpha = 0.55f), style = Stroke(width = s * 0.008f))

        // 左侧高光带：让金属看起来有转折
        val shine = Path().apply {
            moveTo(s * 0.345f, s * 0.235f)
            cubicTo(s * 0.335f, s * 0.34f, s * 0.35f, s * 0.43f, s * 0.39f, s * 0.50f)
            lineTo(s * 0.43f, s * 0.47f)
            cubicTo(s * 0.39f, s * 0.40f, s * 0.385f, s * 0.31f, s * 0.395f, s * 0.235f)
            close()
        }
        drawPath(shine, Color.White.copy(alpha = if (unlocked) 0.30f else 0.12f))

        // 杯口
        drawRoundRect(
            brush = brush,
            topLeft = Offset(s * 0.27f, s * 0.165f),
            size = Size(s * 0.46f, s * 0.065f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.03f)
        )
        drawRoundRect(
            color = edgeColor.copy(alpha = 0.35f),
            topLeft = Offset(s * 0.27f, s * 0.222f),
            size = Size(s * 0.46f, s * 0.012f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.006f)
        )

        // 双耳
        val handleStroke = Stroke(width = s * 0.042f, cap = StrokeCap.Round)
        val leftHandle = Path().apply {
            moveTo(s * 0.30f, s * 0.255f)
            cubicTo(s * 0.14f, s * 0.245f, s * 0.13f, s * 0.41f, s * 0.30f, s * 0.455f)
        }
        val rightHandle = Path().apply {
            moveTo(s * 0.70f, s * 0.255f)
            cubicTo(s * 0.86f, s * 0.245f, s * 0.87f, s * 0.41f, s * 0.70f, s * 0.455f)
        }
        drawPath(leftHandle, edgeColor.copy(alpha = 0.95f), style = handleStroke)
        drawPath(rightHandle, edgeColor.copy(alpha = 0.95f), style = handleStroke)
        drawPath(
            leftHandle,
            Color.White.copy(alpha = if (unlocked) 0.45f else 0.18f),
            style = Stroke(width = s * 0.014f, cap = StrokeCap.Round)
        )
        drawPath(
            rightHandle,
            Color.White.copy(alpha = if (unlocked) 0.45f else 0.18f),
            style = Stroke(width = s * 0.014f, cap = StrokeCap.Round)
        )

        // 杯身高光
        drawOval(
            color = Color.White.copy(alpha = if (unlocked) 0.26f else 0.14f),
            topLeft = Offset(s * 0.355f, s * 0.235f),
            size = Size(s * 0.07f, s * 0.20f)
        )

        // 杯身星
        drawTrophyStar(
            center = Offset(s * 0.5f, s * 0.335f),
            radius = s * 0.070f,
            color = if (unlocked) Color.White.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.55f)
        )

        // 杯柄
        drawRoundRect(
            brush = brush,
            topLeft = Offset(s * 0.455f, s * 0.57f),
            size = Size(s * 0.09f, s * 0.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.02f)
        )
        // 底座
        drawRoundRect(
            brush = brush,
            topLeft = Offset(s * 0.33f, s * 0.705f),
            size = Size(s * 0.34f, s * 0.075f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.03f)
        )
        drawRoundRect(
            color = edgeColor.copy(alpha = 0.30f),
            topLeft = Offset(s * 0.33f, s * 0.772f),
            size = Size(s * 0.34f, s * 0.012f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.006f)
        )

        if (unlocked) {
            // 杯口小星芒
            drawTrophyStar(Offset(s * 0.735f, s * 0.205f), s * 0.020f, Color.White.copy(alpha = 0.9f))
            drawTrophyStar(Offset(s * 0.255f, s * 0.30f), s * 0.013f, Color.White.copy(alpha = 0.7f))
            // 底座铭牌
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f),
                topLeft = Offset(s * 0.395f, s * 0.723f),
                size = Size(s * 0.21f, s * 0.022f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.011f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrophyStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    val points = 5
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = (-90.0 + i * 180.0 / points) * Math.PI / 180.0
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** 奖杯墙：一行五个，未解锁为灰。 */
@Composable
fun TrophyRow(
    unlocked: Set<String>,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val list = com.zhuri.daybyday.model.AchievementCatalog
    list.chunked(5).forEach { rowItems ->
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rowItems.forEach { a ->
                val isUnlocked = a.id in unlocked
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TrophyBadge(
                        tier = a.tier,
                        unlocked = isUnlocked,
                        size = 40.dp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isUnlocked) a.name else "${a.days} 天",
                        fontSize = 8.sp,
                        color = if (isUnlocked) colors.ink2 else colors.ink3.copy(alpha = 0.6f),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
            repeat(5 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** 完成全部日程时的轻庆祝层 */
@Composable
fun AllDoneCelebration(onDismiss: () -> Unit) {
    val colors = Zhuri.colors
    val progress = remember { Animatable(0f) }
    val scale = remember { Animatable(0.86f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f))
    }
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }

    // 不铺全屏遮罩、不拦截触摸：庆祝只是浮一层，手可以直接继续操作
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Confetti(progress = progress.value)
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .padding(horizontal = 34.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(28.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                IconCheck(colors.accentInk, Modifier.size(24.dp), 2.6.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text("全部完成", style = Zhuri.type.h2, color = colors.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "今天的安排都做完了，太棒了",
                fontSize = 13.sp,
                color = colors.ink3,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 新奖杯解锁的庆祝层 */
@Composable
fun TrophyCelebration(achievement: Achievement, onDismiss: () -> Unit) {
    val colors = Zhuri.colors
    val loop = rememberInfiniteTransition(label = "trophyCelebration")
    val progress by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiLoop"
    )
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 220f))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Confetti(progress = progress)
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .padding(horizontal = 30.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(colors.surface)
                .padding(horizontal = 30.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("解锁新奖杯", fontSize = 11.sp, color = colors.accent, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                paletteOf(achievement.tier).base.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                TrophyBadge(
                    tier = achievement.tier,
                    unlocked = true,
                    size = 148.dp,
                    animated = true
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                achievement.name,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "连续完成 ${achievement.days} 天",
                fontSize = 12.sp,
                color = colors.ink3
            )
            Spacer(Modifier.height(10.dp))
            Text(
                achievement.motto,
                fontSize = 12.sp,
                color = colors.ink3,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text("轻触收下", fontSize = 10.sp, color = colors.ink3.copy(alpha = 0.7f))
        }
    }
}

/** 简单的纸屑，progress 0..1 控制下落 */
@Composable
private fun Confetti(progress: Float) {
    val colors = Zhuri.colors
    val palette = listOf(colors.accent, colors.note, Color(0xFFC9A15B), colors.ink3)
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (progress <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        for (i in 0 until 34) {
            val seed = (i * 37 % 100) / 100f
            val startX = w * ((i * 53 % 97) / 97f)
            val drift = sin((progress * 3.2f + seed) * Math.PI).toFloat() * w * 0.06f
            // 一开始就散布在整屏，再缓慢下落，保证任何一帧都看得见
            val baseY = h * (0.06f + seed * 0.9f)
            val y = baseY + h * progress * 0.10f
            val alpha = (1f - progress * 0.65f).coerceIn(0f, 1f)
            if (alpha <= 0f) continue
            val color = palette[i % palette.size].copy(alpha = alpha * 0.9f)
            rotate(degrees = progress * 420f + i * 18f, pivot = Offset(startX + drift, y)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(startX + drift, y),
                    size = Size(w * 0.020f, h * 0.009f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.010f)
                )
            }
        }
    }
}
