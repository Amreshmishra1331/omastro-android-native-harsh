package com.omastro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omastro.app.ui.theme.*

/**
 * GlassCard — Glassmorphism card with gradient border glow
 *
 * @param modifier Modifier
 * @param cornerRadius Corner radius (default 16.dp)
 * @param glowColor Glow/border color (default gold)
 * @param glowAlpha Alpha of the glow border (0f-1f)
 * @param elevation Shadow elevation
 * @param content Card content
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glowColor: Color = SacredGold,
    glowAlpha: Float = 0.3f,
    elevation: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glowColor.copy(alpha = glowAlpha),
            glowColor.copy(alpha = glowAlpha * 0.2f),
            Color.Transparent,
            glowColor.copy(alpha = glowAlpha * 0.8f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = glowColor.copy(alpha = (glowAlpha * 0.4f).coerceIn(0f, 1f)),
                spotColor = glowColor.copy(alpha = (glowAlpha * 0.6f).coerceIn(0f, 1f))
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CosmicCardBg.copy(alpha = 0.90f),
                        CosmicSurface.copy(alpha = 0.80f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * AnimatedGlassCard — GlassCard with animated shimmer border
 */
@Composable
fun AnimatedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glassShimmerTransition")
    val animatedGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animatedGlowAlpha"
    )

    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        glowColor = SacredGold,
        glowAlpha = animatedGlowAlpha,
        elevation = 10.dp,
        content = content
    )
}

/**
 * PremiumCard — Gold gradient border card for premium features
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val goldSaffronBorder = Brush.linearGradient(
        colors = listOf(
            SacredGold,
            SacredSaffron,
            SacredGoldGlow
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = SacredGold.copy(alpha = 0.35f),
                spotColor = SacredSaffron.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CosmicElevated.copy(alpha = 0.95f),
                        CosmicCardBg.copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = goldSaffronBorder,
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * AnimatedLinearProgressIndicator — Smoothly animated progress indicator bar
 */
@Composable
fun AnimatedLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = SacredGold,
    trackColor: Color = CosmicCardBorder.copy(alpha = 0.5f),
    strokeCap: StrokeCap = StrokeCap.Round
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "animatedProgress"
    )

    androidx.compose.material3.LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap
    )
}

