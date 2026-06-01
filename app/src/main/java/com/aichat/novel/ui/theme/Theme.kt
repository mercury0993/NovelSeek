package com.aichat.novel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Custom color palette for NovelSeek, inspired by DeepSeek's visual style.
 */
object NovelSeekColors {
    val Primary = Color(0xFF4D6BFE)          // DeepSeek Blue
    val BackgroundLight = Color(0xFFFFFFFF)  // White
    val SurfaceLight = Color(0xFFFFFFFF)     // White
    val UserBubble = Color(0xFFE8F0FE)       // Light blue
    val AiBubble = Color(0xFFF5F5F5)         // Light gray
    val TextPrimary = Color(0xFF1A1A1A)      // Near-black
    val TextSecondary = Color(0xFF666666)    // Gray
    val ThinkingBackground = Color(0xFFF0F0F0) // Light gray for thinking area
}

private val LightColorScheme = lightColorScheme(
    primary = NovelSeekColors.Primary,
    onPrimary = Color.White,
    background = NovelSeekColors.BackgroundLight,
    onBackground = NovelSeekColors.TextPrimary,
    surface = NovelSeekColors.SurfaceLight,
    onSurface = NovelSeekColors.TextPrimary,
)

@Composable
fun NovelSeekTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
