package com.omastro.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun cosmicColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = SacredGold,
        onPrimary = CosmicBackground,
        primaryContainer = SacredGoldDark,
        onPrimaryContainer = SacredGoldLight,
        secondary = SacredSaffron,
        onSecondary = CosmicBackground,
        secondaryContainer = MysticPurple,
        onSecondaryContainer = SacredGoldLight,
        tertiary = MysticPurpleLight,
        background = CosmicBackground,
        onBackground = TextPrimary,
        surface = CosmicSurface,
        onSurface = TextPrimary,
        surfaceVariant = CosmicCardBg,
        onSurfaceVariant = TextSecondary,
        outline = CosmicBorder
    )
} else {
    lightColorScheme(
        primary = SacredGold,
        onPrimary = CosmicBackground,
        primaryContainer = SacredGoldDark,
        onPrimaryContainer = CosmicBackground,
        secondary = SacredSaffron,
        onSecondary = CosmicBackground,
        secondaryContainer = MysticPurpleLight,
        onSecondaryContainer = CosmicBackground,
        tertiary = MysticPurple,
        background = CosmicBackground,
        onBackground = TextPrimary,
        surface = CosmicSurface,
        onSurface = TextPrimary,
        surfaceVariant = CosmicCardBg,
        onSurfaceVariant = TextSecondary,
        outline = CosmicBorder
    )
}

/**
 * App theme with working dark/light mode. Applies the palette (custom tokens)
 * before composing so every screen picks up the active mode.
 */
@Composable
fun OmAstroTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    applyAppPalette(darkTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = CosmicBackground.toArgb()
                it.navigationBarColor = CosmicBackground.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = cosmicColorScheme(darkTheme),
        typography = Typography,
        content = content
    )
}
