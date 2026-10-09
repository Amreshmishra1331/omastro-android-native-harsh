package com.omastro.app.ui.screens.kundali

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.components.PlanetMark
import com.omastro.app.ui.components.VedicDiamondChart
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundaliScreen(
    onBack: () -> Unit,
    onNavigateToSavedCharts: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    viewModel: KundaliViewModel = viewModel()
) {
    val hasData by viewModel.hasData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val lastRequest by viewModel.lastRequest.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        com.omastro.app.ui.navigation.PendingChartHolder.consume()?.let { chart ->
            viewModel.loadSavedChart(chart)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Janam Kundali", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SacredGold)
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSavedCharts) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "Saved charts", tint = SacredGold)
                    }
                    if (hasData) {
                        IconButton(onClick = {
                            saveName = lastRequest?.name ?: ""
                            saveError = null
                            saveSuccess = false
                            showSaveDialog = true
                        }) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Save this chart", tint = SacredGold)
                        }
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
                else -> KundaliResults(viewModel, lastRequest?.name ?: "")
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showSaveDialog = false },
            title = { Text("Save this chart") },
            text = {
                Column {
                    Text(
                        "Give this chart a name to find it later in Saved Charts.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it; saveError = null },
                        label = { Text("Chart name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                    if (saveError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(saveError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    if (saveSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chart saved.", color = SacredGold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val req = lastRequest ?: return@TextButton
                        if (saveName.trim().isEmpty()) {
                            saveError = "Please enter a name for this chart."
                            return@TextButton
                        }
                        isSaving = true
                        saveError = null
                        scope.launch {
                            com.omastro.app.di.AppContainer.savedChartsRepository
                                .saveChart(saveName.trim(), req)
                                .onSuccess {
                                    isSaving = false
                                    saveSuccess = true
                                }
                                .onFailure { err ->
                                    isSaving = false
                                    saveError = err.message ?: "Could not save chart."
                                }
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Birth details entry form — grouped sections like the website
// ---------------------------------------------------------------------------

@Composable
private fun BirthDetailsForm(viewModel: KundaliViewModel) {
    val name by viewModel.birthName.collectAsState()
    val gender by viewModel.birthGender.collectAsState()
    val dob by viewModel.birthDob.collectAsState()
    val tob by viewModel.birthTob.collectAsState()
    val place by viewModel.birthPlace.collectAsState()
    val lat by viewModel.birthLat.collectAsState()
    val lon by viewModel.birthLon.collectAsState()
    val formError by viewModel.formError.collectAsState()

    LaunchedEffect(Unit) {
        if (lat.isBlank()) viewModel.setBirthLat("28.6139")
        if (lon.isBlank()) viewModel.setBirthLon("77.2090")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Janam Kundali",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Enter birth details exactly as on record. The chart is computed on our servers using the Lahiri ayanamsa.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        FormSectionCard(title = "Personal Details") {
            BirthField(name, viewModel::setBirthName, "Full Name", "e.g. Rahul Sharma", KeyboardType.Text)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Gender", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to "Male", "female" to "Female", "other" to "Other").forEach { (value, label) ->
                    val selected = gender == value
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) SacredGold else CosmicCharcoal,
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setBirthGender(value) }
                    ) {
                        Text(
                            text = label,
                            color = if (selected) CosmicDeepBlue else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        FormSectionCard(title = "Date & Time of Birth") {
            BirthField(dob, viewModel::setBirthDob, "Date of Birth", "YYYY-MM-DD  e.g. 1995-08-15", KeyboardType.Number)
            BirthField(tob, viewModel::setBirthTob, "Time of Birth", "HH:MM 24-hour  e.g. 08:30", KeyboardType.Number)
            Text(
                text = "Use 24-hour format. If the exact time is unknown, leave 12:00 and note it — predictions will be approximate.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        FormSectionCard(title = "Place of Birth") {
            PlaceSearchField(
                value = place,
                onValueChange = viewModel::setBirthPlace,
                onPlaceSelected = { selected ->
                    viewModel.setBirthLat(selected.lat.toString())
                    viewModel.setBirthLon(selected.lon.toString())
                },
                label = "Birth City",
                placeholder = "Type city name, e.g. Jaipur"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    BirthField(lat, viewModel::setBirthLat, "Latitude", "e.g. 26.9124", KeyboardType.Number)
                }
                Box(modifier = Modifier.weight(1f)) {
                    BirthField(lon, viewModel::setBirthLon, "Longitude", "e.g. 75.7873", KeyboardType.Number)
                }
            }
            Text(
                text = "Select your birth city above to auto-fill coordinates, or enter them manually.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        formError?.let {
            Text(
                text = it,
                color = InauspiciousRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = { viewModel.submitBirthDetails() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Generate Kundali", color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FormSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            content()
        }
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
// Results
// ---------------------------------------------------------------------------

@Composable
private fun KundaliResults(viewModel: KundaliViewModel, subjectName: String) {
    val kundali by viewModel.kundaliData.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedDiv by viewModel.selectedDivisional.collectAsState()
    val chartStyle by viewModel.chartStyle.collectAsState()

    val tabTitles = listOf("Charts", "Grahas", "Dasha", "Yogas", "Doshas", "Ashtaka", "Lal Kitab", "Analysis")

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

            item { SubjectHeader(kundali, subjectName) }

            when (selectedTab) {
                0 -> chartsTab(viewModel, kundali, selectedDiv, chartStyle)
                1 -> grahasTab(kundali)
                2 -> dashaTab(kundali)
                3 -> yogasTab(kundali)
                4 -> doshasTab(kundali)
                5 -> ashtakavargaTab(kundali)
                6 -> lalKitabTab(kundali)
                7 -> analysisTab(kundali)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SubjectHeader(kundali: KundaliResponse?, subjectName: String) {
    if (kundali == null) return
    val meta = kundali.meta
    val lagna = kundali.lagna
    CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (subjectName.isNotBlank()) {
                Text(subjectName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Text(
                text = "Lagna: ${lagna.rashi.ifBlank { meta.lagnaEn }}${if (lagna.rashiHi.isNotBlank()) " (${lagna.rashiHi})" else ""}  ${lagna.deg}°${lagna.min}'",
                color = SacredGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (meta.nakshatra.isNotBlank()) {
                    Text("✦ ${meta.nakshatra}", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
                if (meta.moonSignEn.isNotBlank()) {
                    Text(
                        "☽ ${meta.moonSignEn}${if (meta.moonSignHi.isNotBlank()) " (${meta.moonSignHi})" else ""}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val curDasha = kundali.vimshottariDasha?.currentDasha?.ifBlank { meta.curDasha } ?: meta.curDasha
            if (curDasha.isNotBlank()) {
                Text("Current Dasha: $curDasha", color = TextSecondary, fontSize = 12.sp)
            }
            if (meta.ayanamsa.isNotBlank()) {
                Text("Ayanamsa: ${meta.ayanamsa.replaceFirstChar { it.uppercase() }}", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Charts tab
// ---------------------------------------------------------------------------

private fun LazyListScope.chartsTab(
    viewModel: KundaliViewModel,
    kundali: KundaliResponse?,
    selectedDiv: String,
    chartStyle: String
) {
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

    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val divs = listOf("D1" to "D1 · Lagna", "D9" to "D9 · Navamsha", "D10" to "D10 · Dashamsha")
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

    item {
        if (kundali == null) {
            EmptySection("No chart data available.")
        } else if (chartStyle == "South Indian") {
            SouthIndianChart(
                planets = southPlanetsFor(kundali, selectedDiv),
                ascendantSign = southLagnaFor(kundali, selectedDiv),
                chartTitle = "South Indian — $selectedDiv Chart"
            )
        } else {
            val (lagnaSign, marks) = northMarksFor(kundali, selectedDiv)
            VedicDiamondChart(
                lagnaSign = lagnaSign,
                housePlanets = marks,
                chartTitle = "North Indian Diamond — $selectedDiv Chart"
            )
        }
    }

    item {
        SectionTitle("12 Bhavas (Houses)")
    }
    item {
        if (kundali == null) {
            EmptySection("No house data available.")
        } else {
            val houses = housesFor(kundali, selectedDiv)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                houses.sortedBy { it.number }.forEach { h ->
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "House ${h.number} · ${h.rashi}${if (h.rashiHi.isNotBlank()) " (${h.rashiHi})" else ""}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Lord: ${h.lord}${if (h.sav > 0) "  ·  ${h.sav} bindus" else ""}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            h.bhavBala?.takeIf { it.label.isNotBlank() }?.let { bala ->
                                Surface(
                                    color = SacredGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        bala.label,
                                        color = SacredGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Houses list for the selected divisional. */
private fun housesFor(kundali: KundaliResponse, div: String): List<HouseInfo> = when (div) {
    "D9" -> kundali.d9Houses
    "D10" -> kundali.divisionalCharts["Dashamsha"]?.houses ?: emptyList()
    else -> kundali.houses
}

/** (lagnaSign, houseNumber -> planet marks) for the North-Indian diamond. */
private fun northMarksFor(kundali: KundaliResponse, div: String): Pair<Int, Map<Int, List<PlanetMark>>> {
    return when (div) {
        "D9" -> kundali.navamshaLagnaIndex() to kundali.d9PlanetsByHouse()
            .mapValues { (_, names) -> names.map { PlanetMark(it, kundali.symbolFor(it)) } }
        "D10" -> {
            val dc = kundali.divisionalCharts["Dashamsha"]
            val lagna = dc?.let { rashiNameToIndex(it.lagnaRashi) } ?: 1
            val byHouse = dc?.houses?.associate { h ->
                h.number to h.planets.map { PlanetMark(it.name, kundali.symbolFor(it.name)) }
            } ?: emptyMap()
            lagna to byHouse
        }
        else -> kundali.lagnaSignIndex() to kundali.d1PlanetsByHouse()
            .mapValues { (_, names) -> names.map { PlanetMark(it, kundali.symbolFor(it)) } }
    }
}

/** Planet list with varga-relative rashiIndex for the South-Indian grid. */
private fun southPlanetsFor(kundali: KundaliResponse, div: String): List<ApiPlanet> {
    return when (div) {
        "D9" -> kundali.planets.map { it.copy(rashiIndex = rashiNameToIndex(it.d9Rashi.ifBlank { it.rashi })) }
        "D10" -> {
            val dc = kundali.divisionalCharts["Dashamsha"] ?: return emptyList()
            val rashiByPlanet = mutableMapOf<String, Int>()
            dc.houses.forEach { h ->
                h.planets.forEach { p -> rashiByPlanet[p.name.lowercase()] = rashiNameToIndex(h.rashi) }
            }
            kundali.planets.mapNotNull { p ->
                val idx = rashiByPlanet[p.name.lowercase()] ?: return@mapNotNull null
                p.copy(rashiIndex = idx)
            }
        }
        else -> kundali.planets
    }
}

private fun southLagnaFor(kundali: KundaliResponse, div: String): Int = when (div) {
    "D9" -> kundali.navamshaLagnaIndex()
    "D10" -> kundali.divisionalCharts["Dashamsha"]?.let { rashiNameToIndex(it.lagnaRashi) } ?: 1
    else -> kundali.lagnaSignIndex()
}

// ---------------------------------------------------------------------------
// Grahas tab — detailed planet table
// ---------------------------------------------------------------------------

private fun LazyListScope.grahasTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Graha Details — Sign, House, Degree, Nakshatra, Dignity")
    }
    val planets = kundali?.planets
        ?.filter { it.name.isNotBlank() }
        ?.sortedBy { it.house } ?: emptyList()
    if (planets.isEmpty()) {
        item { EmptySection("No planetary data returned for this chart.") }
        return
    }
    item {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .background(CosmicCharcoal, RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell("GRAHA", 150, isHeader = true)
                    TableCell("RASHI", 118, isHeader = true)
                    TableCell("HOUSE", 62, isHeader = true)
                    TableCell("DEGREE", 84, isHeader = true)
                    TableCell("NAKSHATRA", 150, isHeader = true)
                    TableCell("DIGNITY / STATUS", 190, isHeader = true)
                }
                planets.forEachIndexed { index, planet ->
                    val bg = if (index % 2 == 0) Color.Transparent else CosmicCharcoal.copy(alpha = 0.45f)
                    Row(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Graha
                        Column(modifier = Modifier.width(150.dp).padding(horizontal = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = planet.symbol.ifBlank { planetGlyphFallback(planet.name) },
                                    color = SacredGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        planet.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    val hi = planet.nameHi.ifBlank { planetHindiFallback(planet.name) }
                                    if (hi.isNotBlank()) {
                                        Text(hi, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        // Rashi
                        Column(modifier = Modifier.width(118.dp).padding(horizontal = 4.dp)) {
                            Text(planet.rashi, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (planet.rashiHi.isNotBlank()) {
                                Text(planet.rashiHi, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        // House
                        Box(modifier = Modifier.width(62.dp), contentAlignment = Alignment.Center) {
                            Text("${planet.house}", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        // Degree
                        Column(modifier = Modifier.width(84.dp).padding(horizontal = 4.dp)) {
                            Text(planet.degreeFormatted(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (planet.baladiAvastha.isNotBlank()) {
                                Text(planet.baladiAvastha.substringBefore(" ("), color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        // Nakshatra
                        Column(modifier = Modifier.width(150.dp).padding(horizontal = 4.dp)) {
                            Text(
                                "${planet.nakshatra}${if (planet.pada > 0) " (P${planet.pada})" else ""}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val sub = buildString {
                                if (planet.nakshatraLord.isNotBlank()) append("Lord: ${planet.nakshatraLord}")
                                if (planet.d9Rashi.isNotBlank()) {
                                    if (isNotEmpty()) append("  ·  ")
                                    append("D9: ${planet.d9Rashi}")
                                }
                            }
                            if (sub.isNotBlank()) {
                                Text(sub, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        // Dignity / status chips
                        Column(
                            modifier = Modifier.width(190.dp).padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val chips = planet.dignityChips()
                            if (chips.isEmpty() && planet.status.isBlank()) {
                                Text("—", color = TextSecondary, fontSize = 12.sp)
                            } else {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    chips.forEach { chip -> DignityChip(chip) }
                                }
                                if (planet.status.isNotBlank()) {
                                    Text(planet.status, color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scroll horizontally to see all columns.",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TableCell(text: String, widthDp: Int, isHeader: Boolean = false) {
    Box(modifier = Modifier.width(widthDp.dp).padding(horizontal = 4.dp)) {
        Text(
            text = text,
            color = if (isHeader) SacredGold else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DignityChip(label: String) {
    val color = when (label.lowercase()) {
        "exalted" -> Color(0xFF10B981)
        "debilitated", "combust" -> Color(0xFFEF4444)
        "own sign", "yogakaraka", "vargottama" -> SacredGold
        "retrograde" -> Color(0xFF38BDF8)
        else -> TextSecondary
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Dasha tab — current chain + expandable timeline
// ---------------------------------------------------------------------------

private fun LazyListScope.dashaTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Vimshottari Dasha")
    }
    val dasha = kundali?.vimshottariDasha
    if (dasha == null || (dasha.currentMahadasha == null && dasha.allMahadashas.isEmpty())) {
        item { EmptySection("No dasha data returned for this chart.") }
        return
    }

    // Current period hero chain: MD → AD → PD
    item {
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Current Period", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = SacredGold, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "ACTIVE",
                            color = CosmicDeepBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                dasha.currentMahadasha?.let { md ->
                    DashaChainRow("Mahadasha", md.planet, "${md.start} → ${md.end}", "${md.years} years")
                }
                dasha.currentAntardasha?.let { ad ->
                    DashaChainRow("Antardasha", ad.planet, "${ad.start} → ${ad.end}", null)
                }
                dasha.currentPratyantar?.let { pd ->
                    DashaChainRow("Pratyantardasha", pd.planet, "${pd.start} → ${pd.end}", null)
                }
            }
        }
    }

    item {
        Text(
            "Mahadasha Timeline — tap to expand antardashas",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }

    val currentMdPlanet = dasha.currentMahadasha?.planet
    items(dasha.allMahadashas) { md ->
        ExpandableDashaNode(
            node = md,
            level = 0,
            isCurrent = currentMdPlanet != null && md.planet.equals(currentMdPlanet, ignoreCase = true),
            currentAntardasha = dasha.currentAntardasha?.planet
        )
    }
}

@Composable
private fun DashaChainRow(level: String, planet: String, dates: String, duration: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(level, color = TextSecondary, fontSize = 10.sp)
            Text(
                "${planetGlyph(planet)} $planet",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(dates, color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (duration != null) {
                Text(duration, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

/** Planet glyph by English name (for dasha rows). */
private fun planetGlyph(name: String): String = planetGlyphFallback(name)

@Composable
private fun ExpandableDashaNode(
    node: DashaNode,
    level: Int,
    isCurrent: Boolean,
    currentAntardasha: String? = null
) {
    var expanded by remember { mutableStateOf(isCurrent) }
    val children = when (level) {
        0 -> node.antardasha
        1 -> node.pratyantar
        else -> node.sookshma
    }
    val levelLabel = when (level) {
        0 -> "Mahadasha"
        1 -> "Antardasha"
        else -> "Pratyantar"
    }

    Column {
        CosmicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (isCurrent) SacredGold else Color.White.copy(alpha = 0.08f)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = children.isNotEmpty()) { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            planetGlyph(node.planet),
                            color = SacredGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    node.planet,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(color = SacredGold, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "ACTIVE",
                                            color = CosmicDeepBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "$levelLabel · ${node.start} → ${node.end}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (node.years > 0) {
                            Text("${node.years}y", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (children.isNotEmpty()) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SacredGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                if (expanded && children.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    children.forEach { child ->
                        val childCurrent = currentAntardasha != null &&
                            level == 0 &&
                            child.planet.equals(currentAntardasha, ignoreCase = true)
                        Box(modifier = Modifier.padding(start = (12 + level * 8).dp)) {
                            ExpandableDashaNode(
                                node = child,
                                level = level + 1,
                                isCurrent = childCurrent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Yogas tab (auspicious combinations — separate from doshas)
// ---------------------------------------------------------------------------

private fun LazyListScope.yogasTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Yogas — Auspicious Combinations")
    }
    val yogas = kundali?.yogas ?: emptyList()
    if (yogas.isEmpty()) {
        item { EmptySection("No yoga data returned for this chart.") }
        return
    }
    items(yogas) { yoga ->
        val auspicious = yoga.isAuspicious()
        CosmicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (auspicious) SacredGold.copy(alpha = 0.45f) else InauspiciousRed.copy(alpha = 0.45f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (auspicious) SacredGold.copy(alpha = 0.16f) else InauspiciousRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            yoga.typeLabel(),
                            color = if (auspicious) SacredGold else InauspiciousRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(yoga.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp)
                yoga.description.split("|").forEach { part ->
                    val p = part.trim()
                    if (p.isNotEmpty()) {
                        Text(p, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                if (yoga.prediction.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Phal: ${yoga.prediction.substringBefore("|").trim()}",
                        color = RadiantAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Doshas tab (afflictions — separate from yogas)
// ---------------------------------------------------------------------------

private fun LazyListScope.doshasTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Doshas — Afflictions & Remedies")
    }
    val doshas = kundali?.doshas ?: emptyList()
    if (doshas.isEmpty()) {
        item {
            CosmicCard(modifier = Modifier.fillMaxWidth(), borderColor = AuspiciousGreen.copy(alpha = 0.4f)) {
                Text(
                    "No major doshas detected in this chart.",
                    color = AuspiciousGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }
    items(doshas) { dosha ->
        val sevColor = when (dosha.severity.lowercase()) {
            "critical", "high" -> InauspiciousRed
            "warning", "medium" -> Color(0xFFFF9933)
            else -> SacredGold
        }
        CosmicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = sevColor.copy(alpha = 0.45f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dosha.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (dosha.severity.isNotBlank()) {
                            Surface(color = sevColor.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    dosha.severity.replaceFirstChar { it.uppercase() },
                                    color = sevColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Surface(
                            color = if (dosha.active) InauspiciousRed.copy(alpha = 0.16f) else AuspiciousGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (dosha.active) "Active" else "Not Active",
                                color = if (dosha.active) InauspiciousRed else AuspiciousGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (dosha.detail.isNotBlank()) {
                    Text(dosha.detail, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (dosha.analysis.isNotBlank()) {
                    Text(dosha.analysis, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (dosha.prediction.isNotBlank()) {
                    Text(
                        "Phal: ${dosha.prediction}",
                        color = RadiantAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )
                }
                if (dosha.remedies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Upay (Remedies)", color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    dosha.remedies.forEachIndexed { index, remedy ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("${index + 1}. ", color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(remedy, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ashtakavarga tab — Sarvashtakavarga bindus
// ---------------------------------------------------------------------------

private fun LazyListScope.ashtakavargaTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Sarvashtakavarga — Bindu Strength")
    }
    val av = kundali?.ashtakavarga
    val rows = av?.savByHouse ?: emptyList()
    if (rows.isEmpty()) {
        item { EmptySection("No ashtakavarga data returned for this chart.") }
        return
    }
    item {
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Bindus", color = TextSecondary, fontSize = 12.sp)
                    Text("${av?.savTotal ?: 0}", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Text(
                    "Average ${(av?.savTotal ?: 0) / 12} per house",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
    items(rows.sortedBy { it.house }) { row ->
        val strength = when {
            row.points >= 30 -> "Strong" to Color(0xFF10B981)
            row.points >= 25 -> "Average" to Color(0xFFFF9933)
            else -> "Weak" to InauspiciousRed
        }
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "House ${row.house} · ${row.rashi}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(strength.first, color = strength.second, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("${row.points}", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Lal Kitab tab
// ---------------------------------------------------------------------------

private fun LazyListScope.lalKitabTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Lal Kitab")
    }
    val lk = kundali?.lalKitab
    if (lk == null || (lk.note.isBlank() && lk.yogas.isEmpty())) {
        item { EmptySection("No Lal Kitab data returned for this chart.") }
        return
    }
    if (lk.lagnaRashi.isNotBlank()) {
        item {
            CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
                Text("Lal Kitab Lagna: ${lk.lagnaRashi}", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
    if (lk.note.isNotBlank()) {
        item {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Text(lk.note, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
    items(lk.yogas) { yoga ->
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Text(yoga, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Analysis tab — rich website-style analysis from real server data
// ---------------------------------------------------------------------------

/** Life area -> houses used for its score (from server bhav-bala). */
private data class LifeAreaDef(
    val title: String,
    val hindi: String,
    val glyph: String,
    val houses: List<Int>,
    val description: String
)

private val LIFE_AREAS = listOf(
    LifeAreaDef("Career", "करियर", "♄", listOf(10, 6), "Profession, recognition and authority"),
    LifeAreaDef("Wealth", "धन", "♃", listOf(2, 11), "Income, savings and accumulated assets"),
    LifeAreaDef("Marriage", "विवाह", "♀", listOf(7), "Spouse, partnerships and marital harmony"),
    LifeAreaDef("Health", "स्वास्थ्य", "☉", listOf(1, 6), "Vitality, immunity and physical strength"),
    LifeAreaDef("Education", "विद्या", "☿", listOf(4, 5), "Learning, intellect and academic success"),
    LifeAreaDef("Children", "संतान", "♃", listOf(5), "Progeny and creative fulfilment"),
    LifeAreaDef("Spirituality", "आध्यात्म", "☊", listOf(9, 12), "Dharma, fortune and inner growth"),
    LifeAreaDef("Property", "भूमि", "♂", listOf(4), "Land, vehicles and fixed assets")
)

/** Normalize a bhav-bala score (server scale) to 0..10 for display. */
private fun balaToTen(score: Double): Double {
    // Server bhav-bala typically ranges ~0..~400+. Map generously: 120+ -> 10.
    return (score / 12.0).coerceIn(0.0, 10.0)
}

private fun scoreColor(score: Double): Color = when {
    score >= 7.0 -> AuspiciousGreen
    score >= 4.5 -> SacredGold
    else -> InauspiciousRed
}

private fun LazyListScope.analysisTab(kundali: KundaliResponse?) {
    item { SectionTitle("Life Area Scores") }
    item {
        Text(
            "Derived from server-computed Bhava Bala (house strength).",
            color = TextSecondary, fontSize = 12.sp
        )
    }
    val housesByNum = kundali?.houses?.associateBy { it.number } ?: emptyMap()
    if (housesByNum.isEmpty()) {
        item { EmptySection("House strength data not available for this chart.") }
    } else {
        val scores = LIFE_AREAS.map { area ->
            val avg = area.houses.mapNotNull { housesByNum[it]?.bhavBala?.score }
                .average().let { if (it.isNaN()) 0.0 else it }
            area to balaToTen(avg)
        }
        // 2-column grid of score cards
        val rows = scores.chunked(2)
        rows.forEach { row ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (area, score) ->
                        CosmicCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(area.glyph, fontSize = 18.sp, color = SacredGold)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            area.title, color = Color.White,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            area.hindi, color = TextSecondary, fontSize = 11.sp
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "%.1f/10".format(score),
                                        color = scoreColor(score),
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = (score / 10f).toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = scoreColor(score),
                                    trackColor = CosmicCharcoal
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(area.description, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    item { Spacer(Modifier.height(4.dp)) }
    item { SectionTitle("Planet Analysis") }
    val planets = kundali?.planets ?: emptyList()
    if (planets.isEmpty()) {
        item { EmptySection("Planet data not available.") }
    } else {
        items(planets) { planet -> PlanetAnalysisCard(planet, kundali) }
    }

    item { Spacer(Modifier.height(4.dp)) }
    item { SectionTitle("Twelve Houses") }
    val houses = kundali?.houses?.sortedBy { it.number } ?: emptyList()
    if (houses.isEmpty()) {
        item { EmptySection("House data not available.") }
    } else {
        items(houses) { house -> HouseAnalysisCard(house) }
    }

    item { Spacer(Modifier.height(4.dp)) }
    item { SectionTitle("Birth Panchang") }
    item { BirthPanchangCard(kundali) }

    item { Spacer(Modifier.height(4.dp)) }
    item { SectionTitle("Remedies (Upay)") }
    item { RemediesCard(kundali) }
}

@Composable
private fun PlanetAnalysisCard(planet: ApiPlanet, kundali: KundaliResponse?) {
    var expanded by remember { mutableStateOf(false) }
    val glyph = planet.symbol.ifBlank { planetGlyphFallback(planet.name) }
    val hindi = planet.nameHi.ifBlank { planetHindiFallback(planet.name) }
    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(glyph, fontSize = 24.sp, color = SacredGold)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${planet.name} $hindi".trim(),
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${planet.rashi} · House ${planet.house} · ${planet.degreeFormatted()}",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = SacredGold
                )
            }
            // dignity chips
            val chips = planet.dignityChips().toMutableList()
            if (planet.isRetrograde() && !chips.contains("Retrograde")) chips.add("Retrograde")
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(chips) { chip ->
                        val c = when (chip) {
                            "Exalted", "Own Sign", "Vargottama", "Yogakaraka" -> AuspiciousGreen
                            "Debilitated", "Combust" -> InauspiciousRed
                            else -> SacredGold
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = c.copy(alpha = 0.15f)
                        ) {
                            Text(
                                chip, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CosmicBorder, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                PlanetDetailRow("Nakshatra", "${planet.nakshatra} · Pada ${planet.pada} · Lord ${planet.nakshatraLord}")
                if (planet.d9Rashi.isNotBlank())
                    PlanetDetailRow("Navamsha", "${planet.d9Rashi} · House ${planet.d9House}")
                if (planet.baladiAvastha.isNotBlank())
                    PlanetDetailRow("Avastha", planet.baladiAvastha)
                if (planet.charaKaraka.isNotBlank())
                    PlanetDetailRow("Chara Karaka", planet.charaKaraka)
                if (planet.ruledHousesFmt.isNotBlank())
                    PlanetDetailRow("Rules Houses", planet.ruledHousesFmt)
                if (planet.aspects.isNotEmpty())
                    PlanetDetailRow("Aspects", planet.aspects.joinToString(", ") { "House $it" })
                Text(
                    planetInterpretation(planet),
                    color = StarlightWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanetDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "$label: ", color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(110.dp)
        )
        Text(value, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

/**
 * Honest interpretation built ONLY from the planet's real attributes —
 * dignity, house, nakshatra. No invented predictions.
 */
private fun planetInterpretation(p: ApiPlanet): String {
    val sb = StringBuilder()
    val dignity = p.dignityChips()
    when {
        dignity.contains("Exalted") -> sb.append("${p.name} is exalted in ${p.rashi}, giving its full strength in house ${p.house}. ")
        dignity.contains("Debilitated") -> sb.append("${p.name} is debilitated in ${p.rashi}; its significations need conscious strengthening. ")
        dignity.contains("Own Sign") -> sb.append("${p.name} is in its own sign ${p.rashi}, steady and reliable in house ${p.house}. ")
        else -> sb.append("${p.name} occupies ${p.rashi} in house ${p.house}. ")
    }
    if (p.isRetrograde()) sb.append("Retrograde motion turns its energy inward — review and refinement over outward action. ")
    if (p.combust) sb.append("Combust by the Sun, its independent expression is subdued. ")
    if (p.vargottama) sb.append("Vargottama (same sign in Navamsha) — results are stable and dependable. ")
    if (p.nakshatra.isNotBlank()) sb.append("In ${p.nakshatra} nakshatra (pada ${p.pada}), coloured by ${p.nakshatraLord}. ")
    if (p.charaKaraka.isNotBlank()) sb.append("As ${p.charaKaraka}, it shapes key life relationships. ")
    return sb.toString().trim()
}

@Composable
private fun HouseAnalysisCard(house: HouseInfo) {
    var expanded by remember { mutableStateOf(false) }
    val bala = house.bhavBala
    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SacredGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${house.number}", color = SacredGold,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${house.rashi} ${house.rashiHi}".trim(),
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Lord: ${house.lord}" +
                            (if (house.sav > 0) " · SAV ${house.sav}" else "") +
                            (if (bala != null && bala.label.isNotBlank()) " · ${bala.label}" else ""),
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                if (bala != null) {
                    Text(
                        "%.1f".format(balaToTen(bala.score)),
                        color = scoreColor(balaToTen(bala.score)),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = SacredGold
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (house.planets.isNotEmpty()) {
                    Text(
                        "Planets: " + house.planets.joinToString(", ") { it.name },
                        color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    houseSignificance(house.number),
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }
    }
}

/** Standard house significations (static jyotish knowledge, not per-user data). */
private fun houseSignificance(n: Int): String = when (n) {
    1 -> "Self, personality, health and overall vitality."
    2 -> "Wealth, family, speech and accumulated resources."
    3 -> "Courage, siblings, communication and short journeys."
    4 -> "Mother, home, property, vehicles and inner peace."
    5 -> "Children, intellect, creativity, romance and past-life merit."
    6 -> "Enemies, debts, disease, competition and daily work."
    7 -> "Marriage, spouse, partnerships and public dealings."
    8 -> "Longevity, transformation, inheritance and occult knowledge."
    9 -> "Fortune, dharma, guru, father and higher learning."
    10 -> "Career, karma, status, authority and public image."
    11 -> "Gains, elder siblings, friends and aspirations."
    12 -> "Expenses, foreign lands, spirituality and liberation."
    else -> ""
}

@Composable
private fun BirthPanchangCard(kundali: KundaliResponse?) {
    val birth = kundali?.birth
    val meta = kundali?.meta
    CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Birth Panchang", color = SacredGold,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            if (birth == null) {
                Text("Birth details unavailable.", color = TextSecondary, fontSize = 12.sp)
                return@Column
            }
            val dateStr = "%02d-%02d-%04d".format(birth.day, birth.month, birth.year)
            val timeStr = "%02d:%02d".format(birth.hour, birth.minute)
            PanchangRow("Date", dateStr)
            PanchangRow("Time", "$timeStr · ${birth.place}")
            if (!meta?.nakshatra.isNullOrBlank())
                PanchangRow("Nakshatra", "${meta?.nakshatra} · Pada ${meta?.nakshatraPada}")
            if (!meta?.moonSign.isNullOrBlank())
                PanchangRow("Moon Sign", meta?.moonSign ?: "")
            if (kundali?.lagna?.rashi?.isNotBlank() == true)
                PanchangRow("Lagna", kundali.lagna.formatted())
            if (!meta?.ayanamsa.isNullOrBlank())
                PanchangRow("Ayanamsa", meta?.ayanamsa?.replaceFirstChar { it.uppercase() } ?: "")
        }
    }
}

@Composable
private fun PanchangRow(label: String, value: String) {
    Row {
        Text(
            label, color = TextSecondary, fontSize = 12.sp,
            modifier = Modifier.width(100.dp)
        )
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RemediesCard(kundali: KundaliResponse?) {
    val apiRemedies = kundali?.doshas
        ?.filter { it.active }
        ?.flatMap { dosha -> dosha.remedies.map { remedy -> dosha.name to remedy } }
        ?: emptyList()
    val planetRemedies = buildPlanetRemedies(kundali)
    if (apiRemedies.isEmpty() && planetRemedies.isEmpty()) {
        EmptySection("No specific remedies indicated. General upay: chant Gayatri mantra, respect elders, feed birds.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        apiRemedies.forEach { (doshaName, remedy) ->
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        doshaName, color = InauspiciousRed,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(remedy, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
        planetRemedies.forEach { (planet, remedy) ->
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "$planet Upay", color = SacredGold,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(remedy, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

/**
 * Standard strengthening upay for afflicted planets — derived from the
 * planet's REAL dignity state in this chart, not invented.
 */
private fun buildPlanetRemedies(kundali: KundaliResponse?): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    kundali?.planets?.forEach { p ->
        val afflicted = p.dignityChips().contains("Debilitated") || p.combust
        if (!afflicted) return@forEach
        val remedy = when {
            p.name.startsWith("Saturn", ignoreCase = true) ->
                "Chant 'Om Sham Shanicharaya Namah' 108 times on Saturdays; donate black sesame or mustard oil; serve the elderly."
            p.name.startsWith("Mars", ignoreCase = true) ->
                "Chant 'Om Mangalaya Namah' on Tuesdays; donate red lentils; avoid aggression, exercise regularly."
            p.name.startsWith("Sun", ignoreCase = true) ->
                "Offer water to the rising Sun daily; chant 'Om Suryaya Namah'; respect father and authority."
            p.name.startsWith("Moon", ignoreCase = true) ->
                "Chant 'Om Chandraya Namah' on Mondays; donate white items (rice, milk); keep the mind calm through meditation."
            p.name.startsWith("Mercury", ignoreCase = true) ->
                "Chant 'Om Budhaya Namah' on Wednesdays; donate green moong; study and speak truthfully."
            p.name.startsWith("Jupiter", ignoreCase = true) ->
                "Chant 'Om Gurave Namah' on Thursdays; donate yellow items; respect teachers and gurus."
            p.name.startsWith("Venus", ignoreCase = true) ->
                "Chant 'Om Shukraya Namah' on Fridays; donate white sweets; keep surroundings clean and harmonious."
            p.name.startsWith("Rahu", ignoreCase = true) ->
                "Chant 'Om Rahave Namah' on Saturdays; donate black blankets; avoid shortcuts and intoxicants."
            p.name.startsWith("Ketu", ignoreCase = true) ->
                "Chant 'Om Ketave Namah' on Tuesdays; feed dogs; practice detachment and charity."
            else -> ""
        }
        if (remedy.isNotBlank()) out.add(p.name to remedy)
    }
    return out
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

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
