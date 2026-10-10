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
import com.omastro.app.data.model.DELETE_ACCOUNT_CONFIRM_PHRASE
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
    onAccountDeleted: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepo = AppContainer.authRepository
    val user by authRepo.currentUser.collectAsState()
    val walletBalanceInr by AppContainer.walletRepository.walletBalanceInr.collectAsState()

    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var deleteStage by remember { mutableStateOf(DeleteDialogStage.None) }
    var deleteTypedPhrase by remember { mutableStateOf("") }
    var deleteErrorMsg by remember { mutableStateOf<String?>(null) }
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
                        .clickable {
                            deleteTypedPhrase = ""
                            deleteErrorMsg = null
                            deleteStage = if (user != null) DeleteDialogStage.Step1
                            else DeleteDialogStage.FailedNoSession
                        }
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

    // ============ DELETE ACCOUNT FLOW (Cycle 7) ============
    // Step 1: warn about permanent data loss. Step 2: double-confirm by
    // typing DELETE. Then DELETE /me (Bearer, body confirm_phrase=DELETE);
    // on {"deleted":true} the server account is gone, so ALL local user data
    // is wiped and the user is logged out. If the endpoint call fails, the
    // website's /delete-account/ page (HTTP 200, verified live) is offered as
    // fallback — deletion is never faked.
    fun openDeleteAccountWebPage() {
        val webIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.omastroai.com/delete-account/")
        )
        try { context.startActivity(webIntent) } catch (e: Exception) { /* no browser */ }
    }

    if (deleteStage == DeleteDialogStage.Step1) {
        AlertDialog(
            onDismissRequest = { deleteStage = DeleteDialogStage.None },
            title = {
                Text(
                    W.t("Delete Account"),
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    W.t("delete_acct_step1_body"),
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { deleteStage = DeleteDialogStage.Step2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    )
                ) {
                    Text(W.t("delete_acct_step1_confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteStage = DeleteDialogStage.None }) {
                    Text(W.t("Cancel"), color = SacredGold)
                }
            },
            containerColor = CosmicDeepBlue
        )
    }

    if (deleteStage == DeleteDialogStage.Step2) {
        AlertDialog(
            onDismissRequest = { deleteStage = DeleteDialogStage.None },
            title = {
                Text(
                    W.t("delete_acct_step2_title"),
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        W.t("delete_acct_step2_body"),
                        color = StarlightWhite.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteTypedPhrase,
                        onValueChange = { deleteTypedPhrase = it },
                        label = { Text(W.t("delete_acct_type_hint")) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StarlightWhite,
                            unfocusedTextColor = StarlightWhite,
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFFFF5252),
                            unfocusedLabelColor = StarlightWhite.copy(alpha = 0.5f),
                            cursorColor = Color(0xFFFF5252)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteStage = DeleteDialogStage.Deleting
                        coroutineScope.launch {
                            AppContainer.deleteAccountEverywhere(context.applicationContext)
                                .onSuccess { onAccountDeleted() }
                                .onFailure { e ->
                                    deleteErrorMsg = e.message
                                    deleteStage = DeleteDialogStage.Failed
                                }
                        }
                    },
                    enabled = deleteTypedPhrase.trim() == DELETE_ACCOUNT_CONFIRM_PHRASE,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFFF5252).copy(alpha = 0.35f)
                    )
                ) {
                    Text(W.t("delete_acct_confirm_final"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteStage = DeleteDialogStage.None }) {
                    Text(W.t("Cancel"), color = SacredGold)
                }
            },
            containerColor = CosmicDeepBlue
        )
    }

    if (deleteStage == DeleteDialogStage.Deleting) {
        AlertDialog(
            onDismissRequest = { /* non-dismissible while the request is in flight */ },
            title = {
                Text(
                    W.t("Delete Account"),
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        W.t("delete_acct_deleting"),
                        color = StarlightWhite.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {},
            containerColor = CosmicDeepBlue
        )
    }

    // Endpoint failed (or no session): honest fallback — complete deletion on
    // the website's Delete Account page instead of faking it in-app.
    if (deleteStage == DeleteDialogStage.Failed || deleteStage == DeleteDialogStage.FailedNoSession) {
        AlertDialog(
            onDismissRequest = { deleteStage = DeleteDialogStage.None },
            title = {
                Text(
                    W.t("delete_acct_failed_title"),
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (deleteStage == DeleteDialogStage.FailedNoSession) {
                        W.t("delete_acct_nosession_body")
                    } else {
                        W.tf("delete_acct_failed_body", deleteErrorMsg ?: "")
                    },
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteStage = DeleteDialogStage.None
                        openDeleteAccountWebPage()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SacredGold,
                        contentColor = CosmicDeepBlue
                    )
                ) {
                    Text(W.t("delete_acct_open"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteStage = DeleteDialogStage.None }) {
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

/** Stages of the Settings → Delete Account flow (Cycle 7). */
enum class DeleteDialogStage {
    None,
    /** Step 1: warn about permanent data loss. */
    Step1,
    /** Step 2: double-confirm by typing DELETE. */
    Step2,
    /** DELETE /me in flight — non-dismissible. */
    Deleting,
    /** Endpoint failed: offer the website fallback. */
    Failed,
    /** No signed-in session: offer the website fallback directly. */
    FailedNoSession
}
