package com.omastro.app.ui.screens.horoscope

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.HoroscopeSign
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoroscopeScreen(
    onBack: () -> Unit,
    viewModel: HoroscopeViewModel = viewModel()
) {
    val selectedSign by viewModel.selectedSign.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val horoscope by viewModel.horoscope.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Rashifal", color = Color.White, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sign Selector Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(viewModel.zodiacSigns) { sign ->
                    SignItem(
                        sign = sign,
                        isSelected = sign.id == selectedSign.id,
                        onClick = { viewModel.selectSign(sign) }
                    )
                }
            }

            // Period Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodChip("Today", selectedPeriod == "today", Modifier.weight(1f)) { viewModel.selectPeriod("today") }
                PeriodChip("Tomorrow", selectedPeriod == "tomorrow", Modifier.weight(1f)) { viewModel.selectPeriod("tomorrow") }
                PeriodChip("Weekly", selectedPeriod == "weekly", Modifier.weight(1f)) { viewModel.selectPeriod("weekly") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SacredGold)
                }
            } else if (errorMessage != null || horoscope == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Horoscope Unavailable",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "No horoscope data available for this selection.",
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Text("Retry", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val h = horoscope!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sign Overview Header
                    item {
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedSign.symbol, fontSize = 40.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "${selectedSign.name} (${selectedSign.hindiName})",
                                        color = SacredGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${selectedSign.dates} • ${selectedSign.element} Sign",
                                        color = StarlightWhite.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Astrological Prediction
                    item {
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "COSMIC GUIDANCE",
                                    color = SacredGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = h.prediction,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    // Life Area Scores (Love, Career, Health)
                    item {
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "ENERGY SPECTRUM",
                                    color = SacredGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                MetricBar(label = "Love & Relationships", score = h.loveScore, color = Color(0xFFFF5252))
                                MetricBar(label = "Career & Wealth", score = h.careerScore, color = RadiantAmber)
                                MetricBar(label = "Health & Vitality", score = h.healthScore, color = Color(0xFF4CAF50))
                            }
                        }
                    }

                    // Lucky Factors & Vedic Remedy
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CosmicCard(modifier = Modifier.weight(1f)) {
                                Column {
                                    Text("LUCKY NUMBER", color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${h.luckyNumber}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            CosmicCard(modifier = Modifier.weight(1f)) {
                                Column {
                                    Text("LUCKY COLOR", color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(h.luckyColor, color = RadiantAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        CosmicCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MysticPurple.copy(alpha = 0.5f)
                        ) {
                            Column {
                                Text("VEDIC REMEDY (UPAY)", color = MysticPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = h.remedy,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SignItem(sign: HoroscopeSign, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SacredGold else CosmicCharcoal,
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = sign.symbol, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sign.hindiName,
                color = if (isSelected) CosmicDeepBlue else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PeriodChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SacredGold else CosmicCharcoal,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) CosmicDeepBlue else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MetricBar(label: String, score: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 12.sp)
            Text(text = "$score%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CosmicCharcoal
        )
    }
}
