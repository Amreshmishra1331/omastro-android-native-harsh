package com.omastro.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.theme.*

/**
 * GradientText — Text with brush gradient (gold to violet or custom)
 * 
 * Usage: GradientText("OmAstro AI", style = MaterialTheme.typography.headlineLarge)
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    gradient: Brush = Brush.horizontalGradient(
        colors = listOf(SacredGold, SacredGoldGlow, CosmicVioletLight)
    )
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = gradient)
    )
}

/** Gold shimmer text — for headings */
@Composable
fun GoldText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    GradientText(
        text = text,
        modifier = modifier,
        style = TextStyle(fontSize = fontSize, fontWeight = fontWeight),
        gradient = Brush.horizontalGradient(
            colors = listOf(SacredGoldGlow, SacredGold, SacredGoldDim, SacredGold)
        )
    )
}

/** Cosmic gradient text — violet to gold */
@Composable
fun CosmicText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    GradientText(
        text = text,
        modifier = modifier,
        style = TextStyle(fontSize = fontSize, fontWeight = fontWeight),
        gradient = Brush.horizontalGradient(
            colors = listOf(CosmicVioletLight, SacredGold)
        )
    )
}
