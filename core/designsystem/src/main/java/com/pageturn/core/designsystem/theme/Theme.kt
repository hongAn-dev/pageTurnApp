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
    background = Color(0xFFF6F8FA),
    onBackground = PtTextMain,
    surface = Color.White,
    onSurface = PtTextMain,
    outline = Color(0xFFE2E7EC)
)

private val WarmColorScheme = lightColorScheme(
    primary = PtNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = PtGoldLight,
    onPrimaryContainer = PtTextWarm,
    background = PtBackgroundWarm,
    onBackground = PtTextWarm,
    surface = PtSurfaceWarm,
    onSurface = PtTextWarm,
    outline = PtDividerWarm
)

private val DarkColorScheme = darkColorScheme(
    primary = PtDarkPrimary,
    onPrimary = PtNavyDark,
    primaryContainer = PtDarkPrimaryContainer,
    onPrimaryContainer = PtDarkOnPrimaryContainer,
    background = PtDarkBg,
    onBackground = PtDarkText,
    surface = PtDarkSurface,
    onSurface = PtDarkText,
    outline = PtDarkOutline
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

