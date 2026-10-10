package com.omastro.app.ui.screens.kundali

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.model.*
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.PlanetMark
import com.omastro.app.ui.components.VedicDiamondChart
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.share.ReadingCardShareButton
import com.omastro.app.ui.share.shareCardContent
import com.omastro.app.ui.theme.*

// ---------------------------------------------------------------------------
// Shared helpers (Kx- prefix: KundaliScreen.kt owns SectionTitle/EmptySection)
// ---------------------------------------------------------------------------

@Composable
internal fun KxSectionTitle(text: String) {
    Text(text, color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun KxEmpty(text: String) {
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

@Composable
private fun KxLabelValue(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            label, color = TextSecondary, fontSize = 12.sp,
            modifier = Modifier.width(110.dp)
        )
        Text(value, color = StarlightWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

/**
 * Renders a website analysis_tabs text block. The server separates segments
 * with "|" and newlines; each segment becomes its own paragraph.
 */
@Composable
internal fun KxAnalysisTextBlock(text: String) {
    val paras = text.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    if (paras.isEmpty()) {
        KxEmpty(W.t("kundali_empty_analysis"))
        return
    }
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            paras.forEach { p ->
                Text(p, color = StarlightWhite.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

/** Chart legend: Exalted / Debilitated / Retrograde / Combust / Vargottama. */
@Composable
internal fun KxLegendCard() {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(W.t("kundali_sec_legend"), color = SacredGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            listOf(
                "kundali_lg_exalted", "kundali_lg_debilitated", "kundali_lg_retro",
                "kundali_lg_combust", "kundali_lg_vargottama", "kundali_lg_lagna"
            ).forEach { key ->
                Text(
                    "• ${W.t(key)}",
                    color = StarlightWhite.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/** house number -> PlanetMark list from a houses list (any lagna-based chart). */
internal fun kxNorthMarks(
    houses: List<HouseInfo>,
    symbolFor: (String) -> String
): Map<Int, List<PlanetMark>> =
    houses.associate { h ->
        h.number to h.planets.map { PlanetMark(it.name, symbolFor(it.name)) }
    }

/** Synthetic ApiPlanet list (sign-relative) for the South-Indian grid. */
internal fun kxSouthPlanets(houses: List<HouseInfo>): List<ApiPlanet> {
    val out = mutableListOf<ApiPlanet>()
    houses.forEach { h ->
        val signIdx = rashiNameToIndex(h.rashi)
        h.planets.forEach { p ->
            out.add(ApiPlanet(name = p.name, nameHi = p.nameHi, rashiIndex = signIdx, symbol = planetGlyphFallback(p.name)))
        }
    }
    return out
}

internal fun kxLagnaIndex(houses: List<HouseInfo>, lagnaRashi: String): Int {
    if (lagnaRashi.isNotBlank()) return rashiNameToIndex(lagnaRashi)
    houses.firstOrNull { it.number == 1 }?.rashi?.let { return rashiNameToIndex(it) }
    return 1
}

/** NI + SI chart pair for any houses-based chart. */
@Composable
internal fun KxChartPair(
    houses: List<HouseInfo>,
    lagnaRashi: String,
    lagnaIndex: Int,
    symbolFor: (String) -> String,
    chartStyle: String,
    title: String
) {
    if (houses.isEmpty()) {
        KxEmpty(W.t("kundali_empty_section"))
        return
    }
    val styleName = if (chartStyle == "South Indian") W.t("kundali_chart_south")
    else W.t("kundali_chart_north")
    if (chartStyle == "South Indian") {
        SouthIndianChart(
            planets = kxSouthPlanets(houses),
            ascendantSign = lagnaIndex,
            chartTitle = W.tf("kundali_chart_title", styleName, title)
        )
    } else {
        VedicDiamondChart(
            lagnaSign = lagnaIndex,
            housePlanets = kxNorthMarks(houses, symbolFor),
            chartTitle = W.tf("kundali_chart_title", styleName, title)
        )
    }
}

/** Planet table for a houses list (name, degree, status, dignity). */
@Composable
internal fun KxHousePlanetTable(houses: List<HouseInfo>, symbolFor: (String) -> String) {
    val rows = houses.sortedBy { it.number }.flatMap { h ->
        h.planets.map { p -> Triple(h.number, h.rashi, p) }
    }
    if (rows.isEmpty()) {
        KxEmpty(W.t("kundali_empty_planets"))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { (house, rashi, p) ->
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            symbolFor(p.name).ifBlank { planetGlyphFallback(p.name) },
                            color = SacredGold, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(p.name, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "$rashi · House $house" + if (p.deg.isNotBlank()) " · ${p.deg}" else "",
                                color = TextSecondary, fontSize = 11.sp
                            )
                        }
                    }
                    if (p.status.isNotBlank()) {
                        Surface(color = SacredGold.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                p.status, color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Lazy report state renderer (Personality / BNN / Lal Kitab / Jaimini)
// ---------------------------------------------------------------------------

@Composable
internal fun KxReportState(
    state: ReportUiState,
    tabIndex: Int,
    onRetry: () -> Unit,
    summaryText: String = ""
) {
    when {
        state.loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SacredGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(W.t("kundali_loading_report"), color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        state.error != null -> {
            CosmicCard(modifier = Modifier.fillMaxWidth(), borderColor = InauspiciousRed.copy(alpha = 0.4f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(state.error, color = StarlightWhite, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(W.t("kundali_retry"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (summaryText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                KxSectionTitle(W.t("kundali_summary"))
                KxAnalysisTextBlock(summaryText)
            }
        }
        state.data != null -> {
            val text = state.data.bestText()
            if (text.isBlank()) {
                KxEmpty(W.t("kundali_empty_report_server"))
            } else {
                KxAnalysisTextBlock(text)
            }
        }
        else -> {
            if (summaryText.isNotBlank()) KxAnalysisTextBlock(summaryText)
            else KxEmpty(W.t("kundali_empty_report"))
        }
    }
}

@Composable
internal fun KxJaiminiState(
    state: JaiminiUiState,
    tabIndex: Int,
    onRetry: () -> Unit,
    content: @Composable (JaiminiData) -> Unit
) {
    when {
        state.loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SacredGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(W.t("kundali_loading_jaimini"), color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        state.error != null -> {
            CosmicCard(modifier = Modifier.fillMaxWidth(), borderColor = InauspiciousRed.copy(alpha = 0.4f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(state.error, color = StarlightWhite, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(W.t("kundali_retry"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        state.data != null -> content(state.data)
        else -> KxEmpty(W.t("kundali_empty_jaimini"))
    }
}

// ---------------------------------------------------------------------------
// "Ask AI about this" — shared card + per-tab context blocks (all 23 tabs).
// Flow (Palmistry/Vastu parity): on tap the tab's data block is stored via
// PendingPhotoContextHolder, then AI chat opens; AiChatViewModel consumes it
// as a context chip so follow-up questions reference the actual tab data.
// ---------------------------------------------------------------------------

/**
 * Seeker + chart identity header shared by every Kundali AI context block.
 * Only server values are used — nothing is ever invented client-side.
 */
internal fun kxSeekerHeader(kundali: KundaliResponse?, section: String): String {
    val sb = StringBuilder("=== KUNDALI CONTEXT · $section ===\n")
    val k = kundali
    if (k != null) {
        val birth = k.birth
        sb.append("Seeker: ").append(birth.name.ifBlank { "Seeker" }).append('\n')
        if (birth.year > 0)
            sb.append("DOB: ").append("%02d-%02d-%04d".format(birth.day, birth.month, birth.year)).append('\n')
        if (birth.hour > 0 || birth.minute > 0)
            sb.append("TOB: ").append("%02d:%02d".format(birth.hour, birth.minute)).append('\n')
        if (birth.place.isNotBlank()) sb.append("Birth place: ").append(birth.place).append('\n')
        if (birth.gender.isNotBlank()) sb.append("Gender: ").append(birth.gender).append('\n')
        val l = k.lagna
        sb.append("Lagna: ").append(l.rashi.ifBlank { k.meta.lagnaEn })
            .append(' ').append(l.deg).append('°').append(l.min).append('\'')
        if (l.lord.isNotBlank()) sb.append(" (Lord ").append(l.lord).append(')')
        sb.append('\n')
        val m = k.meta
        val moon = m.moonSignEn.ifBlank { m.moonSign }
        if (moon.isNotBlank() || m.nakshatra.isNotBlank()) {
            sb.append("Moon:")
            if (moon.isNotBlank()) sb.append(' ').append(moon)
            if (m.nakshatra.isNotBlank()) sb.append("  Nakshatra: ").append(m.nakshatra)
            if (m.nakshatraPada > 0) sb.append(" Pada ").append(m.nakshatraPada)
            sb.append('\n')
        }
    }
    sb.append("Answer the seeker's questions using ONLY the data in this block. ")
        .append("Base every prediction strictly on these values; never invent placements, dates or scores.\n")
    return sb.toString()
}

/** One line per planet: name, sign, house, degree, dignity chips. */
internal fun kxPlanetLines(planets: List<ApiPlanet>): String {
    val sb = StringBuilder()
    planets.filter { it.name.isNotBlank() }.forEach { p ->
        sb.append("- ").append(p.name).append(": ")
            .append(p.rashi.ifBlank { "—" })
            .append(" House ").append(p.house)
            .append(' ').append(p.degreeFormatted())
        if (p.nakshatra.isNotBlank()) {
            sb.append("  Nakshatra ").append(p.nakshatra)
            if (p.pada > 0) sb.append(" Pada ").append(p.pada)
            if (p.nakshatraLord.isNotBlank()) sb.append(" (Lord ").append(p.nakshatraLord).append(')')
        }
        val chips = p.dignityChips().toMutableList()
        if (p.isRetrograde() && !chips.contains("Retrograde")) chips.add("Retrograde")
        if (p.combust && !chips.contains("Combust")) chips.add("Combust")
        if (chips.isNotEmpty()) sb.append("  [").append(chips.joinToString(", ")).append(']')
        sb.append('\n')
    }
    return sb.toString()
}

/** House list (any chart) as text: house number, rashi, lord, planets. */
internal fun kxHouseLines(houses: List<HouseInfo>): String {
    val sb = StringBuilder()
    houses.sortedBy { it.number }.forEach { h ->
        sb.append("House ").append(h.number).append(": ")
            .append(h.rashi.ifBlank { "—" })
        if (h.lord.isNotBlank()) sb.append(" (Lord ").append(h.lord).append(')')
        val names = h.planets.map { it.name }.filter { it.isNotBlank() }
        if (names.isNotEmpty()) sb.append(" — planets: ").append(names.joinToString(", "))
        sb.append('\n')
    }
    return sb.toString()
}

/** One line per dasha node, recursive to the depth the server provides. */
internal fun kxDashaLines(nodes: List<DashaNode>, level: Int = 0, out: StringBuilder = StringBuilder()): String {
    val pad = "  ".repeat(level)
    nodes.forEach { n ->
        out.append(pad).append("- ").append(n.planet)
            .append(": ").append(n.start).append(" → ").append(n.end)
        if (n.years > 0) out.append(" (").append(n.years).append("y)")
        out.append('\n')
        val children = listOf(n.antardasha, n.pratyantar, n.sookshma, n.prana, n.adi, n.deha)
            .firstOrNull { it.isNotEmpty() }
        if (children != null) kxDashaLines(children, level + 1, out)
    }
    return out.toString()
}

/**
 * Shared gold "Ask AI about this" card. Placed at the end of every Kundali tab.
 * [label] is the chat chip label (e.g. "Kundali · Yogas"); [contextBlock] is
 * built lazily on click so lazy-loaded tabs always attach fresh data.
 */
@Composable
internal fun KxAskAiCard(
    label: String,
    onOpenAiChat: () -> Unit,
    contextBlock: () -> String
) {
    CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                W.t("kundali_ask_ai_title"),
                color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                W.t("kundali_ask_ai_sub"),
                color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Button(
                onClick = {
                    val block = contextBlock()
                    if (block.isNotBlank()) {
                        PendingPhotoContextHolder.set(
                            PhotoContextAttachment(label = label, contextBlock = block)
                        )
                    }
                    onOpenAiChat()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    W.t("kundali_ask_ai_btn"),
                    color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 1 — Birth Info (+ Lucky Elements)
// ---------------------------------------------------------------------------

internal fun LazyListScope.birthInfoTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit,
    subjectName: String = ""
) {
    item { KxSectionTitle(W.t("kundali_sec_birth_info")) }
    if (kundali == null) {
        item { KxEmpty(W.t("kundali_empty_chart")) }
        return
    }
    val birth = kundali.birth
    val meta = kundali.meta
    val lagna = kundali.lagna

    item {
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(W.t("kundali_sec_seeker"), color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (birth.name.isNotBlank()) KxLabelValue(W.t("kundali_lb_name"), birth.name)
                if (birth.year > 0) KxLabelValue(
                    W.t("kundali_lb_dob"),
                    "%02d-%02d-%04d".format(birth.day, birth.month, birth.year)
                )
                if (birth.hour > 0 || birth.minute > 0) KxLabelValue(
                    W.t("kundali_lb_tob"),
                    "%02d:%02d".format(birth.hour, birth.minute)
                )
                if (birth.place.isNotBlank()) KxLabelValue(W.t("kundali_lb_pob"), birth.place)
                if (birth.gender.isNotBlank()) KxLabelValue(
                    W.t("kundali_lb_gender"), birth.gender.replaceFirstChar { it.uppercase() }
                )
                if (meta.ayanamsa.isNotBlank()) KxLabelValue(
                    W.t("kundali_lb_ayanamsa"), meta.ayanamsa.replaceFirstChar { it.uppercase() }
                )
            }
        }
    }

    item {
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(W.t("kundali_sec_lagna_moon"), color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                KxLabelValue(
                    W.t("kundali_lb_lagna"),
                    "${lagna.rashi.ifBlank { meta.lagnaEn }}${if (lagna.rashiHi.isNotBlank()) " (${lagna.rashiHi})" else ""} · Lord ${lagna.lord} · ${lagna.deg}°${lagna.min}'"
                )
                if (meta.moonSignEn.isNotBlank() || meta.moonSign.isNotBlank())
                    KxLabelValue(W.t("kundali_lb_moon_sign"), meta.moonSignEn.ifBlank { meta.moonSign })
                if (meta.nakshatra.isNotBlank())
                    KxLabelValue(W.t("kundali_lb_nakshatra"), "${meta.nakshatra}${if (meta.nakshatraPada > 0) " · ${W.t("kundali_pada")} ${meta.nakshatraPada}" else ""}")
                kundali.navamshaLagna?.let { d9 ->
                    if (d9.rashi.isNotBlank()) KxLabelValue(W.t("kundali_lb_d9_lagna"), d9.rashi)
                }
            }
        }
    }

    item { KxSectionTitle(W.t("kundali_sec_lucky")) }
    item {
        val lagnaIdx = kundali.lagnaSignIndex()
        val lucky = luckyElementsFor(lagnaIdx, lagna.lord)
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    W.t("kundali_lucky_from").replace("{l}", lagna.rashi.ifBlank { meta.lagnaEn }),
                    color = TextSecondary, fontSize = 11.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(luckySwatch(lagnaIdx))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(W.t("kundali_lb_lucky_colour"), color = TextSecondary, fontSize = 11.sp)
                        Text(lucky.color, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(W.t("kundali_lb_lucky_number"), color = TextSecondary, fontSize = 11.sp)
                        Text("${lucky.number}", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(2f)) {
                        Text(W.t("kundali_lb_lucky_gem"), color = TextSecondary, fontSize = 11.sp)
                        Text(lucky.gemstone, color = StarlightWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Birth Info",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Birth Info"))
            sb.append("Ayanamsa: ").append(kundali.meta.ayanamsa).append('\n')
            kundali.navamshaLagna?.let { d9 ->
                if (d9.rashi.isNotBlank()) sb.append("Navamsha Lagna: ").append(d9.rashi).append('\n')
            }
            val li = kundali.lagnaSignIndex()
            val lucky = luckyElementsFor(li, kundali.lagna.lord)
            sb.append("Lucky elements (from Lagna): colour=").append(lucky.color)
                .append(", number=").append(lucky.number)
                .append(", gemstone=").append(lucky.gemstone).append('\n')
            sb.toString()
        }
    }

    // Shareable Kundali summary card — built only from the REAL server
    // meta (lagna, moon sign, nakshatra, dasha, yogas). Hidden when the
    // meta carries nothing meaningful.
    item {
        ReadingCardShareButton(content = kundali?.shareCardContent(subjectName))
    }
}

/** Approximate swatch colour per lagna rashi for the lucky-colour dot. */
private fun luckySwatch(rashiIndex: Int): Color = when (rashiIndex) {
    1, 8 -> Color(0xFFEF4444)
    2, 7 -> Color(0xFFF8FAFC)
    3, 6 -> Color(0xFF10B981)
    4 -> Color(0xFFE2E8F0)
    5 -> Color(0xFFFFB300)
    9, 12 -> Color(0xFFFACC15)
    10 -> Color(0xFF1E3A8A)
    11 -> Color(0xFF7C3AED)
    else -> SacredGold
}

// ---------------------------------------------------------------------------
// Tab 2 — Birth Chart (D1): existing chartsTab + legend, pills, graha detail,
// D1 analysis, Sade Sati strip, current-transits strip
// ---------------------------------------------------------------------------

internal fun LazyListScope.birthChartTab(
    viewModel: KundaliViewModel,
    kundali: KundaliResponse?,
    selectedDiv: String,
    chartStyle: String,
    onOpenAiChat: () -> Unit
) {
    chartsTab(viewModel, kundali, selectedDiv, chartStyle)

    item { Spacer(modifier = Modifier.height(2.dp)) }
    item { KxLegendCard() }

    kundali?.analysisTabs?.ppills?.takeIf { it.isNotBlank() }?.let { pills ->
        item { KxSectionTitle(W.t("kundali_sec_pills")) }
        item {
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Text(pills, color = StarlightWhite.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 22.sp)
            }
        }
    }

    kundali?.analysisTabs?.grahaDetail?.takeIf { it.isNotBlank() }?.let { detail ->
        item { KxSectionTitle(W.t("kundali_sec_graha")) }
        item { KxAnalysisTextBlock(detail) }
    }

    kundali?.analysisTabs?.d1Analysis?.takeIf { it.isNotBlank() }?.let { analysis ->
        item { KxSectionTitle(W.t("kundali_sec_d1")) }
        item { KxAnalysisTextBlock(analysis) }
    }

    kundali?.saturnTransit?.takeIf { it.active }?.let { st ->
        item { KxSectionTitle(W.t("kundali_sec_saturn")) }
        item { KxSaturnAlertCard(st) }
    }

    if (!kundali?.currentTransits.isNullOrEmpty()) {
        item { KxSectionTitle(W.t("kundali_sec_gochar")) }
        item { KxTransitsStrip(kundali!!.currentTransits) }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Birth Chart (D1)",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Birth Chart (D1)"))
            sb.append("--- D1 planet placements ---\n")
                .append(kxPlanetLines(kundali?.planets ?: emptyList()))
            kundali?.analysisTabs?.grahaDetail?.takeIf { it.isNotBlank() }?.let {
                sb.append("--- Graha Detail ---\n").append(it.replace("|", "\n")).append('\n')
            }
            kundali?.analysisTabs?.d1Analysis?.takeIf { it.isNotBlank() }?.let {
                sb.append("--- D1 Analysis ---\n").append(it.replace("|", "\n")).append('\n')
            }
            kundali?.saturnTransit?.takeIf { it.active }?.let { st ->
                sb.append("Saturn transit alert: ").append(st.nameEn)
                if (st.type.isNotBlank()) sb.append(" (").append(st.type).append(')')
                if (st.phase.isNotBlank()) sb.append(" phase ").append(st.phase)
                sb.append('\n')
                if (st.descriptionEn.isNotBlank()) sb.append(st.descriptionEn).append('\n')
            }
            sb.toString()
        }
    }
}

@Composable
internal fun KxSaturnAlertCard(st: SaturnTransit) {
    CosmicCard(modifier = Modifier.fillMaxWidth(), borderColor = InauspiciousRed.copy(alpha = 0.5f)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("♄", color = InauspiciousRed, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    st.nameEn.ifBlank { W.t("Saturn Transit Active") },
                    color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            if (st.descriptionEn.isNotBlank()) {
                Text(st.descriptionEn, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 19.sp)
            }
            if (st.phase.isNotBlank() || st.type.isNotBlank()) {
                Text(
                    listOf(st.type, st.phase).filter { it.isNotBlank() }.joinToString(" · ")
                        .replaceFirstChar { it.uppercase() },
                    color = InauspiciousRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun KxTransitsStrip(transits: Map<String, TransitInfo>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(transits.entries.toList()) { (planet, t) ->
            CosmicCard(modifier = Modifier.width(190.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            planetGlyphFallback(planet), color = SacredGold,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(planet, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (t.retro) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(R)", color = NeutralBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "${rashiGlyph(rashiNameToIndex(t.rashi))} ${t.rashi}",
                        color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp
                    )
                    if (t.natalMoonHouse > 0) {
                        Text(
                            "House ${t.natalMoonHouse} from Moon",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                    if (t.transitFocus.isNotBlank()) {
                        Text(t.transitFocus, color = RadiantAmber, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 3 — Navamsha (D9)
// ---------------------------------------------------------------------------

internal fun LazyListScope.navamshaTab(
    kundali: KundaliResponse?,
    chartStyle: String,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_navamsha")) }
    if (kundali == null || kundali.d9Houses.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_navamsha")) }
        return
    }
    val symbolFor: (String) -> String = { kundali.symbolFor(it) }
    item {
        KxChartPair(
            houses = kundali.d9Houses,
            lagnaRashi = kundali.navamshaLagna?.rashi ?: "",
            lagnaIndex = kundali.navamshaLagnaIndex(),
            symbolFor = symbolFor,
            chartStyle = chartStyle,
            title = W.t("kundali_d9_chart_title")
        )
    }
    item { KxLegendCard() }
    item { KxSectionTitle(W.t("kundali_sec_d9places")) }
    item { KxHousePlanetTable(kundali.d9Houses, symbolFor) }
    kundali.analysisTabs?.d9Analysis?.takeIf { it.isNotBlank() }?.let { analysis ->
        item { KxSectionTitle(W.t("kundali_sec_d9analysis")) }
        item { KxAnalysisTextBlock(analysis) }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Navamsha (D9)",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Navamsha (D9)"))
            val kk = kundali
            val d9Lagna = kk?.navamshaLagna?.rashi?.ifBlank {
                kk.d9Houses.firstOrNull { it.number == 1 }?.rashi ?: ""
            } ?: ""
            if (d9Lagna.isNotBlank()) sb.append("Navamsha Lagna: ").append(d9Lagna).append('\n')
            sb.append("--- D9 planet placements ---\n")
                .append(kxHouseLines(kundali?.d9Houses ?: emptyList()))
            kundali?.analysisTabs?.d9Analysis?.takeIf { it.isNotBlank() }?.let {
                sb.append("--- D9 Analysis ---\n").append(it.replace("|", "\n")).append('\n')
            }
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 4 — Karakamsha (Jaimini)
// ---------------------------------------------------------------------------

internal fun LazyListScope.karakamshaTab(
    state: JaiminiUiState,
    onRetry: () -> Unit,
    chartStyle: String,
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_karakamsha")) }
    item {
        KxJaiminiState(state, 3, onRetry) { data ->
            KarakamshaContent(data, chartStyle)
        }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Karakamsha",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Karakamsha (Jaimini)"))
            val data = state.data
            if (data == null) {
                sb.append("Karakamsha data not loaded for this chart.\n")
            } else {
                val kk = data.karakamsha
                if (kk != null && kk.signEn.isNotBlank())
                    sb.append("Karakamsha Lagna: ").append(kk.signEn).append('\n')
                if (kk?.akPlanet?.isNotBlank() == true)
                    sb.append("Atmakaraka: ").append(kk.akPlanet).append('\n')
                data.karakas?.details?.ordered()?.forEach { (label, k) ->
                    if (k != null && k.name.isNotBlank())
                        sb.append(label).append(": ").append(k.name)
                            .append(" in ").append(k.rashi).append('\n')
                }
                if (data.karakamshaHouses.isNotEmpty()) {
                    sb.append("--- Karakamsha houses ---\n")
                    data.karakamshaHouses.sortedBy { it.n }.forEach { h ->
                        sb.append("House ").append(h.n).append(": ")
                            .append(h.r?.en ?: "—")
                        val pls = h.pl.map { it.en }.filter { it.isNotBlank() }
                        if (pls.isNotEmpty()) sb.append(" — ").append(pls.joinToString(", "))
                        if (h.isLg) sb.append("  [Karakamsha Lagna]")
                        sb.append('\n')
                    }
                }
            }
            sb.toString()
        }
    }
}

@Composable
private fun KarakamshaContent(data: JaiminiData, chartStyle: String) {
    val kk = data.karakamsha
    val akPlanet = kk?.akPlanet ?: data.karakas?.details?.ak?.name ?: ""
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(W.t("kundali_kk_title"), color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    W.t("kundali_kk_intro").replace("{name}", if (akPlanet.isNotBlank()) " ($akPlanet)" else ""),
                    color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 19.sp
                )
                if (kk != null && kk.signEn.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = SacredGold.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "${W.t("kundali_lb_kk_lagna")}: ${kk.signEn}${if (kk.signHi.isNotBlank()) " (${kk.signHi})" else ""}",
                            color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Chara Karakas — the seven soul significators from /kundali/jaimini
        val karakas = data.karakas?.details?.ordered()
        if (!karakas.isNullOrEmpty() && karakas.any { it.second?.name?.isNotBlank() == true }) {
            Text(W.t("kundali_sec_chara_karakas"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            karakas.forEach { (label, k) ->
                if (k != null && k.name.isNotBlank()) {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    "${planetGlyphFallback(k.name)} ${k.name}",
                                    color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${rashiGlyph(rashiNameToIndex(k.rashi))} ${k.rashi}",
                                    color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                                if (k.deg > 0) {
                                    Text(
                                        "${"%.2f".format(k.deg)}°",
                                        color = TextSecondary, fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val houses = data.karakamshaHouses
        if (houses.isNotEmpty()) {
            val lgHouse = houses.firstOrNull { it.isLg }
            val lagnaIdx = (lgHouse?.ri ?: (kk?.signIdx ?: 0)) + 1
            val symbolFor: (String) -> String = { planetGlyphFallback(it) }
            val houseInfos = houses.sortedBy { it.n }.map { h ->
                HouseInfo(
                    number = h.n,
                    rashi = h.r?.en ?: "",
                    rashiHi = h.r?.hi ?: "",
                    planets = h.pl.map { p -> HousePlanet(name = p.en, nameHi = p.hi) }
                )
            }
            if (chartStyle == "South Indian") {
                val siPlanets = mutableListOf<ApiPlanet>()
                houses.forEach { h ->
                    h.pl.forEach { p ->
                        siPlanets.add(ApiPlanet(name = p.en, nameHi = p.hi, symbol = p.sym.ifBlank { planetGlyphFallback(p.en) }, rashiIndex = h.ri + 1))
                    }
                }
                SouthIndianChart(
                    planets = siPlanets,
                    ascendantSign = lagnaIdx,
                    chartTitle = W.tf("kundali_chart_title", W.t("kundali_chart_south"), W.t("kundali_tab_3"))
                )
            } else {
                VedicDiamondChart(
                    lagnaSign = lagnaIdx,
                    housePlanets = houseInfos.associate { hi ->
                        hi.number to hi.planets.map { p -> PlanetMark(p.name, symbolFor(p.name)) }
                    },
                    chartTitle = W.tf("kundali_chart_title", W.t("kundali_chart_north"), W.t("kundali_tab_3"))
                )
            }
            KxLegendCard()

            Text(W.t("kundali_sec_housewise"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            houses.sortedBy { it.n }.forEach { h ->
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                W.tf("kund_house_list_fmt", h.n, h.r?.en ?: "", if (!h.r?.hi.isNullOrBlank()) " (${h.r?.hi})" else ""),
                                color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                            if (h.isLg) {
                                Surface(color = SacredGold, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        W.t("kundali_lb_kk_lagna"), color = CosmicDeepBlue, fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (h.pl.isNotEmpty()) {
                            Text(
                                h.pl.joinToString(" · ") { "${it.sym.ifBlank { planetGlyphFallback(it.en) }} ${it.en}" },
                                color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(W.t("kundali_no_planets"), color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            KxEmpty(W.t("kundali_empty_karakamsha_houses"))
        }
    }
}

// ---------------------------------------------------------------------------
// Tabs 5/6/7 — Chandra Lagna, Surya Lagna, Chalit (generic lagna-chart tab)
// ---------------------------------------------------------------------------

internal fun LazyListScope.lagnaChartTab(
    title: String,
    subtitle: String,
    chart: ChartData?,
    kundali: KundaliResponse?,
    chartStyle: String,
    onOpenAiChat: () -> Unit,
    analysisText: String = ""
) {
    item { KxSectionTitle(title) }
    if (!subtitle.isBlank()) {
        item { Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp) }
    }
    if (chart == null || chart.houses.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_for").replace("{t}", title)) }
        return
    }
    val symbolFor: (String) -> String = { kundali?.symbolFor(it) ?: planetGlyphFallback(it) }
    item {
        KxChartPair(
            houses = chart.houses,
            lagnaRashi = chart.lagnaRashi,
            lagnaIndex = chart.lagnaSignIndex(),
            symbolFor = symbolFor,
            chartStyle = chartStyle,
            title = title
        )
    }
    item { KxLegendCard() }
    item { KxSectionTitle(W.t("kundali_sec_places")) }
    item { KxHousePlanetTable(chart.houses, symbolFor) }
    if (analysisText.isNotBlank()) {
        item { KxSectionTitle(W.t("kundali_sec_analysis")) }
        item { KxAnalysisTextBlock(analysisText) }
    }

    item {
        KxAskAiCard(
            label = "Kundali · $title",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, title))
            if (chart != null) {
                if (chart.lagnaRashi.isNotBlank())
                    sb.append("$title Lagna: ").append(chart.lagnaRashi).append('\n')
                sb.append("--- House placements ---\n").append(kxHouseLines(chart.houses))
            }
            if (analysisText.isNotBlank())
                sb.append("--- Analysis ---\n").append(analysisText.replace("|", "\n")).append('\n')
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 8 — Divisional (all vargas D1–D60)
// ---------------------------------------------------------------------------

/** Standard varga significations (reference knowledge). */
internal val VARGA_MEANINGS = mapOf(
    "D1" to "Lagna — body, self and the overall arc of life",
    "D2" to "Hora — wealth, family and accumulated resources",
    "D3" to "Drekkana — siblings, courage, vitality and initiative",
    "D4" to "Chaturthamsha — property, mother, home and happiness",
    "D7" to "Saptamsha — children, progeny and creativity",
    "D9" to "Navamsha — marriage, dharma and inner strength",
    "D10" to "Dashamsha — career, profession and status",
    "D12" to "Dvadashamsha — parents, lineage and ancestry",
    "D16" to "Shodashamsha — vehicles, comforts and luxuries",
    "D20" to "Vimshamsha — spiritual progress and sadhana",
    "D24" to "Chaturvimshamsha — education, learning and intellect",
    "D27" to "Bhamsha — inherent strengths and weaknesses",
    "D30" to "Trimshamsha — misfortunes, protection and resilience",
    "D40" to "Khavedamsha — maternal legacy and inheritance",
    "D45" to "Akshavedamsha — paternal legacy and inheritance",
    "D60" to "Shashtiamsha — past-life karma and the finest detail"
)

private fun vargaSortKey(code: String): Int =
    code.filter { it.isDigit() }.toIntOrNull() ?: 999

internal fun LazyListScope.divisionalTab(
    kundali: KundaliResponse?,
    viewModel: KundaliViewModel,
    selectedDiv: String,
    chartStyle: String,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_divisional")) }
    if (kundali == null) {
        item { KxEmpty(W.t("kundali_empty_chart")) }
        return
    }
    val vargas = if (kundali.allDivisionalCharts.isNotEmpty()) kundali.allDivisionalCharts else kundali.divisionalCharts
    if (vargas.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_divisional")) }
        return
    }
    val codes = vargas.keys.sortedBy(::vargaSortKey)
    val active = codes.firstOrNull { it.equals(selectedDiv, ignoreCase = true) } ?: codes.first()

    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(codes) { code ->
                val isSelected = code.equals(active, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SacredGold else CosmicCharcoal,
                    modifier = Modifier.clickable { viewModel.selectDivisional(code) }
                ) {
                    Text(
                        text = code,
                        color = if (isSelected) CosmicDeepBlue else StarlightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    val varga = vargas[active] ?: vargas[codes.first()]!!
    val symbolFor: (String) -> String = { kundali.symbolFor(it) }

    item {
        KxChartPair(
            houses = varga.houses,
            lagnaRashi = varga.lagnaRashi,
            lagnaIndex = kxLagnaIndex(varga.houses, varga.lagnaRashi),
            symbolFor = symbolFor,
            chartStyle = chartStyle,
            title = varga.title.ifBlank { W.tf("kundali_varga_chart_title", active) }
        )
    }
    item { KxLegendCard() }

    item { KxSectionTitle(W.t("kundali_sec_significance").replace("{v}", active)) }
    item {
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    varga.title.ifBlank { active },
                    color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Text(
                    VARGA_MEANINGS[active.uppercase()] ?: "A divisional chart refining the promises of the birth chart.",
                    color = StarlightWhite.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 20.sp
                )
                val lagnaTxt = varga.lagnaRashi.ifBlank {
                    varga.houses.firstOrNull { it.number == 1 }?.rashi ?: ""
                }
                if (lagnaTxt.isNotBlank()) {
                    Text("$active Lagna: $lagnaTxt", color = TextSecondary, fontSize = 12.sp)
                }
                val placed = varga.houses.flatMap { h -> h.planets.map { it.name } }.distinct()
                if (placed.isNotEmpty()) {
                    Text(
                        "Planets placed: " + placed.joinToString(", "),
                        color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Divisional ($active)",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Divisional Chart ($active)"))
            val allDiv = kundali?.allDivisionalCharts
            val vgs = if (!allDiv.isNullOrEmpty()) allDiv else kundali?.divisionalCharts ?: emptyMap()
            val vcs = vgs.keys.sortedBy(::vargaSortKey)
            val act = vcs.firstOrNull { it.equals(selectedDiv, ignoreCase = true) } ?: vcs.firstOrNull() ?: ""
            sb.append("Varga: ").append(act).append(" — ")
                .append(VARGA_MEANINGS[act.uppercase()] ?: "A divisional chart refining the promises of the birth chart.")
                .append('\n')
            vgs[act]?.let { v ->
                val lg = v.lagnaRashi.ifBlank { v.houses.firstOrNull { it.number == 1 }?.rashi ?: "" }
                if (lg.isNotBlank()) sb.append("$act Lagna: ").append(lg).append('\n')
                sb.append("--- $act house placements ---\n").append(kxHouseLines(v.houses))
            }
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 11 — Sodasvarga (16-varga table)
// ---------------------------------------------------------------------------

internal fun LazyListScope.sodasvargaTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_sodasvarga")) }
    val sv = kundali?.sodasvarga
    if (sv.isNullOrEmpty()) {
        item { KxEmpty(W.t("kundali_empty_sodasvarga")) }
        return
    }
    val vargaCodes = sv.keys.sortedBy(::vargaSortKey)
    // Planet order follows the birth chart when possible
    val chartOrder = kundali?.planets?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
    val planetNames = (chartOrder + sv.values.flatten().map { it.name }.distinct())
        .distinct().filter { it.isNotBlank() }
    if (planetNames.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_sodasvarga")) }
        return
    }
    item {
        Text(
            W.t("kundali_sodasvarga_note"),
            color = TextSecondary, fontSize = 11.sp
        )
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
                    Box(modifier = Modifier.width(96.dp).padding(horizontal = 4.dp)) {
                        Text("GRAHA", color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    vargaCodes.forEach { code ->
                        Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                            Text(code, color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                planetNames.forEachIndexed { index, planet ->
                    val bg = if (index % 2 == 0) Color.Transparent else CosmicCharcoal.copy(alpha = 0.45f)
                    Row(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.width(96.dp).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                planetGlyphFallback(planet), color = SacredGold,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(planet, color = StarlightWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        vargaCodes.forEach { code ->
                            val entry = sv[code]?.firstOrNull { it.name.equals(planet, ignoreCase = true) }
                            Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val rashi = entry?.rashi ?: ""
                                    Text(
                                        rashiGlyph(rashiNameToIndex(rashi)).takeIf { rashi.isNotBlank() } ?: "—",
                                        color = if (rashi.isNotBlank()) StarlightWhite else TextSecondary.copy(alpha = 0.4f),
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                                    )
                                    if (rashi.isNotBlank()) {
                                        Text(rashi.take(3), color = TextSecondary, fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(W.t("kundali_scroll_vargas"), color = TextSecondary, fontSize = 11.sp)
    }

    item {
        KxAskAiCard(
            label = "Kundali · Sodasvarga",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Sodasvarga (16 vargas)"))
            val sv = kundali?.sodasvarga
            if (sv.isNullOrEmpty()) {
                sb.append("No Sodasvarga data returned for this chart.\n")
            } else {
                val codes = sv.keys.sortedBy(::vargaSortKey)
                sb.append("--- Varga x planet: sign occupied ---\n")
                val planets = ((kundali?.planets ?: emptyList()).map { it.name } + sv.values.flatten().map { it.name })
                    .distinct().filter { it.isNotBlank() }
                planets.forEach { planet ->
                    val cells = codes.map { code ->
                        val r = sv[code]?.firstOrNull { it.name.equals(planet, ignoreCase = true) }?.rashi
                        "$code=${r ?: "—"}"
                    }
                    sb.append(planet).append(": ").append(cells.joinToString(" ")).append('\n')
                }
            }
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 14 — Personality (lazy: POST /kundali/personality)
// ---------------------------------------------------------------------------

internal fun LazyListScope.personalityTab(
    kundali: KundaliResponse?,
    state: ReportUiState,
    onRetry: () -> Unit,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_personality")) }
    item {
        KxReportState(
            state = state,
            tabIndex = 13,
            onRetry = onRetry,
            summaryText = kundali?.analysisTabs?.personality ?: ""
        )
    }

    item {
        KxAskAiCard(
            label = "Kundali · Personality",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Personality Analysis"))
            val text = state.data?.bestText()?.ifBlank { kundali?.analysisTabs?.personality } ?: ""
            if (text.isNotBlank()) sb.append("--- Personality report ---\n")
                .append(text.replace("|", "\n")).append('\n')
            else sb.append("Personality report not loaded for this chart.\n")
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 15 — Planets (12-column website table)
// ---------------------------------------------------------------------------

internal fun LazyListScope.planetsFullTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_planets")) }
    val planets = kundali?.planets
        ?.filter { it.name.isNotBlank() }
        ?.sortedBy { it.house } ?: emptyList()
    if (planets.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_planets")) }
        return
    }
    item {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                Row(
                    modifier = Modifier
                        .background(CosmicCharcoal, RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KxTableCell("PLANET", 118, isHeader = true)
                    KxTableCell("SYMBOL", 62, isHeader = true)
                    KxTableCell("SIGN", 104, isHeader = true)
                    KxTableCell("DEGREE", 76, isHeader = true)
                    KxTableCell("DMS", 96, isHeader = true)
                    KxTableCell("NAKSHATRA", 128, isHeader = true)
                    KxTableCell("PADA", 48, isHeader = true)
                    KxTableCell("NAK. LORD", 96, isHeader = true)
                    KxTableCell("HOUSE", 58, isHeader = true)
                    KxTableCell("D9 SIGN", 96, isHeader = true)
                    KxTableCell("D9 HOUSE", 70, isHeader = true)
                    KxTableCell("STATUS", 170, isHeader = true)
                    KxTableCell("", 36, isHeader = true)
                }
                planets.forEachIndexed { index, planet ->
                    val bg = if (index % 2 == 0) Color.Transparent else CosmicCharcoal.copy(alpha = 0.45f)
                    // Expandable detail: ruled houses, aspects, avastha, chara
                    // karaka, vargottama — all from the server ApiPlanet, never
                    // fabricated. Tap a row to reveal.
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.clickable { expanded = !expanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Planet
                            Column(modifier = Modifier.width(118.dp).padding(horizontal = 4.dp)) {
                                Text(planet.name, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val hi = planet.nameHi.ifBlank { planetHindiFallback(planet.name) }
                                if (hi.isNotBlank()) Text(hi, color = TextSecondary, fontSize = 10.sp)
                            }
                            // Symbol
                            Box(modifier = Modifier.width(62.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    planet.symbol.ifBlank { planetGlyphFallback(planet.name) },
                                    color = SacredGold, fontSize = 18.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            // Sign
                            Column(modifier = Modifier.width(104.dp).padding(horizontal = 4.dp)) {
                                Text(planet.rashi, color = StarlightWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (planet.rashiHi.isNotBlank()) Text(planet.rashiHi, color = TextSecondary, fontSize = 10.sp)
                            }
                            // Degree
                            Box(modifier = Modifier.width(76.dp).padding(horizontal = 4.dp)) {
                                Text(planet.degreeFormatted(), color = StarlightWhite, fontSize = 12.sp)
                            }
                            // DMS
                            Box(modifier = Modifier.width(96.dp).padding(horizontal = 4.dp)) {
                                Text(
                                    "${planet.deg}°${planet.min}'${planet.sec}\"",
                                    color = StarlightWhite, fontSize = 12.sp
                                )
                            }
                            // Nakshatra
                            Box(modifier = Modifier.width(128.dp).padding(horizontal = 4.dp)) {
                                Text(planet.nakshatra.ifBlank { "—" }, color = StarlightWhite, fontSize = 12.sp)
                            }
                            // Pada
                            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                Text(if (planet.pada > 0) "${planet.pada}" else "—", color = RadiantAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            // Nakshatra lord
                            Box(modifier = Modifier.width(96.dp).padding(horizontal = 4.dp)) {
                                Text(planet.nakshatraLord.ifBlank { "—" }, color = StarlightWhite, fontSize = 12.sp)
                            }
                            // House
                            Box(modifier = Modifier.width(58.dp), contentAlignment = Alignment.Center) {
                                Text("${planet.house}", color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            // D9 sign
                            Box(modifier = Modifier.width(96.dp).padding(horizontal = 4.dp)) {
                                Text(planet.d9Rashi.ifBlank { "—" }, color = StarlightWhite, fontSize = 12.sp)
                            }
                            // D9 house
                            Box(modifier = Modifier.width(70.dp), contentAlignment = Alignment.Center) {
                                Text(if (planet.d9House > 0) "${planet.d9House}" else "—", color = StarlightWhite, fontSize = 12.sp)
                            }
                            // Status chips
                            Column(
                                modifier = Modifier.width(170.dp).padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val chips = planet.dignityChips().toMutableList()
                                if (planet.isRetrograde() && !chips.contains("Retrograde")) chips.add("Retrograde")
                                if (planet.combust && !chips.contains("Combust")) chips.add("Combust")
                                if (chips.isEmpty()) {
                                    Text(planet.status.ifBlank { "—" }, color = TextSecondary, fontSize = 11.sp)
                                } else {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        chips.forEach { chip -> KxDignityChip(chip) }
                                    }
                                    if (planet.status.isNotBlank() && !chips.any { planet.status.contains(it, ignoreCase = true) }) {
                                        Text(planet.status, color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        // Expand affordance
                        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SacredGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = CosmicBorder.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                W.t("kundali_planet_detail"),
                                color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                            val rules = planet.ruledHousesFmt.ifBlank {
                                planet.ruledHouses.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: ""
                            }
                            if (rules.isNotBlank()) KxLabelValue(W.t("kundali_lb_rules"), rules)
                            if (planet.aspects.isNotEmpty())
                                KxLabelValue(W.t("kundali_lb_aspects"), planet.aspects.joinToString(", "))
                            if (planet.baladiAvastha.isNotBlank())
                                KxLabelValue(W.t("kundali_lb_avastha"), planet.baladiAvastha)
                            if (planet.charaKaraka.isNotBlank())
                                KxLabelValue(W.t("kundali_lb_charakaraka"), planet.charaKaraka)
                            KxLabelValue(
                                W.t("kundali_lb_vargottama"),
                                W.t(if (planet.vargottama) "kundali_yes" else "kundali_no")
                            )
                        }
                    }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(W.t("kundali_scroll_table"), color = TextSecondary, fontSize = 11.sp)
    }

    item {
        KxAskAiCard(
            label = "Kundali · Planets",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Planets (full detail)"))
            (kundali?.planets ?: emptyList()).filter { it.name.isNotBlank() }.forEach { p ->
                sb.append("== ").append(p.name).append(" ==\n")
                sb.append("Sign: ").append(p.rashi).append(' ').append(p.degreeFormatted())
                    .append("  House: ").append(p.house).append('\n')
                if (p.nakshatra.isNotBlank()) {
                    sb.append("Nakshatra: ").append(p.nakshatra)
                    if (p.pada > 0) sb.append(" Pada ").append(p.pada)
                    if (p.nakshatraLord.isNotBlank()) sb.append(" (Lord ").append(p.nakshatraLord).append(')')
                    sb.append('\n')
                }
                if (p.d9Rashi.isNotBlank()) {
                    sb.append("D9: ").append(p.d9Rashi)
                    if (p.d9House > 0) sb.append(" House ").append(p.d9House)
                    sb.append('\n')
                }
                if (p.ruledHousesFmt.isNotBlank())
                    sb.append("Rules houses: ").append(p.ruledHousesFmt).append('\n')
                else if (p.ruledHouses.isNotEmpty())
                    sb.append("Rules houses: ").append(p.ruledHouses.joinToString(", ")).append('\n')
                if (p.aspects.isNotEmpty())
                    sb.append("Aspects houses: ").append(p.aspects.joinToString(", ")).append('\n')
                val chips = p.dignityChips().toMutableList()
                if (p.isRetrograde() && !chips.contains("Retrograde")) chips.add("Retrograde")
                if (p.combust && !chips.contains("Combust")) chips.add("Combust")
                if (p.vargottama && !chips.contains("Vargottama")) chips.add("Vargottama")
                sb.append("Status: ").append(if (chips.isNotEmpty()) chips.joinToString(", ") else p.status.ifBlank { "—" }).append('\n')
                if (p.baladiAvastha.isNotBlank()) sb.append("Avastha: ").append(p.baladiAvastha).append('\n')
                if (p.charaKaraka.isNotBlank()) sb.append("Chara Karaka: ").append(p.charaKaraka).append('\n')
            }
            sb.toString()
        }
    }
}

@Composable
private fun KxTableCell(text: String, widthDp: Int, isHeader: Boolean = false) {
    Box(modifier = Modifier.width(widthDp.dp).padding(horizontal = 4.dp)) {
        Text(
            text = text,
            color = if (isHeader) SacredGold else StarlightWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KxDignityChip(label: String) {
    val color = when (label.lowercase()) {
        "exalted" -> Color(0xFF10B981)
        "debilitated", "combust" -> Color(0xFFEF4444)
        "own sign", "yogakaraka", "vargottama" -> SacredGold
        "retrograde" -> Color(0xFF38BDF8)
        else -> TextSecondary
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
        Text(
            label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Tab 16 — Panchang (birth)
// ---------------------------------------------------------------------------

internal fun LazyListScope.panchangTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_panchang")) }
    val p = kundali?.panchang
    if (p == null) {
        item { KxEmpty(W.t("kundali_empty_panchang")) }
    } else {
        item {
            CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(W.t("kundali_sec_panchang_birth"), color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (p.tithi.isNotBlank())
                        KxLabelValue("Tithi", "$p.tithi${if (p.tithiNum > 0) " · ${p.tithiNum}/30" else ""}")
                    if (p.paksha.isNotBlank()) KxLabelValue("Paksha", p.paksha)
                    if (p.vara.isNotBlank()) KxLabelValue("Vara (Weekday)", p.vara)
                    if (p.nakshatra.isNotBlank()) KxLabelValue(
                        "Nakshatra",
                        "${p.nakshatra}${if (p.nakshatraHi.isNotBlank()) " (${p.nakshatraHi})" else ""}" +
                            "${if (p.pada > 0) " · Pada ${p.pada}" else ""}" +
                            "${if (p.nakshatraLord.isNotBlank()) " · Lord ${p.nakshatraLord}" else ""}"
                    )
                    if (p.yoga.isNotBlank()) KxLabelValue("Yoga", p.yoga)
                    if (p.karana.isNotBlank()) KxLabelValue("Karana", p.karana)
                    if (p.sunRashi.isNotBlank()) KxLabelValue("Sun Rashi", p.sunRashi)
                    if (p.moonRashi.isNotBlank()) KxLabelValue("Moon Rashi (Rashi)", p.moonRashi)
                }
            }
        }
    }
    kundali?.analysisTabs?.panchang?.takeIf { it.isNotBlank() }?.let { text ->
        item { KxSectionTitle(W.t("kundali_sec_panchang_analysis")) }
        item { KxAnalysisTextBlock(text) }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Birth Panchang",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Birth Panchang"))
            val p = kundali?.panchang
            if (p == null) {
                sb.append("No birth Panchang returned for this chart.\n")
            } else {
                sb.append("--- Panchang at birth ---\n")
                if (p.tithi.isNotBlank()) {
                    sb.append("Tithi: ").append(p.tithi)
                    if (p.tithiNum > 0) sb.append(" (${p.tithiNum}/30)")
                    sb.append('\n')
                }
                if (p.paksha.isNotBlank()) sb.append("Paksha: ").append(p.paksha).append('\n')
                if (p.vara.isNotBlank()) sb.append("Vara: ").append(p.vara).append('\n')
                if (p.nakshatra.isNotBlank()) {
                    sb.append("Nakshatra: ").append(p.nakshatra)
                    if (p.pada > 0) sb.append(" Pada ").append(p.pada)
                    if (p.nakshatraLord.isNotBlank()) sb.append(" (Lord ").append(p.nakshatraLord).append(')')
                    sb.append('\n')
                }
                if (p.yoga.isNotBlank()) sb.append("Yoga: ").append(p.yoga).append('\n')
                if (p.karana.isNotBlank()) sb.append("Karana: ").append(p.karana).append('\n')
                if (p.sunRashi.isNotBlank()) sb.append("Sun Rashi: ").append(p.sunRashi).append('\n')
                if (p.moonRashi.isNotBlank()) sb.append("Moon Rashi: ").append(p.moonRashi).append('\n')
            }
            kundali?.analysisTabs?.panchang?.takeIf { it.isNotBlank() }?.let {
                sb.append("--- Panchang Analysis ---\n").append(it.replace("|", "\n")).append('\n')
            }
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 17 — Shadbal (Shadbala + Bhavabala + Vimshopaka)
// ---------------------------------------------------------------------------

private val SHADBALA_COLS = listOf(
    "sthana" to "Sthana",
    "dig" to "Dig",
    "kala" to "Kala",
    "chesta" to "Cheshta",
    "naisargika" to "Naisargika",
    "drik" to "Drik"
)

internal fun LazyListScope.shadbalTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_shadbal")) }
    val sb = kundali?.shadbala
    if (sb == null || sb.shadbala.isNullOrEmpty()) {
        item { KxEmpty(W.t("kundali_empty_shadbal")) }
    } else {
        val shadbalaMap: Map<String, Map<String, Any>> = sb.shadbala ?: emptyMap()
        val order = kundali?.planets?.map { it.name }?.filter { it.isNotBlank() } ?: emptyList()
        val planets = (order + (shadbalaMap.keys - order.toSet())).distinct()
        item { KxSectionTitle(W.t("kundali_sec_shadbala6")) }
        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    Row(
                        modifier = Modifier.background(CosmicCharcoal, RoundedCornerShape(10.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KxTableCell("GRAHA", 100, isHeader = true)
                        SHADBALA_COLS.forEach { (_, label) -> KxTableCell(label.uppercase(), 84, isHeader = true) }
                        KxTableCell("TOTAL", 84, isHeader = true)
                        KxTableCell("VERDICT", 110, isHeader = true)
                    }
                    planets.forEachIndexed { index, planet ->
                        val m = shadbalaMap[planet] ?: shadbalaMap.entries
                            .firstOrNull { it.key.equals(planet, ignoreCase = true) }?.value
                        val bg = if (index % 2 == 0) Color.Transparent else CosmicCharcoal.copy(alpha = 0.45f)
                        Row(
                            modifier = Modifier.background(bg, RoundedCornerShape(8.dp))
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.width(100.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(planetGlyphFallback(planet), color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(planet, color = StarlightWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            SHADBALA_COLS.forEach { (key, _) ->
                                Box(modifier = Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                                    Text(anyToDisplay(m?.get(key)), color = StarlightWhite, fontSize = 12.sp)
                                }
                            }
                            val total = (m?.get("total_rupa") as? Number)?.toDouble()
                                ?: (m?.get("total") as? Number)?.toDouble()
                            Box(modifier = Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    total?.let { "%.2f".format(it) } ?: anyToDisplay(m?.get("total_rupa")),
                                    color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            val sufficient = m?.get("sufficient") as? Boolean
                            val minReq = (m?.get("min_required") as? Number)?.toDouble()
                            Box(modifier = Modifier.width(110.dp), contentAlignment = Alignment.Center) {
                                when {
                                    sufficient == true -> KxVerdictChip("Strong", AuspiciousGreen)
                                    sufficient == false -> KxVerdictChip("Weak", InauspiciousRed)
                                    total != null && minReq != null ->
                                        KxVerdictChip(
                                            if (total >= minReq) "Strong" else "Weak",
                                            if (total >= minReq) AuspiciousGreen else InauspiciousRed
                                        )
                                    else -> Text("—", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Scroll horizontally to see all six strengths.", color = TextSecondary, fontSize = 11.sp)
        }

        if (!sb.bhavabala.isNullOrEmpty()) {
            item { KxSectionTitle(W.t("kundali_sec_bhavabala")) }
            items(sb.bhavabala.entries.sortedBy { it.key.toIntOrNull() ?: 99 }) { (house, m) ->
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("House $house", color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val lord = anyToDisplay(m["lord"])
                            if (lord.isNotBlank()) Text("Lord: $lord", color = TextSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                anyToDisplay(m["total"]).ifBlank { anyToDisplay(m["score"]) },
                                color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                            val sav = anyToDisplay(m["sav"])
                            if (sav.isNotBlank()) Text("SAV $sav", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        if (!sb.vimshopaka.isNullOrEmpty()) {
            item { KxSectionTitle(W.t("kundali_sec_vimshopaka")) }
            items(sb.vimshopaka.entries.toList()) { (planet, m) ->
                // Honesty: a missing "total" must never become a fake 0/20 red
                // bar. Null stays null and the row says the score is unavailable.
                val total: Double? = (m["total"] as? Number)?.toDouble()
                val outOf: Double? = (m["out_of"] as? Number)?.toDouble()
                val pct: Double? = (m["percent"] as? Number)?.toDouble()
                    ?: if (outOf != null && outOf > 0 && total != null) total / outOf * 100 else null
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(planetGlyphFallback(planet), color = SacredGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(planet, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (total != null) {
                                Text(
                                    "${anyToDisplay(m["total"])} / ${anyToDisplay(m["out_of"])}",
                                    color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    "—",
                                    color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                            }
                        }
                        if (pct != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = (pct / 100.0).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (pct >= 50) AuspiciousGreen else if (pct >= 30) SacredGold else InauspiciousRed,
                                trackColor = CosmicCharcoal
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                W.t("honesty_vimshopaka_na"),
                                color = TextSecondary, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
    kundali?.analysisTabs?.shadbal?.takeIf { it.isNotBlank() }?.let { text ->
        item { KxSectionTitle(W.t("kundali_sec_shadbal_analysis")) }
        item { KxAnalysisTextBlock(text) }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Shadbal",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Shadbal (planetary strength)"))
            val data = kundali?.shadbala
            if (data == null || data.shadbala.isNullOrEmpty()) {
                sb.append("No Shadbala data returned for this chart.\n")
            } else {
                sb.append("--- Shadbala totals (rupas) ---\n")
                data.shadbala!!.forEach { (planet, m) ->
                    val total = (m["total_rupa"] as? Number)?.toDouble()
                        ?: (m["total"] as? Number)?.toDouble()
                    val sufficient = m["sufficient"] as? Boolean
                    sb.append(planet).append(": ")
                        .append(total?.let { "%.2f".format(it) } ?: "—")
                        .append(" rupas")
                    if (sufficient != null) sb.append(" — ").append(if (sufficient) "Strong" else "Weak")
                    sb.append('\n')
                }
                if (!data.bhavabala.isNullOrEmpty()) {
                    sb.append("--- Bhavabala (house strength) ---\n")
                    data.bhavabala!!.entries.sortedBy { it.key.toIntOrNull() ?: 99 }.forEach { (house, m) ->
                        sb.append("House ").append(house).append(": score ")
                            .append(anyToDisplay(m["total"]).ifBlank { anyToDisplay(m["score"]) })
                        val lord = anyToDisplay(m["lord"])
                        if (lord.isNotBlank()) sb.append(" (Lord ").append(lord).append(')')
                        sb.append('\n')
                    }
                }
                if (!data.vimshopaka.isNullOrEmpty()) {
                    sb.append("--- Vimshopaka (varga dignity, out of 20) ---\n")
                    data.vimshopaka!!.forEach { (planet, m) ->
                        sb.append(planet).append(": ")
                            .append(anyToDisplay(m["total"])).append(" / ").append(anyToDisplay(m["out_of"]))
                            .append('\n')
                    }
                }
            }
            kundali?.analysisTabs?.shadbal?.takeIf { it.isNotBlank() }?.let {
                sb.append("--- Shadbal Analysis ---\n").append(it.replace("|", "\n")).append('\n')
            }
            sb.toString()
        }
    }
}

@Composable
private fun KxVerdictChip(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
        Text(
            label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Tab 18 — Bhrigu Nadi (lazy: POST /kundali/bnn)
// ---------------------------------------------------------------------------

internal fun LazyListScope.bnnTab(
    kundali: KundaliResponse?,
    state: ReportUiState,
    onRetry: () -> Unit,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_bnn")) }
    item {
        KxReportState(
            state = state,
            tabIndex = 17,
            onRetry = onRetry,
            summaryText = kundali?.analysisTabs?.bnn ?: ""
        )
    }

    item {
        KxAskAiCard(
            label = "Kundali · Bhrigu Nadi",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Bhrigu Nadi"))
            val text = state.data?.bestText()?.ifBlank { kundali?.analysisTabs?.bnn } ?: ""
            if (text.isNotBlank()) sb.append("--- Bhrigu Nadi report ---\n")
                .append(text.replace("|", "\n")).append('\n')
            else sb.append("Bhrigu Nadi report not loaded for this chart.\n")
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 19 — Lal Kitab (lazy: POST /kundali/lal-kitab, full report)
// ---------------------------------------------------------------------------

internal fun LazyListScope.lalKitabFullTab(
    kundali: KundaliResponse?,
    state: ReportUiState,
    onRetry: () -> Unit,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_lalkitab")) }
    val summary = kundali?.lalKitab
    if (summary != null && (summary.lagnaRashi.isNotBlank() || summary.note.isNotBlank() || summary.yogas.isNotEmpty())) {
        if (summary.lagnaRashi.isNotBlank()) {
            item {
                CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
                    Text(
                        "${W.t("kundali_lb_lalkitab_lagna")}: ${summary.lagnaRashi}",
                        color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
            }
        }
        if (summary.note.isNotBlank()) {
            item { KxSectionTitle(W.t("kundali_sec_lalkitab_note")) }
            item { KxAnalysisTextBlock(summary.note) }
        }
        if (summary.yogas.isNotEmpty()) {
            item { KxSectionTitle(W.t("kundali_sec_lalkitab_yogas")) }
            items(summary.yogas) { yoga ->
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        yoga,
                        color = StarlightWhite.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
    item {
        KxReportState(state = state, tabIndex = 18, onRetry = onRetry)
    }

    item {
        KxAskAiCard(
            label = "Kundali · Lal Kitab",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Lal Kitab"))
            kundali?.lalKitab?.let { lk ->
                if (lk.lagnaRashi.isNotBlank())
                    sb.append("Lal Kitab Lagna: ").append(lk.lagnaRashi).append('\n')
                if (lk.note.isNotBlank()) sb.append(lk.note).append('\n')
                if (lk.yogas.isNotEmpty())
                    sb.append("Lal Kitab yogas: ").append(lk.yogas.joinToString("; ")).append('\n')
            }
            val text = state.data?.bestText() ?: ""
            if (text.isNotBlank()) sb.append("--- Lal Kitab report ---\n")
                .append(text.replace("|", "\n")).append('\n')
            else sb.append("Lal Kitab report not loaded for this chart.\n")
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 20 — Numerology (embedded hub link, birth data pre-filled)
// ---------------------------------------------------------------------------

internal fun LazyListScope.numerologyTab(
    kundali: KundaliResponse?,
    onOpenNumerology: () -> Unit,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_numerology")) }
    item {
        Text(
            W.t("kundali_num_intro"),
            color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
    if (kundali == null) {
        item { KxEmpty(W.t("kundali_empty_chart")) }
        return
    }
    val birth = kundali.birth
    item {
        CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(W.t("kundali_sec_num_birth"), color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (birth.name.isNotBlank()) KxLabelValue(W.t("kundali_lb_name"), birth.name)
                if (birth.year > 0) KxLabelValue(
                    W.t("kundali_lb_dob"),
                    "%04d-%02d-%02d".format(birth.year, birth.month, birth.day)
                )
                val dobApi = "%04d-%02d-%02d".format(birth.year, birth.month, birth.day)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (birth.year > 0) {
                            com.omastro.app.ui.navigation.PendingNumerologyHolder.set(
                                birth.name, dobApi
                            )
                        }
                        onOpenNumerology()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        W.t("kundali_open_num_hub"),
                        color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Text(
                    W.t("kundali_num_carry"),
                    color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    item {
        CosmicCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(W.t("kundali_sec_num_inside"), color = StarlightWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    W.t("kundali_num_inside_body"),
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Numerology",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Numerology (Ank Jyotish)"))
            val birth = kundali?.birth
            if (birth != null && birth.year > 0) {
                sb.append("Name: ").append(birth.name.ifBlank { "—" }).append('\n')
                sb.append("Date of birth: ")
                    .append("%04d-%02d-%02d".format(birth.year, birth.month, birth.day)).append('\n')
                sb.append("Compute Mulank, Bhagyank and other numbers from this birth date ")
                    .append("using standard Chaldean/Vedic numerology.\n")
            } else {
                sb.append("No birth details available for numerology.\n")
            }
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 21 — Char Dasha / Jaimini (lazy: POST /kundali/jaimini)
// ---------------------------------------------------------------------------

internal fun LazyListScope.charDashaTab(
    state: JaiminiUiState,
    onRetry: () -> Unit,
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_chardasha")) }
    item {
        KxJaiminiState(state, 20, onRetry) { data ->
            CharDashaContent(data)
        }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Char Dasha",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Char Dasha (Jaimini)"))
            val data = state.data
            if (data == null) {
                sb.append("Char Dasha data not loaded for this chart.\n")
            } else {
                data.curCharMD?.let { md ->
                    if (md.signEn.isNotBlank()) {
                        sb.append("Current Char Mahadasha: ").append(md.signEn)
                            .append(" (").append(julianDayToDateString(md.startJD))
                            .append(" → ").append(julianDayToDateString(md.endJD)).append(')')
                            .append('\n')
                    }
                }
                data.curCharAD?.let { ad ->
                    if (ad.signEn.isNotBlank()) {
                        sb.append("Current Char Antardasha: ").append(ad.signEn)
                            .append(" (").append(julianDayToDateString(ad.startJD))
                            .append(" → ").append(julianDayToDateString(ad.endJD)).append(')')
                            .append('\n')
                    }
                }
                data.karakas?.details?.ordered()?.forEach { (label, k) ->
                    if (k != null && k.name.isNotBlank())
                        sb.append(label).append(": ").append(k.name)
                            .append(" in ").append(k.rashi).append('\n')
                }
                if (data.charDashas.isNotEmpty()) {
                    sb.append("--- Char Mahadasha sequence ---\n")
                    data.charDashas.forEach { cd ->
                        sb.append("- ").append(cd.signEn)
                            .append(" (").append(julianDayToDateString(cd.startJD))
                            .append(" → ").append(julianDayToDateString(cd.endJD)).append(")")
                        if (cd.years > 0) sb.append(" %.1fy".format(cd.years))
                        sb.append('\n')
                    }
                }
                if (data.charAntars.isNotEmpty()) {
                    sb.append("--- Char Antardashas (current cycle) ---\n")
                    data.charAntars.forEach { ca ->
                        sb.append("- ").append(ca.signEn)
                            .append(" (").append(julianDayToDateString(ca.startJD))
                            .append(" → ").append(julianDayToDateString(ca.endJD)).append(")")
                            .append('\n')
                    }
                }
            }
            sb.toString()
        }
    }
}

@Composable
private fun CharDashaContent(data: JaiminiData) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Chara Karakas
        val karakas = data.karakas?.details
        if (karakas != null) {
            Text(W.t("kundali_sec_chara_karakas"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            karakas.ordered().forEach { (label, k) ->
                if (k != null && k.name.isNotBlank()) {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    "${planetGlyphFallback(k.name)} ${k.name}",
                                    color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                            Text(
                                "${rashiGlyph(rashiNameToIndex(k.rashi))} ${k.rashi}",
                                color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Current Char MD / AD hero
        val curMD = data.curCharMD
        val curAD = data.curCharAD
        if (curMD != null && curMD.signEn.isNotBlank()) {
            Text(W.t("kundali_sec_cur_char"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            CosmicCard(modifier = Modifier.fillMaxWidth(), borderGold = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current Char Dasha", color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = SacredGold, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "ACTIVE", color = CosmicDeepBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    KxCharDashaRow("Mahadasha", curMD)
                    if (curAD != null && curAD.signEn.isNotBlank()) {
                        KxCharDashaRow("Antardasha", curAD)
                    }
                }
            }
        }

        // Char Mahadasha sequence
        if (data.charDashas.isNotEmpty()) {
            Text(
                W.t("kundali_sec_char_seq").replace("{n}", "${data.charDashas.size}"),
                color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
            val curSign = curMD?.sign
            data.charDashas.forEach { cd ->
                val isCurrent = curSign != null && cd.sign == curSign &&
                    cd.startJD == curMD?.startJD && cd.endJD == curMD?.endJD
                CosmicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isCurrent) SacredGold else StarlightWhite.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                rashiGlyph(cd.sign + 1), color = SacredGold,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cd.signEn, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = SacredGold, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                "ACTIVE", color = CosmicDeepBlue, fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                val start = julianDayToDateString(cd.startJD)
                                val end = julianDayToDateString(cd.endJD)
                                Text(
                                    if (start.isNotBlank() && end.isNotBlank()) "$start → $end" else "",
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                        }
                        if (cd.years > 0) {
                            Text(
                                "${if (cd.years % 1.0 == 0.0) cd.years.toInt().toString() else "%.1f".format(cd.years)}y",
                                color = RadiantAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Char Antardashas
        if (data.charAntars.isNotEmpty()) {
            Text(
                W.t("kundali_sec_char_antars"),
                color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
            data.charAntars.forEach { ca ->
                val isCurrent = curAD != null && ca.sign == curAD.sign &&
                    ca.startJD == curAD.startJD && ca.endJD == curAD.endJD
                CosmicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isCurrent) SacredGold else StarlightWhite.copy(alpha = 0.08f)
                ) {
                    KxCharDashaRow(
                        label = if (isCurrent) "${ca.signEn} — ACTIVE" else ca.signEn,
                        cd = ca
                    )
                }
            }
        }
    }
}

@Composable
private fun KxCharDashaRow(label: String, cd: CharDasha) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text(
                "${rashiGlyph(cd.sign + 1)} ${cd.signEn}",
                color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val start = julianDayToDateString(cd.startJD)
            val end = julianDayToDateString(cd.endJD)
            if (start.isNotBlank() && end.isNotBlank()) {
                Text("$start → $end", color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (cd.years > 0) {
                Text(
                    "${if (cd.years % 1.0 == 0.0) cd.years.toInt().toString() else "%.2f".format(cd.years)} years",
                    color = TextSecondary, fontSize = 10.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 22 — Faladesh (transit forecast + AI chat note)
// ---------------------------------------------------------------------------

internal fun LazyListScope.faladeshTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_faladesh")) }
    item {
        Text(
            W.t("kundali_faladesh_intro"),
            color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
    kundali?.saturnTransit?.let { st ->
        if (st.active || st.nameEn.isNotBlank()) {
            item { KxSectionTitle(W.t("kundali_sec_saturn_only")) }
            item { KxSaturnAlertCard(st.copy(active = true)) }
        }
    }
    val transits = kundali?.currentTransits ?: emptyMap()
    if (transits.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_transits")) }
    } else {
        item { KxSectionTitle(W.t("kundali_sec_gochar_detail")) }
        items(transits.entries.toList()) { (planet, t) ->
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            planetGlyphFallback(planet), color = SacredGold,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(planet, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (t.retro) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(color = NeutralBlue.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "Retrograde", color = NeutralBlue, fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "${rashiGlyph(rashiNameToIndex(t.rashi))} ${t.rashi}" +
                                    if (t.rashiDeg > 0) " · ${"%.1f".format(t.rashiDeg)}°" else "",
                                color = TextSecondary, fontSize = 12.sp
                            )
                        }
                        if (t.natalMoonHouse > 0) {
                            Surface(color = SacredGold.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "House ${t.natalMoonHouse}\nfrom Moon",
                                    color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (t.transitFocus.isNotBlank()) {
                        Text(
                            t.transitFocus,
                            color = StarlightWhite.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
    item {
        KxAskAiCard(
            label = "Kundali · Faladesh",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Faladesh (transit forecast)"))
            kundali?.saturnTransit?.let { st ->
                if (st.active || st.nameEn.isNotBlank()) {
                    sb.append("Saturn transit: ").append(st.nameEn)
                    if (st.type.isNotBlank()) sb.append(" (").append(st.type).append(')')
                    if (st.phase.isNotBlank()) sb.append(" phase ").append(st.phase)
                    sb.append('\n')
                    if (st.descriptionEn.isNotBlank()) sb.append(st.descriptionEn).append('\n')
                }
            }
            val transits = kundali?.currentTransits ?: emptyMap()
            if (transits.isEmpty()) {
                sb.append("No current transit data returned for this chart.\n")
            } else {
                sb.append("--- Current Gochar, planet by planet ---\n")
                transits.forEach { (planet, t) ->
                    sb.append("- ").append(planet).append(": ").append(t.rashi)
                    if (t.rashiDeg > 0) sb.append(" %.1f°".format(t.rashiDeg))
                    if (t.retro) sb.append(" (Retrograde)")
                    if (t.natalMoonHouse > 0) sb.append(" — House ").append(t.natalMoonHouse).append(" from Moon")
                    sb.append('\n')
                    if (t.transitFocus.isNotBlank()) sb.append("  Focus: ").append(t.transitFocus).append('\n')
                }
            }
            sb.toString()
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 23 — Remedies (5 sub-tabs, auto-computed from chart data — no AI call)
// ---------------------------------------------------------------------------

/** Standard Vedic remedy correspondences per planet (reference data). */
private data class PlanetRemedy(
    val gem: String,
    val rudraksha: String,
    val metal: String,
    val root: String,
    val mantra: String,
    val daan: String,
    val day: String
)

private val PLANET_REMEDIES = mapOf(
    "sun" to PlanetRemedy(
        "Ruby (Manik)", "1 or 12 Mukhi", "Gold / Copper", "Bel (Bilva) root",
        "Om Suryaya Namah", "Wheat, jaggery, copper vessel", "Sunday"
    ),
    "moon" to PlanetRemedy(
        "Pearl (Moti)", "2 Mukhi", "Silver", "Khirni root",
        "Om Chandraya Namah", "Rice, milk, white cloth", "Monday"
    ),
    "mars" to PlanetRemedy(
        "Red Coral (Moonga)", "3 Mukhi", "Copper", "Anantmool root",
        "Om Mangalaya Namah", "Red lentils (masoor dal)", "Tuesday"
    ),
    "mercury" to PlanetRemedy(
        "Emerald (Panna)", "4 Mukhi", "Brass", "Vidhara root",
        "Om Budhaya Namah", "Green moong, books", "Wednesday"
    ),
    "jupiter" to PlanetRemedy(
        "Yellow Sapphire (Pukhraj)", "5 Mukhi", "Gold", "Banana (Kela) root",
        "Om Gurave Namah", "Turmeric, chana dal, yellow cloth", "Thursday"
    ),
    "venus" to PlanetRemedy(
        "Diamond (Heera) / Opal", "6 Mukhi", "Silver", "Sarponkha root",
        "Om Shukraya Namah", "White sweets, rice, perfume", "Friday"
    ),
    "saturn" to PlanetRemedy(
        "Blue Sapphire (Neelam)", "7 Mukhi", "Iron / Steel", "Shami root",
        "Om Sham Shanicharaya Namah", "Black sesame, mustard oil", "Saturday"
    ),
    "rahu" to PlanetRemedy(
        "Hessonite (Gomed)", "8 Mukhi", "Lead", "Durva grass",
        "Om Rahave Namah", "Black blanket, black sesame", "Saturday"
    ),
    "ketu" to PlanetRemedy(
        "Cat's Eye (Lehsunia)", "9 Mukhi", "", "Kusha (Darbha) grass",
        "Om Ketave Namah", "Feed dogs, donate blankets", "Tuesday"
    )
)

private fun remedyKey(planetName: String): String {
    val n = planetName.lowercase().trim()
    return when {
        n.startsWith("sun") || n == "surya" -> "sun"
        n.startsWith("moon") || n == "chandra" -> "moon"
        n.startsWith("mars") || n == "mangal" -> "mars"
        n.startsWith("mercury") || n == "budh" -> "mercury"
        n.startsWith("jupiter") || n == "guru" -> "jupiter"
        n.startsWith("venus") || n == "shukra" -> "venus"
        n.startsWith("saturn") || n == "shani" -> "saturn"
        n.startsWith("rahu") -> "rahu"
        n.startsWith("ketu") -> "ketu"
        else -> ""
    }
}

private val REMEDY_SUBTAB_KEYS = listOf(
    "kundali_remedy_tab_ratna",
    "kundali_remedy_tab_rudraksh",
    "kundali_remedy_tab_dhatu",
    "kundali_remedy_tab_jad",
    "kundali_remedy_tab_vedic"
)

internal fun LazyListScope.remediesTab(
    kundali: KundaliResponse?,
    onOpenAiChat: () -> Unit
) {
    item { KxSectionTitle(W.t("kundali_sec_remedies")) }
    item {
        Text(
            W.t("kundali_remedies_intro"),
            color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
    if (kundali == null || kundali.planets.isEmpty()) {
        item { KxEmpty(W.t("kundali_empty_chart")) }
        return
    }

    // Weak/afflicted planets drive every sub-tab; strong planets get strengthening notes
    val planets = kundali.planets.filter { it.name.isNotBlank() }
    val weak = planets.filter {
        it.dignityChips().contains("Debilitated") || it.combust
    }
    val lagnaLord = kundali.lagna.lord
    val focusPlanets = (weak.map { it.name } + listOf(lagnaLord)).filter { it.isNotBlank() }.distinct()

    item {
        var selected by remember { mutableStateOf(0) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(REMEDY_SUBTAB_KEYS.size) { i ->
                    val isSelected = selected == i
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) SacredGold else CosmicCharcoal,
                        modifier = Modifier.clickable { selected = i }
                    ) {
                        Text(
                            W.t(REMEDY_SUBTAB_KEYS[i]),
                            color = if (isSelected) CosmicDeepBlue else StarlightWhite,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            when (selected) {
                0 -> RatnaContent(planets, focusPlanets, weak.map { it.name }.toSet())
                1 -> RudrakshContent(planets, focusPlanets)
                2 -> DhatuContent(planets, focusPlanets)
                3 -> JadContent(planets, focusPlanets)
                else -> VedicUpayContent(kundali, planets, focusPlanets)
            }
        }
    }

    item {
        KxAskAiCard(
            label = "Kundali · Remedies",
            onOpenAiChat = onOpenAiChat
        ) {
            val sb = StringBuilder(kxSeekerHeader(kundali, "Remedies (Upay)"))
            val planets = (kundali?.planets ?: emptyList()).filter { it.name.isNotBlank() }
            val weakNames = planets.filter {
                it.dignityChips().contains("Debilitated") || it.combust
            }.map { it.name }
            if (weakNames.isNotEmpty())
                sb.append("Weak/afflicted planets: ").append(weakNames.joinToString(", ")).append('\n')
            val lagnaLord = kundali?.lagna?.lord ?: ""
            if (lagnaLord.isNotBlank()) sb.append("Lagna lord: ").append(lagnaLord).append('\n')
            sb.append("--- Recommended remedies ---\n")
            (weakNames + listOf(lagnaLord)).filter { it.isNotBlank() }.distinct().forEach { name ->
                val r = PLANET_REMEDIES[remedyKey(name)] ?: return@forEach
                sb.append("- ").append(name).append(": gem ").append(r.gem)
                    .append(" | rudraksha ").append(r.rudraksha)
                    .append(" | metal ").append(r.metal)
                    .append(" | root ").append(r.root)
                    .append(" | mantra \"").append(r.mantra).append('"')
                    .append(" | daan ").append(r.daan)
                    .append(" | day ").append(r.day).append('\n')
            }
            val doshaRemedies = (kundali?.doshas ?: emptyList()).filter { it.active }
                .flatMap { d -> d.remedies.map { r -> d.name to r } }
            if (doshaRemedies.isNotEmpty()) {
                sb.append("--- Dosha-specific upay ---\n")
                doshaRemedies.forEach { (doshaName, remedy) ->
                    sb.append("- ").append(doshaName).append(": ").append(remedy).append('\n')
                }
            }
            sb.toString()
        }
    }
}

@Composable
private fun RemedyHeader(planetName: String, note: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            planetGlyphFallback(planetName), color = SacredGold,
            fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(planetName, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (note.isNotBlank()) Text(note, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RatnaContent(planets: List<ApiPlanet>, focus: List<String>, weakNames: Set<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            W.t("kundali_rem_ratna"),
            color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp
        )
        focus.forEach { name ->
            val r = PLANET_REMEDIES[remedyKey(name)] ?: return@forEach
            val planet = planets.firstOrNull { it.name.equals(name, ignoreCase = true) }
            val chips = planet?.dignityChips()?.joinToString(", ") ?: ""
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemedyHeader(
                        name,
                        when {
                            weakNames.any { it.equals(name, ignoreCase = true) } -> "${W.t("kundali_rem_afflicted")}${if (chips.isNotBlank()) " · $chips" else ""}"
                            else -> "${W.t("kundali_rem_lagna_lord")}${if (chips.isNotBlank()) " · $chips" else ""}"
                        }
                    )
                    Text(
                        W.t("kundali_rem_recommended").replace("{g}", r.gem),
                        color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        W.t("kundali_rem_ratna_how")
                            .replace("{d}", r.day)
                            .replace("{m}", r.metal.ifBlank { W.t("kundali_rem_as_advised") }),
                        color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RudrakshContent(planets: List<ApiPlanet>, focus: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            W.t("kundali_rem_rudra"),
            color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp
        )
        focus.forEach { name ->
            val r = PLANET_REMEDIES[remedyKey(name)] ?: return@forEach
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemedyHeader(name, W.t("kundali_rem_for").replace("{p}", planetHindiFallback(name).ifBlank { name }))
                    Text(
                        W.t("kundali_rem_wear_rudra").replace("{r}", r.rudraksha),
                        color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        W.t("kundali_rem_energise")
                            .replace("{m}", r.mantra)
                            .replace("{d}", r.day),
                        color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DhatuContent(planets: List<ApiPlanet>, focus: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            W.t("kundali_rem_dhatu"),
            color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp
        )
        focus.forEach { name ->
            val r = PLANET_REMEDIES[remedyKey(name)] ?: return@forEach
            if (r.metal.isBlank()) return@forEach
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemedyHeader(name, "")
                    Text(
                        W.t("kundali_rem_metal").replace("{m}", r.metal),
                        color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        W.t("kundali_rem_dhatu_how")
                            .replace("{m}", r.metal.lowercase())
                            .replace("{d}", r.day)
                            .replace("{p}", name),
                        color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun JadContent(planets: List<ApiPlanet>, focus: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            W.t("kundali_rem_jad"),
            color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp
        )
        focus.forEach { name ->
            val r = PLANET_REMEDIES[remedyKey(name)] ?: return@forEach
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemedyHeader(name, "")
                    Text(
                        W.t("kundali_rem_root").replace("{r}", r.root),
                        color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        W.t("kundali_rem_jad_how")
                            .replace("{m}", r.mantra)
                            .replace("{r}", r.root.lowercase())
                            .replace("{d}", r.day),
                        color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VedicUpayContent(kundali: KundaliResponse, planets: List<ApiPlanet>, focus: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            W.t("kundali_rem_vedic"),
            color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp
        )
        // Chart-driven planet upay
        focus.forEach { name ->
            val r = PLANET_REMEDIES[remedyKey(name)] ?: return@forEach
            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemedyHeader(name, W.t("kundali_rem_every").replace("{d}", r.day))
                    Text(
                        W.t("kundali_rem_mantra").replace("{m}", r.mantra),
                        color = StarlightWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        W.t("kundali_rem_daan").replace("{d}", r.daan),
                        color = StarlightWhite.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
        // Dosha remedies straight from the server
        val doshaRemedies = kundali.doshas.filter { it.active }
            .flatMap { d -> d.remedies.map { r -> d.name to r } }
        if (doshaRemedies.isNotEmpty()) {
            Text(W.t("kundali_rem_dosha_upay"), color = StarlightWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            doshaRemedies.forEach { (doshaName, remedy) ->
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(doshaName, color = InauspiciousRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(remedy, color = StarlightWhite, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}
