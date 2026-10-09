package com.omastro.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Kundali : Screen("kundali", "Kundali", Icons.Default.AutoAwesome)
    object Horoscope : Screen("horoscope", "Rashifal", Icons.Default.WbSunny)
    object Panchang : Screen("panchang", "Panchang", Icons.Default.CalendarToday)
    object Matchmaking : Screen("matchmaking", "Milan", Icons.Default.Favorite)
    object Astrologers : Screen("astrologers", "Astrologers", Icons.Default.SupportAgent)
    object AiChat : Screen("ai_chat", "AI Chat", Icons.Default.Chat)
    object Chat : Screen("chat/{astrologerId}", "Consultation") {
        fun createRoute(astrologerId: String) = "chat/$astrologerId"
    }
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AstroHealth : Screen("astro_health", "Astro Health", Icons.Default.HealthAndSafety)
    object Numerology : Screen("numerology", "Numerology", Icons.Default.Calculate)
    object SavedCharts : Screen("saved_charts", "Saved Charts", Icons.Default.Bookmarks)
    object Gochar : Screen("gochar", "Gochar", Icons.Default.Public)
    object Muhurat : Screen("muhurat", "Muhurat", Icons.Default.Schedule)
    object Auth : Screen("auth", "Sign In")
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Kundali,
    Screen.AiChat,
    Screen.Horoscope,
    Screen.Astrologers,
    Screen.Settings
)
