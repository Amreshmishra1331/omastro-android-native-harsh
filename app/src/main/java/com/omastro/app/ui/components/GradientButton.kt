package com.omastro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.theme.CosmicBackground
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.SacredGoldDark
import com.omastro.app.ui.theme.StarlightWhite

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    gradient: Brush = Brush.horizontalGradient(
        colors = listOf(SacredGold, SacredGoldDark)
    ),
    textColor: Color = CosmicBackground,
    cornerRadius: Dp = 18.dp,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = if (enabled) gradient else Brush.horizontalGradient(
                    colors = listOf(Color.Gray, Color.DarkGray)
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else StarlightWhite.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
