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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.*
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.DatePickerField
import com.omastro.app.ui.components.TimePickerField
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
    onNavigateToNumerology: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
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
                else -> KundaliResults(viewModel, lastRequest?.name ?: "", onNavigateToNumerology, onNavigateToAiChat)
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
            DatePickerField(
                apiDate = dob,
                onDateChange = viewModel::setBirthDob,
                label = "Date of Birth"
            )
            Spacer(modifier = Modifier.height(12.dp))
            TimePickerField(
                displayTime = tob,
                onTimeChange = viewModel::setBirthTob,
                label = "Time of Birth"
            )
            Text(
                text = "Tap the fields to pick date and time. If the exact time is unknown, leave 12:00 PM — predictions will be approximate.",
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
private fun KundaliResults(
    viewModel: KundaliViewModel,
    subjectName: String,
    onNavigateToNumerology: () -> Unit,
    onNavigateToAiChat: () -> Unit
) {
    val kundali by viewModel.kundaliData.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedDiv by viewModel.selectedDivisional.collectAsState()
    val chartStyle by viewModel.chartStyle.collectAsState()
    val personalityState by viewModel.personalityState.collectAsState()
    val bnnState by viewModel.bnnState.collectAsState()
    val lalKitabState by viewModel.lalKitabState.collectAsState()
    val jaiminiState by viewModel.jaiminiState.collectAsState()

    val tabTitles = KUNDALI_TABS

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
                            fontSize = 11.sp,
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
                0 -> birthInfoTab(kundali)
                1 -> birthChartTab(viewModel, kundali, selectedDiv, chartStyle)
                2 -> navamshaTab(kundali, chartStyle)
                3 -> karakamshaTab(jaiminiState, { viewModel.retryReport(3) }, chartStyle)
                4 -> lagnaChartTab(
                    "Chandra Lagna — Moon Chart", "The chart cast from the Moon as ascendant: mind, emotions and perception.",
                    kundali?.chandraChart, kundali, chartStyle
                )
                5 -> lagnaChartTab(
                    "Surya Lagna — Sun Chart", "The chart cast from the Sun as ascendant: soul, vitality and authority.",
                    kundali?.suryaChart, kundali, chartStyle
                )
                6 -> lagnaChartTab(
                    "Chalit — Bhava Chalit", "House cusps shift planets by their exact degrees; a planet near a cusp may move houses.",
                    kundali?.chalitChart, kundali, chartStyle
                )
                7 -> divisionalTab(kundali, viewModel, selectedDiv, chartStyle)
                8 -> dashaTab(kundali)
                9 -> ashtakavargaTab(kundali)
                10 -> sodasvargaTab(kundali)
                11 -> yogasTab(kundali)
                12 -> doshasTab(kundali)
                13 -> personalityTab(kundali, personalityState) { viewModel.retryReport(13) }
                14 -> planetsFullTab(kundali)
                15 -> panchangTab(kundali)
                16 -> shadbalTab(kundali)
                17 -> bnnTab(kundali, bnnState) { viewModel.retryReport(17) }
                18 -> lalKitabFullTab(kundali, lalKitabState) { viewModel.retryReport(18) }
                19 -> numerologyTab(kundali, onNavigateToNumerology)
                20 -> charDashaTab(jaiminiState) { viewModel.retryReport(20) }
                21 -> faladeshTab(kundali, onNavigateToAiChat)
                22 -> remediesTab(kundali)
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

internal fun LazyListScope.chartsTab(
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
// Dasha tab — current chain + expandable timeline
// ---------------------------------------------------------------------------

private fun LazyListScope.dashaTab(kundali: KundaliResponse?) {
    item {
        SectionTitle("Vimshottari Dasha — All 7 Levels")
    }
    val dasha = kundali?.vimshottariDasha
    if (dasha == null || (dasha.currentMahadasha == null && dasha.allMahadashas.isEmpty())) {
        item { EmptySection("No dasha data returned for this chart.") }
        return
    }

    // Current period hero chain: MD → AD → PD → Sookshma → Prana → Adi → Deha
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
                dasha.currentSookshma?.let { sd ->
                    DashaChainRow("Sookshmadasha", sd.planet, "${sd.start} → ${sd.end}", null)
                }
                dasha.currentPrana?.let { pr ->
                    DashaChainRow("Pranadasha", pr.planet, "${pr.start} → ${pr.end}", null)
                }
                dasha.currentAdi?.let { ad2 ->
                    DashaChainRow("Adidasha", ad2.planet, "${ad2.start} → ${ad2.end}", null)
                }
                dasha.currentDeha?.let { dh ->
                    DashaChainRow("Dehadasha", dh.planet, "${dh.start} → ${dh.end}", null)
                }
            }
        }
    }

    item {
        Text(
            "Mahadasha Timeline — tap to expand each level",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }

    val currentChain = listOf(
        dasha.currentMahadasha?.planet,
        dasha.currentAntardasha?.planet,
        dasha.currentPratyantar?.planet,
        dasha.currentSookshma?.planet,
        dasha.currentPrana?.planet,
        dasha.currentAdi?.planet,
        dasha.currentDeha?.planet
    )
    items(dasha.allMahadashas) { md ->
        ExpandableDashaNode(
            node = md,
            level = 0,
            currentChain = currentChain
        )
    }

    kundali?.analysisTabs?.dasha?.takeIf { it.isNotBlank() }?.let { text ->
        item { SectionTitle("Dasha Analysis") }
        item { KxAnalysisTextBlock(text) }
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
    currentChain: List<String?>
) {
    // Children = first non-empty sub-level (antardasha -> pratyantar -> sookshma
    // -> prana -> adi -> deha), so the tree renders to whatever depth the
    // server provides (up to 7 levels).
    val children = listOf(
        node.antardasha, node.pratyantar, node.sookshma,
        node.prana, node.adi, node.deha
    ).firstOrNull { it.isNotEmpty() } ?: emptyList()
    val levelLabel = when (level) {
        0 -> "Mahadasha"
        1 -> "Antardasha"
        2 -> "Pratyantardasha"
        3 -> "Sookshmadasha"
        4 -> "Pranadasha"
        5 -> "Adidasha"
        else -> "Dehadasha"
    }
    val isCurrent = currentChain.getOrNull(level)?.let { current ->
        node.planet.equals(current, ignoreCase = true)
    } == true
    var expanded by remember { mutableStateOf(isCurrent) }

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
                        Box(modifier = Modifier.padding(start = (12 + level * 8).dp)) {
                            ExpandableDashaNode(
                                node = child,
                                level = level + 1,
                                currentChain = currentChain
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
