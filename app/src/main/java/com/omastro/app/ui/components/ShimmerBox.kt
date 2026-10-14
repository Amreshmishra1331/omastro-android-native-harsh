package com.omastro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omastro.app.ui.theme.*

/**
 * ShimmerBox — Animated loading placeholder with shimmer effect
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translateX"
    )

    val shimmerColors = listOf(
        CosmicCardBg,
        CosmicElevated,
        CosmicCardBg
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateX - 400f, y = translateX - 400f),
        end = Offset(x = translateX, y = translateX)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/** Shimmer for circular avatar */
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmerCircleTransition")
    val translateX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circleTranslateX"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            CosmicCardBg,
            CosmicElevated,
            CosmicCardBg
        ),
        start = Offset(x = translateX - 400f, y = translateX - 400f),
        end = Offset(x = translateX, y = translateX)
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}

/** Shimmer for a card placeholder */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicCardBg.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp),
                cornerRadius = 6.dp
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp),
                cornerRadius = 4.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                cornerRadius = 8.dp
            )
        }
    }
}

/** Shimmer for horizontal list item */
@Composable
fun ShimmerListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicCardBg.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerCircle(size = 48.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp),
                cornerRadius = 4.dp
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp),
                cornerRadius = 4.dp
            )
        }
    }
}
