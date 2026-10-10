package com.omastro.app.ui.screens.prashna

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.remote.GeocodePlace
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.*

/**
 * Prashna Kundali (horary astrology) — Cycle 4.
 *
 * Ask a question -> POST /prashna-kundali -> chart of the question's moment
 * with a verdict, lagna/panchang/planet detail, timing and remedies.
 * Renders only keys actually present in the payload — never invented values.
 * Server emoji icons are intentionally skipped (no-emoji rule); text is kept.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrashnaScreen(
    onBack: () -> Unit,
    viewModel: PrashnaViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showForm by remember { mutableStateOf(true) }

    var question by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lon by remember { mutableStateOf<Double?>(null) }
    var questionError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("prashna_title"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
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
                        Text(W.t("prashna_loading"), color = StarlightWhite, fontSize = 13.sp)
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
                            text = W.t("prashna_no_data"),
                            color = StarlightWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: W.t("prashna_err_load"),
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.clearResult()
                                showForm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Text(W.t("prashna_try_again"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
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

                    item {
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = SacredGold,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = W.t("prashna_sub"),
                                    color = StarlightWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            W.t("prashna_question"),
                            color = SacredGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it; questionError = null },
                            placeholder = { Text(W.t("prashna_question_ph"), color = TextSecondary, fontSize = 13.sp) },
                            supportingText = questionError?.let {
                                { Text(it, color = Color(0xFFFF8A80), fontSize = 11.sp) }
                            },
                            isError = questionError != null,
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StarlightWhite,
                                unfocusedTextColor = StarlightWhite,
                                focusedBorderColor = SacredGold,
                                unfocusedBorderColor = CosmicBorder,
                                errorBorderColor = Color(0xFFFF8A80),
                                cursorColor = SacredGold
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        Text(
                            W.t("prashna_place_label"),
                            color = SacredGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    item {
                        PlaceSearchField(
                            value = place,
                            onValueChange = { place = it; lat = null; lon = null },
                            onPlaceSelected = { p: GeocodePlace ->
                                place = p.displayName
                                lat = p.lat
                                lon = p.lon
                            },
                            label = W.t("prashna_place_label"),
                            placeholder = W.t("prashna_place_ph")
                        )
                        Text(
                            text = W.t("prashna_place_note"),
                            color = StarlightWhite.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                if (question.isBlank()) {
                                    questionError = W.t("prashna_err_question")
                                    return@Button
                                }
                                showForm = false
                                viewModel.ask(question.trim(), lat, lon)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(W.t("prashna_ask"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            else -> {
                val data = result!!.data ?: emptyMap()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Verdict card (analysis_text)
                    val analysis = data["analysis_text"] as? Map<*, *>
                    if (analysis != null) {
                        item {
                            CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = W.t("prashna_verdict"),
                                        color = SacredGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    (analysis["verdict"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Text(it.uppercase(), color = StarlightWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                    }
                                    val score = (analysis["score"] as? Number)?.toInt()
                                    val max = (analysis["max"] as? Number)?.toInt()
                                    if (score != null && max != null && max > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$score / $max", color = SacredGold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    (analysis["summary"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                                    }
                                    (analysis["timing"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }

                    // Question + prashn moment meta
                    item {
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                (data["question"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                    Text("\"$it\"", color = StarlightWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                listOfNotNull(
                                    (data["prashn_time"] as? String)?.let { W.tf("prashna_time_fmt", it) },
                                    (data["location"] as? String)?.let { W.tf("prashna_location_fmt", it) }
                                ).forEach {
                                    Text(it, color = TextSecondary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.clearResult()
                                        showForm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(W.t("prashna_another"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Key factors
                    val factors = analysis?.get("factors")
                    if (factors is List<*>) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_factors")) {
                                factors.filterIsInstance<List<*>>().forEach { pair ->
                                    val text = pair.getOrNull(1)?.toString()?.takeIf { it.isNotBlank() }
                                    if (text != null) {
                                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                            Text("•  ", color = SacredGold, fontSize = 14.sp)
                                            Text(text, color = StarlightWhite, fontSize = 14.sp, lineHeight = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Lagna details
                    val lagnaInfo = data["lagna_info"] as? Map<*, *>
                    if (lagnaInfo != null && lagnaInfo.isNotEmpty()) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_lagna")) {
                                PrashnaValueRenderer(lagnaInfo)
                            }
                        }
                    }

                    // Panchang
                    val panchang = data["panchang"] as? Map<*, *>
                    if (panchang != null && panchang.isNotEmpty()) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_panchang")) {
                                PrashnaValueRenderer(panchang)
                            }
                        }
                    }

                    // Planetary positions
                    val planets = data["planets"]
                    if (planets is List<*>) {
                        item {
                            Text(
                                W.t("prashna_planets"),
                                color = SacredGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        planets.filterIsInstance<Map<*, *>>().forEach { planet ->
                            item {
                                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        val name = (planet["name"] as? String) ?: "Planet"
                                        val glyph = planetGlyph(name)
                                        val rashi = (planet["rashi"] as? String)
                                        Text(
                                            "$glyph $name" + if (rashi != null) " — $rashi" else "",
                                            color = StarlightWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        listOfNotNull(
                                            (planet["house"] as? Number)?.let { W.tf("prashna_house_fmt", it.toInt()) },
                                            (planet["deg"] as? String)?.let { W.tf("prashna_deg_fmt", it) },
                                            (planet["retro"] as? String)?.takeIf { it.isNotBlank() }?.let { W.tf("prashna_retro_fmt", it) }
                                        ).forEach {
                                            Text(it, color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // House analysis
                    val houses = data["house_analysis"]
                    if (houses is List<*> && houses.isNotEmpty()) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_houses")) {
                                houses.filterIsInstance<Map<*, *>>().forEach { h ->
                                    val house = (h["house"] as? String) ?: ""
                                    val title = (h["title"] as? String) ?: ""
                                    if (title.isNotBlank()) {
                                        Text(title, color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    (h["interpretation"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    // KP + Moon analysis
                    val kp = data["kp_analysis"] as? Map<*, *>
                    (kp?.get("interpretation") as? String)?.takeIf { it.isNotBlank() }?.let { interp ->
                        item {
                            PrashnaSectionCard(title = W.t("prashna_kp")) {
                                Text(interp, color = StarlightWhite, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                    (data["moon_analysis"] as? String)?.takeIf { it.isNotBlank() }?.let { moon ->
                        item {
                            PrashnaSectionCard(title = W.t("prashna_moon")) {
                                Text(moon, color = StarlightWhite, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                    }

                    // Planetary strength (dasha_analysis block)
                    val dasha = data["dasha_analysis"] as? Map<*, *>
                    if (dasha != null) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_strength")) {
                                (dasha["title"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                listOfNotNull(
                                    (dasha["strong"] as? String)?.takeIf { it.isNotBlank() },
                                    (dasha["weak"] as? String)?.takeIf { it.isNotBlank() },
                                    (dasha["note"] as? String)?.takeIf { it.isNotBlank() }
                                ).forEach {
                                    Text(it, color = StarlightWhite, fontSize = 13.sp, lineHeight = 19.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    // Timing
                    val timing = data["timing"] as? Map<*, *>
                    if (timing != null && timing.isNotEmpty()) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_timing")) {
                                (timing["moon_to_gains"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                val indicators = timing["indicators"]
                                if (indicators is List<*>) {
                                    indicators.filterIsInstance<String>().filter { it.isNotBlank() }.forEach {
                                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text("•  ", color = SacredGold, fontSize = 14.sp)
                                            Text(it, color = StarlightWhite, fontSize = 13.sp, lineHeight = 19.sp)
                                        }
                                    }
                                }
                                (timing["quick_period"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(it, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
                                }
                                (timing["avoid_period"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(it, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
                                }
                            }
                        }
                    }

                    // Special yogas (server emoji icons skipped — no-emoji rule)
                    val yogas = data["special_yogas"]
                    if (yogas is List<*> && yogas.isNotEmpty()) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_yogas")) {
                                yogas.filterIsInstance<Map<*, *>>().forEach { y ->
                                    (y["name"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    (y["detail"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // Remedies (server emoji icons skipped — no-emoji rule)
                    val remedies = data["remedies"]
                    if (remedies is List<*> && remedies.isNotEmpty()) {
                        item {
                            PrashnaSectionCard(title = W.t("prashna_remedies")) {
                                remedies.filterIsInstance<Map<*, *>>().forEach { r ->
                                    (r["title"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    (r["detail"] as? String)?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
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
private fun PrashnaSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Defensive renderer for unknown backend shapes — renders only what exists,
 * never invents data.
 */
@Composable
private fun PrashnaValueRenderer(value: Any?) {
    when (value) {
        null -> Text(W.t("honesty_not_available"), color = TextSecondary, fontSize = 13.sp)
        is String -> if (value.isNotBlank()) {
            Text(value, color = StarlightWhite, fontSize = 14.sp, lineHeight = 21.sp)
        }
        is Number, is Boolean -> Text(value.toString(), color = StarlightWhite, fontSize = 14.sp)
        is Map<*, *> -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                value.entries.forEach { (k, v) ->
                    if (k == null || v == null) return@forEach
                    val key = k.toString().replace("_", " ")
                        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    when (v) {
                        is String, is Number, is Boolean -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(key, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(v.toString(), color = StarlightWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            }
                        }
                        else -> {
                            Text(key, color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            PrashnaValueRenderer(v)
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
                        PrashnaValueRenderer(item)
                    }
                }
            }
        }
        else -> Text(value.toString(), color = StarlightWhite, fontSize = 13.sp)
    }
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
