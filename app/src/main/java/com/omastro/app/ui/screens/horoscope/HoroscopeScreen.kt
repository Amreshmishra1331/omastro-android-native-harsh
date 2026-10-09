package com.omastro.app.ui.screens.horoscope

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.HoroscopeSign
import com.omastro.app.data.model.RashifalArea
import com.omastro.app.data.model.RashifalData
import com.omastro.app.data.model.SaturnTransit
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

private val MaroonBanner = Color(0xFF5C0B0B)
private val MaroonBannerLight = Color(0xFF7A1515)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoroscopeScreen(
    onBack: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
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
                title = { Text("Rashifal", color = StarlightWhite, fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        if (selectedSign == null) {
            // ---- STAGE 1: sign selector -------------------------------------
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item { RashifalBanner() }
                item {
                    Text(
                        text = "Select Your Sign",
                        color = SacredGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(560.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(viewModel.zodiacSigns) { sign ->
                            SignGridCard(
                                sign = sign,
                                isSelected = false,
                                onClick = { viewModel.selectSign(sign) }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } else {
            val sign = selectedSign!!
            // ---- STAGE 2: detail panel + periods + results ------------------
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Detail panel: "♌ Leo / Lord: Sun" + Change
                item {
                    CosmicCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = SacredGold.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(sign.glyphColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = sign.symbol, fontSize = 26.sp, color = StarlightWhite)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${sign.symbol} ${sign.name}",
                                        color = StarlightWhite,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Lord: ${sign.ruler}",
                                        color = StarlightWhite.copy(alpha = 0.65f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearSign() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f))
                            ) {
                                Text("Change", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Period tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.periods.forEach { period ->
                            val label = viewModel.periodLabels[period] ?: period
                            PeriodTab(
                                label = label,
                                isSelected = selectedPeriod == period,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.selectPeriod(period) }
                        }
                    }
                }

                // View Rashifal button (hidden once a result is showing for this selection)
                if (horoscope == null && !isLoading) {
                    item {
                        Button(
                            onClick = { viewModel.fetchHoroscope() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Text(
                                "View Rashifal",
                                color = CosmicDeepBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Loading / error / results
                when {
                    isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = SacredGold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Reading the stars...",
                                        color = StarlightWhite.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    errorMessage != null -> {
                        item {
                            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Rashifal Unavailable",
                                        color = StarlightWhite,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage ?: "Something went wrong.",
                                        color = StarlightWhite.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.retry() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                                    ) {
                                        Text("Retry", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    horoscope != null -> {
                        val h = horoscope!!
                        item { RashifalTitleCard(sign = sign, data = h, periodLabel = viewModel.periodLabels[selectedPeriod] ?: selectedPeriod) }
                        h.saturnTransit?.takeIf { it.active }?.let { transit ->
                            item { SaturnAlertCard(transit = transit) }
                        }
                        item { RashifalSummaryCard(summary = h.summary) }
                        item { CategoryScoresCard(areas = h.areas) }
                        h.lucky?.let { lucky ->
                            item { LuckySignsCard(sign = sign, data = h) }
                        }
                        if (h.remedies.isNotEmpty()) {
                            item { RemediesCard(remedies = h.remedies) }
                        }
                        h.dasha?.let { dasha ->
                            item { DashaStripCard(dashaMd = dasha.md, dashaAd = dasha.ad, primaryName = dasha.primaryName) }
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Banner
// ---------------------------------------------------------------------------
@Composable
private fun RashifalBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(MaroonBannerLight, MaroonBanner)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RASHIFAL",
                color = SacredGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select your sign and read detailed horoscope",
                color = StarlightWhite.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sign grid card
// ---------------------------------------------------------------------------
@Composable
private fun SignGridCard(sign: HoroscopeSign, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) SacredGold else CosmicBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicCardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(sign.glyphColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = sign.symbol, fontSize = 24.sp, color = StarlightWhite)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sign.name,
            color = StarlightWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Ruler: ${sign.ruler}",
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Period tab
// ---------------------------------------------------------------------------
@Composable
private fun PeriodTab(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(label, color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold),
            border = androidx.compose.foundation.BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Result: title card with OVERALL x/10
// ---------------------------------------------------------------------------
@Composable
private fun RashifalTitleCard(sign: HoroscopeSign, data: RashifalData, periodLabel: String) {
    val overall = data.scores["overall"]
    CosmicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SacredGold.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${sign.name} — $periodLabel Rashifal",
                    color = SacredGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (data.summary.isNotBlank()) {
                    Text(
                        text = "Lahiri Ayanamsa · Live transit computation",
                        color = StarlightWhite.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
            }
            if (overall != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "OVERALL",
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$overall/10",
                        color = scoreColor(overall),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Result: Saturn transit alert
// ---------------------------------------------------------------------------
@Composable
private fun SaturnAlertCard(transit: SaturnTransit) {
    val bg = Color(0xFF3A1420)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = transit.nameEn.ifBlank { "Saturn Transit Alert" },
                color = Color(0xFFFF8A80),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE53935).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Active",
                    color = Color(0xFFFF8A80),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        if (transit.descriptionEn.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = transit.descriptionEn,
                color = StarlightWhite.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Result: summary paragraph
// ---------------------------------------------------------------------------
@Composable
private fun RashifalSummaryCard(summary: String) {
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
                text = summary,
                color = StarlightWhite,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Result: 6 category score cards with progress bars
// ---------------------------------------------------------------------------
@Composable
private fun CategoryScoresCard(areas: Map<String, RashifalArea>) {
    val order = listOf("career", "finance", "health", "love", "family", "travel")
    val ordered = order.mapNotNull { key -> areas[key]?.let { key to it } } +
            areas.filterKeys { it !in order }.toList()
    if (ordered.isEmpty()) return

    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "LIFE AREA ENERGIES",
                color = SacredGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            ordered.forEach { (_, area) ->
                CategoryScoreRow(area = area)
            }
        }
    }
}

@Composable
private fun CategoryScoreRow(area: RashifalArea) {
    // Honesty: a missing server score renders "—" with no bar — never a fake 0/10.
    val score = area.score
    val barColor = if (score != null) scoreColor(score) else StarlightWhite.copy(alpha = 0.3f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = area.label.ifBlank { "Energy" },
                color = StarlightWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (score != null) "$score/10" else "—",
                color = barColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (score != null) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (score.coerceIn(0, 10)) / 10f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = CosmicCharcoal
            )
        }
        if (area.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = area.text,
                color = StarlightWhite.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 8 -> Color(0xFF43A047)
    score >= 6 -> Color(0xFFFF9040)
    else -> Color(0xFFE53935)
}

// ---------------------------------------------------------------------------
// Result: lucky signs (number / color / direction / gem / lucky time)
// ---------------------------------------------------------------------------
@Composable
private fun LuckySignsCard(sign: HoroscopeSign, data: RashifalData) {
    val lucky = data.lucky ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A2410))
            .border(1.dp, SacredGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Lucky Signs",
            color = SacredGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            LuckyCell(label = "Number", value = "${lucky.number}", modifier = Modifier.weight(1f))
            LuckyCell(label = "Color", value = lucky.color, modifier = Modifier.weight(1f))
            LuckyCell(label = "Direction", value = lucky.direction, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            LuckyCell(label = "Gem", value = lucky.gem, modifier = Modifier.weight(1f))
            LuckyCell(label = "Lucky Time", value = lucky.time, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LuckyCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "—" },
            color = StarlightWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Result: remedies
// ---------------------------------------------------------------------------
@Composable
private fun RemediesCard(remedies: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicCardBg)
            .border(1.dp, Color(0xFF42A5F5).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Vedic Remedies",
            color = Color(0xFF90CAF9),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        remedies.forEachIndexed { index, remedy ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "${index + 1}.",
                    color = SacredGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = remedy,
                    color = StarlightWhite.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index < remedies.lastIndex) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Result: current dasha strip
// ---------------------------------------------------------------------------
@Composable
private fun DashaStripCard(dashaMd: String, dashaAd: String, primaryName: String) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "CURRENT PLANETARY PERIOD",
                color = SacredGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DashaCell("Mahadasha", dashaMd)
                DashaCell("Antardasha", dashaAd)
                if (primaryName.isNotBlank()) DashaCell("Dominant", primaryName)
            }
        }
    }
}

@Composable
private fun DashaCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "—" },
            color = StarlightWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
