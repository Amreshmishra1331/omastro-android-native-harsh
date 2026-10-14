package com.omastro.app.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.omastro.app.data.local.ReadingFeatureType
import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.repository.ReadingHistoryRepository
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.components.*
import com.omastro.app.ui.navigation.PendingChartHolder
import com.omastro.app.ui.navigation.Screen
import com.omastro.app.ui.theme.*
import com.omastro.app.ui.theme.CosmicBackground as CosmicBgColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Advanced Dashboard (Home) Screen for OmAstro AI.
 *
 * Implements modern Cosmic Vedic aesthetic with complete production sections:
 * 1. CosmicTopBar with greeting, avatar & notification badge
 * 2. Today's Cosmic Energy Card (AnimatedGlassCard) with Moon phase, Nakshatra, Panchang, Graha transits
 * 3. Quick Actions Grid (2x4 GlassCard grid)
 * 4. Today's Horoscope Strip (LazyRow of 12 signs + selected preview)
 * 5. AI Chat Promo Card (PremiumCard with gradient CTA)
 * 6. Live Astrologers Strip (LazyRow with online badges & ratings)
 * 7. Premium Features Section (Palmistry, Face Reading, Vastu)
 * 8. Reading History (recent analyses from local DB with fallback)
 */
@Composable
fun DashboardScreen(
    onNavigateToKundali: () -> Unit,
    onNavigateToHoroscope: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToMatching: () -> Unit,
    onNavigateToPanchang: () -> Unit,
    onNavigateToNumerology: () -> Unit,
    onNavigateToVastu: () -> Unit,
    onNavigateToAstrologers: () -> Unit,
    onNavigateToPalmistry: () -> Unit,
    onNavigateToFaceReading: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val totalInr by viewModel.walletRepository.walletBalanceInr.collectAsState()
    val savedCharts by viewModel.savedCharts.collectAsState()
    val aiChats by viewModel.aiChats.collectAsState()
    val liveAstrologers by viewModel.astrologers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Notification bell badge
    val notificationRepo = AppContainer.notificationRepository
    val unreadCount by notificationRepo.unreadCount.collectAsState()
    LaunchedEffect(Unit) {
        try { notificationRepo.refresh() } catch (_: Exception) {}
    }

    // Local reading history repository
    val historyRepo = remember(context) { ReadingHistoryRepository(context) }
    var localReadings by remember { mutableStateOf<List<ReadingHistoryRepository.SavedReading>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            localReadings = historyRepo.list().take(3)
        } catch (_: Exception) {}
    }

    val displayName = profile?.name?.takeIf { it.isNotBlank() }
        ?: profile?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.name?.takeIf { it.isNotBlank() }
        ?: "Seeker"
    val avatarUrl = profile?.avatar.orEmpty()

    // Horoscope strip selection state (0 = Aries default)
    var selectedZodiacIndex by remember { mutableIntStateOf(0) }

    SimpleCosmicBg(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. CosmicTopBar
            CosmicTopBar(
                title = "OmAstro AI",
                userName = displayName,
                userAvatar = avatarUrl.ifBlank { null },
                notificationCount = unreadCount,
                onAvatarClick = onNavigateToProfile,
                onNotificationClick = onNavigateToNotifications,
                showLogo = true,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CosmicCardBg.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onNavigateToWallet() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                text = "₹${String.format(Locale.getDefault(), "%.0f", totalInr)}",
                                color = SacredGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            )

            // Main scrollable content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Async shimmer placeholder if initial load
                if (isLoading && savedCharts.isEmpty() && profile == null) {
                    item {
                        ShimmerCard(modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        ShimmerListItem(modifier = Modifier.fillMaxWidth())
                    }
                }

                // 2. Today's Cosmic Energy Card
                item {
                    CosmicEnergyCard(
                        onExplorePanchang = onNavigateToPanchang
                    )
                }

                // 3. Quick Actions Grid (2x4 grid)
                item {
                    QuickActionsSection(
                        onNavigateToKundali = onNavigateToKundali,
                        onNavigateToHoroscope = onNavigateToHoroscope,
                        onNavigateToAiChat = onNavigateToAiChat,
                        onNavigateToMatching = onNavigateToMatching,
                        onNavigateToPanchang = onNavigateToPanchang,
                        onNavigateToNumerology = onNavigateToNumerology,
                        onNavigateToVastu = onNavigateToVastu,
                        onNavigateToAstrologers = onNavigateToAstrologers
                    )
                }

                // 4. Today's Horoscope Strip
                item {
                    HoroscopeStripSection(
                        selectedIndex = selectedZodiacIndex,
                        onSelectZodiac = { selectedZodiacIndex = it },
                        onViewFullHoroscope = onNavigateToHoroscope
                    )
                }

                // 5. AI Chat Promo Card
                item {
                    AiChatPromoCard(
                        onChatNow = onNavigateToAiChat
                    )
                }

                // 6. Live Astrologers Strip
                item {
                    LiveAstrologersSection(
                        astrologers = liveAstrologers,
                        onAstrologerClick = onNavigateToAstrologers,
                        onViewAll = onNavigateToAstrologers
                    )
                }

                // 7. Premium Features Section (Palmistry, Face Reading, Vastu)
                item {
                    PremiumFeaturesSection(
                        onNavigateToPalmistry = onNavigateToPalmistry,
                        onNavigateToFaceReading = onNavigateToFaceReading,
                        onNavigateToVastu = onNavigateToVastu
                    )
                }

                // 8. Reading History (last 2-3 entries from local DB)
                item {
                    ReadingHistorySection(
                        localReadings = localReadings,
                        savedCharts = savedCharts,
                        onKundaliClick = { chartId ->
                            savedCharts.find { it.id == chartId }?.let {
                                PendingChartHolder.set(it)
                            }
                            onNavigateToKundali()
                        },
                        onReadingClick = { readingType ->
                            when (readingType) {
                                ReadingFeatureType.PALMISTRY -> onNavigateToPalmistry()
                                ReadingFeatureType.FACE -> onNavigateToFaceReading()
                                ReadingFeatureType.VASTU -> onNavigateToVastu()
                                else -> onNavigateToKundali()
                            }
                        },
                        onStartNewReading = onNavigateToKundali
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatible overload for [DashboardScreen] matching existing NavHost call sites.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
    onMenuClick: (() -> Unit)? = null
) {
    DashboardScreen(
        onNavigateToKundali = { navController.navigate(Screen.Kundali.route) },
        onNavigateToHoroscope = { navController.navigate(Screen.Horoscope.route) },
        onNavigateToAiChat = { navController.navigate(Screen.AiChat.route) { launchSingleTop = true } },
        onNavigateToMatching = { navController.navigate(Screen.Matchmaking.route) },
        onNavigateToPanchang = { navController.navigate(Screen.Panchang.route) },
        onNavigateToNumerology = { navController.navigate(Screen.Numerology.route) },
        onNavigateToVastu = { navController.navigate(Screen.Vastu.route) },
        onNavigateToAstrologers = { navController.navigate(Screen.Astrologers.route) },
        onNavigateToPalmistry = { navController.navigate(Screen.Palmistry.route) },
        onNavigateToFaceReading = { navController.navigate(Screen.FaceReading.route) },
        onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
        onNavigateToProfile = { onMenuClick?.invoke() ?: navController.navigate(Screen.Settings.route) },
        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
        viewModel = viewModel
    )
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 2: TODAY'S COSMIC ENERGY CARD
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun CosmicEnergyCard(
    onExplorePanchang: () -> Unit
) {
    AnimatedGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExplorePanchang),
        cornerRadius = 18.dp
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SacredGoldSubtle)
                        .border(1.dp, SacredGold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ॐ", color = SacredGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    GoldText(text = "आज का ब्रह्मांडीय ऊर्जा", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Cosmic Alignment & Daily Panchang",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SacredGold.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "शुभ दिन",
                    color = AuspiciousGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Moon Phase + Nakshatra Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Moon phase box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicElevated.copy(alpha = 0.8f))
                    .border(1.dp, CosmicCardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌔", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "चंद्र कला (Moon)", color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = "शुक्ल अष्टमी",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(text = "Waxing 58%", color = TextGold, fontSize = 10.sp)
                    }
                }
            }

            // Nakshatra box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicElevated.copy(alpha = 0.8f))
                    .border(1.dp, CosmicCardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌟", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "नक्षत्र (Nakshatra)", color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = "रोहिणी",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(text = "चंद्र देव स्वामी", color = CosmicVioletLight, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Panchang snippet pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PanchangSnippetPill(label = "तिथि", value = "शुक्ल अष्टमी", icon = "🕉️")
            PanchangSnippetPill(label = "वार", value = "बुधवार", icon = "⏳")
            PanchangSnippetPill(label = "योग", value = "सिद्धि योग", icon = "🌀")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Graha Transits Strip
        Text(
            text = "ग्रह गोचर (Planetary Transits)",
            color = SacredGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlanetTransitChip(name = "सूर्य", planetColor = SunColor, sign = "मेष 12°", icon = "☀️")
            PlanetTransitChip(name = "चंद्र", planetColor = MoonColor, sign = "वृषभ 08° (उच्च)", icon = "🌙")
            PlanetTransitChip(name = "मंगल", planetColor = MarsColor, sign = "मिथुन 24°", icon = "♂️")
            PlanetTransitChip(name = "बुध", planetColor = MercuryColor, sign = "मीन 16°", icon = "☿")
            PlanetTransitChip(name = "गुरु", planetColor = JupiterColor, sign = "वृषभ 21°", icon = "♃")
            PlanetTransitChip(name = "शुक्र", planetColor = VenusColor, sign = "मीन 28° (उच्च)", icon = "♀️")
            PlanetTransitChip(name = "शनि", planetColor = SaturnColor, sign = "कुंभ 18°", icon = "♄")
        }
    }
}

@Composable
private fun PanchangSnippetPill(
    label: String,
    value: String,
    icon: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CosmicCardBg.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(text = label, color = TextSecondary, fontSize = 9.sp)
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PlanetTransitChip(
    name: String,
    planetColor: Color,
    sign: String,
    icon: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CosmicElevated.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, planetColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$name: ",
                color = planetColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = sign,
                color = TextPrimary,
                fontSize = 11.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 3: QUICK ACTIONS GRID (2x4 Grid)
// ══════════════════════════════════════════════════════════════════════

private data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsSection(
    onNavigateToKundali: () -> Unit,
    onNavigateToHoroscope: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToMatching: () -> Unit,
    onNavigateToPanchang: () -> Unit,
    onNavigateToNumerology: () -> Unit,
    onNavigateToVastu: () -> Unit,
    onNavigateToAstrologers: () -> Unit
) {
    val actions = remember {
        listOf(
            QuickActionItem("Kundali", "जन्म कुंडली", "⭐", onNavigateToKundali),
            QuickActionItem("Horoscope", "दैनिक राशिफल", "☀️", onNavigateToHoroscope),
            QuickActionItem("AI Chat", "AI ज्योतिषी", "🤖", onNavigateToAiChat),
            QuickActionItem("Matching", "गुण मिलान", "💑", onNavigateToMatching),
            QuickActionItem("Panchang", "दैनिक पंचांग", "📅", onNavigateToPanchang),
            QuickActionItem("Numerology", "अंक ज्योतिष", "🔢", onNavigateToNumerology),
            QuickActionItem("Vastu", "गृह वास्तु", "🏠", onNavigateToVastu),
            QuickActionItem("Astrologers", "लाइव ज्योतिषी", "👨‍💼", onNavigateToAstrologers)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "त्वरित सेवाएं (Quick Services)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "वैदिक साधन",
                color = TextGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 2x4 grid layout (4 rows of 2 columns)
        for (i in actions.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    item = actions[i],
                    modifier = Modifier.weight(1f)
                )
                if (i + 1 < actions.size) {
                    QuickActionCard(
                        item = actions[i + 1],
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    item: QuickActionItem,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.clickable(onClick = item.onClick),
        cornerRadius = 14.dp,
        glowColor = SacredGold,
        glowAlpha = 0.25f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SacredGoldSubtle)
                    .border(1.dp, SacredGold.copy(alpha = 0.5f), CircleShape)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = SacredGoldGlow.copy(alpha = 0.3f),
                        spotColor = SacredGold.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 4: TODAY'S HOROSCOPE STRIP (LazyRow)
// ══════════════════════════════════════════════════════════════════════

private data class ZodiacCardData(
    val emoji: String,
    val hindi: String,
    val english: String,
    val elementColor: Color,
    val luckyNumber: Int,
    val luckyColor: String,
    val auspiciousTime: String,
    val summary: String
)

private val ZODIAC_SIGNS = listOf(
    ZodiacCardData("♈", "मेष", "Aries", FireElement, 9, "लाल (Red)", "10:30 AM - 12:00 PM", "कार्यक्षेत्र में नए अवसर और सकारात्मक ऊर्जा रहेगी। वित्तीय लाभ के योग हैं।"),
    ZodiacCardData("♉", "वृषभ", "Taurus", EarthElement, 6, "सफेद (White)", "02:15 PM - 03:45 PM", "पारिवारिक जीवन में सुख-शांति रहेगी। रुके हुए कार्य पूरे होंगे।"),
    ZodiacCardData("♊", "मिथुन", "Gemini", AirElement, 5, "हरा (Green)", "09:00 AM - 10:30 AM", "संवाद कौशल से लाभ होगा। व्यापार में नई योजनाएं सफल रहेंगी।"),
    ZodiacCardData("♋", "कर्क", "Cancer", WaterElement, 2, "चांदी (Silver)", "04:00 PM - 05:30 PM", "मानसिक शांति बनी रहेगी। भावनात्मक निर्णय सोच-समझकर लें।"),
    ZodiacCardData("♌", "सिंह", "Leo", FireElement, 1, "सुनहरा (Gold)", "11:45 AM - 01:15 PM", "आत्मविश्वास में वृद्धि होगी। नेतृत्व क्षमता की सराहना होगी।"),
    ZodiacCardData("♍", "कन्या", "Virgo", EarthElement, 7, "गहरा हरा (Green)", "08:15 AM - 09:45 AM", "योजनाबद्ध तरीके से काम करें। स्वास्थ्य के प्रति सचेत रहें।"),
    ZodiacCardData("♎", "तुला", "Libra", AirElement, 6, "गुलाबी (Pink)", "03:30 PM - 05:00 PM", "रिश्तों में मधुरता आएगी। साझेदारी से लाभ प्राप्त हो सकता है।"),
    ZodiacCardData("♏", "वृश्चिक", "Scorpio", WaterElement, 9, "केसरिया (Saffron)", "01:00 PM - 02:30 PM", "साहस और पराक्रम में वृद्धि होगी। गुप्त शत्रुओं से सावधान रहें।"),
    ZodiacCardData("♐", "धनु", "Sagittarius", FireElement, 3, "पीला (Yellow)", "07:30 AM - 09:00 AM", "भाग्य का पूरा साथ मिलेगा। आध्यात्मिक कार्यों में रुचि बढ़ेगी।"),
    ZodiacCardData("♑", "मकर", "Capricorn", EarthElement, 8, "नीला (Blue)", "12:30 PM - 02:00 PM", "कड़ी मेहनत का फल मिलेगा। वरिष्ठ अधिकारियों का सहयोग प्राप्त होगा।"),
    ZodiacCardData("♒", "कुंभ", "Aquarius", AirElement, 4, "आसमानी (Sky)", "05:00 PM - 06:30 PM", "नवीन विचारों से उन्नति होगी। मित्रों से सहयोग मिलेगा।"),
    ZodiacCardData("♓", "मीन", "Pisces", WaterElement, 3, "हल्दी पीला", "09:30 AM - 11:00 AM", "रचनात्मक कार्यों में सफलता मिलेगी। अंतर्ज्ञान पर विश्वास रखें।")
)

@Composable
private fun HoroscopeStripSection(
    selectedIndex: Int,
    onSelectZodiac: (Int) -> Unit,
    onViewFullHoroscope: () -> Unit
) {
    val selectedSign = ZODIAC_SIGNS.getOrElse(selectedIndex) { ZODIAC_SIGNS.first() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "दैनिक राशिफल (Today's Horoscope)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "सभी देखें →",
                color = SacredGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onViewFullHoroscope)
            )
        }

        // Horizontal zodiac strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(ZODIAC_SIGNS) { index, sign ->
                val isSelected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) CosmicVioletDim.copy(alpha = 0.6f) else CosmicCardBg.copy(alpha = 0.8f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) SacredGold else CosmicCardBorder
                    ),
                    modifier = Modifier
                        .width(86.dp)
                        .clickable { onSelectZodiac(index) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = sign.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sign.hindi,
                            color = if (isSelected) SacredGold else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = sign.english,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Selected sign preview card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewFullHoroscope),
            cornerRadius = 14.dp,
            glowColor = SacredGold,
            glowAlpha = 0.2f
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = selectedSign.emoji, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedSign.hindi} (${selectedSign.english})",
                            color = SacredGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = selectedSign.elementColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "आज का फल",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedSign.summary,
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lucky items pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "लकी नंबर: ${selectedSign.luckyNumber} • रंग: ${selectedSign.luckyColor}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "विस्तार से पढ़ें →",
                    color = SacredGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 5: AI CHAT PROMO CARD (PremiumCard)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun AiChatPromoCard(
    onChatNow: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SacredGoldSubtle)
                        .border(1.5.dp, SacredGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🤖", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    GoldText(
                        text = "AI ज्योतिषी से बात करें",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "अपने सवालों के जवाब पाएं - Lite, Pro, Expert मॉडल",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Model highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiModelPill(title = "Lite", desc = "त्वरित प्रश्न", color = NeutralBlue, modifier = Modifier.weight(1f))
                AiModelPill(title = "Pro", desc = "गहन विश्लेषण", color = CosmicVioletLight, modifier = Modifier.weight(1f))
                AiModelPill(title = "Expert", desc = "सटीक कुंडली", color = SacredGold, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            GradientButton(
                text = "Chat Now / अभी बात करें ✨",
                onClick = onChatNow,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AiModelPill(
    title: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CosmicCardBg.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 6: LIVE ASTROLOGERS STRIP (LazyRow)
// ══════════════════════════════════════════════════════════════════════

private data class DemoAstrologer(
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviews: Int,
    val experience: Int,
    val rate: String,
    val initials: String,
    val avatarBg: Color
)

private val DEMO_ASTROLOGERS = listOf(
    DemoAstrologer("Pt. Radhey Shyam", "Vedic & Kundali", 4.9, 1240, 15, "₹15/min", "RS", CosmicVioletDim),
    DemoAstrologer("Acharya Devrat", "Vastu & Muhurat", 4.8, 980, 12, "₹20/min", "AD", SacredGoldDim),
    DemoAstrologer("Dr. Sunita Sharma", "Tarot & Career", 4.9, 1560, 18, "₹25/min", "SS", SacredSaffronDim),
    DemoAstrologer("Swami Anand", "KP & Prashna", 4.8, 830, 10, "₹18/min", "SA", CosmicElevated),
    DemoAstrologer("Jyotish Shastri", "Milan & Vivah", 4.7, 720, 14, "₹12/min", "JS", CosmicViolet)
)

@Composable
private fun LiveAstrologersSection(
    astrologers: List<Astrologer>,
    onAstrologerClick: () -> Unit,
    onViewAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AuspiciousGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "लाइव ज्योतिषी (Live Astrologers)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Text(
                text = "सभी देखें →",
                color = SacredGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (astrologers.isNotEmpty()) {
                items(astrologers.take(6)) { astro ->
                    AstrologerMiniCard(
                        name = astro.name,
                        specialty = astro.specialisations.firstOrNull() ?: "Vedic Astrologer",
                        rating = if (astro.rating > 0) astro.rating else 4.8,
                        rate = astro.displayRate,
                        initials = astro.name.take(2).uppercase(),
                        avatarBg = CosmicVioletDim,
                        avatarUrl = astro.img.ifBlank { null },
                        onClick = onAstrologerClick
                    )
                }
            } else {
                items(DEMO_ASTROLOGERS) { demo ->
                    AstrologerMiniCard(
                        name = demo.name,
                        specialty = demo.specialty,
                        rating = demo.rating,
                        rate = demo.rate,
                        initials = demo.initials,
                        avatarBg = demo.avatarBg,
                        avatarUrl = null,
                        onClick = onAstrologerClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AstrologerMiniCard(
    name: String,
    specialty: String,
    rating: Double,
    rate: String,
    initials: String,
    avatarBg: Color,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
        glowColor = CosmicVioletLight,
        glowAlpha = 0.2f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with online green dot
            Box {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = name,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, SacredGold, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(avatarBg)
                            .border(1.5.dp, SacredGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                // Online indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AuspiciousGreen)
                        .border(2.dp, CosmicBackground, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = specialty,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "⭐ ${String.format(Locale.getDefault(), "%.1f", rating)}", color = TextGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "• $rate", color = TextSecondary, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SacredGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = "Chat / बात करें",
                    color = CosmicBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 7: PREMIUM FEATURES SECTION (Palmistry, Face Reading, Vastu)
// ══════════════════════════════════════════════════════════════════════

private data class PremiumFeatureItem(
    val title: String,
    val hindiTitle: String,
    val subtitle: String,
    val icon: String,
    val gradientColors: List<Color>,
    val onClick: () -> Unit
)

@Composable
private fun PremiumFeaturesSection(
    onNavigateToPalmistry: () -> Unit,
    onNavigateToFaceReading: () -> Unit,
    onNavigateToVastu: () -> Unit
) {
    val features = remember {
        listOf(
            PremiumFeatureItem(
                title = "Palmistry",
                hindiTitle = "हस्तरेखा",
                subtitle = "Life & Fate lines",
                icon = "✋",
                gradientColors = listOf(Color(0xFF2A1647), Color(0xFF130F2A)),
                onClick = onNavigateToPalmistry
            ),
            PremiumFeatureItem(
                title = "Face Reading",
                hindiTitle = "सामुद्रिक शास्त्र",
                subtitle = "Face wisdom",
                icon = "👤",
                gradientColors = listOf(Color(0xFF381E25), Color(0xFF130F2A)),
                onClick = onNavigateToFaceReading
            ),
            PremiumFeatureItem(
                title = "Vastu Shastra",
                hindiTitle = "वास्तु दोष",
                subtitle = "Home energy audit",
                icon = "🏠",
                gradientColors = listOf(Color(0xFF16323B), Color(0xFF130F2A)),
                onClick = onNavigateToVastu
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "प्रीमियम विश्लेषण (AI Vision & Readings)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SacredGoldSubtle
            ) {
                Text(
                    text = "AI Vision",
                    color = SacredGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            features.forEach { feature ->
                PremiumFeatureCard(
                    feature = feature,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    feature: PremiumFeatureItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(colors = feature.gradientColors)
            )
            .border(1.dp, SacredGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = feature.onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SacredGoldSubtle)
                    .border(1.dp, SacredGold.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = feature.icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feature.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = feature.hindiTitle,
                color = SacredGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tap to explore →",
                color = TextGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  SECTION 8: READING HISTORY (Local DB + Saved Charts)
// ══════════════════════════════════════════════════════════════════════

private data class UnifiedHistoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val type: String,
    val chartId: String? = null
)

@Composable
private fun ReadingHistorySection(
    localReadings: List<ReadingHistoryRepository.SavedReading>,
    savedCharts: List<com.omastro.app.data.remote.SavedChart>,
    onKundaliClick: (String) -> Unit,
    onReadingClick: (String) -> Unit,
    onStartNewReading: () -> Unit
) {
    // Combine saved charts and local vision readings into top 3 entries
    val displayItems = remember(localReadings, savedCharts) {
        val list = mutableListOf<UnifiedHistoryItem>()

        // Add saved kundalis
        savedCharts.take(2).forEach { chart ->
            list.add(
                UnifiedHistoryItem(
                    id = chart.id,
                    title = chart.name.ifBlank { "Janam Kundali" },
                    subtitle = "जन्म पत्रिका • ${chart.birthDate}",
                    icon = "⭐",
                    type = "kundali",
                    chartId = chart.id
                )
            )
        }

        // Add vision readings
        localReadings.take(2).forEach { reading ->
            val icon = when (reading.featureType) {
                ReadingFeatureType.PALMISTRY -> "✋"
                ReadingFeatureType.FACE -> "👤"
                ReadingFeatureType.VASTU -> "🏠"
                else -> "🔮"
            }
            val dateStr = try {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(reading.createdAt))
            } catch (_: Exception) { "Recent" }

            list.add(
                UnifiedHistoryItem(
                    id = reading.id,
                    title = reading.label,
                    subtitle = "AI विश्लेषण • $dateStr",
                    icon = icon,
                    type = reading.featureType
                )
            )
        }

        // If completely empty, provide fallback demo history
        if (list.isEmpty()) {
            listOf(
                UnifiedHistoryItem("demo1", "Aarav - Janam Kundali", "वैदिक जन्म पत्रिका • 14 Oct 2026", "⭐", "kundali"),
                UnifiedHistoryItem("demo2", "Home Vastu Analysis", "ईशान कोण ऊर्जा संतुलन • 12 Oct 2026", "🏠", "vastu"),
                UnifiedHistoryItem("demo3", "AI Palmistry Reading", "हस्तरेखा व भाग्य रेखा • 10 Oct 2026", "✋", "palmistry")
            )
        } else {
            list.take(3)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = SacredGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "हालिया अध्ययन (Reading History)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Text(
                text = "नया बनाएं +",
                color = SacredGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onStartNewReading)
            )
        }

        displayItems.forEach { item ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (item.chartId != null) {
                            onKundaliClick(item.chartId)
                        } else {
                            onReadingClick(item.type)
                        }
                    },
                cornerRadius = 14.dp,
                glowColor = SacredGold,
                glowAlpha = 0.15f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SacredGoldSubtle)
                            .border(1.dp, SacredGold.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.icon, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.subtitle,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = SacredGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
