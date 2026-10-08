package com.omastro.app.ui.screens.kundali

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.omastro.app.data.model.*
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
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedDiv by viewModel.selectedDivisional.collectAsState()
    val chartStyle by viewModel.chartStyle.collectAsState()

    var selectedHouseNumber by remember { mutableStateOf<Int?>(null) }

    val tabTitles = listOf(
        "Charts",
        "Grahas",
        "Dasha",
        "Yogas & Doshas",
        "Ashtakavarga",
        "Lal Kitab & BNN",
        "Jaimini & Health"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Janam Kundali Workbench", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.fetchKundali("Arjun Sharma", "1995-10-15", "08:30", "New Delhi")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculate", tint = SacredGold)
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
            // Horizontal Scrollable Luxury Analysis Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CosmicCharcoal,
                contentColor = SacredGold,
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedTab == index) SacredGold else StarlightWhite.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    when (selectedTab) {
                        0 -> {
                            // ── TAB 1: CHARTS WORKBENCH ──
                            item {
                                // Style Selector: North vs South
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.selectChartStyle("North Indian") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (chartStyle == "North Indian") SacredGold else CosmicCharcoal
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            "North Indian Diamond",
                                            color = if (chartStyle == "North Indian") CosmicDeepBlue else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.selectChartStyle("South Indian") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (chartStyle == "South Indian") SacredGold else CosmicCharcoal
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            "South Indian Grid",
                                            color = if (chartStyle == "South Indian") CosmicDeepBlue else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Divisional Chart Pills (D1, D9, D10, D7, D60)
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val divs = listOf("D1 Lagna", "D9 Navamsha", "D10 Career", "D7 Children", "D60 Destiny")
                                    items(divs) { div ->
                                        val isSelected = selectedDiv.startsWith(div.take(3))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) SacredGold else CosmicCharcoal,
                                            modifier = Modifier.clickable { viewModel.selectDivisional(div.take(3)) }
                                        ) {
                                            Text(
                                                text = div,
                                                color = if (isSelected) CosmicDeepBlue else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Vedic Diamond Chart Rendered
                            item {
                                val houses = if (selectedDiv.startsWith("D9")) {
                                    kundali?.d9Houses ?: emptyMap()
                                } else {
                                    kundali?.d1Houses ?: mapOf("1" to listOf("Su", "Ma"), "2" to listOf("Mo"), "10" to listOf("Ju"))
                                }
                                VedicDiamondChart(
                                    ascendantSign = if (selectedDiv.startsWith("D9")) ((kundali?.ascendantSign ?: 1) + 4) % 12 + 1 else (kundali?.ascendantSign ?: 1),
                                    housesPlanets = houses,
                                    chartTitle = "$chartStyle — $selectedDiv Chart"
                                )
                            }

                            // 12 Bhavas Quick Touch Explorer
                            item {
                                Text("12 Bhavas (Houses) Explorer", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (1..6).forEach { h ->
                                        HouseQuickChip(h, modifier = Modifier.weight(1f)) { selectedHouseNumber = h }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (7..12).forEach { h ->
                                        HouseQuickChip(h, modifier = Modifier.weight(1f)) { selectedHouseNumber = h }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ── TAB 2: GRAHAS & PLANETARY STRENGTHS ──
                            item {
                                Text("Planetary Longitudes, Dignities & Strengths", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            val planets = kundali?.planets ?: emptyList()
                            items(planets) { planet ->
                                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(planet.name, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                if (planet.isRetrograde) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(color = Color(0xFFFF5252), shape = RoundedCornerShape(6.dp)) {
                                                        Text("R", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                }
                                            }
                                            Text("Rashi: ${planet.sign} (House ${planet.house})", color = Color.White, fontSize = 12.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${String.format("%.2f", planet.degree)}°", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${planet.nakshatra} (Pada ${planet.pada})", color = MysticPurple, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ── TAB 3: VIMSHOTTARI DASHA ──
                            item {
                                Text("Vimshottari Dasha Timeline", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            val dashaList = kundali?.dashaTimeline ?: emptyList()
                            items(dashaList) { dasha ->
                                CosmicCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = if (dasha.isCurrent) SacredGold else Color.White.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(dasha.lord, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (dasha.isCurrent) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(color = SacredGold, shape = RoundedCornerShape(6.dp)) {
                                                        Text("ACTIVE", color = CosmicDeepBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                    }
                                                }
                                            }
                                            Text("${dasha.durationYears} Years Period", color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                        Text("${dasha.startDate} - ${dasha.endDate}", color = SacredGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // ── TAB 4: YOGAS & DOSHAS ──
                            item {
                                Text("Vedic Yogas & Doshas Analysis", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            val yogas = kundali?.yogas ?: emptyList()
                            items(yogas) { yoga ->
                                CosmicCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = if (yoga.isAuspicious) SacredGold.copy(alpha = 0.5f) else Color(0xFFFF5252).copy(alpha = 0.5f)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(yoga.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Surface(
                                                color = if (yoga.isAuspicious) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    yoga.type,
                                                    color = if (yoga.isAuspicious) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(yoga.description, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp)
                                        if (yoga.remedy != null) {
                                            Text("🕉️ Upay: ${yoga.remedy}", color = RadiantAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // ── TAB 5: ASHTAKAVARGA ──
                            item {
                                Text("Sarvashtakavarga (337 Bindus Grid)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            val ashtakaList = kundali?.ashtakavarga ?: emptyList()
                            items(ashtakaList) { item ->
                                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("House ${item.house} (${item.sign})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Strength: ${item.strength}", color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                        Text("${item.totalBindus} Bindus", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        5 -> {
                            // ── TAB 6: LAL KITAB & BNN ──
                            item {
                                Text("Lal Kitab Teva & Remedies", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            val remedies = kundali?.lalKitabRemedies ?: emptyList()
                            items(remedies) { item ->
                                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${item.planet} in House ${item.house}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(item.indication, color = SacredGold, fontSize = 11.sp)
                                        }
                                        Text("Upay: ${item.remedy}", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                }
                            }
                            item {
                                Text("Bhrigu Nandi Nadi (BNN) Planetary Alignments", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            val bnnList = kundali?.bnnCombinations ?: emptyList()
                            items(bnnList) { bnn ->
                                CosmicCard(modifier = Modifier.fillMaxWidth(), borderColor = MysticPurple.copy(alpha = 0.5f)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(bnn.title, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(bnn.lifeSignificance, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        6 -> {
                            // ── TAB 7: JAIMINI & HEALTH ──
                            item {
                                Text("Jaimini Karakas (Soul & Destiny Significations)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            val jaiminiList = kundali?.jaiminiKarakas ?: emptyList()
                            items(jaiminiList) { k ->
                                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(k.role, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(k.signification, color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                        Text(k.planet, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                            item {
                                Text("Medical Astrology Vitality Assessment", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            val healthList = kundali?.healthIndicators ?: emptyList()
                            items(healthList) { h ->
                                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(h.bodySystem, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(h.astrologicalFactor, color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                        Text(
                                            h.status,
                                            color = if (h.status == "Strong") Color(0xFF4CAF50) else Color(0xFFFF9933),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HouseQuickChip(houseNumber: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CosmicCharcoal,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text("H$houseNumber", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

