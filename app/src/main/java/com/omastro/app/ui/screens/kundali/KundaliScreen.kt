package com.omastro.app.ui.screens.kundali

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    val hasData by viewModel.hasData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Janam Kundali", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    if (hasData) {
                        IconButton(onClick = { viewModel.editBirthDetails() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit birth details", tint = SacredGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SacredGold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Computing your Kundali…", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = InauspiciousRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error ?: "Something went wrong.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.clearError() },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.editBirthDetails() }) {
                            Text("Edit birth details", color = SacredGoldLight)
                        }
                    }
                }
                !hasData -> BirthDetailsForm(viewModel)
                else -> KundaliResults(viewModel)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Birth details entry form
// ---------------------------------------------------------------------------

@Composable
private fun BirthDetailsForm(viewModel: KundaliViewModel) {
    val name by viewModel.birthName.collectAsState()
    val dob by viewModel.birthDob.collectAsState()
    val tob by viewModel.birthTob.collectAsState()
    val place by viewModel.birthPlace.collectAsState()
    val lat by viewModel.birthLat.collectAsState()
    val lon by viewModel.birthLon.collectAsState()
    val formError by viewModel.formError.collectAsState()

    // Sensible editable defaults (New Delhi); the user should correct them.
    LaunchedEffect(Unit) {
        if (lat.isBlank()) viewModel.setBirthLat("28.6139")
        if (lon.isBlank()) viewModel.setBirthLon("77.2090")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Enter Birth Details",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your Kundali is computed from these details using the Lahiri ayanamsa.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BirthField(name, viewModel::setBirthName, "Full Name", "e.g. Rahul Sharma", KeyboardType.Text)
                BirthField(dob, viewModel::setBirthDob, "Date of Birth", "YYYY-MM-DD  e.g. 1995-08-15", KeyboardType.Number)
                BirthField(tob, viewModel::setBirthTob, "Time of Birth", "HH:MM 24-hour  e.g. 08:30", KeyboardType.Number)
                BirthField(place, viewModel::setBirthPlace, "Place of Birth", "e.g. Jaipur, Rajasthan", KeyboardType.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        BirthField(lat, viewModel::setBirthLat, "Latitude", "e.g. 26.9124", KeyboardType.Number)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        BirthField(lon, viewModel::setBirthLon, "Longitude", "e.g. 75.7873", KeyboardType.Number)
                    }
                }
                Text(
                    text = "Tip: find coordinates for any city in a maps app and enter them here.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        formError?.let {
            Text(text = it, color = InauspiciousRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { viewModel.submitBirthDetails() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Generate Kundali", color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BirthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = CosmicBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = SacredGoldLight,
            unfocusedLabelColor = TextSecondary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------------
// Results (7 analysis tabs)
// ---------------------------------------------------------------------------

@Composable
private fun KundaliResults(viewModel: KundaliViewModel) {
    val kundali by viewModel.kundaliData.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedDiv by viewModel.selectedDivisional.collectAsState()
    val chartStyle by viewModel.chartStyle.collectAsState()

    val tabTitles = listOf(
        "Charts", "Grahas", "Dasha", "Yogas & Doshas",
        "Ashtakavarga", "Lal Kitab & BNN", "Jaimini & Health"
    )

    Column(modifier = Modifier.fillMaxSize()) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Subject header (from the API response, never hardcoded)
            item {
                kundali?.let { k ->
                    CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
                        Column {
                            Text(k.ascendantSignName, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Nakshatra: ${k.nakshatra}", color = Color.White, fontSize = 13.sp)
                            Text("Current: ${k.currentDasha}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> chartsTab(viewModel, kundali, selectedDiv, chartStyle)
                1 -> grahasTab(kundali)
                2 -> dashaTab(kundali)
                3 -> yogasTab(kundali)
                4 -> ashtakavargaTab(kundali)
                5 -> lalKitabTab(kundali)
                6 -> jaiminiHealthTab(kundali)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private fun LazyListScope.chartsTab(
    viewModel: KundaliViewModel,
    kundali: KundaliResponse?,
    selectedDiv: String,
    chartStyle: String
) {
    // Style selector — both options genuinely render.
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("North Indian", "South Indian").forEach { style ->
                val selected = chartStyle == style
                Button(
                    onClick = { viewModel.selectChartStyle(style) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) SacredGold else CosmicCharcoal
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (style == "North Indian") "North Indian Diamond" else "South Indian Grid",
                        color = if (selected) CosmicDeepBlue else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Divisional pills backed by real API maps (D1, D9, D10).
    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val divs = listOf("D1" to "D1 Lagna", "D9" to "D9 Navamsha", "D10" to "D10 Career")
            items(divs) { (code, label) ->
                val isSelected = selectedDiv == code
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SacredGold else CosmicCharcoal,
                    modifier = Modifier.clickable { viewModel.selectDivisional(code) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) CosmicDeepBlue else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Chart render. South-Indian needs per-planet signs, which the API provides
    // only for D1 — divisionals render as the varga-relative North-Indian diamond.
    item {
        val houses = when (selectedDiv) {
            "D9" -> kundali?.d9Houses ?: emptyMap()
            "D10" -> kundali?.d10Houses ?: emptyMap()
            else -> kundali?.d1Houses ?: emptyMap()
        }
        // AND-14 fix: use the ascendant exactly as the API returns it — no invented offsets.
        val asc = kundali?.ascendantSign ?: 1
        if (chartStyle == "South Indian" && selectedDiv == "D1") {
            SouthIndianChart(
                planets = kundali?.planets ?: emptyList(),
                ascendantSign = asc,
                chartTitle = "South Indian Chart — D1"
            )
        } else {
            VedicDiamondChart(
                ascendantSign = asc,
                housesPlanets = houses,
                chartTitle = "$chartStyle — $selectedDiv Chart"
            )
        }
    }

    item {
        Text("12 Bhavas (Houses)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
    item {
        val houses = when (selectedDiv) {
            "D9" -> kundali?.d9Houses ?: emptyMap()
            "D10" -> kundali?.d10Houses ?: emptyMap()
            else -> kundali?.d1Houses ?: emptyMap()
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..6).forEach { h ->
                    HouseChip(h, houses[h.toString()] ?: emptyList(), modifier = Modifier.weight(1f))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (7..12).forEach { h ->
                    HouseChip(h, houses[h.toString()] ?: emptyList(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HouseChip(houseNumber: Int, planets: List<String>, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CosmicCharcoal,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("H$houseNumber", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(
                planets.joinToString(",").ifBlank { "—" },
                color = StarlightWhite.copy(alpha = 0.7f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun LazyListScope.grahasTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Planetary Longitudes, Dignities and Strengths")
    }
    val planets = kundali?.planets ?: emptyList()
    if (planets.isEmpty()) {
        item { EmptySection("No planetary data returned for this chart.") }
    }
    items(planets) { planet ->
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    Text("${planet.dignity}", color = TextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${String.format("%.2f", planet.degree)}°", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${planet.nakshatra} (Pada ${planet.pada})", color = MysticPurple, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun LazyListScope.dashaTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Vimshottari Dasha Timeline")
    }
    val dashaList = kundali?.dashaTimeline ?: emptyList()
    if (dashaList.isEmpty()) {
        item { EmptySection("No dasha data returned for this chart.") }
    }
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

private fun LazyListScope.yogasTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Vedic Yogas and Doshas")
    }
    val yogas = kundali?.yogas ?: emptyList()
    if (yogas.isEmpty()) {
        item { EmptySection("No yoga/dosha data returned for this chart.") }
    }
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
                    Text(yoga.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                    Text("Remedy: ${yoga.remedy}", color = RadiantAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun LazyListScope.ashtakavargaTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Sarvashtakavarga Bindu Grid")
    }
    val ashtakaList = kundali?.ashtakavarga ?: emptyList()
    if (ashtakaList.isEmpty()) {
        item { EmptySection("No ashtakavarga data returned for this chart.") }
    }
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

private fun LazyListScope.lalKitabTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Lal Kitab Remedies")
    }
    val remedies = kundali?.lalKitabRemedies ?: emptyList()
    if (remedies.isEmpty()) {
        item { EmptySection("No Lal Kitab data returned for this chart.") }
    }
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
                Text("Remedy: ${item.remedy}", color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
    item {
        SectionTitle("Bhrigu Nandi Nadi Combinations")
    }
    val bnnList = kundali?.bnnCombinations ?: emptyList()
    if (bnnList.isEmpty()) {
        item { EmptySection("No BNN data returned for this chart.") }
    }
    items(bnnList) { bnn ->
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderColor = MysticPurple.copy(alpha = 0.5f)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(bnn.title, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(bnn.lifeSignificance, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

private fun LazyListScope.jaiminiHealthTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Jaimini Karakas")
    }
    val jaiminiList = kundali?.jaiminiKarakas ?: emptyList()
    if (jaiminiList.isEmpty()) {
        item { EmptySection("No Jaimini data returned for this chart.") }
    }
    items(jaiminiList) { k ->
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(k.role, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(k.signification, color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Text(k.planet, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
    item {
        SectionTitle("Health Indicators")
    }
    val healthList = kundali?.healthIndicators ?: emptyList()
    if (healthList.isEmpty()) {
        item { EmptySection("No health data returned for this chart.") }
    }
    items(healthList) { h ->
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptySection(text: String) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}
