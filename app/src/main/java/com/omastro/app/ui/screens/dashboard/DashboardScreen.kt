package com.omastro.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.theme.*

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

    var selectedTab by remember { mutableStateOf(DashTab.HOME) }

    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: user?.name ?: "Seeker"
    val displayEmail = profile?.email ?: user?.email ?: ""
    val avatarUrl = profile?.avatar ?: ""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDeepBlue)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Header: $displayName", color = Color.White)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DashTab.values()) { tab ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedTab == tab) SacredGold else CosmicCharcoal,
                        modifier = Modifier.clickable { selectedTab = tab }
                    ) {
                        Text(
                            text = tab.title,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            DashTab.HOME -> {
                item {
                    Text(text = "Home tab", color = Color.White)
                }
            }
            DashTab.ACTIVITY -> {
                item {
                    Text(text = "Activity tab", color = Color.White)
                }
            }
            DashTab.PROFILE -> {
                item {
                    Text(text = "Profile tab", color = Color.White)
                }
            }
            DashTab.PASSWORD -> {
                item {
                    Text(text = "Password tab", color = Color.White)
                }
            }
            DashTab.PLAN -> {
                item {
                    Text(text = "Plan tab", color = Color.White)
                }
            }
            DashTab.BILLING -> {
                item {
                    Text(text = "Billing tab", color = Color.White)
                }
            }
            DashTab.ACCOUNT -> {
                item {
                    Text(text = "Account tab", color = Color.White)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

enum class DashTab(val title: String) {
    HOME("Home"),
    ACTIVITY("Activity"),
    PROFILE("Profile"),
    PASSWORD("Password"),
    PLAN("Plan"),
    BILLING("Billing"),
    ACCOUNT("Account")
}
