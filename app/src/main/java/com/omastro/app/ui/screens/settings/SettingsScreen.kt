package com.omastro.app.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepo = AppContainer.authRepository
    val user by authRepo.currentUser.collectAsState()

    var dailyHoroscopeNotif by remember { mutableStateOf(true) }
    var panchangNotif by remember { mutableStateOf(true) }
    var whatsappUpdates by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English (en)") }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmicDeepBlue
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Account Status Card
            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = user?.name ?: "Vedic Seeker",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = user?.email ?: user?.phone ?: "Sign in for synced reports",
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        if (user != null) {
                            OutlinedButton(
                                onClick = { authRepo.logout() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Logout", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = onNavigateToAuth,
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Sign In", color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Wallet Shortcut
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWallet() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Recharge & Coin Balance", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SacredGold)
                    }
                }
            }

            // Appearance & Theme Mode
            item {
                Text("Appearance & Display", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Theme",
                                    tint = SacredGold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Theme Mode", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(if (isDarkMode) "Cosmic Deep Space (Dark)" else "Sacred Temple Gold (Light)", color = MysticPurple, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { isDarkMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CosmicDeepBlue,
                                    checkedTrackColor = SacredGold,
                                    uncheckedThumbColor = SacredGold,
                                    uncheckedTrackColor = CosmicNavy
                                )
                            )
                        }

                        Divider(color = CosmicNavy)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = "Language", tint = SacredGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Jyotish Language", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(selectedLanguage, color = MysticPurple, fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    selectedLanguage = if (selectedLanguage.startsWith("English")) "हिन्दी (Hindi)" else "English (en)"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicNavy),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Switch", color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Notification Preferences
            item {
                Text("Notifications & Alerts", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleRow("Daily Rashifal Alert (07:00 AM)", dailyHoroscopeNotif) { dailyHoroscopeNotif = it }
                        ToggleRow("Shubh Muhurat & Panchang", panchangNotif) { panchangNotif = it }
                        ToggleRow("WhatsApp Kundali & Consultation Alerts", whatsappUpdates) { whatsappUpdates = it }
                    }
                }
            }

            // App Actions & Store Review
            item {
                Text("Support & Feedback", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val activity = context as? Activity
                            if (activity != null) {
                                coroutineScope.launch {
                                    val reviewed = AppContainer.requestInAppReviewUseCase.launchReview(activity)
                                    if (reviewed) {
                                        snackbarHostState.showSnackbar("Thank you for rating OmAstro!")
                                    } else {
                                        snackbarHostState.showSnackbar("Play Store review flow initiated.")
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = SacredGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Rate Us on Google Play Store", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SacredGold)
                    }
                }
            }

            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SacredGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Privacy & Astrological Disclaimers", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SacredGold)
                    }
                }
            }

            // App Version Footer
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OmAstro AI v1.0.0 (Build 100)", color = StarlightWhite.copy(alpha = 0.5f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("100% Native Android • Vedic AI Engine", color = SacredGold.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CosmicDeepBlue,
                checkedTrackColor = SacredGold,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = CosmicCharcoal
            )
        )
    }
}
