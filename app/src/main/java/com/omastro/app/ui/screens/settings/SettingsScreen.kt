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

    var showDisclaimerDialog by remember { mutableStateOf(false) }

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

            // Wallet Shortcut (INR terminology to match website)
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
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SacredGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Wallet Balance & Add Money", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SacredGold)
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

            // Privacy & Astrological Disclaimers (now opens a real dialog)
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDisclaimerDialog = true }
                ) {
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

    // Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = {
                Text(
                    "Privacy & Disclaimers",
                    color = Color.White,
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
            titleContentColor = Color.White
        )
    }
}
