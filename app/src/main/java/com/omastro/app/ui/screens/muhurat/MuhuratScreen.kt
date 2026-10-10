package com.omastro.app.ui.screens.muhurat

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.MUHURAT_EVENTS
import com.omastro.app.data.model.MuhuratDay
import com.omastro.app.data.model.NAKSHATRA_NAMES
import com.omastro.app.data.model.VARA_NAMES
import com.omastro.app.data.model.VARA_NAMES_SHORT
import com.omastro.app.data.model.YOGA_NAMES
import com.omastro.app.data.model.muhuratDayTithiName
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.GradientButton
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.theme.AuspiciousGreen
import com.omastro.app.ui.theme.CosmicBackground
import com.omastro.app.ui.theme.InauspiciousRed
import com.omastro.app.ui.theme.NeutralBlue
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.TextPrimary
import com.omastro.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuhuratScreen(
    onBack: () -> Unit,
    viewModel: MuhuratViewModel = viewModel()
) {
    val event by viewModel.event.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val daysAhead by viewModel.daysAhead.collectAsState()
    val place by viewModel.place.collectAsState()
    val muhurat by viewModel.muhurat.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    fun openDatePicker() {
        val (y, m, d) = MuhuratViewModel.datePickerParts(startDate)
        DatePickerDialog(context, { _, year, month, day ->
            viewModel.setStartDate(
                String.format("%04d-%02d-%02d", year, month + 1, day)
            )
        }, y, m, d).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("Shubh Muhurat"), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBackground)
            )
        },
        containerColor = CosmicBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Find auspicious dates for important life events, computed from classical muhurta rules.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Event selector
            item {
                Text(W.t("Select Event"), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MUHURAT_EVENTS) { ev ->
                        FilterChip(
                            selected = event == ev.key,
                            onClick = { viewModel.setEvent(ev.key) },
                            label = { Text(ev.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold,
                                selectedLabelColor = CosmicBackground,
                                containerColor = Color.Transparent,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = event == ev.key,
                                borderColor = SacredGold.copy(alpha = 0.5f),
                                selectedBorderColor = SacredGold
                            )
                        )
                    }
                }
                val selectedEvent = MUHURAT_EVENTS.firstOrNull { it.key == event }
                if (selectedEvent != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(selectedEvent.subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Search form
            item {
                CosmicCard(borderGold = true) {
                    // Start date
                    Text(W.t("Start Date"), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SacredGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { openDatePicker() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SacredGold)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            MuhuratViewModel.displayDate(startDate),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Days ahead
                    Text(W.t("Search Window"), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 15, 30, 60, 90).forEach { days ->
                            FilterChip(
                                selected = daysAhead == days,
                                onClick = { viewModel.setDaysAhead(days) },
                                label = { Text("$days days") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGold,
                                    selectedLabelColor = CosmicBackground,
                                    containerColor = Color.Transparent,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = daysAhead == days,
                                    borderColor = SacredGold.copy(alpha = 0.5f),
                                    selectedBorderColor = SacredGold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Location
                    Text(W.t("Location"), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    PlaceSearchField(
                        value = place,
                        onValueChange = { /* selection drives value */ },
                        onPlaceSelected = { geo ->
                            viewModel.setPlace(geo.displayName, geo.lat, geo.lon)
                        },
                        label = "Location",
                        placeholder = "e.g. New Delhi"
                    )

                    Spacer(Modifier.height(18.dp))

                    GradientButton(
                        text = "Find Muhurat",
                        onClick = { viewModel.findMuhurat() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Loading
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SacredGold)
                            Spacer(Modifier.height(12.dp))
                            Text("Computing auspicious dates...", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Error
            errorMessage?.let { msg ->
                item {
                    CosmicCard(borderColor = InauspiciousRed.copy(alpha = 0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = InauspiciousRed)
                            Spacer(Modifier.width(10.dp))
                            Text(msg, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        GradientButton(
                            text = "Try Again",
                            onClick = { viewModel.findMuhurat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Results
            muhurat?.let { data ->
                val sorted = data.coreData.sortedByDescending { it.rating }
                val excellent = sorted.count { it.verdict == "excellent" }
                val good = sorted.count { it.verdict == "good" }

                item {
                    CosmicCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuspiciousGreen)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "${sorted.size} days analyzed",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "$excellent excellent · $good good",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                if (sorted.isEmpty()) {
                    item {
                        CosmicCard {
                            Text(
                                "No suitable dates found in this window. Try a longer search window or a different start date.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(sorted) { day ->
                    MuhuratDayCard(day = day)
                }

                if (!data.analysisText.isNullOrBlank()) {
                    item {
                        CosmicCard {
                            Text("Analysis", color = SacredGold, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(data.analysisText, color = TextPrimary, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }

            // Initial empty state
            if (!isLoading && muhurat == null && errorMessage == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = SacredGold.copy(alpha = 0.6f),
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                "Select an event and tap Find Muhurat",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MuhuratDayCard(day: MuhuratDay) {
    val verdictColor = when (day.verdict.lowercase()) {
        "excellent" -> AuspiciousGreen
        "good" -> SacredGold
        else -> NeutralBlue
    }
    val verdictLabel = day.verdict.replaceFirstChar { it.uppercase() }

    CosmicCard(borderColor = verdictColor.copy(alpha = 0.35f)) {
        // Header: date + verdict badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    MuhuratViewModel.displayDate(day.date),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    VARA_NAMES.getOrElse(day.vara) { "" },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(verdictColor.copy(alpha = 0.15f))
                    .border(1.dp, verdictColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(verdictLabel, color = verdictColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Rating bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(W.t("Auspiciousness"), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${day.score}/${day.maxScore}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (day.rating / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = verdictColor,
            trackColor = verdictColor.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(12.dp))

        // Panchang details
        MuhuratDetailRow("Tithi", muhuratDayTithiName(day), day.checks?.tithi?.ok)
        MuhuratDetailRow(
            "Nakshatra",
            NAKSHATRA_NAMES.getOrElse(day.nakshatra) { "Nakshatra ${day.nakshatra}" },
            day.checks?.nakshatra?.ok
        )
        MuhuratDetailRow("Yoga", YOGA_NAMES.getOrElse(day.yoga) { "Yoga ${day.yoga}" }, null)
        MuhuratDetailRow(
            "Vara",
            "${VARA_NAMES_SHORT.getOrElse(day.vara) { "" }} · ${VARA_NAMES.getOrElse(day.vara) { "" }}",
            day.checks?.vara?.ok
        )
    }
}

@Composable
private fun MuhuratDetailRow(label: String, value: String, passed: Boolean?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (passed != null) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (passed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (passed) AuspiciousGreen else InauspiciousRed,
                    modifier = Modifier.padding(0.dp)
                )
            }
        }
    }
}
