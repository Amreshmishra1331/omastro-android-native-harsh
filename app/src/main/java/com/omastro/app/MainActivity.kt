package com.omastro.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.components.DrawerDialog
import com.omastro.app.ui.components.DrawerDialogHost
import com.omastro.app.ui.components.OmAstroDrawerContent
import com.omastro.app.ui.navigation.BottomNavBar
import com.omastro.app.ui.navigation.PendingChartHolder
import com.omastro.app.ui.navigation.Screen
import kotlinx.coroutines.launch
import com.omastro.app.ui.screens.astrologers.AstrologersScreen
import com.omastro.app.ui.screens.astrologers.ChatScreen
import com.omastro.app.ui.screens.auth.AuthScreen
import com.omastro.app.ui.screens.dashboard.DashboardScreen
import com.omastro.app.ui.screens.horoscope.HoroscopeScreen
import com.omastro.app.ui.screens.kundali.KundaliScreen
import com.omastro.app.ui.screens.matching.MatchmakingScreen
import com.omastro.app.ui.screens.panchang.PanchangScreen
import com.omastro.app.ui.screens.savedcharts.SavedChartsScreen
import com.omastro.app.ui.screens.settings.SettingsScreen
import com.omastro.app.ui.screens.wallet.WalletScreen
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.ui.theme.OmAstroTheme

class MainActivity : ComponentActivity() {

    /** Maps an FCM `notification_type` extra to an in-app route. */
    private fun routeForNotificationType(type: String?): String? {
        return when (type) {
            "ai_chat", "chat" -> Screen.AiChat.route
            "astrologer" -> Screen.Astrologers.route
            "panchang" -> Screen.Panchang.route
            "horoscope" -> Screen.Horoscope.route
            else -> null
        }
    }

    /** Deep link arriving while the activity is already alive (notification tap). */
    private val pendingDeepLink = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = routeForNotificationType(
            intent.getStringExtra("notification_type")
        )
        intent.removeExtra("notification_type")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Consume the launch-time deep link so it isn't re-applied.
        val launchDeepLink = routeForNotificationType(
            intent.getStringExtra("notification_type")
        ).also { intent.removeExtra("notification_type") }
        setContent {
            // Theme toggle: key() forces full recomposition so the swapped
            // palette tokens are picked up by every screen.
            val isDarkTheme by com.omastro.app.data.local.ThemePrefs.isDarkTheme.collectAsState()
            androidx.compose.runtime.key(isDarkTheme) {
            OmAstroTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                // Navigate when a notification arrives while the app is alive.
                val liveDeepLink by pendingDeepLink
                LaunchedEffect(liveDeepLink) {
                    liveDeepLink?.let { route ->
                        pendingDeepLink.value = null
                        navController.navigate(route)
                    }
                }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Auth gate: check login state on startup
                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) {
                    isLoggedIn = try {
                        ApiClient.isLoggedIn()
                    } catch (e: Exception) {
                        false
                    }
                }

                when (isLoggedIn) {
                    null -> {
                        // Show splash/loading while checking auth
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                    else -> {
                        // Deep link: notification taps land on the relevant
                        // screen instead of always opening home.
                        val startDestination = if (isLoggedIn == true) {
                            launchDeepLink ?: Screen.Home.route
                        } else {
                            Screen.Auth.route
                        }

                        // Navigation drawer state
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val drawerScope = rememberCoroutineScope()
                        var drawerDialog by remember { mutableStateOf<DrawerDialog?>(null) }
                        val drawerUser by AppContainer.authRepository.currentUser.collectAsState()
                        val drawerBalance by AppContainer.walletRepository.coinBalance.collectAsState()
                        val openDrawer: () -> Unit = { drawerScope.launch { drawerState.open() } }
                        val activity = this@MainActivity

                        fun drawerNavigate(route: String) {
                            drawerScope.launch { drawerState.close() }
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }

                        fun drawerShowDialog(kind: DrawerDialog) {
                            drawerScope.launch { drawerState.close() }
                            drawerDialog = kind
                        }

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Kundali.route,
                    Screen.AiChat.route,
                    Screen.Horoscope.route,
                    Screen.Astrologers.route,
                    Screen.Settings.route
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = drawerState.isOpen,
                        drawerContent = {
                            OmAstroDrawerContent(
                                userName = drawerUser?.name,
                                userEmail = drawerUser?.email ?: drawerUser?.phone,
                                walletBalance = drawerBalance,
                                currentRoute = currentRoute,
                                onNavigate = ::drawerNavigate,
                                onWalletClick = { drawerNavigate(Screen.Wallet.route) },
                                onRateApp = {
                                    drawerScope.launch { drawerState.close() }
                                    drawerScope.launch {
                                        AppContainer.requestInAppReviewUseCase.launchReview(activity)
                                    }
                                },
                                onReportBug = {
                                    drawerScope.launch { drawerState.close() }
                                    val bugIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:support@omastroai.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "OmAstro App Bug Report")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "App version: ${com.omastro.app.BuildConfig.VERSION_NAME} " +
                                                "(${com.omastro.app.BuildConfig.VERSION_CODE})\n\n" +
                                                "Describe the issue:\n"
                                        )
                                    }
                                    try {
                                        activity.startActivity(bugIntent)
                                    } catch (e: Exception) {
                                        // No email app — fall back silently; nothing fake shown.
                                    }
                                },
                                onLogout = { drawerShowDialog(DrawerDialog.LogoutConfirm) },
                                onComingSoon = { feature -> drawerShowDialog(DrawerDialog.ComingSoon(feature)) },
                                onShowDialog = ::drawerShowDialog
                            )
                        }
                    ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            DashboardScreen(
                                navController = navController,
                                onMenuClick = openDrawer
                            )
                        }
                        composable(Screen.Kundali.route) {
                            KundaliScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToSavedCharts = {
                                    navController.navigate(Screen.SavedCharts.route)
                                },
                                onNavigateToNumerology = {
                                    navController.navigate(Screen.Numerology.route)
                                },
                                onNavigateToAiChat = {
                                    navController.navigate(Screen.AiChat.route)
                                },
                                onMenuClick = openDrawer
                            )
                        }
                        composable(Screen.SavedCharts.route) {
                            SavedChartsScreen(
                                onBack = { navController.popBackStack() },
                                onChartSelected = { chart ->
                                    PendingChartHolder.set(chart)
                                    navController.navigate(Screen.Kundali.route) {
                                        popUpTo(Screen.SavedCharts.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Horoscope.route) {
                            HoroscopeScreen(
                                onBack = { navController.popBackStack() },
                                onMenuClick = openDrawer
                            )
                        }
                        composable(Screen.AiChat.route) {
                            com.omastro.app.ui.screens.aichat.AiChatScreen(
                                onBack = { navController.popBackStack() },
                                onMenuClick = openDrawer
                            )
                        }
                        composable(Screen.AiChat.HISTORY_ROUTE) {
                            com.omastro.app.ui.screens.aichat.AiChatScreen(
                                onBack = { navController.popBackStack() },
                                onMenuClick = openDrawer,
                                openHistoryOnStart = true
                            )
                        }
                        composable(Screen.DivineChat.route) {
                            com.omastro.app.ui.screens.aichat.DivineChatScreen(
                                onBack = { navController.popBackStack() },
                                onMenuClick = openDrawer
                            )
                        }
                        composable(Screen.Panchang.route) {
                            PanchangScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Muhurat.route) {
                            com.omastro.app.ui.screens.muhurat.MuhuratScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Matchmaking.route) {
                            MatchmakingScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Astrologers.route) {
                            AstrologersScreen(
                                onAstrologerClick = { id ->
                                    navController.navigate(Screen.Chat.createRoute(id))
                                },
                                onMenuClick = openDrawer
                            )
                        }
                        composable(
                            route = Screen.Chat.route,
                            arguments = listOf(navArgument("slug") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val slug = backStackEntry.arguments?.getString("slug") ?: ""
                            ChatScreen(
                                astrologerSlug = slug,
                                onBack = { navController.popBackStack() },
                                onNavigateToWallet = { navController.navigate(Screen.Wallet.route) }
                            )
                        }
                        composable(Screen.Wallet.route) {
                            WalletScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                                onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                                onNavigateToSavedCharts = { navController.navigate(Screen.SavedCharts.route) },
                                onMenuClick = openDrawer
                            )
                        }
                        composable(Screen.AstroHealth.route) {
                            com.omastro.app.ui.screens.astrohealth.AstroHealthScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Numerology.route) {
                            com.omastro.app.ui.screens.numerology.NumerologyScreen(
                                onBack = { navController.popBackStack() },
                                onToolSelected = { toolId ->
                                    navController.navigate(Screen.NumerologyTool.createRoute(toolId))
                                }
                            )
                        }
                        composable(
                            route = Screen.NumerologyTool.route,
                            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val toolId = backStackEntry.arguments?.getString("toolId") ?: "moolank"
                            com.omastro.app.ui.screens.numerology.NumerologyToolScreen(
                                toolId = toolId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Gochar.route) {
                            com.omastro.app.ui.screens.gochar.GocharScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Palmistry.route) {
                            com.omastro.app.ui.screens.palmistry.PalmistryScreen(
                                onBack = { navController.popBackStack() },
                                onOpenAiChat = {
                                    navController.navigate(Screen.AiChat.route)
                                },
                                onOpenHistory = {
                                    navController.navigate(Screen.ReadingHistory.route)
                                }
                            )
                        }
                        composable(Screen.FaceReading.route) {
                            com.omastro.app.ui.screens.facereading.FaceReadingScreen(
                                onBack = { navController.popBackStack() },
                                onOpenAiChat = {
                                    navController.navigate(Screen.AiChat.route)
                                },
                                onOpenHistory = {
                                    navController.navigate(Screen.ReadingHistory.route)
                                }
                            )
                        }
                        composable(Screen.Vastu.route) {
                            com.omastro.app.ui.screens.vastu.VastuScreen(
                                onBack = { navController.popBackStack() },
                                onOpenAiChat = {
                                    navController.navigate(Screen.AiChat.route)
                                },
                                onOpenHistory = {
                                    navController.navigate(Screen.ReadingHistory.route)
                                }
                            )
                        }
                        composable(Screen.ReadingHistory.route) {
                            com.omastro.app.ui.screens.history.ReadingHistoryScreen(
                                onBack = { navController.popBackStack() },
                                onOpenDetail = { id ->
                                    navController.navigate(
                                        Screen.ReadingHistoryDetail.createRoute(id)
                                    )
                                }
                            )
                        }
                        composable(
                            route = Screen.ReadingHistoryDetail.route,
                            arguments = listOf(navArgument("readingId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val readingId = backStackEntry.arguments?.getString("readingId") ?: ""
                            com.omastro.app.ui.screens.history.ReadingHistoryDetailScreen(
                                readingId = readingId,
                                onBack = { navController.popBackStack() },
                                onOpenAiChat = {
                                    navController.navigate(Screen.AiChat.route)
                                },
                                onNewAnalysis = { featureType ->
                                    val route = when (featureType) {
                                        com.omastro.app.data.local.ReadingFeatureType.VASTU -> Screen.Vastu.route
                                        com.omastro.app.data.local.ReadingFeatureType.FACE -> Screen.FaceReading.route
                                        else -> Screen.Palmistry.route
                                    }
                                    navController.navigate(route) {
                                        popUpTo(Screen.ReadingHistory.route) {
                                            inclusive = false
                                        }
                                    }
                                }
                            )
                        }
                        composable(Screen.Auth.route) {
                            AuthScreen(
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Auth.FORGOT_ROUTE) {
                            AuthScreen(
                                initialMode = com.omastro.app.ui.screens.auth.AuthMode.FORGOT_PASSWORD,
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Notifications.route) {
                            com.omastro.app.ui.screens.notifications.NotificationsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    } // ModalNavigationDrawer content
                } // Scaffold
                    // Drawer dialogs (coming-soon, privacy, terms, language, logout)
                    DrawerDialogHost(
                        dialog = drawerDialog,
                        onDismiss = { drawerDialog = null },
                        onLogoutConfirmed = {
                            drawerDialog = null
                            AppContainer.authRepository.logout()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                } // else -> block
                } // when (isLoggedIn)
            } // key(isDarkTheme)
            } // OmAstroTheme
        } // setContent
    } // onCreate
} // MainActivity