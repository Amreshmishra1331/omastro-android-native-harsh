package com.omastro.app.ui.screens.numerology

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumerologyScreen(
    onBack: () -> Unit
) {
    var nameInput by remember { mutableStateOf("Astro Seeker") }
    var dobInput by remember { mutableStateOf("15/08/1995") }

    val mulank = 6 // (1+5 = 6, Venus)
    val bhagyank = 2 // (1+5+0+8+1+9+9+5 = 38 -> 11 -> 2, Moon)
    val namank = 1 // Sun

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vedic & Chaldean Numerology", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Mulank • Bhagyank • Namank", color = MysticPurple, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Triple Pillar Numbers Hero
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumberPillarCard("Mulank (Life Path)", mulank, "Venus (शुक्र)", modifier = Modifier.weight(1f))
                    NumberPillarCard("Bhagyank (Destiny)", bhagyank, "Moon (चन्द्र)", modifier = Modifier.weight(1f))
                    NumberPillarCard("Namank (Name)", namank, "Sun (सूर्य)", modifier = Modifier.weight(1f))
                }
            }

            // In-Depth Insights
            item {
                Text("Vibrational Analysis & Harmony", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mulank 6: The Harmonizer & Artist", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Governed by Shukra (Venus). You are blessed with refined artistic taste, charm, diplomatic balance, and an innate eye for luxury and beauty.",
                            color = StarlightWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Divider(color = CosmicNavy)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lucky Gem: Diamond / White Zircon", color = MysticPurple, fontSize = 12.sp)
                            Text("Lucky Day: Friday", color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bhagyank 2: The Intuitive Peacekeeper", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Governed by Chandra (Moon). Indicates deep emotional intuition, empathy, strong imagination, and cooperative leadership.",
                            color = StarlightWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Divider(color = CosmicNavy)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lucky Gem: Pearl / Moonstone", color = MysticPurple, fontSize = 12.sp)
                            Text("Lucky Day: Monday", color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cosmic Lucky Elements", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("• Lucky Numbers: 6, 2, 1, 9", color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("• Lucky Colors: Cream, Light Blue, White, Pastel Gold", color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("• Unfavorable Numbers: 3, 8 (Exercise caution during major agreements)", color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun NumberPillarCard(title: String, number: Int, planet: String, modifier: Modifier = Modifier) {
    CosmicCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CosmicNavy),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", color = SacredGold, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
            Text(planet, color = MysticPurple, fontSize = 10.sp, maxLines = 1)
        }
    }
}
