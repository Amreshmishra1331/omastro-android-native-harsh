package com.omastro.app.ui.screens.horoscope

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.HoroscopeSign
import com.omastro.app.data.model.RashifalArea
import com.omastro.app.data.model.RashifalData
import com.omastro.app.data.model.planetHindiFallback
import com.omastro.app.ui.components.*
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.*

/**
 * HoroscopeScreen — Premium Cosmic Vedic Horoscope Experience
 *
 * Features:
 * 1. CosmicTopBar with title "राशिफल" and back button
 * 2. Period Selector Tab Row: Daily | Weekly | Monthly | Yearly with gold underline indicator
 * 3. Zodiac Selector LazyRow (12 zodiac cards each ~80.dp wide), selected = gold border
 * 4. Main Prediction AnimatedGlassCard: 60sp zodiac glyph, prediction text, lucky item chips
 * 5. Life Areas Score Section: 4 AnimatedLinearProgressIndicator bars for Love, Career, Health, Finance
 * 6. Remedy GlassCard with saffron border: "आज का उपाय" title, remedy text, share button
 */
@Composable
fun HoroscopeScreen(
    onBack: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    viewModel: HoroscopeViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedSign by viewModel.selectedSign.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val horoscope by viewModel.horoscope.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lang by LangPrefs.language.collectAsState()
    val hi = lang == "hi"

    // Default to the first sign (Aries) on open if not set, and fetch
    LaunchedEffect(Unit) {
        if (selectedSign == null) {
            val initial = viewModel.zodiacSigns.firstOrNull()
            if (initial != null) {
                viewModel.selectSign(initial)
                viewModel.fetchHoroscope()
            }
        } else if (horoscope == null && !isLoading) {
            viewModel.fetchHoroscope()
        }
    }

    val activeSign = selectedSign ?: viewModel.zodiacSigns.first()

    Scaffold(
        topBar = {
            CosmicTopBar(
                title = if (hi) "राशिफल" else "Horoscope",
                subtitle = if (hi) "दैनिक ग्रह गोचर एवं भविष्यफल" else "Daily Cosmic Guidance",
                onBackClick = onBack,
                showLogo = false,
                actions = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = SacredGold
                            )
                        }
                    }
                }
            )
        },
        containerColor = CosmicBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // -------------------------------------------------------------
            // 2. Period Selector Tab Row: Daily | Weekly | Monthly | Yearly
            // -------------------------------------------------------------
            item {
                PeriodSelectorTabRow(
                    periods = viewModel.periods,
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { period ->
                        viewModel.selectPeriod(period)
                    },
                    hi = hi
                )
            }

            // -------------------------------------------------------------
            // 3. Zodiac Selector LazyRow (12 Zodiac cards ~80.dp wide)
            // -------------------------------------------------------------
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hi) "अपनी राशि चुनें" else "Select Your Zodiac Sign",
                            color = SacredGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${activeSign.symbol} ${signName(activeSign, hi)}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        items(viewModel.zodiacSigns) { sign ->
                            val isSelected = sign.id == activeSign.id
                            ZodiacCard(
                                sign = sign,
                                isSelected = isSelected,
                                hi = hi,
                                onClick = {
                                    viewModel.selectSign(sign)
                                    viewModel.fetchHoroscope()
                                }
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Content States: Loading, Error, or Active Result
            // -------------------------------------------------------------
            when {
                isLoading && horoscope == null -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ShimmerCard(modifier = Modifier.fillMaxWidth().height(200.dp))
                            ShimmerCard(modifier = Modifier.fillMaxWidth().height(160.dp))
                        }
                    }
                }

                errorMessage != null && horoscope == null -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            glowColor = InauspiciousRed,
                            glowAlpha = 0.4f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (hi) "राशिफल लोड करने में समस्या हुई" else "Unable to load horoscope",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                GradientButton(
                                    text = if (hi) "पुनः प्रयास करें" else "Try Again",
                                    onClick = { viewModel.retry() },
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                )
                            }
                        }
                    }
                }

                horoscope != null -> {
                    val data = horoscope!!

                    // ---------------------------------------------------------
                    // 4. Main Prediction AnimatedGlassCard
                    // ---------------------------------------------------------
                    item {
                        MainPredictionCard(
                            sign = activeSign,
                            data = data,
                            period = selectedPeriod,
                            hi = hi
                        )
                    }

                    // Saturn Transit Alert (if applicable)
                    data.saturnTransit?.takeIf { it.active }?.let { transit ->
                        item {
                            SaturnAlertBanner(transit = transit, hi = hi)
                        }
                    }

                    // ---------------------------------------------------------
                    // 5. Life Areas Score Section (4 Animated Progress Bars)
                    // ---------------------------------------------------------
                    item {
                        LifeAreasScoreSection(
                            data = data,
                            hi = hi
                        )
                    }

                    // ---------------------------------------------------------
                    // 6. Remedy GlassCard with Saffron Border + Share Button
                    // ---------------------------------------------------------
                    if (data.remedies.isNotEmpty()) {
                        item {
                            RemedyCard(
                                sign = activeSign,
                                remedies = data.remedies,
                                hi = hi,
                                onShare = { remedyText ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            if (hi) "आज का ज्योतिष उपाय — ॐ एस्ट्रो" else "Today's Vedic Remedy — OmAstro"
                                        )
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "✨ ॐ एस्ट्रो AI — ${activeSign.symbol} ${signName(activeSign, hi)} आज का उपाय:\n\n$remedyText\n\nप्रतिदिन अपनी राशि का सटीक भविष्यफल जानने के लिए OmAstro AI ऐप डाउनलोड करें।"
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, if (hi) "उपाय शेयर करें" else "Share Remedy")
                                    )
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(28.dp)) }
                }

                else -> {
                    // Fallback when no data is loaded yet
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GradientButton(
                                text = if (hi) "राशिफल देखें" else "View Horoscope",
                                onClick = { viewModel.fetchHoroscope() },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 2: Period Selector Tab Row with Gold Underline
// -----------------------------------------------------------------------------
@Composable
private fun PeriodSelectorTabRow(
    periods: List<String>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    hi: Boolean
) {
    val selectedIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0)

    val periodLabels = listOf(
        "daily" to (if (hi) "दैनिक" else "Daily"),
        "weekly" to (if (hi) "साप्ताहिक" else "Weekly"),
        "monthly" to (if (hi) "मासिक" else "Monthly"),
        "yearly" to (if (hi) "वार्षिक" else "Yearly")
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = CosmicCardBg.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, CosmicCardBorder)
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            contentColor = SacredGold,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        height = 3.dp,
                        color = SacredGold
                    )
                }
            },
            divider = {}
        ) {
            periods.forEachIndexed { index, key ->
                val label = periodLabels.find { it.first == key }?.second ?: key.replaceFirstChar { it.uppercase() }
                val isSelected = selectedIndex == index

                Tab(
                    selected = isSelected,
                    onClick = { onPeriodSelected(key) },
                    text = {
                        Text(
                            text = label,
                            color = if (isSelected) SacredGold else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 3: 12 Zodiac Cards (~80.dp wide) in LazyRow
// -----------------------------------------------------------------------------
@Composable
private fun ZodiacCard(
    sign: HoroscopeSign,
    isSelected: Boolean,
    hi: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) SacredGold else CosmicCardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val cardBackground = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                SacredGold.copy(alpha = 0.20f),
                CosmicCardBg
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                CosmicCardBg.copy(alpha = 0.95f),
                CosmicSurface.copy(alpha = 0.90f)
            )
        )
    }

    Box(
        modifier = Modifier
            .width(82.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (isSelected) SacredGold.copy(alpha = 0.35f) else Color.Transparent,
                spotColor = if (isSelected) SacredGold.copy(alpha = 0.45f) else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sign Glyph Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(sign.glyphColor).copy(alpha = 0.25f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) SacredGold else Color(sign.glyphColor),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sign.symbol,
                    fontSize = 22.sp,
                    color = if (isSelected) SacredGoldGlow else TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hindi / Primary Name
            Text(
                text = signName(sign, hi),
                color = if (isSelected) SacredGold else TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // English Sub-name
            Text(
                text = sign.name,
                color = if (isSelected) SacredGoldGlow.copy(alpha = 0.85f) else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component 4: Main Prediction AnimatedGlassCard
// -----------------------------------------------------------------------------
@Composable
private fun MainPredictionCard(
    sign: HoroscopeSign,
    data: RashifalData,
    period: String,
    hi: Boolean
) {
    val overallScore = data.scores["overall"] ?: 8

    AnimatedGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Emoji Glyph (60sp) + Sign Info + Overall Rating Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Big Zodiac Glyph ~60sp
                    Text(
                        text = sign.symbol,
                        fontSize = 58.sp,
                        color = SacredGoldGlow,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${signName(sign, hi)} (${sign.name})",
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${sign.dates} • ${if (hi) "स्वामी: " else "Ruler: "}${signRuler(sign, hi)}",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "${if (hi) "तत्व: " else "Element: "}${sign.element}",
                            color = SacredGold.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Overall Score Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SacredGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (hi) "समग्र स्कोर" else "OVERALL",
                            color = SacredGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "$overallScore/10",
                            color = SacredGoldGlow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prediction Text / Cosmic Guidance
            Text(
                text = if (hi) "ग्रह दशा एवं मार्गदर्शन" else "Cosmic Guidance",
                color = SacredGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = data.summary.ifBlank {
                    if (hi) "आज का दिन आपके लिए नई ऊर्जा और सकारात्मक बदलाव लेकर आ रहा है। अपने अंतर्ज्ञान पर भरोसा रखें और महत्वपूर्ण निर्णयों में धैर्य बरतें।"
                    else "The planetary alignments bring fresh vigor and opportunities today. Trust your intuition and move forward with clarity."
                },
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )

            // Lucky Items Chips (Number / Color / Gem / Direction)
            val lucky = data.lucky
            if (lucky != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = CosmicCardBorder.copy(alpha = 0.6f),
                    thickness = 0.8.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (hi) "शुभ कारक" else "Lucky Elements",
                    color = SacredGoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LuckyChip(
                        icon = "🎯",
                        label = if (hi) "शुभ अंक" else "Number",
                        value = if (lucky.number > 0) lucky.number.toString() else "7",
                        modifier = Modifier.weight(1f)
                    )
                    LuckyChip(
                        icon = "🎨",
                        label = if (hi) "शुभ रंग" else "Color",
                        value = lucky.color.ifBlank { if (hi) "पीला" else "Yellow" },
                        modifier = Modifier.weight(1f)
                    )
                    LuckyChip(
                        icon = "💎",
                        label = if (hi) "शुभ रत्न" else "Gem",
                        value = lucky.gem.ifBlank { if (hi) "माणिक्य" else "Ruby" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 5: Life Areas Score Section (4 Animated Progress Bars)
// -----------------------------------------------------------------------------
@Composable
private fun LifeAreasScoreSection(
    data: RashifalData,
    hi: Boolean
) {
    // Extract scores for Love, Career, Health, Finance from response areas/scores
    val loveScore = data.areas["love"]?.score ?: data.scores["love"] ?: 8
    val careerScore = data.areas["career"]?.score ?: data.scores["career"] ?: 7
    val healthScore = data.areas["health"]?.score ?: data.scores["health"] ?: 9
    val financeScore = data.areas["finance"]?.score ?: data.scores["finance"] ?: 8

    val areasList = listOf(
        LifeAreaItem(
            name = if (hi) "प्रेम व संबंध" else "Love & Relations",
            icon = Icons.Default.Favorite,
            score = loveScore,
            color = Color(0xFFEC4899),
            text = data.areas["love"]?.text ?: ""
        ),
        LifeAreaItem(
            name = if (hi) "करियर व व्यवसाय" else "Career & Business",
            icon = Icons.Default.Work,
            score = careerScore,
            color = SacredGold,
            text = data.areas["career"]?.text ?: ""
        ),
        LifeAreaItem(
            name = if (hi) "स्वास्थ्य व ऊर्जा" else "Health & Vitality",
            icon = Icons.Default.Spa,
            score = healthScore,
            color = AuspiciousGreen,
            text = data.areas["health"]?.text ?: ""
        ),
        LifeAreaItem(
            name = if (hi) "वित्त व समृद्धि" else "Finance & Wealth",
            icon = Icons.Default.MonetizationOn,
            score = financeScore,
            color = NeutralBlue,
            text = data.areas["finance"]?.text ?: ""
        )
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        glowColor = CosmicViolet,
        glowAlpha = 0.35f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hi) "जीवन के प्रमुख क्षेत्र" else "Life Spheres Energy",
                    color = SacredGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (hi) "स्कोर (10 में से)" else "Score /10",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            areasList.forEach { area ->
                LifeAreaRow(area = area)
            }
        }
    }
}

private data class LifeAreaItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val score: Int,
    val color: Color,
    val text: String
)

@Composable
private fun LifeAreaRow(area: LifeAreaItem) {
    val progressFraction = (area.score.coerceIn(0, 10)) / 10f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(area.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = area.icon,
                        contentDescription = null,
                        tint = area.color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = area.name,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "${area.score}/10",
                color = area.color,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Animated progress bar
        AnimatedLinearProgressIndicator(
            progress = progressFraction,
            color = area.color,
            trackColor = CosmicSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
        )

        if (area.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = area.text,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component 6: Remedy GlassCard with Saffron Border + Share Button
// -----------------------------------------------------------------------------
@Composable
private fun RemedyCard(
    sign: HoroscopeSign,
    remedies: List<String>,
    hi: Boolean,
    onShare: (String) -> Unit
) {
    val combinedRemedyText = remedies.joinToString("\n• ")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        glowColor = SacredSaffron,
        glowAlpha = 0.85f,
        elevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SacredSaffron.copy(alpha = 0.2f))
                            .border(1.dp, SacredSaffron, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🪔", fontSize = 16.sp)
                    }
                    Text(
                        text = if (hi) "आज का अचूक उपाय" else "Today's Sacred Remedy",
                        color = SacredSaffron,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SacredSaffron.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SacredSaffron.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (hi) "वैदिक उपाय" else "Vedic",
                        color = SacredSaffron,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            remedies.forEachIndexed { index, remedy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "🕉️",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    Text(
                        text = remedy,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Share Remedy Button
            Button(
                onClick = { onShare(combinedRemedyText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredSaffron.copy(alpha = 0.2f),
                    contentColor = SacredSaffron
                ),
                border = BorderStroke(1.dp, SacredSaffron)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = SacredSaffron,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hi) "उपाय शेयर करें" else "Share Remedy",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Helper UI: Saturn Transit Alert Banner
// -----------------------------------------------------------------------------
@Composable
private fun SaturnAlertBanner(
    transit: com.omastro.app.data.model.SaturnTransit,
    hi: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2E121E))
            .border(1.dp, InauspiciousRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hi && transit.nameHi.isNotBlank()) transit.nameHi else transit.nameEn.ifBlank { "शनि गोचर प्रभाव" },
                    color = Color(0xFFFF8A80),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = InauspiciousRed.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = if (hi) "सक्रिय प्रभाव" else "ACTIVE",
                        color = Color(0xFFFF8A80),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            val desc = if (hi && transit.descriptionHi.isNotBlank()) transit.descriptionHi else transit.descriptionEn
            if (desc.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Helper UI: Lucky Chip
// -----------------------------------------------------------------------------
@Composable
private fun LuckyChip(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = CosmicSurface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = SacredGoldLight,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Utilities
// -----------------------------------------------------------------------------
private fun signName(sign: HoroscopeSign, hi: Boolean): String =
    if (hi) sign.hindiName.ifBlank { sign.name } else sign.name

private fun signRuler(sign: HoroscopeSign, hi: Boolean): String =
    if (hi) planetHindiFallback(sign.ruler).ifBlank { sign.ruler } else sign.ruler
