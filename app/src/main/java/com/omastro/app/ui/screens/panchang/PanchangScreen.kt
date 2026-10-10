package com.omastro.app.ui.screens.panchang

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.HoraData
import com.omastro.app.data.model.HoraHour
import com.omastro.app.data.model.PanchangLookups
import com.omastro.app.data.model.PanchangResponse
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val MaroonBanner = Color(0xFF5C0B0B)
private val MaroonDeep = Color(0xFF3D0808)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanchangScreen(
    onBack: () -> Unit,
    viewModel: PanchangViewModel = viewModel()
) {
    val panchang by viewModel.panchang.collectAsState()
    val hora by viewModel.hora.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    val dateLabel = remember(selectedDate) { PanchangLookups.apiDateToLabel(selectedDate) }

    if (showDatePicker) {
        PanchangDatePicker(
            initialDateApi = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { apiDate ->
                showDatePicker = false
                viewModel.loadPanchang(apiDate)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("Panchang"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Maroon banner (matches website)
            item { PanchangBanner() }

            // 2. Controls: date picker + timezone + Get Panchang
            item {
                ControlsCard(
                    dateLabel = dateLabel,
                    onPickDate = { showDatePicker = true },
                    onGetPanchang = { viewModel.loadPanchang() }
                )
            }

            when {
                isLoading && panchang == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SacredGold)
                        }
                    }
                }
                errorMessage != null && panchang == null -> {
                    item { PanchangErrorCard(message = errorMessage, onRetry = { viewModel.retry() }) }
                }
                panchang != null -> {
                    val p = panchang!!

                    // 3. Date banner with sunrise/sunset
                    item { DateBannerCard(p = p) }

                    // 4. Panchang details
                    item {
                        SectionHeader("Panchang Details")
                    }
                    item { PanchangDetailsCard(p = p) }

                    // 5. Hora — planetary hours (real /hora API)
                    if (hora != null) {
                        item {
                            SectionHeader("Hora — Planetary Hours")
                        }
                        item { HoraCard(hora = hora!!, selectedDate = selectedDate) }
                    }

                    // 6. Samvat / calendar info
                    if (p.samvatLine != null || p.pravishte != null) {
                        item {
                            SectionHeader("Calendar")
                        }
                        item { SamvatCard(p = p) }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Banner
// ---------------------------------------------------------------------------

@Composable
private fun PanchangBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(colors = listOf(MaroonBanner, MaroonDeep))
            )
            .border(1.dp, SacredGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PANCHANG",
            color = TextGold,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tithi · Vara · Nakshatra · Yoga · Karana",
            color = StarlightWhite.copy(alpha = 0.85f),
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

@Composable
private fun ControlsCard(
    dateLabel: String,
    onPickDate: () -> Unit,
    onGetPanchang: () -> Unit
) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DATE",
            color = TextGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicDeepBlue)
                .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onPickDate)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = SacredGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = dateLabel,
                color = StarlightWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(text = "Change", color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TIMEZONE",
            color = TextGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "IST +5:30 (India)",
            color = StarlightWhite.copy(alpha = 0.85f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onGetPanchang,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Get Panchang",
                color = CosmicDeepBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanchangDatePicker(
    initialDateApi: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val ist = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val initialMillis = remember(initialDateApi) {
        try {
            val parse = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = ist }
            parse.parse(initialDateApi)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: initialMillis
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = ist }
                onDateSelected(fmt.format(java.util.Date(millis)))
            }) {
                Text(W.t("OK"), color = SacredGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(W.t("Cancel"), color = StarlightWhite.copy(alpha = 0.7f))
            }
        }
    ) {
        DatePicker(state = state)
    }
}

// ---------------------------------------------------------------------------
// Date banner
// ---------------------------------------------------------------------------

@Composable
private fun DateBannerCard(p: PanchangResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF9E6))
            .padding(16.dp)
    ) {
        Text(
            text = p.dateLabel,
            color = Color(0xFF251A0B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Sunrise · ${p.sunrise}   /   Sunset · ${p.sunset}",
            color = Color(0xFF7C6A50),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------------------------------------------------------------------------
// Details
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = SacredGold,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun PanchangDetailsCard(p: PanchangResponse) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        DetailRow(label = "Tithi", value = p.tithi)
        if (p.tithiStart != null || p.tithiEnd != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    if (p.tithiStart != null) {
                        Text(
                            text = "Starts: ${p.tithiStart}",
                            color = AuspiciousGreen,
                            fontSize = 12.sp
                        )
                    }
                    if (p.tithiEnd != null) {
                        Text(
                            text = "Ends: ${p.tithiEnd}",
                            color = InauspiciousRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        DetailDivider()
        DetailRow(label = "Nakshatra", value = p.nakshatra)
        if (p.nakshatraDetail != null) {
            DetailSubtext(p.nakshatraDetail)
        }
        DetailDivider()
        val yogaValue = if (p.yogaEnd != null) "${p.yoga} · upto ${p.yogaEnd}" else p.yoga
        DetailRow(label = "Yoga", value = yogaValue)
        DetailDivider()
        DetailRow(label = "Karana", value = p.karana1)
        if (p.karana2 != null) {
            DetailRow(label = "Karana", value = p.karana2)
        }
        DetailDivider()
        DetailRow(label = "Paksha", value = p.paksha)
        DetailDivider()
        DetailRow(label = "Weekday", value = p.day)
        if (p.amantaMonth != null) {
            DetailDivider()
            DetailRow(label = "Amanta Month", value = p.amantaMonth)
        }
        if (p.purnimantaMonth != null) {
            DetailDivider()
            DetailRow(label = "Purnimanta Month", value = p.purnimantaMonth)
        }
        DetailDivider()
        DetailRow(label = "Moonsign", value = p.moonSign)
        if (p.sunSign != null) {
            DetailDivider()
            DetailRow(label = "Sunsign", value = p.sunSign)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = StarlightWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

@Composable
private fun DetailSubtext(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

@Composable
private fun DetailDivider() {
    Divider(color = CosmicBorder.copy(alpha = 0.6f), thickness = 0.75.dp)
}

// ---------------------------------------------------------------------------
// Hora
// ---------------------------------------------------------------------------

@Composable
private fun HoraCard(hora: HoraData, selectedDate: String) {
    var showNight by remember { mutableStateOf(false) }
    val isToday = remember(selectedDate) { selectedDate == PanchangLookups.todayApiDate() }

    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HoraTabButton(
                text = "Day Hora",
                selected = !showNight,
                onClick = { showNight = false },
                modifier = Modifier.weight(1f)
            )
            HoraTabButton(
                text = "Night Hora",
                selected = showNight,
                onClick = { showNight = true },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val hours: List<HoraHour> = if (showNight) hora.night else hora.day
        if (hours.isEmpty()) {
            Text(
                text = "Hora data unavailable for this date.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                hours.forEach { h ->
                    HoraRow(hour = h, isCurrent = isCurrentHora(h, isToday))
                }
            }
        }
    }
}

@Composable
private fun HoraTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) SacredGold else CosmicDeepBlue
    val fg = if (selected) CosmicDeepBlue else StarlightWhite.copy(alpha = 0.75f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (selected) SacredGold else CosmicBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HoraRow(hour: HoraHour, isCurrent: Boolean) {
    val border = if (isCurrent) SacredGold else CosmicBorder.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) SacredGold.copy(alpha = 0.10f) else CosmicDeepBlue.copy(alpha = 0.5f))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = hour.lord ?: "—",
            color = SacredGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${hour.start ?: "—"} – ${hour.end ?: "—"}",
            color = StarlightWhite,
            fontSize = 13.sp
        )
        if (isCurrent) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NOW",
                color = CosmicDeepBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SacredGold)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun isCurrentHora(h: HoraHour, isToday: Boolean): Boolean {
    if (!isToday) return false
    return try {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val now = Calendar.getInstance(tz)
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val s = h.start ?: return false
        val e = h.end ?: return false
        val sp = s.split(":")
        val ep = e.split(":")
        val sMin = sp[0].toInt() * 60 + sp[1].toInt()
        var eMin = ep[0].toInt() * 60 + ep[1].toInt()
        if (eMin <= sMin) eMin += 24 * 60
        var n = nowMin
        if (n < sMin) n += 24 * 60
        n in sMin until eMin
    } catch (e: Exception) {
        false
    }
}

// ---------------------------------------------------------------------------
// Samvat
// ---------------------------------------------------------------------------

@Composable
private fun SamvatCard(p: PanchangResponse) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        if (p.samvatLine != null) {
            DetailRow(label = "Samvat", value = p.samvatLine)
        }
        if (p.pravishte != null) {
            DetailDivider()
            DetailRow(label = "Pravishte", value = p.pravishte.toString())
        }
    }
}

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------

@Composable
private fun PanchangErrorCard(message: String?, onRetry: () -> Unit) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Panchang Unavailable",
                color = StarlightWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message ?: "No Panchang data available.",
                color = StarlightWhite.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(W.t("Retry"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
