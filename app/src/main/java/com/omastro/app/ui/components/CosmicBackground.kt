package com.omastro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.omastro.app.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/**
 * CosmicBackground — Animated star field canvas background
 * Renders randomly placed twinkling stars on deep space gradient
 */
@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 80,
    showNebula: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val stars = remember(starCount) {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 1.8f + 0.4f,
                alpha = Random.nextFloat() * 0.7f + 0.2f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cosmic")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Deep background
            drawRect(color = CosmicBackground)

            // Nebula blobs
            if (showNebula) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CosmicViolet.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.3f),
                        radius = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.2f, size.height * 0.3f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SacredGold.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.7f),
                        radius = size.width * 0.4f
                    ),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.8f, size.height * 0.7f)
                )
            }

            // Twinkling stars
            stars.forEachIndexed { index, star ->
                val animatedAlpha = star.alpha * (0.5f + 0.5f * sin((twinkle + index * 0.1f) * PI.toFloat()))
                drawCircle(
                    color = Color.White.copy(alpha = animatedAlpha.coerceIn(0f, 1f)),
                    radius = star.radius,
                    center = Offset(star.x * size.width, star.y * size.height)
                )
            }
        }
        content()
    }
}

/**
 * SimpleCosmicBg — Lightweight version without animation (for performance on list screens)
 */
@Composable
fun SimpleCosmicBg(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CosmicBackground,
                        CosmicSurface,
                        CosmicBackground
                    )
                )
            )
    ) {
        content()
    }
}
