package com.omastro.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.omastro.app.data.model.SavedChatSession
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.data.remote.SavedChart
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard Screen — website parity (/dashboard/).
 *
 * Matches the website's user dashboard exactly:
 * - Header: avatar, name, email, wallet balance
 * - Tabs: Home, Activity, Profile, Password, Plan, Billing, Account
 * - Home: wallet hero, add money, quick actions, recent kundalis
 * - Activity: sub-tabs (Kundali, Chats, Wallet, Stats)
 * - Profile: user info
 * - Password: change password
 * - Plan: membership/wallet info
 * - Billing: wallet + transaction history
 * - Account: danger zone (logout, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
    onMenuClick: (() -> Unit)? = null
) {
    val profile by viewModel.profile.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val walletRepo = viewModel.walletRepository
    val totalInr by walletRepo.walletBalanceInr.collectAsState()
    val paidInr by walletRepo.paidBalanceInr.collectAsState()
    val bonusInr by walletRepo.bonusBalanceInr.collectAsState()
    val transactions by walletRepo.transactions.collectAsState()
    val savedCharts by viewModel.savedCharts.collectAsState()
    val aiChats by viewModel.aiChats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(DashTab.HOME) }
    var activitySubTab by remember { mutableStateOf(ActivitySubTab.KUNDALI) }

    val displayName = profile?.name?.takeIf { it.isNotBlank() }
        ?: profile?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.name?.takeIf { it.isNotBlank() }
        ?: "Seeker"
    val displayEmail = profile?.email ?: user?.email ?: ""
    val avatarUrl = profile?.avatar ?: ""
    val isPro = profile?.plan?.isPro == true
    val isMax = profile?.plan?.isMax == true
    val planName = profile?.plan?.name ?: "Free"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDeepBlue)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ============ HEADER ============
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onMenuClick != null) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = SacredGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CosmicNavy),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase(),
                            color = SacredGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayEmail,
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Wallet balance pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CosmicCharcoal,
                    modifier = Modifier.clickable { selectedTab = DashTab.BILLING }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = SacredGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹${String.format("%.2f", totalInr)}",
                            color = SacredGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ============ TAB NAVIGATION ============
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DashTab.values()) { tab ->
                    DashTabButton(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        isPro = isPro,
                        isMax = isMax,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }

        // ============ TAB CONTENT ============
        when (selectedTab) {
            DashTab.HOME -> {
                item {
                    HomeTabContent(
                        totalInr = totalInr,
                        paidInr = paidInr,
                        bonusInr = bonusInr,
                        savedCharts = savedCharts,
                        isLoading = isLoading,
                        onTabSelect = { selectedTab = it },
                        onKundaliClick = { chartId ->
                            navController.navigate(Screen.Kundali.route)
                        },
                        onAddMoney = {
                            navController.navigate(Screen.Wallet.route)
                        }
                    )
                }
            }
            DashTab.ACTIVITY -> {
                item {
                    ActivityTabContent(
                        subTab = activitySubTab,
                        onSubTabSelect = { activitySubTab = it },
                        savedCharts = savedCharts,
                        aiChats = aiChats,
                        transactions = transactions,
                        totalInr = totalInr,
                        onDeleteChat = { viewModel.deleteAiChat(it) },
                        onClearAllChats = { viewModel.clearAllAiChats() },
                        onDeleteChart = { viewModel.deleteSavedChart(it) },
                        onKundaliClick = { navController.navigate(Screen.Kundali.route) },
                        onChatClick = { navController.navigate(Screen.AiChat.route) },
                        onAddMoney = { navController.navigate(Screen.Wallet.route) }
                    )
                }
            }
            DashTab.PROFILE -> {
                item {
                    ProfileTabContent(
                        displayName = displayName,
                        username = profile?.username ?: "",
                        email = displayEmail,
                        avatarUrl = avatarUrl
                    )
                }
            }
            DashTab.PASSWORD -> {
                item {
                    PasswordTabContent(
                        currentEmail = displayEmail,
                        navController = navController
                    )
                }
            }
            DashTab.PLAN -> {
                item {
                    PlanTabContent(
                        planName = planName,
                        isPro = isPro,
                        isMax = isMax,
                        onAddMoney = { navController.navigate(Screen.Wallet.route) }
                    )
                }
            }
            DashTab.BILLING -> {
                item {
                    BillingTabContent(
                        totalInr = totalInr,
                        paidInr = paidInr,
                        bonusInr = bonusInr,
                        transactions = transactions,
                        onAddMoney = { navController.navigate(Screen.Wallet.route) }
                    )
                }
            }
            DashTab.ACCOUNT -> {
                item {
                    AccountTabContent(
                        navController = navController
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============ TAB DEFINITIONS ============

enum class DashTab(val title: String) {
    HOME("Home"),
    ACTIVITY("Activity"),
    PROFILE("Profile"),
    PASSWORD("Password"),
    PLAN("Plan"),
    BILLING("Billing"),
    ACCOUNT("Account")
}

enum class ActivitySubTab(val title: String) {
    KUNDALI("Kundali"),
    CHATS("Chats"),
    WALLET("Wallet"),
    STATS("Stats")
}

@Composable
private fun DashTabButton(
    tab: DashTab,
    isSelected: Boolean,
    isPro: Boolean,
    isMax: Boolean,
    onClick: () -> Unit
) {
    val planIcon = when {
        tab == DashTab.PLAN && isMax -> "◆"
        tab == DashTab.PLAN && isPro -> "♛"
        else -> ""
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) SacredGold else CosmicCharcoal,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = if (planIcon.isNotEmpty()) "$planIcon ${tab.title}" else tab.title,
            color = if (isSelected) CosmicDeepBlue else StarlightWhite.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// ============ HOME TAB ============

@Composable
private fun HomeTabContent(
    totalInr: Double,
    paidInr: Double,
    bonusInr: Double,
    savedCharts: List<SavedChart>,
    isLoading: Boolean,
    onTabSelect: (DashTab) -> Unit,
    onKundaliClick: (String) -> Unit,
    onAddMoney: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Wallet hero card (orange gradient like website)
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B00),
                                Color(0xFFFF8C00)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "MY WALLET",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%.2f", totalInr)}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Paid: ₹${String.format("%.2f", paidInr)} · Bonus: ₹${String.format("%.2f", bonusInr)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Add Money card
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wallet",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add money to wallet to start chatting",
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onAddMoney,
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                ) {
                    Text(
                        text = "Add Money",
                        color = CosmicDeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Quick actions grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                title = "Activity",
                modifier = Modifier.weight(1f),
                onClick = { onTabSelect(DashTab.ACTIVITY) }
            )
            QuickActionButton(
                title = "Wallet",
                modifier = Modifier.weight(1f),
                onClick = { onTabSelect(DashTab.BILLING) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                title = "Edit Profile",
                modifier = Modifier.weight(1f),
                onClick = { onTabSelect(DashTab.PROFILE) }
            )
            QuickActionButton(
                title = "Change Password",
                modifier = Modifier.weight(1f),
                onClick = { onTabSelect(DashTab.PASSWORD) }
            )
        }

        // Recent Kundalis
        Text(
            text = "Recent Kundalis",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (isLoading) {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Loading...",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (savedCharts.isEmpty()) {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No kundalis yet",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            savedCharts.take(5).forEach { chart ->
                SavedChartRow(
                    chart = chart,
                    onClick = { onKundaliClick(chart.id) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SavedChartRow(
    chart: SavedChart,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chart.name.ifBlank { "Kundali" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chart.birthDate,
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Open",
                tint = SacredGold
            )
        }
    }
}

// ============ ACTIVITY TAB ============

@Composable
private fun ActivityTabContent(
    subTab: ActivitySubTab,
    onSubTabSelect: (ActivitySubTab) -> Unit,
    savedCharts: List<SavedChart>,
    aiChats: List<SavedChatSession>,
    transactions: List<WalletTransaction>,
    totalInr: Double,
    onDeleteChat: (String) -> Unit,
    onClearAllChats: () -> Unit,
    onDeleteChart: (String) -> Unit,
    onKundaliClick: () -> Unit,
    onChatClick: () -> Unit,
    onAddMoney: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Sub-tab navigation
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ActivitySubTab.values()) { tab ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (subTab == tab) SacredGold else CosmicCharcoal,
                    modifier = Modifier.clickable { onSubTabSelect(tab) }
                ) {
                    Text(
                        text = tab.title,
                        color = if (subTab == tab) CosmicDeepBlue else StarlightWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = if (subTab == tab) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        when (subTab) {
            ActivitySubTab.KUNDALI -> {
                Text(
                    text = "${savedCharts.size} Kundalis",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (savedCharts.isEmpty()) {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No kundalis yet",
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onKundaliClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                            ) {
                                Text("Make Kundali", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    savedCharts.forEach { chart ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SavedChartRow(
                                chart = chart,
                                onClick = onKundaliClick
                            )
                        }
                    }
                }
            }
            ActivitySubTab.CHATS -> {
                Text(
                    text = "${aiChats.size} Chats",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (aiChats.isEmpty()) {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No saved chats",
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onChatClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                            ) {
                                Text("Start Chat", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    aiChats.forEach { chat ->
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chat.displayTitle,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${chat.mode ?: "general"} · ${chat.updatedAt ?: ""}",
                                        color = StarlightWhite.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { onDeleteChat(chat.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onClearAllChats,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                    ) {
                        Text("Clear All Chats")
                    }
                }
            }
            ActivitySubTab.WALLET -> {
                // Wallet balance hero
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF6B00), Color(0xFFFF8C00))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Balance",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "₹${String.format("%.2f", totalInr)}",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Button(
                                onClick = onAddMoney,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Text("Add Money", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                // Transactions
                if (transactions.isEmpty()) {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No transactions yet",
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    transactions.take(20).forEach { txn ->
                        TransactionRow(transaction = txn)
                    }
                }
            }
            ActivitySubTab.STATS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        value = "${savedCharts.size}",
                        label = "Kundalis",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${aiChats.size}",
                        label = "Total Chats",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        value = "₹${String.format("%.0f", totalInr)}",
                        label = "Wallet Balance",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${transactions.size}",
                        label = "Transactions",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    CosmicCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                color = SacredGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = StarlightWhite.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: WalletTransaction) {
    val isCredit = transaction.type.equals("credit", ignoreCase = true)
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCredit) Color(0xFF2E7D32).copy(alpha = 0.15f)
                        else Color(0xFFC62828).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isCredit) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { transaction.type },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.date,
                    color = StarlightWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Text(
                text = "₹${String.format("%.2f", transaction.amount / 100.0)}",
                color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFE53935),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============ PROFILE TAB ============

@Composable
private fun ProfileTabContent(
    displayName: String,
    username: String,
    email: String,
    avatarUrl: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CosmicNavy),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = displayName.take(1).uppercase(),
                    color = SacredGold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        ProfileField(label = "Full Name", value = displayName)
        ProfileField(label = "Username", value = username.ifBlank { "—" })
        ProfileField(label = "Email", value = email.ifBlank { "—" })
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = StarlightWhite.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============ PASSWORD TAB ============

@Composable
private fun PasswordTabContent(
    currentEmail: String,
    navController: NavController
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current Email",
                    color = Color(0xFF1E88E5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentEmail.ifBlank { "—" },
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current Password", color = StarlightWhite.copy(alpha = 0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SacredGold,
                unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f)
            )
        )

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password (8+ chars)", color = StarlightWhite.copy(alpha = 0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SacredGold,
                unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f)
            )
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm New Password", color = StarlightWhite.copy(alpha = 0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SacredGold,
                unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f)
            )
        )

        Button(
            onClick = { /* Password change via website - no REST endpoint */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
        ) {
            Text("Change Password", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "Forgot password? Use reset link",
            color = StarlightWhite.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Screen.Auth.route) }
        )
    }
}

// ============ PLAN TAB ============

@Composable
private fun PlanTabContent(
    planName: String,
    isPro: Boolean,
    isMax: Boolean,
    onAddMoney: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        isMax -> "◆"
                        isPro -> "♛"
                        else -> "★"
                    },
                    color = SacredGold,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 14.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = planName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add money to wallet to start chatting",
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddMoney,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
            ) {
                Text("Add Money", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============ BILLING TAB ============

@Composable
private fun BillingTabContent(
    totalInr: Double,
    paidInr: Double,
    bonusInr: Double,
    transactions: List<WalletTransaction>,
    onAddMoney: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Wallet hero
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B00), Color(0xFFFF8C00))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "MY WALLET",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format("%.2f", totalInr)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Total Balance",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paid: ₹${String.format("%.2f", paidInr)} · Bonus: ₹${String.format("%.2f", bonusInr)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAddMoney,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text("Add Money", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Transaction history
        Text(
            text = "Transaction History",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        if (transactions.isEmpty()) {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No transactions yet",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            transactions.forEach { txn ->
                TransactionRow(transaction = txn)
            }
        }
    }
}

// ============ ACCOUNT TAB ============

@Composable
private fun AccountTabContent(
    navController: NavController
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Danger zone
        CosmicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Danger Zone",
                    color = Color(0xFFE53935),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Deleting your account will permanently remove all your data, kundalis, and chat history. This cannot be undone.",
                    color = StarlightWhite.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Logout
        CosmicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Navigate to auth (logout handled by AuthRepository)
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Settings
        CosmicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Screen.Settings.route) }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SacredGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
