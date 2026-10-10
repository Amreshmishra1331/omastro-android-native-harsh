package com.omastro.app.ui.screens.astrohealth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.DatePickerField
import com.omastro.app.ui.components.TimePickerField
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.theme.*

/**
 * Astro-health analysis.
 *
 * The user enters birth details; POST /astro-health is called and the
 * backend's own response is rendered. No organ lists, percentages or dosha
 * values are invented client-side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroHealthScreen(
    onBack: () -> Unit,
    viewModel: AstroHealthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(W.t("Astro Health Analysis"), color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    if (uiState is AstroHealthUiState.Success || uiState is AstroHealthUiState.Error) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "New analysis", tint = SacredGold)
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
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is AstroHealthUiState.Idle -> AstroHealthForm(viewModel)
                is AstroHealthUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = SacredGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Analyzing planetary health indicators…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
                is AstroHealthUiState.Success -> AstroHealthResult(state.data)
                is AstroHealthUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = InauspiciousRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.message, color = StarlightWhite, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.reset() },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroHealthForm(viewModel: AstroHealthViewModel) {
    val name by viewModel.name.collectAsState()
    val dob by viewModel.dob.collectAsState()
    val tob by viewModel.tob.collectAsState()
    val place by viewModel.place.collectAsState()
    val lat by viewModel.lat.collectAsState()
    val lon by viewModel.lon.collectAsState()
    val formError by viewModel.formError.collectAsState()

    LaunchedEffect(Unit) {
        // Honesty: do NOT silently default to Delhi coordinates.
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(W.t("Vedic Health Analysis"), color = StarlightWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Planetary influences on health, computed from your birth chart.",
            color = TextSecondary, fontSize = 13.sp
        )

        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HealthField(name, viewModel::setName, "Full Name", "e.g. Rahul Sharma", KeyboardType.Text)
                DatePickerField(dob, viewModel::setDob, "Date of Birth")
                TimePickerField(tob, viewModel::setTob, "Time of Birth")
                PlaceSearchField(
                    value = place,
                    onValueChange = viewModel::setPlace,
                    onPlaceSelected = { selected ->
                        viewModel.setLat(selected.lat.toString())
                        viewModel.setLon(selected.lon.toString())
                    },
                    label = "Place of Birth",
                    placeholder = "Type city name, e.g. Jaipur"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        HealthField(lat, viewModel::setLat, "Latitude", "e.g. 26.9124", KeyboardType.Number)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        HealthField(lon, viewModel::setLon, "Longitude", "e.g. 75.7873", KeyboardType.Number)
                    }
                }
                Text(
                    text = "Select your birth city above to auto-fill coordinates, or enter them manually.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        formError?.let {
            Text(text = it, color = InauspiciousRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { viewModel.analyze() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(W.t("Analyze Health Indicators"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Text(
            text = "For guidance only — not a medical diagnosis. Consult a qualified doctor for health concerns.",
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HealthField(
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

/**
 * Renders the backend's astro-health response generically: scalar fields as
 * key/value rows, string lists as bullet points, nested objects as sub-cards.
 * Unknown shapes degrade gracefully instead of crashing or inventing data.
 */
@Composable
private fun AstroHealthResult(data: Map<String, Any?>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (key, value) ->
            AnalysisNode(key = readableKey(key), value = value)
        }
        Text(
            text = "For guidance only — not a medical diagnosis. Consult a qualified doctor for health concerns.",
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnalysisNode(key: String, value: Any?, depth: Int = 0) {
    when (value) {
        null -> Unit
        is String, is Number, is Boolean -> {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(key, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(value.toString(), color = StarlightWhite, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
        is List<*> -> {
            if (value.isEmpty()) return
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(key, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    value.forEach { item ->
                        when (item) {
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                AnalysisNode(key = "", value = item as Map<String, Any?>, depth = depth + 1)
                            }
                            else -> Text(
                                "• ${item.toString()}",
                                color = StarlightWhite,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = value as Map<String, Any?>
            if (map.isEmpty()) return
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (key.isNotBlank()) {
                        Text(key, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    map.forEach { (k, v) -> AnalysisNode(key = readableKey(k), value = v, depth = depth + 1) }
                }
            }
        }
        else -> {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Text("$key: ${value.toString()}", color = StarlightWhite, fontSize = 13.sp)
            }
        }
    }
}

private fun readableKey(key: String): String =
    key.replace('_', ' ').replaceFirstChar { it.uppercase() }
