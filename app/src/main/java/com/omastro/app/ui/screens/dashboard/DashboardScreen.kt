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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.GradientButton
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.screens.home.HomeViewModel
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val coins by viewModel.coinBalance.collectAsState()
    val panchang by viewModel.panchang.collectAsState()
    val astrologers by viewModel.astrologers.collectAsState()
    val mantra by viewModel.dailyMantra.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDeepBlue)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (user != null) "Namaste, ${user?.name}" else "Namaste, Seeker",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vedic Wisdom for Modern Living",
                        color = MysticPurple,
                        fontSize = 13.sp
                    )
                }

                // Coin Balance Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CosmicCharcoal,
                    modifier = Modifier.clickable { navController.navigate(Screen.Wallet.route) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$coins Coins",
                            color = SacredGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Daily Panchang Quick Card
        item {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY'S PANCHANG",
                            color = SacredGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = panchang?.tithi ?: "—",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Nakshatra: ${panchang?.nakshatra ?: "—"} | Yog: ${panchang?.yoga ?: "—"}",
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Panchang.route) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View Panchang",
                            tint = SacredGold
                        )
                    }
                }
            }
        }

        // Quick Feature Shortcuts Grid
        item {
            Text(
                text = "Sacred Services",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "Kundali",
                    subtitle = "Birth Chart",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Kundali.route) }
                )
                QuickActionItem(
                    title = "Rashifal",
                    subtitle = "Horoscope",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Horoscope.route) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "Milan",
                    subtitle = "Matchmaking",
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Matchmaking.route) }
                )
                QuickActionItem(
                    title = "Consult",
                    subtitle = "Live Astrologer",
                    icon = Icons.Default.SupportAgent,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Astrologers.route) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "Astro Health",
                    subtitle = "D6 & Organ Vitality",
                    icon = Icons.Default.HealthAndSafety,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.AstroHealth.route) }
                )
                QuickActionItem(
                    title = "Numerology",
                    subtitle = "Mulank & Bhagyank",
                    icon = Icons.Default.Calculate,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Numerology.route) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    title = "Saved Charts",
                    subtitle = "My Kundalis",
                    icon = Icons.Default.Bookmarks,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.SavedCharts.route) }
                )
                QuickActionItem(
                    title = "Panchang",
                    subtitle = "Daily Almanac",
                    icon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Panchang.route) }
                )
            }
        }

        // Daily Mantra Widget
        item {
            CosmicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = MysticPurple.copy(alpha = 0.4f)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DAILY MAHAMANTRA",
                            color = RadiantAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mantra,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Verified Astrologers Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Vedic Astrologers",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All",
                    color = SacredGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { navController.navigate(Screen.Astrologers.route) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(astrologers) { astro ->
                    AstrologerMiniCard(
                        name = astro.name,
                        skills = astro.speciality.joinToString(", "),
                        rate = astro.pricePerMin,
                        rating = astro.rating,
                        onClick = { navController.navigate(Screen.Chat.createRoute(astro.id)) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CosmicNavy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SacredGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AstrologerMiniCard(
    name: String,
    skills: String,
    rate: Int,
    rating: Double,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CosmicNavy),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = SacredGold, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = skills,
                color = MysticPurple,
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹$rate/min",
                    color = RadiantAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "★ $rating",
                    color = SacredGold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
