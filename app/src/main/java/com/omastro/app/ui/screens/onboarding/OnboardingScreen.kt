package com.omastro.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.components.CosmicBackground
import com.omastro.app.ui.components.GradientButton
import com.omastro.app.ui.components.GradientText
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * OnboardingScreen — 3-Page Cosmic Vedic Introduction.
 *
 * Page 1: "ब्रह्मांड का ज्ञान" (Cosmic Knowledge)
 *  - Animated Galaxy icon 🌌
 *  - Title: "वैदिक ज्ञान, AI शक्ति"
 *  - Description: Thousands of years of Vedic wisdom synthesized with modern AI
 *
 * Page 2: "अपनी कुंडली जानें" (Know Your Kundali)
 *  - Animated Star icon ⭐
 *  - Title: "AI Kundali Analysis"
 *  - Description: Janam Kundali, Dasha, Planetary Transits, Yogas
 *  - 3 Feature Chips: Kundali, Horoscope, Matching
 *
 * Page 3: "शुरू करें" (Get Started)
 *  - Animated Crystal Ball icon 🔮
 *  - Title: "आपकी यात्रा शुरू होती है"
 *  - Description: Join now and discover your destiny
 *  - Primary "Get Started" CTA + "Already have account? Login" link
 */
@Composable
fun OnboardingScreen(
    onNavigateToAuth: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val skipAction = onSkip ?: onNavigateToAuth

    CosmicBackground(
        modifier = Modifier.fillMaxSize(),
        starCount = 75,
        showNebula = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Navigation Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sacred Om Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SacredGoldSubtle)
                            .border(1.dp, SacredGold.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ॐ",
                            color = SacredGoldGlow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "OmAstro AI",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Skip button on pages 1 & 2 only
                if (pagerState.currentPage < 2) {
                    TextButton(
                        onClick = skipAction,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Skip",
                            color = SacredGoldLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(width = 48.dp, height = 36.dp))
                }
            }

            // ── Horizontal Pager (Pages 1, 2, 3) ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.95f),
                    exit = fadeOut(tween(300))
                ) {
                    when (page) {
                        0 -> OnboardingPageContent(
                            category = "ब्रह्मांड का ज्ञान",
                            icon = "🌌",
                            title = "वैदिक ज्ञान, AI शक्ति",
                            description = "हजारों साल पुरानी वैदिक ज्योतिष को AI के साथ जोड़कर आपकी जिंदगी को समझें"
                        )
                        1 -> OnboardingPageContent(
                            category = "अपनी कुंडली जानें",
                            icon = "⭐",
                            title = "AI Kundali Analysis",
                            description = "जन्म कुंडली, दशा, ग्रह स्थिति, योग - सब कुछ AI द्वारा विश्लेषण",
                            featureChips = listOf("Kundali", "Horoscope", "Matching")
                        )
                        2 -> OnboardingPageContent(
                            category = "शुरू करें",
                            icon = "🔮",
                            title = "आपकी यात्रा शुरू होती है",
                            description = "अभी join करें और अपनी destiny जानें"
                        )
                    }
                }
            }

            // ── Bottom Area: Indicators & Navigation Actions ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(SacredGoldGlow, SacredGold, SacredSaffron)
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(CosmicCardBorder, CosmicCardBorder)
                                        )
                                    }
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        )
                    }
                }

                // CTA Buttons based on current page
                if (pagerState.currentPage < 2) {
                    GradientButton(
                        text = "Next",
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                } else {
                    // Final Page Actions
                    GradientButton(
                        text = "Get Started",
                        onClick = onNavigateToAuth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onNavigateToAuth,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Already have an account? Login",
                            color = SacredGoldLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable slide layout for each onboarding page.
 */
@Composable
private fun OnboardingPageContent(
    category: String,
    icon: String,
    title: String,
    description: String,
    featureChips: List<String> = emptyList()
) {
    // Breathing & floating animation for large emoji/icon
    val infiniteTransition = rememberInfiniteTransition(label = "iconFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating Animated Icon with Sacred Glowing Aura
        Box(
            modifier = Modifier
                .size(170.dp)
                .offset(y = floatOffset.dp)
                .scale(scalePulse),
            contentAlignment = Alignment.Center
        ) {
            // Cosmic Radial Glow Backdrop
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SacredGold.copy(alpha = 0.22f),
                                CosmicViolet.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Frosted Glass Circle with Gold/Violet Border
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CosmicCardBg.copy(alpha = 0.90f),
                                CosmicSurface.copy(alpha = 0.80f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SacredGoldGlow,
                                SacredGold.copy(alpha = 0.5f),
                                CosmicVioletLight
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 58.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Category Tag Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CosmicVioletSubtle)
                .border(1.dp, SacredGold.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = category,
                color = SacredGoldGlow,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Page Title in Gradient Gold
        GradientText(
            text = title,
            style = TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center
            ),
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    SacredGoldGlow,
                    SacredGold,
                    SacredSaffron
                )
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Page Description
        Text(
            text = description,
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Page 2 Feature Chips
        if (featureChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                featureChips.forEach { chipName ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicCardBg.copy(alpha = 0.85f))
                            .border(1.dp, CosmicCardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SacredGold)
                            )
                            Text(
                                text = chipName,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
