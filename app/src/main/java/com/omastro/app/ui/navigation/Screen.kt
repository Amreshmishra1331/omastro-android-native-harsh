package com.omastro.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Onboarding : Screen("onboarding", "Onboarding")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Kundali : Screen("kundali", "Kundali", Icons.Default.AutoAwesome)
    object Horoscope : Screen("horoscope", "Rashifal", Icons.Default.WbSunny)
    object Panchang : Screen("panchang", "Panchang", Icons.Default.CalendarToday)
    object Matchmaking : Screen("matchmaking", "Milan", Icons.Default.Favorite)
    object Astrologers : Screen("astrologers", "Astrologers", Icons.Default.SupportAgent)
    object AiChat : Screen("ai_chat", "AI Chat", Icons.Default.Chat) {
        /** Opens AI Chat with the saved-sessions history dialog open. */
        const val HISTORY_ROUTE = "ai_chat/history"
    }
    object DivineChat : Screen("divine_chat", "Divine Chat", Icons.Default.AutoAwesome)
    object Chat : Screen("chat/{slug}", "Consultation") {
        fun createRoute(slug: String) = "chat/$slug"
    }
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    /** Refer & Earn — real referral-credit data filtered from GET /wallet/transactions. */
    object ReferEarn : Screen("refer_earn", "Refer & Earn", Icons.Default.CardGiftcard)
    object AstroHealth : Screen("astro_health", "Astro Health", Icons.Default.HealthAndSafety)
    object Numerology : Screen("numerology", "Numerology", Icons.Default.Calculate)
    object NumerologyTool : Screen("numerology_tool/{toolId}", "Numerology Tool") {
        fun createRoute(toolId: String) = "numerology_tool/$toolId"
    }
    object SavedCharts : Screen("saved_charts", "Saved Charts", Icons.Default.Bookmarks)
    object Gochar : Screen("gochar", "Gochar", Icons.Default.Public)
    object Prashna : Screen("prashna", "Prashna Kundali", Icons.Default.QuestionAnswer)
    object Muhurat : Screen("muhurat", "Muhurat", Icons.Default.Schedule)
    object Palmistry : Screen("palmistry", "Palmistry", Icons.Default.PanTool)
    object FaceReading : Screen("face_reading", "Face Reading", Icons.Default.Face)
    object Vastu : Screen("vastu", "Vastu Shastra", Icons.Default.HomeWork)
    object ReadingHistory : Screen("reading_history", "Reading History", Icons.Default.History)
    object ReadingHistoryDetail : Screen("reading_history_detail/{readingId}", "Reading") {
        fun createRoute(readingId: String) = "reading_history_detail/$readingId"
    }
    /** Unified on-device "My Data": Kundalis, readings and numerology results. */
    object MyData : Screen("my_data", "My Data", Icons.Default.SdStorage)
    object Auth : Screen("auth", "Sign In") {
        /** Opens the auth screen directly in forgot-password mode. */
        const val FORGOT_ROUTE = "auth/forgot"
    }
    object Notifications : Screen("notifications", "Notifications", Icons.Default.Notifications)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Kundali,
    Screen.AiChat,
    Screen.Horoscope,
    Screen.Astrologers,
    Screen.Settings
)
