package com.omastro.app.ui.screens.panchang

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanchangScreen(
    onBack: () -> Unit,
    viewModel: PanchangViewModel = viewModel()
) {
    val panchang by viewModel.panchang.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Panchang & Muhurat", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPanchang() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SacredGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Solar / Lunar Timing Bar
                item {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TimingColumn("Sunrise", panchang?.sunrise ?: "06:18 AM")
                            TimingColumn("Sunset", panchang?.sunset ?: "06:22 PM")
                            TimingColumn("Moonrise", panchang?.moonrise ?: "08:45 PM")
                        }
                    }
                }

                // Core 5 Limbs (Pancha-Anga)
                item {
                    Text("Pancha Anga (Five Limbs)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    PanchangDetailRow("Tithi (Lunar Day)", panchang?.tithi ?: "Shukla Paksha Ekadashi")
                }
                item {
                    PanchangDetailRow("Nakshatra (Constellation)", panchang?.nakshatra ?: "Rohini")
                }
                item {
                    PanchangDetailRow("Yoga (Luni-Solar Day)", panchang?.yoga ?: "Siddhi")
                }
                item {
                    PanchangDetailRow("Karana (Half Tithi)", panchang?.karana ?: "Bava")
                }
                item {
                    PanchangDetailRow("Vaar (Day)", panchang?.day ?: "Brihaspativar (Thursday)")
                }

                // Shubh Muhurat vs Ashubh Kaal
                item {
                    Text("Auspicious & Inauspicious Muhurat", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    CosmicCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderGold = true
                    ) {
                        Column {
                            Text("SHUBH MUHURAT (AUSPICIOUS)", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Abhijit Muhurat:", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                                Text(panchang?.abhijitMuhurat ?: "11:51 AM - 12:38 PM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amrit Kaal:", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                                Text(panchang?.amritKaal ?: "02:15 PM - 03:45 PM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    CosmicCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderGold = false
                    ) {
                        Column {
                            Text("ASHUBH KAAL (TO AVOID)", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rahu Kaal:", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                                Text(panchang?.rahuKaal ?: "01:30 PM - 03:00 PM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Yamaganda:", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                                Text(panchang?.yamaganda ?: "06:00 AM - 07:30 AM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gulika Kaal:", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                                Text(panchang?.gulikaKaal ?: "09:00 AM - 10:30 AM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TimingColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = StarlightWhite.copy(alpha = 0.7f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun PanchangDetailRow(title: String, value: String) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(text = value, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
