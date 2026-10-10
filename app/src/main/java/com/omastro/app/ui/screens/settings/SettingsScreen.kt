package com.omastro.app.ui.screens.settings

import android.app.Activity
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
import com.omastro.app.BuildConfig
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.DrawerDialog
import com.omastro.app.ui.components.DrawerDialogHost
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToSavedCharts: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepo = AppContainer.authRepository
    val user by authRepo.currentUser.collectAsState()
    val walletBalanceInr by AppContainer.walletRepository.walletBalanceInr.collectAsState()

    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var drawerDialog by remember { mutableStateOf<DrawerDialog?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun launchReview() {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SacredGold)
                        }
                    }
                },
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

            // ============ PROFILE CARD ============
            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = user?.name?.takeIf { it.isNotBlank() } ?: "Vedic Seeker",
                                color = StarlightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = user?.email?.takeIf { it.isNotBlank() }
                                    ?: user?.phone?.takeIf { it.isNotBlank() }
                                    ?: "Sign in for synced reports",
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            if (user != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Wallet balance: ₹${String.format("%.2f", walletBalanceInr)}",
                                    color = SacredGold,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
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

            // ============ WALLET ============
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWallet() }
                ) {
                    SettingsRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Wallet Balance & Add Money",
                        subtitle = "Current balance: ₹${String.format("%.2f", walletBalanceInr)}"
                    )
                }
            }

            // ============ MY ASTROLOGY ============
            item {
                Text("My Astrology", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSavedCharts() }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Bookmarks,
                        title = "Saved Kundalis",
                        subtitle = "Your saved birth charts"
                    )
                }
            }

            // ============ REWARDS ============
            item {
                Text("Rewards", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { drawerDialog = DrawerDialog.ComingSoon("Refer & Earn") }
                ) {
                    SettingsRow(
                        icon = Icons.Default.CardGiftcard,
                        title = "Refer & Earn",
                        subtitle = "Invite friends, earn rewards"
                    )
                }
            }

            // ============ PREFERENCES ============
            item {
                Text("Preferences", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { drawerDialog = DrawerDialog.Language }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = "App Language",
                        subtitle = "English · हिन्दी"
                    )
                }
            }
            item {
                val isDarkTheme by com.omastro.app.data.local.ThemePrefs.isDarkTheme.collectAsState()
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Appearance", color = StarlightWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (isDarkTheme) "Dark — Cosmic Night" else "Light — Temple Gold",
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { com.omastro.app.data.local.ThemePrefs.setDarkTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SacredGold,
                                checkedTrackColor = SacredGold.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // ============ SUPPORT & FEEDBACK ============
            item {
                Text("Support & Feedback", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchReview() }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Star,
                        title = "Rate Us on Google Play Store",
                        subtitle = "Your feedback helps us grow"
                    )
                }
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val bugIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:support@omastroai.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "OmAstro App Bug Report")
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Describe the issue:\n"
                                )
                            }
                            try { context.startActivity(bugIntent) } catch (e: Exception) { /* no email app */ }
                        }
                ) {
                    SettingsRow(
                        icon = Icons.Default.BugReport,
                        title = "Report a Bug",
                        subtitle = "Email support"
                    )
                }
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDisclaimerDialog = true }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "Privacy & Astrological Disclaimers"
                    )
                }
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { drawerDialog = DrawerDialog.Terms }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Description,
                        title = "Terms of Service"
                    )
                }
            }

            // ============ ABOUT ============
            item {
                Text("About", color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "OmAstro AI v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            color = StarlightWhite.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "100% Native Android · Vedic AI Engine",
                            color = SacredGold.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Privacy & Astrological Disclaimers dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = {
                Text(
                    "Privacy & Disclaimers",
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Astrological Disclaimer:\n\n" +
                    "The astrological readings, predictions, and guidance provided by OmAstro AI are based on traditional Vedic astrology principles and are intended for informational and entertainment purposes only. They should not be considered as professional advice for legal, medical, financial, or personal decisions.\n\n" +
                    "Privacy:\n\n" +
                    "Your birth details and personal information are used solely to generate astrological calculations. We do not share your personal data with third parties without your consent. Your authentication tokens are stored securely using encrypted storage on your device.",
                    color = StarlightWhite.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("Close", color = SacredGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicNavy,
            titleContentColor = StarlightWhite
        )
    }

    // Shared drawer-style dialogs (coming soon, language, terms)
    DrawerDialogHost(
        dialog = drawerDialog,
        onDismiss = { drawerDialog = null }
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = SacredGold, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = StarlightWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(it, color = StarlightWhite.copy(alpha = 0.55f), fontSize = 12.sp)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SacredGold)
    }
}
