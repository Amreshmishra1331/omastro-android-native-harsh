package com.omastro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.omastro.app.ui.theme.*

/**
 * GradientButton — Primary CTA button with gold gradient + press animation
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "gradientButtonScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val goldGradient = Brush.horizontalGradient(
        colors = if (enabled) {
            listOf(SacredGold, SacredSaffron)
        } else {
            listOf(SacredGold.copy(alpha = 0.5f), SacredSaffron.copy(alpha = 0.5f))
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled && !isLoading) 8.dp else 0.dp,
                shape = shape,
                ambientColor = SacredGold.copy(alpha = 0.35f),
                spotColor = SacredGold.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(brush = goldGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * OutlineGlowButton — Secondary button with violet glow border
 */
@Composable
fun OutlineGlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = CosmicViolet,
    icon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "outlineGlowScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val currentBorderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.5f)
    val contentColor = if (enabled) CosmicVioletLight else CosmicVioletLight.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(shape)
            .background(CosmicVioletSubtle.copy(alpha = if (enabled) 0.15f else 0.05f))
            .border(
                width = 1.5.dp,
                color = currentBorderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * PulseGlowButton — Button with continuous pulsing glow animation (for CTAs like "Start Now")
 */
@Composable
fun PulseGlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    glowColor: Color = SacredGold
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlowTransition")
    val pulseElevation by infiniteTransition.animateValue(
        initialValue = 4.dp,
        targetValue = 16.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseElevation"
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowAlpha"
    )

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled && !isLoading) pulseElevation else 0.dp,
                shape = shape,
                ambientColor = glowColor.copy(alpha = if (enabled) pulseGlowAlpha * 0.5f else 0f),
                spotColor = glowColor.copy(alpha = if (enabled) pulseGlowAlpha else 0f)
            )
    ) {
        GradientButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * IconTextButton — Small icon+text chip button
 */
@Composable
fun IconTextButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    val backgroundBrush = if (selected) {
        Brush.horizontalGradient(listOf(SacredGold, SacredSaffron))
    } else {
        Brush.horizontalGradient(
            listOf(
                CosmicCardBg.copy(alpha = 0.85f),
                CosmicSurface.copy(alpha = 0.85f)
            )
        )
    }
    val contentColor = if (selected) CosmicBackground else TextPrimary
    val borderColor = if (selected) SacredGold else CosmicCardBorder

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(brush = backgroundBrush)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
