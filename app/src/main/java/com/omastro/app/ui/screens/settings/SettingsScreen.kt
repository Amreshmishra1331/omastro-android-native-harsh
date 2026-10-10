package com.omastro.app.ui.screens.settings

import com.omastro.app.ui.i18n.W
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
    onNavigateToRefer: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepo = AppContainer.authRepository
    val user by authRepo.currentUser.collectAsState()
    val walletBalanceInr by AppContainer.walletRepository.walletBalanceInr.collectAsState()

    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var drawerDialog by remember { mutableStateOf<DrawerDialog?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun launchReview() {
        val activity = context as? Activity
        if (activity != null) {
            coroutineScope.launch {
                val reviewed = AppContainer.requestInAppReviewUseCase.launchReview(activity)
                if (reviewed) {
                    snackbarHostState.showSnackbar(W.t("settings_snack_rated"))
                } else {
                    snackbarHostState.showSnackbar(W.t("settings_snack_review"))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("settings_title"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
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
                                text = user?.name?.takeIf { it.isNotBlank() } ?: W.t("settings_seeker"),
                                color = StarlightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = user?.email?.takeIf { it.isNotBlank() }
                                    ?: user?.phone?.takeIf { it.isNotBlank() }
                                    ?: W.t("settings_signin_sub"),
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            if (user != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = W.tf("settings_wallet_fmt", String.format("%.2f", walletBalanceInr)),
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
                                Text(W.t("Logout"), fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = onNavigateToAuth,
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(W.t("Sign In"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        title = W.t("Wallet Balance & Add Money"),
                        subtitle = W.tf("settings_wallet_sub_fmt", String.format("%.2f", walletBalanceInr))
                    )
                }
            }

            // ============ ACCOUNT ============
            item {
                Text(W.t("settings_account_title"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteAccountDialog = true }
                ) {
                    // No account-deletion REST endpoint exists on the server (verified
                    // against the live wp-json/omastro/v1 index): deletion is completed
                    // on the website's /delete-account/ page, opened via browser from
                    // the confirm dialog below. The app never fakes deletion.
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = W.t("Delete Account"),
                        subtitle = W.t("delete_acct_sub"),
                        tint = Color(0xFFFF5252)
                    )
                }
            }

            // ============ MY ASTROLOGY ============
            item {
                Text(W.t("My Astrology"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSavedCharts() }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Bookmarks,
                        title = W.t("Saved Kundalis"),
                        subtitle = W.t("settings_saved_sub")
                    )
                }
            }

            // ============ REWARDS ============
            item {
                Text(W.t("Rewards"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRefer() }
                ) {
                    SettingsRow(
                        icon = Icons.Default.CardGiftcard,
                        title = W.t("Refer & Earn"),
                        subtitle = W.t("settings_refer_sub")
                    )
                }
            }

            // ============ PREFERENCES ============
            item {
                Text(W.t("Preferences"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { drawerDialog = DrawerDialog.Language }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = W.t("settings_lang_title"),
                        subtitle = W.t("settings_lang_sub")
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
                            Text(W.t("Appearance"), color = StarlightWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (isDarkTheme) W.t("settings_dark") else W.t("settings_light"),
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
                Text(W.t("settings_support"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            item {
                CosmicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchReview() }
                ) {
                    SettingsRow(
                        icon = Icons.Default.Star,
                        title = W.t("settings_rate_title"),
                        subtitle = W.t("settings_rate_sub")
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
                                putExtra(android.content.Intent.EXTRA_SUBJECT, W.t("settings_bug_mail_subject"))
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    W.t("settings_bug_mail_body")
                                )
                            }
                            try { context.startActivity(bugIntent) } catch (e: Exception) { /* no email app */ }
                        }
                ) {
                    SettingsRow(
                        icon = Icons.Default.BugReport,
                        title = W.t("settings_bug_title"),
                        subtitle = W.t("settings_bug_sub")
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
                        title = W.t("settings_privacy_title")
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
                        title = W.t("settings_terms_title")
                    )
                }
            }

            // ============ ABOUT ============
            item {
                Text(W.t("About"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            W.t("settings_about_line2"),
                            color = SacredGold.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Delete Account confirm dialog — honest: the app has no deletion REST
    // endpoint, so it opens the website's /delete-account/ page (HTTP 200,
    // verified live 2026-10-10). The app never fakes deletion.
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    W.t("Delete Account"),
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    W.t("delete_acct_body"),
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        val webIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.omastroai.com/delete-account/")
                        )
                        try { context.startActivity(webIntent) } catch (e: Exception) { /* no browser */ }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    )
                ) {
                    Text(W.t("delete_acct_open"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(W.t("Cancel"), color = SacredGold)
                }
            },
            containerColor = CosmicDeepBlue
        )
    }

    // Privacy & Astrological Disclaimers dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = {
                Text(
                    W.t("settings_disclaimer_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    W.t("settings_disclaimer_text"),
                    color = StarlightWhite.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text(W.t("Close"), color = SacredGold, fontWeight = FontWeight.Bold)
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
    subtitle: String? = null,
    tint: Color = SacredGold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    color = if (tint == SacredGold) StarlightWhite else tint,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(it, color = StarlightWhite.copy(alpha = 0.55f), fontSize = 12.sp)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SacredGold)
    }
}
