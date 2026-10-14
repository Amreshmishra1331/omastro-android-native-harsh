package com.omastro.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.ui.components.CosmicBackground
import com.omastro.app.ui.components.GradientText
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * SplashScreen — OmAstro AI Cosmic Launch Experience.
 *
 * Visual hierarchy:
 * 1. Deep space CosmicBackground with twinkling starfield and subtle nebula
 * 2. Center: Sacred Om symbol (ॐ) scaling from 0.3f to 1f with spring physics & golden halo
 * 3. Gradient gold "OmAstro AI" title
 * 4. Tagline: "वैदिक ज्ञान, AI शक्ति" in shimmering gold
 * 5. Bottom: Staggered pulsing loading dots (300ms intervals)
 * 6. Checks authentication status and routes to Home or Onboarding after 2.5 seconds
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    SplashScreenContent(
        onNavigateNext = { isLoggedIn ->
            if (isLoggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToOnboarding()
            }
        }
    )
}

/**
 * Overload accepting direct [onNavigateNext] callback with the user's login status.
 */
@Composable
fun SplashScreen(
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    SplashScreenContent(onNavigateNext = onNavigateNext)
}

@Composable
private fun SplashScreenContent(
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Navigation and auth check effect
    LaunchedEffect(Unit) {
        startAnimation = true
        val isLoggedIn = try {
            ApiClient.isLoggedIn()
        } catch (e: Exception) {
            false
        }
        delay(2500L)
        onNavigateNext(isLoggedIn)
    }

    // Om symbol spring scale animation (0.3f -> 1.0f)
    val omScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "omScaleAnimation"
    )

    // Om symbol alpha fade-in (0f -> 1f)
    val omAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "omAlphaAnimation"
    )

    // Content fade-in slightly delayed
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 1100, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "contentAlphaAnimation"
    )

    // Breathing glow animation for sacred aura
    val infiniteTransition = rememberInfiniteTransition(label = "auraTransition")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    CosmicBackground(
        modifier = Modifier.fillMaxSize(),
        starCount = 90,
        showNebula = true
    ) {
        // Centered Vedic Brand Identity
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Sacred Om Symbol with Golden Aura
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(omScale)
                        .alpha(omAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Pulsing Gold Aura
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(auraScale)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SacredGoldGlow.copy(alpha = 0.28f),
                                        SacredGold.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Inner Glassmorphism Circle with Gold Border
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        CosmicCardBg.copy(alpha = 0.85f),
                                        CosmicSurface.copy(alpha = 0.70f)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        SacredGoldGlow,
                                        SacredGold,
                                        SacredSaffron
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ॐ",
                            color = SacredGoldGlow,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Name "OmAstro AI" in Gradient Gold
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(contentAlpha)
                ) {
                    GradientText(
                        text = "OmAstro AI",
                        style = TextStyle(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hindi Sacred Tagline
                    Text(
                        text = "वैदिक ज्ञान, AI शक्ति",
                        color = SacredGoldLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vedic Wisdom • Artificial Intelligence",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom Loading Dots and Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SplashLoadingDots()

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Unveiling Your Cosmic Destiny...",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Animated loading indicator consisting of 3 circles pulsing with staggered 300ms delays.
 */
@Composable
private fun SplashLoadingDots(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashLoadingDots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Alpha"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Alpha"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Alpha"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dots = listOf(dot1Alpha, dot2Alpha, dot3Alpha)
        dots.forEach { alphaVal ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(0.85f + (alphaVal * 0.3f))
                    .clip(CircleShape)
                    .background(SacredGold.copy(alpha = alphaVal))
            )
        }
    }
}
