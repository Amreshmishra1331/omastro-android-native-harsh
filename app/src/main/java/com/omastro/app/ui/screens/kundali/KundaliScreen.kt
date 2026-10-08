package com.omastro.app.ui.screens.kundali

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.VedicDiamondChart
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundaliScreen(
    onBack: () -> Unit,
    viewModel: KundaliViewModel = viewModel()
) {
    val kundali by viewModel.kundaliData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = D1 Chart, 1 = D9 Navamsha, 2 = Graha Table

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Janam Kundali", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.fetchKundali("Astro Seeker", "1995-10-15", "08:30", "New Delhi")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chart Selection Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CosmicCharcoal,
                contentColor = SacredGold
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Lagna (D1)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Navamsha (D9)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Planetary Positions", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SacredGold)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Error loading chart", color = Color.Red, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item {
                                VedicDiamondChart(
                                    ascendantSign = kundali?.ascendantSign ?: 1,
                                    housesPlanets = kundali?.d1Houses ?: mapOf("1" to listOf("Su", "Me"), "4" to listOf("Mo"), "10" to listOf("Ju")),
                                    chartTitle = "Lagna Kundali (D1 Birth Chart)"
                                )
                            }
                            item {
                                ChartSummaryCard(kundali?.ascendantSignName ?: "Aries", kundali?.nakshatra ?: "Ashwini")
                            }
                        }
                        1 -> {
                            item {
                                VedicDiamondChart(
                                    ascendantSign = ((kundali?.ascendantSign ?: 1) + 4) % 12 + 1,
                                    housesPlanets = kundali?.d9Houses ?: mapOf("1" to listOf("Ve"), "7" to listOf("Ma", "Sa"), "9" to listOf("Ju")),
                                    chartTitle = "Navamsha Kundali (D9 Marriage/Destiny Chart)"
                                )
                            }
                        }
                        2 -> {
                            item {
                                Text(
                                    text = "Planetary Dignities & Longitudes",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val planets = kundali?.planets ?: listOf()
                            if (planets.isEmpty()) {
                                item {
                                    DefaultPlanetTable()
                                }
                            } else {
                                items(planets) { planet ->
                                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = planet.name, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text(text = "Rashi: ${planet.sign} (${planet.signNumber})", color = Color.White, fontSize = 13.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "${String.format("%.2f", planet.degree)}°", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(text = "${planet.nakshatra} (Pada ${planet.pada})", color = MysticPurple, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartSummaryCard(ascendant: String, nakshatra: String) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("CHART ESSENCE", color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lagna (Ascendant):", color = StarlightWhite.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(ascendant, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Janma Nakshatra:", color = StarlightWhite.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(nakshatra, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DefaultPlanetTable() {
    val samplePlanets = listOf(
        Triple("Sun (Surya)", "Libra", "28.4°"),
        Triple("Moon (Chandra)", "Cancer", "14.2°"),
        Triple("Mars (Mangal)", "Scorpio", "05.1°"),
        Triple("Mercury (Budh)", "Virgo", "19.8°"),
        Triple("Jupiter (Guru)", "Sagittarius", "11.3°"),
        Triple("Venus (Shukra)", "Libra", "02.7°"),
        Triple("Saturn (Shani)", "Aquarius", "24.6°"),
        Triple("Rahu", "Pisces", "18.0°"),
        Triple("Ketu", "Virgo", "18.0°")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        samplePlanets.forEach { (name, rashi, deg) ->
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = name, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = rashi, color = Color.White, fontSize = 13.sp)
                    Text(text = deg, color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
