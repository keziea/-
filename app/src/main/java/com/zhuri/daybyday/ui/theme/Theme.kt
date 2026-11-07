package com.zhuri.daybyday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计令牌。任何页面都不允许写死颜色 / 字号，只能从这里取。
 * 规格见 docs/03-视觉与交互规范.md
 */
@Immutable
data class ZhuriColors(
    val paper: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val onInk: Color,
    val isDark: Boolean
)

val LightColors = ZhuriColors(
    paper = Color(0xFFF7F4EF),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF1ECE3),
    ink = Color(0xFF1A1714),
    ink2 = Color(0xFF6B635B),
    ink3 = Color(0xFFA69D93),
    line = Color(0xFFEAE3D9),
    accent = Color(0xFFDF5738),
    accentSoft = Color(0xFFFBE9E2),
    accentInk = Color(0xFFB23F26),
    onInk = Color(0xFFFBF7F1),
    isDark = false
)

val DarkColors = ZhuriColors(
    paper = Color(0xFF100E0C),
    surface = Color(0xFF1A1715),
    surfaceAlt = Color(0xFF241F1C),
    ink = Color(0xFFF3EEE7),
    ink2 = Color(0xFFA79E95),
    ink3 = Color(0xFF6E655D),
    line = Color(0xFF2B2622),
    accent = Color(0xFFF2704F),
    accentSoft = Color(0xFF33201A),
    accentInk = Color(0xFFF7A38C),
    onInk = Color(0xFF12100E),
    isDark = true
)

@Immutable
data class ZhuriType(
    val display: TextStyle,
    val displaySerif: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val body: TextStyle,
    val bodySm: TextStyle,
    val label: TextStyle,
    val caption: TextStyle
)

private val Serif = FontFamily.Serif

private val ZhuriTypeDefault = ZhuriType(
    display = TextStyle(
        fontFamily = Serif,
        fontSize = 54.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
        fontWeight = FontWeight.Normal
    ),
    displaySerif = TextStyle(
        fontFamily = Serif,
        fontSize = 50.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp,
        fontWeight = FontWeight.Normal
    ),
    h1 = TextStyle(
        fontSize = 27.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),
    h2 = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Medium
    ),
    bodySm = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    label = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp
    ),
    caption = TextStyle(
        fontSize = 11.sp,
        letterSpacing = 0.2.sp
    )
)

private val LocalZhuriColors = staticCompositionLocalOf { LightColors }

object Zhuri {
    val colors: ZhuriColors
        @Composable
        @ReadOnlyComposable
        get() = LocalZhuriColors.current

    val type: ZhuriType
        get() = ZhuriTypeDefault
}

// 常用间距，统一出口，避免各处散落数字
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val page = 22.dp
    val xxl = 32.dp
}

@Composable
fun ZhuriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onInk,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.ink2,
            outline = colors.line,
            outlineVariant = colors.line,
            error = colors.accent,
            onError = colors.onInk,
            scrim = Color(0xCC000000)
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onInk,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.ink2,
            outline = colors.line,
            outlineVariant = colors.line,
            error = colors.accent,
            onError = colors.onInk,
            scrim = Color(0x66000000)
        )
    }

    CompositionLocalProvider(LocalZhuriColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(),
            content = content
        )
    }
}
