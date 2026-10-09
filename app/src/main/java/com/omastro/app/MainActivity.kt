package com.omastro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.omastro.app.ui.navigation.BottomNavBar
import com.omastro.app.ui.navigation.PendingChartHolder
import com.omastro.app.ui.navigation.Screen
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmAstroTheme {
                val navController = rememberNavController()
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
                        val startDestination = if (isLoggedIn == true) {
                            Screen.Home.route
                        } else {
                            Screen.Auth.route
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
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            DashboardScreen(navController = navController)
                        }
                        composable(Screen.Kundali.route) {
                            KundaliScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToSavedCharts = {
                                    navController.navigate(Screen.SavedCharts.route)
                                }
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
                            HoroscopeScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.AiChat.route) {
                            com.omastro.app.ui.screens.aichat.AiChatScreen(
                                onBack = { navController.popBackStack() }
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
                                }
                            )
                        }
                        composable(
                            route = Screen.Chat.route,
                            arguments = listOf(navArgument("astrologerId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val astrologerId = backStackEntry.arguments?.getString("astrologerId") ?: "1"
                            ChatScreen(
                                astrologerId = astrologerId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Wallet.route) {
                            WalletScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                                onNavigateToWallet = { navController.navigate(Screen.Wallet.route) }
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
                        composable(Screen.Auth.route) {
                            AuthScreen(
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                } // Scaffold
                } // else -> block
                } // when (isLoggedIn)
            } // OmAstroTheme
        } // setContent
    } // onCreate
} // MainActivity