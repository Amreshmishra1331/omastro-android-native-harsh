package com.omastro.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.omastro.app.ui.components.AstrologyHeader
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val panchang by viewModel.panchang.collectAsState()
    val astrologers by viewModel.astrologers.collectAsState()
    val mantra by viewModel.dailyMantra.collectAsState()

    Scaffold(
        topBar = {
            AstrologyHeader(
                title = "OmAstro AI",
                coinBalance = coinBalance,
                onWalletClick = { navController.navigate(Screen.Wallet.route) }
            )
        },
        containerColor = CosmicBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            CosmicCard(borderGold = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Namaste, ${user?.name ?: "Seeker"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = SacredGoldLight
                        )
                        Text(
                            text = "Planets guide, they do not compel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = { navController.navigate(Screen.Kundali.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("My Kundali", color = CosmicBackground, fontSize = 12.sp)
                    }
                }
            }

            // Quick Services Grid
            Text(
                text = "Sacred Services",
                style = MaterialTheme.typography.headlineSmall,
                color = SacredGold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Kundali D1/D9",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Kundali.route) }
                )
                QuickActionItem(
                    title = "Daily Rashifal",
                    icon = Icons.Default.WbSunny,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Horoscope.route) }
                )
                QuickActionItem(
                    title = "Panchang",
                    icon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Panchang.route) }
                )
                QuickActionItem(
                    title = "36 Guna Milan",
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Matchmaking.route) }
                )
            }

            // Today's Panchang Preview Card
            panchang?.let { p ->
                CosmicCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Panchang",
                            style = MaterialTheme.typography.titleMedium,
                            color = SacredGold
                        )
                        Text(
                            text = p.day,
                            fontSize = 12.sp,
                            color = SacredGoldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tithi", fontSize = 11.sp, color = TextSecondary)
                            Text(p.tithi, fontSize = 13.sp, color = TextPrimary)
                        }
                        Column {
                            Text("Nakshatra", fontSize = 11.sp, color = TextSecondary)
                            Text(p.nakshatra, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = CosmicBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Abhijit Muhurat", fontSize = 11.sp, color = AuspiciousGreen)
                            Text(p.abhijitMuhurat, fontSize = 12.sp, color = AuspiciousGreen)
                        }
                        Column {
                            Text("Rahu Kaal", fontSize = 11.sp, color = InauspiciousRed)
                            Text(p.rahuKaal, fontSize = 12.sp, color = InauspiciousRed)
                        }
                    }
                }
            }

            // Live Vedic Astrologers Carousel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Astrologers",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SacredGold
                )
                TextButton(onClick = { navController.navigate(Screen.Astrologers.route) }) {
                    Text("View All", color = SacredGoldLight, fontSize = 13.sp)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(astrologers) { astro ->
                    Surface(
                        modifier = Modifier
                            .width(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { navController.navigate(Screen.Chat.createRoute(astro.id)) },
                        color = CosmicSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SacredSaffron.copy(alpha = 0.2f))
                                    .border(1.dp, SacredGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = astro.name.first().toString(),
                                    color = SacredGold,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = astro.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = TextPrimary
                            )
                            Text(
                                text = "★ ${astro.rating}",
                                fontSize = 11.sp,
                                color = SacredGold
                            )
                            Text(
                                text = "₹${astro.pricePerMin}/min",
                                fontSize = 12.sp,
                                color = SacredGoldLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { navController.navigate(Screen.Chat.createRoute(astro.id)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Chat", color = CosmicBackground, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Daily Mantra / Shloka of the Day
            CosmicCard {
                Text(
                    text = "Mantra of the Day",
                    style = MaterialTheme.typography.titleMedium,
                    color = SacredGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = mantra,
                    fontSize = 14.sp,
                    color = SacredGoldLight,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Chanting Gayatri Mantra brings intellectual brilliance and peace.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = CosmicSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SacredGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
