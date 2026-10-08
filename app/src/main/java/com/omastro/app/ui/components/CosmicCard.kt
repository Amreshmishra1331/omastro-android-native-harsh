package com.omastro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.theme.*

@Composable
fun CosmicCard(
    modifier: Modifier = Modifier,
    borderGold: Boolean = false,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedBorderColor = borderColor
        ?: if (borderGold) CosmicBorderGold else CosmicBorder
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CosmicCardBg, CosmicSurface)
                )
            )
            .border(
                width = 1.dp,
                color = resolvedBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun AstrologyHeader(
    title: String,
    coinBalance: Int = 500,
    onWalletClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SacredGold.copy(alpha = 0.2f))
                    .border(1.dp, SacredGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ॐ",
                    color = SacredGold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = SacredGoldLight
            )
        }

        // Coin Badge
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onWalletClick() },
            color = CosmicSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SacredGoldDark)
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
                    text = "$coinBalance 🪙",
                    color = SacredGoldLight,
                    fontSize = 13.sp
                )
            }
        }
    }
}
