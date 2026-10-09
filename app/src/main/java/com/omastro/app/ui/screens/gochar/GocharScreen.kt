package com.omastro.app.ui.screens.gochar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.remote.GeocodePlace
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.theme.*

/**
 * Gochar (planetary transits) screen.
 *
 * Birth details + transit date -> POST /gochar -> transit analysis.
 * The backend payload shape is rich; the screen renders only keys that are
 * actually present in the response and never invents values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GocharScreen(
    onBack: () -> Unit,
    viewModel: GocharViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showForm by remember { mutableStateOf(true) }

    // Birth detail inputs
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var tob by remember { mutableStateOf("") }
    var pob by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var transitDate by remember { mutableStateOf(viewModel.todayString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gochar (Planetary Transits)", color = Color.White, fontWeight = FontWeight.Bold) },
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
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SacredGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Calculating transits...", color = StarlightWhite, fontSize = 13.sp)
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Transit Analysis Unavailable",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "An unexpected error occurred.",
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.clearResult()
                                showForm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Text("Try Again", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            result == null || showForm -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    if (result == null) {
                        item {
                            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Analyze planetary transits for any date",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Enter birth details and pick a transit date to see how current planetary positions interact with your natal chart.",
                                        color = StarlightWhite.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "BIRTH DETAILS",
                            color = SacredGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    item { GocharInputField("Full Name", name, { name = it }) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GocharInputField("Date of Birth (YYYY-MM-DD)", dob, { dob = it }, Modifier.weight(1f))
                            GocharInputField("Time (HH:MM)", tob, { tob = it }, Modifier.weight(1f))
                        }
                    }
                    item {
                        PlaceSearchField(
                            value = pob,
                            onValueChange = { pob = it },
                            onPlaceSelected = { place: GeocodePlace ->
                                pob = place.displayName
                                lat = place.lat.toString()
                                lon = place.lon.toString()
                            },
                            label = "Place of Birth",
                            placeholder = "Type city name, e.g. Jaipur"
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GocharInputField("Latitude", lat, { lat = it }, Modifier.weight(1f))
                            GocharInputField("Longitude", lon, { lon = it }, Modifier.weight(1f))
                        }
                    }
                    item {
                        Text(
                            "Gender",
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("male", "female", "other").forEach { option ->
                                FilterChip(
                                    selected = gender == option,
                                    onClick = { gender = option },
                                    label = {
                                        Text(
                                            option.replaceFirstChar { it.uppercase() },
                                            color = if (gender == option) CosmicDeepBlue else StarlightWhite
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        containerColor = CosmicCardBg
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "TRANSIT DATE",
                            color = SacredGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    item {
                        GocharInputField(
                            "Transit Date (YYYY-MM-DD)",
                            transitDate,
                            { transitDate = it },
                            supportingText = "The date for which planetary transits are computed"
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && dob.isNotBlank() && transitDate.isNotBlank()) {
                                    showForm = false
                                    viewModel.calculate(
                                        name = name.trim(),
                                        dob = dob.trim(),
                                        tob = tob.trim().ifBlank { "08:30" },
                                        pob = pob.trim().ifBlank { "New Delhi" },
                                        lat = lat.toDoubleOrNull() ?: 28.6139,
                                        lon = lon.toDoubleOrNull() ?: 77.2090,
                                        gender = gender,
                                        transitDate = transitDate.trim()
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Calculate Transits", color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            else -> {
                // Results
                val data = result!!.data ?: emptyMap()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    item {
                        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    "Gochar Analysis",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Transit date: $transitDate",
                                    color = SacredGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.clearResult()
                                        showForm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Analyze Another Date", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Sade Sati section (if present)
                    val sadeSati = data["sade_sati"] ?: data["sadesati"]
                    if (sadeSati != null) {
                        item {
                            GocharSectionCard(title = "Sade Sati (Saturn Transit)") {
                                GocharValueRenderer(sadeSati)
                            }
                        }
                    }

                    // Planetary transits list (if present)
                    val planets = data["planets"] ?: data["transits"] ?: data["graha_transits"]
                    if (planets is List<*>) {
                        item {
                            Text(
                                "PLANETARY TRANSITS",
                                color = SacredGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        planets.filterIsInstance<Map<*, *>>().forEach { planet ->
                            item {
                                GocharSectionCard(title = planetTitle(planet)) {
                                    GocharValueRenderer(planet)
                                }
                            }
                        }
                    }

                    // Timeline (if present)
                    val timeline = data["timeline"]
                    if (timeline is List<*>) {
                        item {
                            GocharSectionCard(title = "Transit Timeline") {
                                timeline.filterIsInstance<Map<*, *>>().forEach { entry ->
                                    GocharValueRenderer(entry)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // Summary / analysis prose (if present)
                    val summary = data["summary"] ?: data["analysis"] ?: data["overview"]
                    if (summary is String && summary.isNotBlank()) {
                        item {
                            GocharSectionCard(title = "Summary") {
                                Text(summary, color = StarlightWhite, fontSize = 14.sp, lineHeight = 22.sp)
                            }
                        }
                    }

                    // Fallback: render remaining top-level scalar entries
                    val renderedKeys = setOf("sade_sati", "sadesati", "planets", "transits", "graha_transits", "timeline", "summary", "analysis", "overview")
                    val remaining = data.filterKeys { it !in renderedKeys }
                    if (remaining.isNotEmpty()) {
                        item {
                            GocharSectionCard(title = "Transit Details") {
                                GocharValueRenderer(remaining)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/** Section card with a gold title header. */
@Composable
private fun GocharSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                title,
                color = SacredGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Defensive renderer for unknown backend shapes.
 * Renders strings/numbers as key-value rows, nested maps recursively,
 * and lists as bullet rows. Renders only what exists — never invents data.
 */
@Composable
private fun GocharValueRenderer(value: Any?, indent: Int = 0) {
    when (value) {
        null -> Text("Not available", color = TextSecondary, fontSize = 13.sp)
        is String -> {
            if (value.isNotBlank()) {
                Text(value, color = StarlightWhite, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        is Number, is Boolean -> {
            Text(value.toString(), color = StarlightWhite, fontSize = 14.sp)
        }
        is Map<*, *> -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                value.entries.forEach { (k, v) ->
                    if (k == null || v == null) return@forEach
                    val key = prettyKey(k.toString())
                    when (v) {
                        is String, is Number, is Boolean -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    key,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    v.toString(),
                                    color = StarlightWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        else -> {
                            Text(key, color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            GocharValueRenderer(v, indent + 1)
                        }
                    }
                }
            }
        }
        is List<*> -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                value.forEach { item ->
                    Row {
                        Text("•  ", color = SacredGold, fontSize = 14.sp)
                        GocharValueRenderer(item)
                    }
                }
            }
        }
        else -> Text(value.toString(), color = StarlightWhite, fontSize = 13.sp)
    }
}

/** Best-effort planet title from a transit entry map. */
private fun planetTitle(planet: Map<*, *>): String {
    val name = (planet["planet"] ?: planet["graha"] ?: planet["name"])?.toString() ?: "Planet"
    val glyph = planetGlyph(name)
    val sign = (planet["sign"] ?: planet["rashi"])?.toString()
    return if (sign != null) "$glyph $name — $sign" else "$glyph $name"
}

/** Planet glyph lookup (Jyotish symbols, no emojis). */
private fun planetGlyph(name: String): String = when (name.lowercase()) {
    "sun", "surya" -> "☉"
    "moon", "chandra" -> "☽"
    "mars", "mangal" -> "♂"
    "mercury", "budh" -> "☿"
    "jupiter", "guru" -> "♃"
    "venus", "shukra" -> "♀"
    "saturn", "shani" -> "♄"
    "rahu" -> "☊"
    "ketu" -> "☋"
    else -> "✦"
}

private fun prettyKey(raw: String): String =
    raw.replace("_", " ")
        .split(" ")
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

@Composable
private fun GocharInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        supportingText = supportingText?.let { { Text(it, color = TextSecondary, fontSize = 11.sp) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = StarlightWhite,
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = CosmicBorder,
            cursorColor = SacredGold
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
