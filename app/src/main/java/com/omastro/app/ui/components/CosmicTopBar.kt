package com.omastro.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.omastro.app.ui.theme.*

/**
 * CosmicTopBar — Premium app bar with gradient + avatar + notification bell
 */
@Composable
fun CosmicTopBar(
    title: String = "OmAstro AI",
    subtitle: String? = null,
    userAvatar: String? = null,
    userName: String? = null,
    notificationCount: Int = 0,
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    showLogo: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CosmicSurface,
                        CosmicBackground.copy(alpha = 0f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: back button or logo + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                } else if (showLogo) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SacredGold.copy(alpha = 0.15f))
                            .border(1.dp, SacredGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ॐ",
                            color = SacredGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Column {
                    if (userName != null) {
                        Text(
                            text = "Namaste, $userName 🙏",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    GoldText(
                        text = title,
                        fontSize = if (userName != null) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Right: actions + notification + avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()

                // Notification bell
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = if (notificationCount > 0) SacredGold else TextSecondary
                        )
                    }
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(InauspiciousRed, CircleShape)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Avatar
                if (userAvatar != null || userName != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .border(2.dp, SacredGold, CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = onAvatarClick)
                    ) {
                        if (userAvatar != null) {
                            AsyncImage(
                                model = userAvatar,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(CosmicViolet, CosmicVioletDim)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName?.firstOrNull()?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
