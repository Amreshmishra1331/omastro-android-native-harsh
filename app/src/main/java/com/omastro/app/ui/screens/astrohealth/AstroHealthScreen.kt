package com.omastro.app.ui.screens.astrohealth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

data class HealthOrganAffliction(
    val organ: String,
    val house: String,
    val rulingPlanet: String,
    val riskLevel: String, // Low, Moderate, High
    val ayurvedicDosha: String, // Vata, Pitta, Kapha
    val lifestyleAdvice: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroHealthScreen(
    onBack: () -> Unit
) {
    val afflictions = remember {
        listOf(
            HealthOrganAffliction("Head & Nervous System", "1st House (Lagna)", "Mars / Sun", "Low", "Pitta", "Practice daily pranayama and avoid sleep deprivation."),
            HealthOrganAffliction("Throat & Vocal Cords", "2nd House", "Venus", "Moderate", "Kapha", "Consume warm herbal water with turmeric and honey."),
            HealthOrganAffliction("Chest & Lungs", "4th House", "Moon", "Low", "Kapha", "Deep breathing exercises; avoid cold exposure during night."),
            HealthOrganAffliction("Digestive Vitality", "6th House (Roga Bhava)", "Mercury / Sun", "Moderate", "Pitta", "Eat satvik light meals; regular fasting on Ekadashi recommended."),
            HealthOrganAffliction("Lower Spine & Kidneys", "7th House", "Venus", "Low", "Vata", "Maintain optimal hydration; daily yoga stretches for lower back."),
            HealthOrganAffliction("Circulation & Longevity", "8th House (Ayur Bhava)", "Saturn", "Low", "Vata", "Chant Maha Mrityunjaya mantra; keep physical active discipline.")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Astro Health & Medical Jyotish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("D6 Shashtamsha & Organ Vulnerability", color = MysticPurple, fontSize = 11.sp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Hero Vitality Card
            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CosmicNavy, shape = RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = SacredGold, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Overall Vitality Score: 84%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Dominant Constitution: Pitta-Vata Balance", color = Color(0xFF4CAF50), fontSize = 12.sp)
                            Text("D6 Engine: No severe 6th/8th house mutual affliction.", color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Text("Anatomical Organ Forecast", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            items(afflictions) { item ->
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.organ, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (item.riskLevel == "Moderate") Color(0xFFFFA000).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (item.riskLevel == "Moderate") Color(0xFFFFA000) else Color(0xFF4CAF50))
                            ) {
                                Text(
                                    text = "${item.riskLevel} Risk",
                                    color = if (item.riskLevel == "Moderate") Color(0xFFFFA000) else Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text("Governed by: ${item.house} (${item.rulingPlanet}) • Dosha: ${item.ayurvedicDosha}", color = MysticPurple, fontSize = 12.sp)
                        Divider(color = CosmicNavy)
                        Text("Vedic Recommendation: ${item.lifestyleAdvice}", color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
