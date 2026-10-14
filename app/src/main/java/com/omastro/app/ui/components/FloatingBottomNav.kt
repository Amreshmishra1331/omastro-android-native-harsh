package com.omastro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.omastro.app.ui.theme.*

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val omAstroNavItems = listOf(
    BottomNavItem("Home",      Icons.Filled.Home,           Icons.Outlined.Home,           "home"),
    BottomNavItem("Kundali",   Icons.Filled.Stars,          Icons.Outlined.Stars,          "kundali"),
    BottomNavItem("AI Chat",   Icons.Filled.AutoAwesome,    Icons.Outlined.AutoAwesome,    "ai_chat"),
    BottomNavItem("Horoscope", Icons.Filled.CalendarMonth,  Icons.Outlined.CalendarMonth,  "horoscope"),
    BottomNavItem("More",      Icons.Filled.GridView,       Icons.Outlined.GridView,       "more")
)

/**
 * FloatingBottomNav — Pill-shaped floating bottom navigation bar
 * Features:
 * - Frosted glass background
 * - Gold indicator dot under selected item
 * - Scale + color animation on selection
 * - Floating above content (not attached to bottom edge)
 */
@Composable
fun FloatingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glass pill container
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = CosmicViolet.copy(alpha = 0.4f),
                    spotColor = SacredGold.copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CosmicCardBg.copy(alpha = 0.95f),
                            CosmicSurface.copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SacredGold.copy(alpha = 0.3f),
                            CosmicViolet.copy(alpha = 0.3f),
                            SacredGold.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            omAstroNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) SacredGold else TextSecondary,
        animationSpec = tween(durationMillis = 250),
        label = "iconColor"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "bgAlpha"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Gold glow background for selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SacredGold.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                )
            }
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = iconColor
        )
        // Gold dot indicator
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    color = SacredGold.copy(alpha = bgAlpha),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
