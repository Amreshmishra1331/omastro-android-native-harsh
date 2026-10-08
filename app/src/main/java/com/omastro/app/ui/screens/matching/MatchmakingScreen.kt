package com.omastro.app.ui.screens.matching

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.GunaDetail
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchmakingScreen(
    onBack: () -> Unit,
    viewModel: MatchmakingViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kundali Milan (Ashtakoota)", color = Color.White, fontWeight = FontWeight.Bold) },
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

                // Score Badge Card
                item {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Love",
                                tint = RadiantAmber,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${result?.totalScore ?: 34.0} / 36",
                                color = SacredGold,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = result?.verdict ?: "Uttam Vivah",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${result?.percentage ?: 94}% Overall Compatibility",
                                color = MysticPurple,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Manglik Dosha Status Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CosmicCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("GROOM (VARA)", color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result?.manglikBoy ?: "Non-Manglik", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        CosmicCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("BRIDE (KANYA)", color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result?.manglikGirl ?: "Non-Manglik", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Ashtakoota Guna Breakdown
                item {
                    Text(
                        text = "8 Koota Analysis (36 Gunas)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val gunas = result?.gunas ?: emptyList()
                items(gunas) { guna ->
                    GunaRow(guna)
                }

                // Vedic Synthesis Conclusion
                item {
                    CosmicCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = SacredGold.copy(alpha = 0.5f)
                    ) {
                        Column {
                            Text("SYNTHESIS & RECOMMENDATION", color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result?.conclusion ?: "Auspicious planetary alignment ensures emotional harmony, enduring prosperity, and joyful marital union.",
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun GunaRow(guna: GunaDetail) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = guna.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = guna.description, color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Text(
                text = "${guna.obtained} / ${guna.maximum}",
                color = SacredGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
