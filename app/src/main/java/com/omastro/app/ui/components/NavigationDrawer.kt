package com.omastro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.theme.*

/**
 * Dialog kinds surfaced from the navigation drawer (and reused by Settings).
 */
sealed interface DrawerDialog {
    data class ComingSoon(val feature: String) : DrawerDialog
    object Privacy : DrawerDialog
    object Terms : DrawerDialog
    object Language : DrawerDialog
    object LogoutConfirm : DrawerDialog
}

private const val PRIVACY_TEXT =
    "Astrological Disclaimer:\n\n" +
    "The astrological readings, predictions, and guidance provided by OmAstro AI are based on " +
    "traditional Vedic astrology principles and are intended for informational and entertainment " +
    "purposes only. They should not be considered as professional advice for legal, medical, " +
    "financial, or personal decisions.\n\n" +
    "Privacy:\n\n" +
    "Your birth details and personal information are used solely to generate astrological " +
    "calculations. We do not share your personal data with third parties without your consent. " +
    "Your authentication tokens are stored securely using encrypted storage on your device."

private const val TERMS_TEXT =
    "Terms of Service\n\n" +
    "The complete Terms of Service governing the use of OmAstro AI are published on our website " +
    "(omastroai.com). An in-app terms page is coming soon.\n\n" +
    "By using this app you agree to use astrological guidance responsibly and understand that " +
    "readings are for guidance and entertainment purposes. Paid consultations with astrologers " +
    "are billed per minute from your wallet balance as displayed before each session."

/**
 * Renders the AlertDialog for a [DrawerDialog]. Used by MainActivity (drawer) and SettingsScreen.
 */
@Composable
fun DrawerDialogHost(
    dialog: DrawerDialog?,
    onDismiss: () -> Unit,
    onLogoutConfirmed: () -> Unit = {}
) {
    when (dialog) {
        null -> Unit
        is DrawerDialog.ComingSoon -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(dialog.feature, color = StarlightWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This feature is coming soon to the app. Thank you for your patience.",
                        color = StarlightWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = SacredGold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CosmicNavy
            )
        }
        DrawerDialog.Privacy -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Privacy & Disclaimers", color = StarlightWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(PRIVACY_TEXT, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = SacredGold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CosmicNavy
            )
        }
        DrawerDialog.Terms -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Terms of Service", color = StarlightWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(TERMS_TEXT, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = SacredGold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CosmicNavy
            )
        }
        DrawerDialog.Language -> {
            val currentLang by com.omastro.app.data.local.LangPrefs.language.collectAsState()
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("App Language", color = StarlightWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { com.omastro.app.data.local.LangPrefs.setLanguage("en") }
                        ) {
                            RadioButton(
                                selected = currentLang == "en",
                                onClick = { com.omastro.app.data.local.LangPrefs.setLanguage("en") },
                                colors = RadioButtonDefaults.colors(selectedColor = SacredGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("English", color = StarlightWhite, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { com.omastro.app.data.local.LangPrefs.setLanguage("hi") }
                        ) {
                            RadioButton(
                                selected = currentLang == "hi",
                                onClick = { com.omastro.app.data.local.LangPrefs.setLanguage("hi") },
                                colors = RadioButtonDefaults.colors(selectedColor = SacredGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("हिन्दी", color = StarlightWhite, fontSize = 14.sp)
                                Text(
                                    "Menus & navigation only (partial)",
                                    color = SacredGold.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = SacredGold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CosmicNavy
            )
        }
        DrawerDialog.LogoutConfirm -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Log Out", color = StarlightWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to log out of OmAstro AI?",
                        color = StarlightWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = onLogoutConfirmed) {
                        Text("Log Out", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = StarlightWhite.copy(alpha = 0.7f))
                    }
                },
                containerColor = CosmicNavy
            )
        }
    }
}

private data class DrawerEntry(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val route: String? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * Rich left navigation drawer matching omastroai.com's sidebar:
 * profile header, grouped menu items, preferences, legal section and log out.
 */
@Composable
fun OmAstroDrawerContent(
    userName: String?,
    userEmail: String?,
    walletBalance: Int,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onWalletClick: () -> Unit,
    onRateApp: () -> Unit,
    onReportBug: () -> Unit,
    onLogout: () -> Unit,
    onComingSoon: (String) -> Unit,
    onShowDialog: (DrawerDialog) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = CosmicSurface,
        drawerContentColor = StarlightWhite,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // ============ PROFILE HEADER ============
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CosmicNavy, CosmicSurface)
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SacredGold, SacredGoldDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName?.firstOrNull()?.uppercase() ?: "O",
                            color = CosmicDeepBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName?.takeIf { it.isNotBlank() } ?: "Vedic Seeker",
                            color = StarlightWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail?.takeIf { it.isNotBlank() } ?: "Sign in for synced reports",
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CosmicCardBg,
                    modifier = Modifier.clickable { onWalletClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = SacredGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "₹$walletBalance",
                            color = SacredGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Money",
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = CosmicBorder, thickness = 1.dp)

            // ============ MENU ============
            DrawerSection("Menu")
            drawerMenuEntries(
                onNavigate = onNavigate,
                onComingSoon = onComingSoon,
                onRateApp = onRateApp,
                onReportBug = onReportBug,
                onShowDialog = onShowDialog
            ).forEach { (section, entries) ->
                if (section != "Menu") {
                    Spacer(modifier = Modifier.height(6.dp))
                    DrawerSection(section)
                }
                entries.forEach { entry ->
                    DrawerRow(
                        entry = entry,
                        selected = entry.route != null && currentRoute == entry.route
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CosmicBorder, thickness = 1.dp)

            // ============ LOG OUT ============
            NavigationDrawerItem(
                label = {
                    Text(
                        "Log Out",
                        color = Color(0xFFFF6E6E),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log out",
                        tint = Color(0xFFFF6E6E)
                    )
                },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title.uppercase(),
        color = SacredGold.copy(alpha = 0.8f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerRow(entry: DrawerEntry, selected: Boolean) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(
                    com.omastro.app.data.local.LangPrefs.label(entry.title),
                    color = if (selected) SacredGold else StarlightWhite,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                entry.subtitle?.let {
                    Text(it, color = StarlightWhite.copy(alpha = 0.55f), fontSize = 11.sp)
                }
            }
        },
        icon = {
            Icon(
                imageVector = entry.icon,
                contentDescription = entry.title,
                tint = if (selected) SacredGold else StarlightWhite.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
        },
        selected = selected,
        onClick = { entry.onClick?.invoke() },
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = CosmicCardBg,
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = SacredGold,
            unselectedIconColor = StarlightWhite.copy(alpha = 0.75f)
        )
    )
}

/**
 * Builds the grouped drawer entries. Navigation entries call [onNavigate] with the route;
 * non-navigating entries wire their real actions (review, dialogs, coming-soon).
 */
private fun drawerMenuEntries(
    onNavigate: (String) -> Unit,
    onComingSoon: (String) -> Unit,
    onRateApp: () -> Unit,
    onReportBug: () -> Unit,
    onShowDialog: (DrawerDialog) -> Unit
): List<Pair<String, List<DrawerEntry>>> {
    fun nav(title: String, icon: ImageVector, route: String, subtitle: String? = null) =
        DrawerEntry(title = title, subtitle = subtitle, icon = icon, route = route,
            onClick = { onNavigate(route) })

    return listOf(
        "Menu" to listOf(
            nav("Dashboard", Icons.Default.Home, Screen.Home.route),
            nav("AI Chat", Icons.Default.Chat, Screen.AiChat.route, "General · Mundane"),
            nav("Divine Chat", Icons.Default.Spa, Screen.DivineChat.route, "Spiritual guidance"),
            nav("Janam Kundali", Icons.Default.AutoAwesome, Screen.Kundali.route),
            nav("Rashifal", Icons.Default.WbSunny, Screen.Horoscope.route),
            nav("Panchang", Icons.Default.CalendarToday, Screen.Panchang.route),
            nav("Kundali Milan", Icons.Default.Favorite, Screen.Matchmaking.route),
            nav("Astrologers", Icons.Default.SupportAgent, Screen.Astrologers.route),
            nav("Numerology", Icons.Default.Calculate, Screen.Numerology.route),
            nav("Gochar", Icons.Default.Public, Screen.Gochar.route, "Planetary transits"),
            nav("Muhurat", Icons.Default.Schedule, Screen.Muhurat.route, "Auspicious timings")
        ),
        "Account" to listOf(
            nav("Add Money", Icons.Default.AccountBalanceWallet, Screen.Wallet.route),
            nav("Saved Kundalis", Icons.Default.Bookmarks, Screen.SavedCharts.route),
            DrawerEntry(
                title = "AI Chat History",
                subtitle = "Saved conversations",
                icon = Icons.Default.History,
                onClick = { onNavigate(Screen.AiChat.HISTORY_ROUTE) }
            ),
            nav("Activity History", Icons.Default.ReceiptLong, Screen.Wallet.route, "Transactions"),
            DrawerEntry(
                title = "Refer & Earn",
                subtitle = "Coming soon",
                icon = Icons.Default.CardGiftcard,
                onClick = { onComingSoon("Refer & Earn") }
            ),
            DrawerEntry(
                title = "Rate this App",
                icon = Icons.Default.Star,
                onClick = onRateApp
            ),
            nav("Settings", Icons.Default.Settings, Screen.Settings.route)
        ),
        "Preferences" to listOf(
            DrawerEntry(
                title = "Language",
                subtitle = "English · हिन्दी (partial)",
                icon = Icons.Default.Language,
                onClick = { onShowDialog(DrawerDialog.Language) }
            )
        ),
        "Legal" to listOf(
            DrawerEntry(
                title = "Privacy Policy",
                icon = Icons.Default.Security,
                onClick = { onShowDialog(DrawerDialog.Privacy) }
            ),
            DrawerEntry(
                title = "Terms of Service",
                icon = Icons.Default.Description,
                onClick = { onShowDialog(DrawerDialog.Terms) }
            ),
            DrawerEntry(
                title = "Report a Bug",
                subtitle = "Email support",
                icon = Icons.Default.BugReport,
                onClick = { onReportBug() }
            )
        )
    )
}
