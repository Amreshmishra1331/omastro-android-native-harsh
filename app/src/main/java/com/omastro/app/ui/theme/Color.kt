package com.omastro.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════
//  OmAstro AI — Cosmic Vedic Dark Color System
// ═══════════════════════════════════════════════

// ── Core Backgrounds ──
val CosmicBackground    = Color(0xFF05030F)   // Pure deep space
val CosmicSurface       = Color(0xFF0D0A1E)   // Dark nebula
val CosmicCardBg        = Color(0xFF130F2A)   // Card surface
val CosmicCardBorder    = Color(0xFF2A1F5C)   // Subtle card border
val CosmicElevated      = Color(0xFF1C1640)   // Elevated surface

// ── Sacred Gold (Primary) ──
val SacredGold          = Color(0xFFFFB300)   // Temple gold
val SacredGoldDim       = Color(0xFFCC8F00)   // Dimmed gold
val SacredGoldGlow      = Color(0xFFFFD700)   // Gold shimmer/glow
val SacredGoldSubtle    = Color(0x33FFB300)   // Gold 20% alpha

// ── Cosmic Violet (Secondary) ──
val CosmicViolet        = Color(0xFF7C3AED)   // Primary violet
val CosmicVioletLight   = Color(0xFFA855F7)   // Light violet / glow
val CosmicVioletDim     = Color(0xFF5B21B6)   // Deep violet
val CosmicVioletSubtle  = Color(0x337C3AED)   // Violet 20% alpha

// ── Sacred Saffron (Tertiary) ──
val SacredSaffron       = Color(0xFFFF6B35)   // Saffron accent
val SacredSaffronDim    = Color(0xFFE55A25)   // Dimmed saffron

// ── Text Colors ──
val TextPrimary         = Color(0xFFF8FAFC)   // Pure white text
val TextSecondary       = Color(0xFF9CA3C7)   // Muted lavender
val TextGold            = Color(0xFFFFD700)   // Gold text
val TextMuted           = Color(0xFF6B7280)   // Very muted
val TextHint            = Color(0xFF4B5563)   // Hint/placeholder

// ── Status Colors ──
val AuspiciousGreen     = Color(0xFF10B981)   // Good/success
val InauspiciousRed     = Color(0xFFEF4444)   // Bad/error
val NeutralBlue         = Color(0xFF3B82F6)   // Info
val WarningAmber        = Color(0xFFF59E0B)   // Warning

// ── Gradient Stops ──
val GradientVioletStart = Color(0xFF4C1D95)
val GradientVioletEnd   = Color(0xFF7C3AED)
val GradientGoldStart   = Color(0xFFFFB300)
val GradientGoldEnd     = Color(0xFFFF6B35)
val GradientCosmicStart = Color(0xFF05030F)
val GradientCosmicEnd   = Color(0xFF1C1640)

// ── Planet Colors ──
val SunColor            = Color(0xFFFFD700)   // सूर्य
val MoonColor           = Color(0xFFE2E8F0)   // चंद्र
val MarsColor           = Color(0xFFEF4444)   // मंगल
val MercuryColor        = Color(0xFF10B981)   // बुध
val JupiterColor        = Color(0xFFF59E0B)   // बृहस्पति
val VenusColor          = Color(0xFFEC4899)   // शुक्र
val SaturnColor         = Color(0xFF6B7280)   // शनि
val RahuColor           = Color(0xFF4B5563)   // राहु
val KetuColor           = Color(0xFFB45309)   // केतु

// ── Zodiac Element Colors ──
val FireElement         = Color(0xFFEF4444)   // Aries, Leo, Sagittarius
val EarthElement        = Color(0xFF92400E)   // Taurus, Virgo, Capricorn
val AirElement          = Color(0xFF0EA5E9)   // Gemini, Libra, Aquarius
val WaterElement        = Color(0xFF1D4ED8)   // Cancer, Scorpio, Pisces

// ── Glass / Blur Effect Colors ──
val GlassWhite          = Color(0x1AFFFFFF)   // White 10%
val GlassGold           = Color(0x1AFFB300)   // Gold 10%
val GlassViolet         = Color(0x1A7C3AED)   // Violet 10%
val GlassBorder         = Color(0x33FFFFFF)   // Border 20%

// ── Light Theme Colors ──
val LightBackground     = Color(0xFFFFFBF3)   // Warm cream
val LightSurface        = Color(0xFFFFFFFF)   // Pure white
val LightCard           = Color(0xFFFFF8E7)   // Warm card
val LightBorder         = Color(0xFFE9DCC3)   // Sand border
val LightGold           = Color(0xFFC77F00)   // Dark gold
val LightText           = Color(0xFF1F1B12)   // Dark warm text
val LightTextSecondary  = Color(0xFF6B5E45)   // Medium brown

// ═══════════════════════════════════════════════
//  Backward-Compat Aliases (existing screens)
// ═══════════════════════════════════════════════
val StarlightWhite      = TextPrimary           // alias → F8FAFC
val CosmicBorder        = CosmicCardBorder      // alias → 2A1F5C
val CosmicBorderGold    = SacredGold            // alias → FFB300
val SacredGoldLight     = SacredGoldGlow        // alias → FFD700
val SacredGoldDark      = SacredGoldDim         // alias → CC8F00
val CosmicDeepBlue      = CosmicVioletDim       // alias → 5B21B6
