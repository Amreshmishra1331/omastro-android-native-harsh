package com.omastro.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * App color tokens. These are `var`s (not `val`s) so [applyAppPalette] can swap
 * the whole palette for dark/light mode; [OmAstroTheme] wraps content in
 * `key(darkTheme)` so every reader recomposes with the new values.
 * Default = dark (the existing Cosmic Dark & Sacred Gold look).
 */
var CosmicBackground = Color(0xFF090A13)
    private set
var CosmicSurface = Color(0xFF131728)
    private set
var CosmicCardBg = Color(0xFF1A1F36)
    private set
var CosmicBorder = Color(0xFF2C324E)
    private set
var CosmicBorderGold = Color(0x66FFB300)
    private set

var SacredGold = Color(0xFFFFB300)
    private set
var SacredGoldDark = Color(0xFFD48800)
    private set
var SacredGoldLight = Color(0xFFFFE082)
    private set
var SacredSaffron = Color(0xFFFF6F00)
    private set
var SacredSaffronLight = Color(0xFFFF9E40)
    private set

var MysticPurple = Color(0xFF4A148C)
    private set
var MysticPurpleLight = Color(0xFF7C3AED)
    private set

var TextPrimary = Color(0xFFF8FAFC)
    private set
var TextSecondary = Color(0xFF94A3B8)
    private set
var TextGold = Color(0xFFFFD54F)
    private set

var AuspiciousGreen = Color(0xFF10B981)
    private set
var InauspiciousRed = Color(0xFFEF4444)
    private set
var NeutralBlue = Color(0xFF38BDF8)
    private set

// Aliases used across screens (follow the palette automatically)
var CosmicDeepBlue = CosmicBackground
    private set
var CosmicNavy = CosmicSurface
    private set
var CosmicCharcoal = CosmicCardBg
    private set
var StarlightWhite = TextPrimary
    private set
var RadiantAmber = SacredGold
    private set

/**
 * Swaps the active palette. Called by [OmAstroTheme] before composing content.
 * Light palette: warm cream/white surfaces, dark text, gold accents (matches
 * the Flutter app's light theme).
 */
fun applyAppPalette(dark: Boolean) {
    if (dark) {
        CosmicBackground = Color(0xFF090A13)
        CosmicSurface = Color(0xFF131728)
        CosmicCardBg = Color(0xFF1A1F36)
        CosmicBorder = Color(0xFF2C324E)
        CosmicBorderGold = Color(0x66FFB300)
        SacredGold = Color(0xFFFFB300)
        SacredGoldDark = Color(0xFFD48800)
        SacredGoldLight = Color(0xFFFFE082)
        SacredSaffron = Color(0xFFFF6F00)
        SacredSaffronLight = Color(0xFFFF9E40)
        MysticPurple = Color(0xFF4A148C)
        MysticPurpleLight = Color(0xFF7C3AED)
        TextPrimary = Color(0xFFF8FAFC)
        TextSecondary = Color(0xFF94A3B8)
        TextGold = Color(0xFFFFD54F)
        AuspiciousGreen = Color(0xFF10B981)
        InauspiciousRed = Color(0xFFEF4444)
        NeutralBlue = Color(0xFF38BDF8)
    } else {
        CosmicBackground = Color(0xFFFFFBF3) // warm cream
        CosmicSurface = Color(0xFFFFFFFF) // white
        CosmicCardBg = Color(0xFFFFFFFF) // white cards
        CosmicBorder = Color(0xFFE9DCC3) // sand border
        CosmicBorderGold = Color(0x66D48800)
        SacredGold = Color(0xFFC77F00) // deeper gold for contrast on light
        SacredGoldDark = Color(0xFF9A6200)
        SacredGoldLight = Color(0xFF8A5A00)
        SacredSaffron = Color(0xFFE65100)
        SacredSaffronLight = Color(0xFFFF9E40)
        MysticPurple = Color(0xFF6A1B9A)
        MysticPurpleLight = Color(0xFF7C3AED)
        TextPrimary = Color(0xFF1F1B12) // near-black warm
        TextSecondary = Color(0xFF6B6257)
        TextGold = Color(0xFF9A6200)
        AuspiciousGreen = Color(0xFF0E7C5B)
        InauspiciousRed = Color(0xFFC62828)
        NeutralBlue = Color(0xFF1565C0)
    }
    // Refresh aliases
    CosmicDeepBlue = CosmicBackground
    CosmicNavy = CosmicSurface
    CosmicCharcoal = CosmicCardBg
    StarlightWhite = TextPrimary
    RadiantAmber = SacredGold
}
