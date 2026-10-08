package com.omastro.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CosmicDarkColorScheme = darkColorScheme(
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

@Composable
fun OmAstroTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = CosmicBackground.toArgb()
                it.navigationBarColor = CosmicBackground.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
