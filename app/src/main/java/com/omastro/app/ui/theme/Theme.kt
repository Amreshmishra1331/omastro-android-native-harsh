package com.omastro.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════
//  OmAstro AI — Material3 Theme
// ═══════════════════════════════════════════════

private val CosmicDarkColorScheme = darkColorScheme(
    primary          = SacredGold,
    onPrimary        = CosmicBackground,
    primaryContainer = SacredGoldSubtle,
    onPrimaryContainer = SacredGoldGlow,
    secondary        = CosmicViolet,
    onSecondary      = TextPrimary,
    secondaryContainer = CosmicVioletSubtle,
    onSecondaryContainer = CosmicVioletLight,
    tertiary         = SacredSaffron,
    onTertiary       = TextPrimary,
    background       = CosmicBackground,
    onBackground     = TextPrimary,
    surface          = CosmicSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = CosmicCardBg,
    onSurfaceVariant = TextSecondary,
    outline          = CosmicCardBorder,
    outlineVariant   = Color(0xFF1E1740),
    error            = InauspiciousRed,
    onError          = TextPrimary,
    errorContainer   = Color(0x33EF4444),
    onErrorContainer = InauspiciousRed,
    inverseSurface   = TextPrimary,
    inverseOnSurface = CosmicBackground,
    inversePrimary   = SacredGoldDim,
    scrim            = Color(0xCC05030F),
    surfaceTint      = CosmicViolet
)

private val CosmicLightColorScheme = lightColorScheme(
    primary          = LightGold,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFF3D0),
    onPrimaryContainer = Color(0xFF7A4E00),
    secondary        = CosmicVioletDim,
    onSecondary      = Color.White,
    background       = LightBackground,
    onBackground     = LightText,
    surface          = LightSurface,
    onSurface        = LightText,
    surfaceVariant   = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline          = LightBorder,
    error            = InauspiciousRed,
    onError          = Color.White
)

@Composable
fun OmAstroTheme(
    darkTheme: Boolean = true,   // Default dark for astro apps
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = CosmicBackground.toArgb()
            window.navigationBarColor = CosmicBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = OmAstroTypography,
        content     = content
    )
}
