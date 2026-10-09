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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.omastro.app.data.model.Astrologer
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.screens.home.HomeViewModel
import com.omastro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    onMenuClick: (() -> Unit)? = null
) {
    val user by viewModel.currentUser.collectAsState()
    val walletBalance by viewModel.coinBalance.collectAsState()
    val panchang by viewModel.panchang.collectAsState()
    val astrologers by viewModel.astrologers.collectAsState()
    val mantra by viewModel.dailyMantra.collectAsState()

    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDeepBlue)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ============ 1. HEADER ============
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (user != null) "Namaste, ${user?.name}" else "Namaste, Seeker",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = todayDate,
                        color = MysticPurple,
                        fontSize = 13.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CosmicCharcoal,
                    modifier = Modifier.clickable { navController.navigate(Screen.Wallet.route) }
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
                            text = "₹$walletBalance",
                            color = SacredGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ============ 2. TODAY'S ORACLE CARD ============
        item {
            CosmicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = SacredGold.copy(alpha = 0.5f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S ORACLE",
                            color = SacredGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "☉",
                            color = SacredGold.copy(alpha = 0.6f),
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hey, Seeker",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mantra,
                        color = StarlightWhite.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (panchang != null) {
                        Text(
                            text = "${panchang?.tithi ?: "—"} · ${panchang?.nakshatra ?: "—"} Nakshatra",
                            color = MysticPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ============ 3. ASK OM ASTRO AI SEARCH BAR ============
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = CosmicCharcoal,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    SacredGold.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.AiChat.route) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask AI",
                        tint = SacredGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ask Om Astro AI",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Kundali · Career · Marriage · Remedies — anything",
                            color = StarlightWhite.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go",
                        tint = SacredGold.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    text = "Career yog",
                    onClick = { navController.navigate(Screen.AiChat.route) }
                )
                SuggestionChip(
                    text = "Marriage timing",
                    onClick = { navController.navigate(Screen.AiChat.route) }
                )
                SuggestionChip(
                    text = "Today's remedy",
                    onClick = { navController.navigate(Screen.AiChat.route) }
                )
            }
        }

        // ============ 4. SERVICE GRID (8 items) ============
        item {
            SectionHeader(title = "Sacred Services")
            Spacer(modifier = Modifier.height(12.dp))
            ServiceGrid(navController = navController)
        }

        // ============ 5. CHAT WITH ASTROLOGERS CAROUSEL ============
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Chat with Astrologers")
                Text(
                    text = "View All",
                    color = SacredGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { navController.navigate(Screen.Astrologers.route) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (astrologers.isEmpty()) {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Loading astrologers...",
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(astrologers.take(10)) { astro ->
                        AstrologerCard(
                            astrologer = astro,
                            onClick = { navController.navigate(Screen.Chat.createRoute(astro.id)) }
                        )
                    }
                }
            }
        }

        // ============ 6. DIVINE CONNECTION ============
        item {
            SectionHeader(title = "Divine Connection")
            Text(
                text = "Seek blessings and guidance from the divine",
                color = StarlightWhite.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(divineDeities) { deity ->
                    DeityCard(
                        name = deity.first,
                        glyph = deity.second,
                        description = deity.third,
                        onClick = { navController.navigate(Screen.AiChat.route) }
                    )
                }
            }
        }

        // ============ 7. NUMEROLOGY SECTION ============
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Numerology")
                Text(
                    text = "Explore",
                    color = SacredGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { navController.navigate(Screen.Numerology.route) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            NumerologyGrid(
                onItemClick = { navController.navigate(Screen.Numerology.route) }
            )
        }

        // ============ 8. SPECIAL ASTROLOGY METHODS ============
        item {
            SectionHeader(title = "Special Astrology Methods")
            Text(
                text = "Advanced Vedic techniques for deeper insight",
                color = StarlightWhite.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpecialMethodCard(
                    title = "Bhrigu Nandi Nadi",
                    description = "Ancient Nadi astrology predictions based on planetary combinations",
                    glyph = "ॐ",
                    onClick = { navController.navigate(Screen.Kundali.route) }
                )
                SpecialMethodCard(
                    title = "Astro Health Analysis",
                    description = "D6 Shashtamsha chart and organ vitality assessment",
                    glyph = "☉",
                    onClick = { navController.navigate(Screen.AstroHealth.route) }
                )
                SpecialMethodCard(
                    title = "Saved Charts",
                    description = "Your previously calculated birth charts",
                    glyph = "♃",
                    onClick = { navController.navigate(Screen.SavedCharts.route) }
                )
            }
        }

        // ============ 9. DAILY PANCHANG QUICK CARD ============
        item {
            SectionHeader(title = "Today's Panchang")
            Spacer(modifier = Modifier.height(12.dp))
            CosmicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Panchang.route) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PanchangRow(label = "Tithi", value = panchang?.tithi ?: "—")
                        PanchangRow(label = "Nakshatra", value = panchang?.nakshatra ?: "—")
                        PanchangRow(label = "Yoga", value = panchang?.yoga ?: "—")
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Panchang",
                        tint = SacredGold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============ HELPER COMPOSABLES ============

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CosmicCharcoal,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MysticPurple.copy(alpha = 0.4f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = StarlightWhite.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PanchangRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            color = MysticPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}

// 8-service grid data: title, subtitle, icon, glyph, navigation action
private data class ServiceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val glyph: String,
    val route: Screen?
)

@Composable
private fun ServiceGrid(navController: NavController) {
    val services = listOf(
        ServiceItem("Kundali", "Birth Chart", Icons.Default.AutoAwesome, "☉", Screen.Kundali),
        ServiceItem("Matching", "Kundali Milan", Icons.Default.Favorite, "♀", Screen.Matchmaking),
        ServiceItem("Faladesh", "Predictions", Icons.Default.Psychology, "☽", Screen.Kundali),
        ServiceItem("Rashifal", "Horoscope", Icons.Default.WbSunny, "♈", Screen.Horoscope),
        ServiceItem("Yoga / Dosh", "Combinations", Icons.Default.Hub, "♂", Screen.Kundali),
        ServiceItem("Transits", "Gochar", Icons.Default.Public, "♄", Screen.Gochar),
        ServiceItem("Panchang", "Daily Almanac", Icons.Default.CalendarToday, "☀", Screen.Panchang),
        ServiceItem("Muhurat", "Auspicious Time", Icons.Default.Schedule, "♃", Screen.Muhurat)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        services.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { service ->
                    ServiceGridItem(
                        service = service,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            service.route?.let { navController.navigate(it.route) }
                        }
                    )
                }
                // Fill remaining space if row has fewer than 4 items
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServiceGridItem(
    service: ServiceItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SacredGold.copy(alpha = 0.25f),
                                CosmicNavy
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = service.glyph,
                    color = SacredGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = service.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = service.subtitle,
                color = StarlightWhite.copy(alpha = 0.55f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AstrologerCard(
    astrologer: Astrologer,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CosmicNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = SacredGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = astrologer.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "★",
                            color = SacredGold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = " ${astrologer.rating}",
                            color = SacredGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " · ${astrologer.experienceYears} yrs",
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = astrologer.speciality.take(2).joinToString(", "),
                color = MysticPurple,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${astrologer.pricePerMin}/min",
                    color = RadiantAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (astrologer.isOnline) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Online",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "Offline",
                        color = StarlightWhite.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = CosmicDeepBlue
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(
                    text = "Chat Now",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// Divine deities: name to glyph to description
private val divineDeities = listOf(
    Triple("Ganesha", "ॐ", "Remover of obstacles"),
    Triple("Shiva", "☉", "Destroyer & transformer"),
    Triple("Vishnu", "☽", "Preserver of dharma"),
    Triple("Lakshmi", "♀", "Goddess of wealth"),
    Triple("Saraswati", "♃", "Goddess of wisdom"),
    Triple("Hanuman", "♂", "Strength & devotion")
)

@Composable
private fun DeityCard(
    name: String,
    glyph: String,
    description: String,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        borderColor = MysticPurple.copy(alpha = 0.4f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MysticPurple.copy(alpha = 0.3f),
                                CosmicNavy
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = glyph,
                    color = RadiantAmber,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                color = StarlightWhite.copy(alpha = 0.55f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Numerology tools from website
private val numerologyTools = listOf(
    "Moolank" to "☉",
    "Lo Shu Grid" to "▦",
    "Name Numerology" to "✦",
    "Time Cycles" to "◐",
    "Karmic Debt" to "♄",
    "Compatibility" to "♀",
    "Gems" to "◆",
    "Lucky Calendar" to "☀",
    "Mobile Number" to "☎",
    "Name Correction" to "✎",
    "Kua Number" to "☷",
    "Planetary Numbers" to "♃"
)

@Composable
private fun NumerologyGrid(onItemClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        numerologyTools.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (name, glyph) ->
                    CosmicCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onItemClick() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = glyph,
                                color = SacredGold,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpecialMethodCard(
    title: String,
    description: String,
    glyph: String,
    onClick: () -> Unit
) {
    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicNavy),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = glyph,
                    color = SacredGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Open",
                tint = SacredGold.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
