package com.pageturn.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PtNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = PtNavyLight,
    onPrimaryContainer = PtTextNavy,
    background = Color(0xFFF8F9FA),
    onBackground = PtTextMain,
    surface = Color.White,
    onSurface = PtTextMain,
    outline = Color(0xFFE5E5EA)
)

private val WarmColorScheme = lightColorScheme(
    primary = PtNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = PtNavyLight,
    onPrimaryContainer = PtTextNavy,
    background = PtBackgroundWarm,
    onBackground = PtTextWarm,
    surface = PtSurfaceWarm,
    onSurface = PtTextWarm,
    outline = PtDividerWarm
)

private val DarkColorScheme = darkColorScheme(
    primary = PtNavyLight,
    onPrimary = PtNavyPrimary,
    primaryContainer = PtNavyDark,
    onPrimaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE3E3E3),
    outline = Color(0xFF333333)
)

@Composable
fun PageTurnTheme(
    theme: String = "warm",
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        else -> WarmColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
