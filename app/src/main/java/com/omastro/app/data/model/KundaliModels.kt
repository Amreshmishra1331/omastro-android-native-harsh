package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Request
// ---------------------------------------------------------------------------

data class KundaliRequest(
    @SerializedName("name") val name: String,
    @SerializedName("dob") val dob: String, // YYYY-MM-DD
    @SerializedName("tob_hr") val tobHr: Int = 8,
    @SerializedName("tob_mn") val tobMn: Int = 30,
    @SerializedName("tob") val tob: String = "08:30",
    // Cycle 4 honesty: no silent Delhi fallback. lat/lon have NO coordinate
    // default — any caller that forgets them fails loudly in init instead of
    // silently calculating a New Delhi chart. Every screen validates coords
    // before constructing this request.
    @SerializedName("pob") val pob: String = "",
    @SerializedName("lat") val lat: Double = Double.NaN,
    @SerializedName("lon") val lon: Double = Double.NaN,
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("ayanamsa") val ayanamsa: String = "lahiri"
) {
    init {
        require(lat.isFinite() && lon.isFinite()) {
            "KundaliRequest requires explicit coordinates — silent Delhi fallback removed."
        }
    }
}

// ---------------------------------------------------------------------------
// Envelope: POST /kundali returns {"success": true, "data": {...}}
// ---------------------------------------------------------------------------

data class KundaliEnvelope(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("data") val data: KundaliResponse? = null
)

// ---------------------------------------------------------------------------
// Main payload (== "data" of the envelope). All fields mirror the production
// API exactly — nothing invented client-side.
// ---------------------------------------------------------------------------

data class KundaliResponse(
    @SerializedName("meta") val meta: KundaliMeta = KundaliMeta(),
    @SerializedName("birth") val birth: KundaliBirth = KundaliBirth(),
    @SerializedName("lagna") val lagna: LagnaInfo = LagnaInfo(),
    @SerializedName("navamshaLagna") val navamshaLagna: SimpleLagna? = null,
    @SerializedName("suryaLagna") val suryaLagna: SimpleLagna? = null,
    @SerializedName("chandraLagna") val chandraLagna: SimpleLagna? = null,
    @SerializedName("planets") val planets: List<ApiPlanet> = emptyList(),
    @SerializedName("houses") val houses: List<HouseInfo> = emptyList(),
    @SerializedName("d9Houses") val d9Houses: List<HouseInfo> = emptyList(),
    @SerializedName("yogas") val yogas: List<ApiYoga> = emptyList(),
    @SerializedName("doshas") val doshas: List<ApiDosha> = emptyList(),
    @SerializedName("vimshottariDasha") val vimshottariDasha: VimshottariDasha? = null,
    @SerializedName("ashtakavarga") val ashtakavarga: AshtakavargaData? = null,
    @SerializedName("lalKitab") val lalKitab: LalKitabData? = null,
    @SerializedName("divisionalCharts") val divisionalCharts: Map<String, DivisionalChart> = emptyMap(),
    // --- Expanded fields for the 23-tab Kundali (verified live 2026-10-09) ---
    @SerializedName("chalitHouses") val chalitHouses: List<HouseInfo> = emptyList(),
    @SerializedName("suryaChart") val suryaChart: ChartData? = null,
    @SerializedName("chandraChart") val chandraChart: ChartData? = null,
    @SerializedName("chalitChart") val chalitChart: ChartData? = null,
    @SerializedName("allDivisionalCharts") val allDivisionalCharts: Map<String, DivisionalChart> = emptyMap(),
    @SerializedName("panchang") val panchang: BirthPanchang? = null,
    @SerializedName("sodasvarga") val sodasvarga: Map<String, List<SodasvargaEntry>>? = null,
    @SerializedName("shadbala") val shadbala: ShadbalaData? = null,
    @SerializedName("saturnTransit") val saturnTransit: SaturnTransit? = null,
    @SerializedName("currentTransits") val currentTransits: Map<String, TransitInfo> = emptyMap(),
    @SerializedName("analysis_tabs") val analysisTabs: AnalysisTabs? = null
) {
    /** Lagna rashi index 1..12, resolved from the houses list or lagna name. */
    fun lagnaSignIndex(): Int {
        houses.firstOrNull { it.number == 1 }?.rashi?.let { return rashiNameToIndex(it) }
        return rashiNameToIndex(lagna.rashi)
    }

    /** Navamsha lagna rashi index 1..12. */
    fun navamshaLagnaIndex(): Int {
        navamshaLagna?.rashi?.let { return rashiNameToIndex(it) }
        d9Houses.firstOrNull { it.number == 1 }?.rashi?.let { return rashiNameToIndex(it) }
        return 1
    }

    /** house number -> planet names occupying it (D1). */
    fun d1PlanetsByHouse(): Map<Int, List<String>> =
        houses.associate { it.number to it.planets.map { p -> p.name } }

    /** house number -> planet names occupying it (D9). */
    fun d9PlanetsByHouse(): Map<Int, List<String>> =
        d9Houses.associate { it.number to it.planets.map { p -> p.name } }

    /** Planet name -> server-provided glyph (falls back to a local map). */
    fun symbolFor(planetName: String): String {
        planets.firstOrNull { it.name.equals(planetName, ignoreCase = true) }
            ?.takeIf { it.symbol.isNotBlank() }?.let { return it.symbol }
        return planetGlyphFallback(planetName)
    }
}

data class KundaliMeta(
    @SerializedName("name") val name: String = "",
    @SerializedName("lagna") val lagna: String = "",
    @SerializedName("lagna_en") val lagnaEn: String = "",
    @SerializedName("lagna_hi") val lagnaHi: String = "",
    @SerializedName("d9_lagna") val d9Lagna: String = "",
    @SerializedName("moon_sign") val moonSign: String = "",
    @SerializedName("moon_sign_en") val moonSignEn: String = "",
    @SerializedName("moon_sign_hi") val moonSignHi: String = "",
    @SerializedName("nakshatra") val nakshatra: String = "",
    @SerializedName("nakshatra_name") val nakshatraName: String = "",
    @SerializedName("nakshatra_pada") val nakshatraPada: Int = 0,
    @SerializedName("nakshatra_hi") val nakshatraHi: String = "",
    @SerializedName("cur_dasha") val curDasha: String = "",
    @SerializedName("doshas") val doshasSummary: String = "",
    @SerializedName("yogas") val yogasSummary: String = "",
    @SerializedName("ayanamsa") val ayanamsa: String = "lahiri"
)

data class KundaliBirth(
    @SerializedName("name") val name: String = "",
    @SerializedName("year") val year: Int = 0,
    @SerializedName("month") val month: Int = 0,
    @SerializedName("day") val day: Int = 0,
    @SerializedName("hour") val hour: Int = 0,
    @SerializedName("minute") val minute: Int = 0,
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("place") val place: String = "",
    @SerializedName("gender") val gender: String = ""
)

data class LagnaInfo(
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("rashiHi") val rashiHi: String = "",
    @SerializedName("lord") val lord: String = "",
    @SerializedName("deg") val deg: Int = 0,
    @SerializedName("min") val min: Int = 0,
    @SerializedName("sec") val sec: Int = 0
) {
    fun formatted(): String = "$rashi ${deg}°${min}'"
}

data class SimpleLagna(
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("rashiHi") val rashiHi: String = "",
    @SerializedName("deg") val deg: Int = 0,
    @SerializedName("min") val min: Int = 0
)

// ---------------------------------------------------------------------------
// Planets (full server detail)
// ---------------------------------------------------------------------------

data class ApiPlanet(
    @SerializedName("name") val name: String = "",
    @SerializedName("nameHi") val nameHi: String = "",
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("rashiHi") val rashiHi: String = "",
    @SerializedName("rashiIndex") val rashiIndex: Int = 1,
    @SerializedName("deg") val deg: Int = 0,
    @SerializedName("min") val min: Int = 0,
    @SerializedName("sec") val sec: Int = 0,
    @SerializedName("house") val house: Int = 1,
    @SerializedName("nakshatra") val nakshatra: String = "",
    @SerializedName("nakshatraHi") val nakshatraHi: String = "",
    @SerializedName("nakshatraLord") val nakshatraLord: String = "",
    @SerializedName("pada") val pada: Int = 0,
    @SerializedName("d9Rashi") val d9Rashi: String = "",
    @SerializedName("d9RashiHi") val d9RashiHi: String = "",
    @SerializedName("d9House") val d9House: Int = 0,
    @SerializedName("vargottama") val vargottama: Boolean = false,
    @SerializedName("status") val status: String = "",
    @SerializedName("retrograde") val retrograde: Boolean = false,
    @SerializedName("combust") val combust: Boolean = false,
    @SerializedName("ruledHouses") val ruledHouses: List<Int> = emptyList(),
    @SerializedName("ruledHousesFmt") val ruledHousesFmt: String = "",
    @SerializedName("aspects") val aspects: List<Int> = emptyList(),
    @SerializedName("baladiAvastha") val baladiAvastha: String = "",
    @SerializedName("specialStatus") val specialStatus: List<String> = emptyList(),
    @SerializedName("charaKaraka") val charaKaraka: String = ""
) {
    fun degreeFormatted(): String = "${deg}°${min}'"

    /** Dignity chips derived from the server's specialStatus array. */
    fun dignityChips(): List<String> {
        val chips = mutableListOf<String>()
        specialStatus.forEach {
            when {
                it.equals("Own Sign", ignoreCase = true) -> chips.add("Own Sign")
                it.equals("Exalted", ignoreCase = true) -> chips.add("Exalted")
                it.equals("Debilitated", ignoreCase = true) -> chips.add("Debilitated")
                it.equals("Vargottama", ignoreCase = true) -> chips.add("Vargottama")
                it.equals("Yogakaraka", ignoreCase = true) -> chips.add("Yogakaraka")
                it.equals("Combust", ignoreCase = true) -> chips.add("Combust")
                it.equals("Retrograde", ignoreCase = true) -> chips.add("Retrograde")
            }
        }
        return chips.distinct()
    }

    fun isRetrograde(): Boolean = retrograde || specialStatus.any { it.equals("Retrograde", ignoreCase = true) }
}

// ---------------------------------------------------------------------------
// Houses (D1 / D9)
// ---------------------------------------------------------------------------

data class HousePlanet(
    @SerializedName("name") val name: String = "",
    @SerializedName("nameHi") val nameHi: String = "",
    @SerializedName("deg") val deg: String = "",
    @SerializedName("status") val status: String = ""
)

data class BhavBala(
    @SerializedName("score") val score: Double = 0.0,
    @SerializedName("label") val label: String = ""
)

data class HouseInfo(
    @SerializedName("number") val number: Int = 1,
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("rashiHi") val rashiHi: String = "",
    @SerializedName("lord") val lord: String = "",
    @SerializedName("sav") val sav: Int = 0,
    @SerializedName("planets") val planets: List<HousePlanet> = emptyList(),
    @SerializedName("bhavBala") val bhavBala: BhavBala? = null
)

// ---------------------------------------------------------------------------
// Yogas & Doshas (separate server lists)
// ---------------------------------------------------------------------------

data class ApiYoga(
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("prediction") val prediction: String = ""
) {
    fun isAuspicious(): Boolean = !type.equals("negative", ignoreCase = true)
    fun typeLabel(): String = if (isAuspicious()) "Yoga" else "Ava-Yoga"
}

data class ApiDosha(
    @SerializedName("name") val name: String = "",
    @SerializedName("active") val active: Boolean = false,
    @SerializedName("severity") val severity: String = "",
    @SerializedName("detail") val detail: String = "",
    @SerializedName("analysis") val analysis: String = "",
    @SerializedName("prediction") val prediction: String = "",
    @SerializedName("remedies") val remedies: List<String> = emptyList()
)

// ---------------------------------------------------------------------------
// Vimshottari Dasha (nested: mahadasha -> antardasha -> pratyantar -> ...)
// ---------------------------------------------------------------------------

data class DashaNode(
    @SerializedName("planet") val planet: String = "",
    @SerializedName("start") val start: String = "",
    @SerializedName("end") val end: String = "",
    @SerializedName("years") val years: Int = 0,
    @SerializedName("days") val days: Double = 0.0,
    @SerializedName("antardasha") val antardasha: List<DashaNode> = emptyList(),
    @SerializedName("pratyantar") val pratyantar: List<DashaNode> = emptyList(),
    @SerializedName("sookshma") val sookshma: List<DashaNode> = emptyList(),
    @SerializedName("prana") val prana: List<DashaNode> = emptyList(),
    @SerializedName("adi") val adi: List<DashaNode> = emptyList(),
    @SerializedName("deha") val deha: List<DashaNode> = emptyList()
)

data class VimshottariDasha(
    @SerializedName("currentDasha") val currentDasha: String = "",
    @SerializedName("currentMahadasha") val currentMahadasha: DashaNode? = null,
    @SerializedName("currentAntardasha") val currentAntardasha: DashaNode? = null,
    @SerializedName("currentPratyantar") val currentPratyantar: DashaNode? = null,
    @SerializedName("currentSookshma") val currentSookshma: DashaNode? = null,
    @SerializedName("currentPrana") val currentPrana: DashaNode? = null,
    @SerializedName("currentAdi") val currentAdi: DashaNode? = null,
    @SerializedName("currentDeha") val currentDeha: DashaNode? = null,
    @SerializedName("allMahadashas") val allMahadashas: List<DashaNode> = emptyList()
)

// ---------------------------------------------------------------------------
// Ashtakavarga & Lal Kitab
// ---------------------------------------------------------------------------

data class SavHouse(
    @SerializedName("house") val house: Int = 1,
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("points") val points: Int = 0
)

data class AshtakavargaData(
    @SerializedName("savByHouse") val savByHouse: List<SavHouse> = emptyList(),
    // Honesty: null when the server omits it — never a fabricated 0.
    @SerializedName("savTotal") val savTotal: Int? = null
)

data class LalKitabData(
    @SerializedName("lagnaRashi") val lagnaRashi: String = "",
    @SerializedName("note") val note: String = "",
    @SerializedName("yogas") val yogas: List<String> = emptyList()
)

// ---------------------------------------------------------------------------
// Divisional (varga) charts: Navamsha, Dashamsha, ...
// ---------------------------------------------------------------------------

data class DivisionalChart(
    @SerializedName("title") val title: String = "",
    @SerializedName("lagnaRashi") val lagnaRashi: String = "",
    @SerializedName("lagnaRashiHi") val lagnaRashiHi: String = "",
    @SerializedName("houses") val houses: List<HouseInfo> = emptyList()
)

// ---------------------------------------------------------------------------
// Jyotish glyph helpers (professional glyphs only, no emojis)
// ---------------------------------------------------------------------------

private val RASHI_GLYPHS = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")

/** Rashi glyphs ♈..♓ for sign index 1..12. */
fun rashiGlyph(index: Int): String {
    val i = ((index - 1) % 12 + 12) % 12
    return RASHI_GLYPHS[i]
}

fun rashiNameToIndex(name: String): Int = when (name.lowercase().trim()) {
    "aries", "mesha", "मेष" -> 1
    "taurus", "vrishabha", "vrishabh", "वृषभ" -> 2
    "gemini", "mithuna", "मिथुन" -> 3
    "cancer", "karka", "kark", "कर्क" -> 4
    "leo", "simha", "सिंह" -> 5
    "virgo", "kanya", "कन्या" -> 6
    "libra", "tula", "तुला" -> 7
    "scorpio", "vrishchika", "vrishchik", "वृश्चिक" -> 8
    "sagittarius", "dhanu", "धनु" -> 9
    "capricorn", "makara", "makar", "मकर" -> 10
    "aquarius", "kumbha", "कुंभ", "कुम्भ" -> 11
    "pisces", "meena", "मीन" -> 12
    else -> 1
}

/** Fallback planet glyphs ☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋ when the server omits `symbol`. */
fun planetGlyphFallback(name: String): String {
    val n = name.lowercase().trim()
    if (n.startsWith("sun") || n == "surya") return "☉"
    if (n.startsWith("moon") || n == "chandra") return "☽"
    if (n.startsWith("mars") || n == "mangal") return "♂"
    if (n.startsWith("mercury") || n == "budh") return "☿"
    if (n.startsWith("jupiter") || n == "guru") return "♃"
    if (n.startsWith("venus") || n == "shukra") return "♀"
    if (n.startsWith("saturn") || n == "shani") return "♄"
    if (n.startsWith("rahu")) return "☊"
    if (n.startsWith("ketu")) return "☋"
    return name.take(2)
}

/** Hindi name for a planet (used when the server omits nameHi). */
fun planetHindiFallback(name: String): String {
    val n = name.lowercase().trim()
    if (n.startsWith("sun")) return "सूर्य"
    if (n.startsWith("moon")) return "चन्द्र"
    if (n.startsWith("mars")) return "मंगल"
    if (n.startsWith("mercury")) return "बुध"
    if (n.startsWith("jupiter")) return "गुरु"
    if (n.startsWith("venus")) return "शुक्र"
    if (n.startsWith("saturn")) return "शनि"
    if (n.startsWith("rahu")) return "राहु"
    if (n.startsWith("ketu")) return "केतु"
    return ""
}

// ---------------------------------------------------------------------------
// Generic lagna-based chart (Surya / Chandra / Chalit).
// Verified live: {"lagnaRashi","lagnaRashiHi","houses":[{number,rashi,lord,planets[]}]}.
// Chalit houses additionally carry madyaDeg/startDeg/endDeg (ignored).
// ---------------------------------------------------------------------------

data class ChartData(
    @SerializedName("lagnaRashi") val lagnaRashi: String = "",
    @SerializedName("lagnaRashiHi") val lagnaRashiHi: String = "",
    @SerializedName("houses") val houses: List<HouseInfo> = emptyList()
) {
    fun lagnaSignIndex(): Int {
        if (lagnaRashi.isNotBlank()) return rashiNameToIndex(lagnaRashi)
        houses.firstOrNull { it.number == 1 }?.rashi?.let { return rashiNameToIndex(it) }
        return 1
    }

    fun planetsByHouse(): Map<Int, List<String>> =
        houses.associate { it.number to it.planets.map { p -> p.name } }
}

// ---------------------------------------------------------------------------
// Birth Panchang (verified live shape)
// ---------------------------------------------------------------------------

data class BirthPanchang(
    @SerializedName("tithi") val tithi: String = "",
    @SerializedName("tithiNum") val tithiNum: Int = 0,
    @SerializedName("paksha") val paksha: String = "",
    @SerializedName("vara") val vara: String = "",
    @SerializedName("nakshatra") val nakshatra: String = "",
    @SerializedName("nakshatraHi") val nakshatraHi: String = "",
    @SerializedName("nakshatraLord") val nakshatraLord: String = "",
    @SerializedName("pada") val pada: Int = 0,
    @SerializedName("yoga") val yoga: String = "",
    @SerializedName("karana") val karana: String = "",
    @SerializedName("sunRashi") val sunRashi: String = "",
    @SerializedName("moonRashi") val moonRashi: String = ""
)

// ---------------------------------------------------------------------------
// Sodasvarga: varga code (D1..D60) -> per-planet rashi list (verified live)
// ---------------------------------------------------------------------------

data class SodasvargaEntry(
    @SerializedName("name") val name: String = "",
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("rashiIdx") val rashiIdx: Int = 0
)

// ---------------------------------------------------------------------------
// Shadbala + Bhavabala + Vimshopaka (verified live).
// Strength maps use Any values because the server mixes doubles, ints and
// booleans (e.g. "sufficient": true) inside the same object.
// ---------------------------------------------------------------------------

data class ShadbalaData(
    @SerializedName("shadbala") val shadbala: Map<String, Map<String, Any>>? = null,
    @SerializedName("bhavabala") val bhavabala: Map<String, Map<String, Any>>? = null,
    @SerializedName("vimshopaka") val vimshopaka: Map<String, Map<String, Any>>? = null
)

// ---------------------------------------------------------------------------
// Current gochar (verified live): currentTransits is a MAP keyed by planet
// name, not a list.
// ---------------------------------------------------------------------------

data class TransitInfo(
    @SerializedName("rashi") val rashi: String = "",
    @SerializedName("rashi_deg") val rashiDeg: Double = 0.0,
    @SerializedName("natal_moon_house") val natalMoonHouse: Int = 0,
    @SerializedName("transit_focus") val transitFocus: String = "",
    @SerializedName("retro") val retro: Boolean = false
)

// ---------------------------------------------------------------------------
// analysis_tabs: pre-rendered website text blocks (pipe/line separated).
// ---------------------------------------------------------------------------

data class AnalysisTabs(
    @SerializedName("ppills") val ppills: String = "",
    @SerializedName("grahaDetail") val grahaDetail: String = "",
    @SerializedName("d1Analysis") val d1Analysis: String = "",
    @SerializedName("d9Analysis") val d9Analysis: String = "",
    @SerializedName("dasha") val dasha: String = "",
    @SerializedName("panchang") val panchang: String = "",
    @SerializedName("shadbal") val shadbal: String = "",
    @SerializedName("personality") val personality: String = "",
    @SerializedName("bnn") val bnn: String = "",
    @SerializedName("chartTableD1") val chartTableD1: String = "",
    @SerializedName("chartTableD9") val chartTableD9: String = ""
)

// ---------------------------------------------------------------------------
// Dedicated report endpoints: /kundali/personality, /kundali/bnn,
// /kundali/lal-kitab -> {success, data: {htmlReport, textReport, analysis_text}}
// (verified live 2026-10-09)
// ---------------------------------------------------------------------------

data class ReportData(
    @SerializedName("htmlReport") val htmlReport: String = "",
    @SerializedName("textReport") val textReport: String = "",
    @SerializedName("analysis_text") val analysisText: String = ""
) {
    /** Best plain-text rendering of the report (server sends all three in sync). */
    fun bestText(): String =
        textReport.ifBlank { analysisText }.ifBlank { stripHtmlToText(htmlReport) }
}

data class ReportEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: ReportData? = null,
    @SerializedName("message") val message: String? = null
)

/** Minimal HTML stripper for server htmlReport fallback (never invents content). */
fun stripHtmlToText(html: String): String {
    if (html.isBlank()) return ""
    return html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(p|div|h[1-6]|li|tr)>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

// ---------------------------------------------------------------------------
// Jaimini endpoint: POST /kundali/jaimini (verified live 2026-10-09)
// karakamshaHouses use 0-based rashi index (ri: 10 = Aquarius).
// charDashas/charAntars use 0-based sign index + Julian-day date bounds.
// ---------------------------------------------------------------------------

data class JaiminiKarakaDetail(
    @SerializedName("name") val name: String = "",
    @SerializedName("deg") val deg: Double = 0.0,
    @SerializedName("rashi") val rashi: String = ""
)

data class JaiminiKarakas(
    @SerializedName("AK") val ak: JaiminiKarakaDetail? = null,
    @SerializedName("AmK") val amk: JaiminiKarakaDetail? = null,
    @SerializedName("BK") val bk: JaiminiKarakaDetail? = null,
    @SerializedName("MK") val mk: JaiminiKarakaDetail? = null,
    @SerializedName("PiK") val pik: JaiminiKarakaDetail? = null,
    @SerializedName("GK") val gk: JaiminiKarakaDetail? = null,
    @SerializedName("DK") val dk: JaiminiKarakaDetail? = null
) {
    fun ordered(): List<Pair<String, JaiminiKarakaDetail?>> = listOf(
        "Atmakaraka (AK)" to ak,
        "Amatyakaraka (AmK)" to amk,
        "Bhratrukaraka (BK)" to bk,
        "Matrukaraka (MK)" to mk,
        "Putrakaraka (PiK)" to pik,
        "Gnatikaraka (GK)" to gk,
        "Darakaraka (DK)" to dk
    )
}

data class JaiminiKarakaWrapper(
    @SerializedName("details") val details: JaiminiKarakas? = null
)

data class KarakamshaInfo(
    @SerializedName("sign_idx") val signIdx: Int = 0,
    @SerializedName("sign_en") val signEn: String = "",
    @SerializedName("sign_hi") val signHi: String = "",
    @SerializedName("ak_planet") val akPlanet: String = ""
)

data class KarakamshaRashi(
    @SerializedName("en") val en: String = "",
    @SerializedName("hi") val hi: String = ""
)

data class KarakamshaPlanet(
    @SerializedName("sym") val sym: String = "",
    @SerializedName("en") val en: String = "",
    @SerializedName("hi") val hi: String = ""
)

data class KarakamshaHouse(
    @SerializedName("n") val n: Int = 1,
    @SerializedName("ri") val ri: Int = 0,
    @SerializedName("r") val r: KarakamshaRashi? = null,
    @SerializedName("pl") val pl: List<KarakamshaPlanet> = emptyList(),
    @SerializedName("isLg") val isLg: Boolean = false
)

data class CharDasha(
    @SerializedName("sign") val sign: Int = 0,
    @SerializedName("sign_en") val signEn: String = "",
    @SerializedName("sign_hi") val signHi: String = "",
    @SerializedName("abbr") val abbr: String = "",
    @SerializedName("years") val years: Double = 0.0,
    @SerializedName("balance") val balance: Double = 0.0,
    @SerializedName("startJD") val startJD: Double = 0.0,
    @SerializedName("endJD") val endJD: Double = 0.0
)

data class JaiminiData(
    @SerializedName("karakas") val karakas: JaiminiKarakaWrapper? = null,
    @SerializedName("karakamsha") val karakamsha: KarakamshaInfo? = null,
    @SerializedName("karakamshaHouses") val karakamshaHouses: List<KarakamshaHouse> = emptyList(),
    @SerializedName("charDashas") val charDashas: List<CharDasha> = emptyList(),
    @SerializedName("curCharMD") val curCharMD: CharDasha? = null,
    @SerializedName("curCharAD") val curCharAD: CharDasha? = null,
    @SerializedName("charAntars") val charAntars: List<CharDasha> = emptyList()
)

data class JaiminiEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: JaiminiData? = null,
    @SerializedName("message") val message: String? = null
)

/**
 * Julian Day Number -> Gregorian date (Fliegel–Van Flandern algorithm).
 * Used only to display the server's char-dasha JD bounds as calendar dates.
 */
fun julianDayToDateString(jd: Double): String {
    if (jd <= 0) return ""
    return try {
        var l = (jd + 68569.0).toLong()
        val n = (4 * l / 146097).toLong()
        l -= (146097 * n + 3) / 4
        val i = (4000 * (l + 1) / 1461001).toLong()
        l -= (1461 * i / 4) + 31
        val j = (80 * l / 2447).toLong()
        val day = (l - 2447 * j / 80).toInt()
        l = j / 11
        val month = (j + 2 - 12 * l).toInt()
        val year = (100 * (n - 49) + i + l).toInt()
        "%02d-%02d-%04d".format(day, month, year)
    } catch (e: Exception) {
        ""
    }
}

// ---------------------------------------------------------------------------
// Lucky Elements — standard Vedic correspondences for the Lagna rashi.
// Derived ONLY from the server's lagna (rashi + lord); static reference data.
// ---------------------------------------------------------------------------

data class LuckyElements(
    val color: String,
    val number: Int,
    val gemstone: String
)

/** Planet name -> Vedic lucky number (Sun 1, Moon 2, Jupiter 3, Mercury 5, Venus 6, Saturn 8, Mars 9). */
fun planetLuckyNumber(planetName: String): Int {
    val n = planetName.lowercase().trim()
    return when {
        n.startsWith("sun") || n == "surya" -> 1
        n.startsWith("moon") || n == "chandra" -> 2
        n.startsWith("jupiter") || n == "guru" -> 3
        n.startsWith("mercury") || n == "budh" -> 5
        n.startsWith("venus") || n == "shukra" -> 6
        n.startsWith("saturn") || n == "shani" -> 8
        n.startsWith("mars") || n == "mangal" -> 9
        else -> 0
    }
}

/** (lucky color, gemstone) per lagna rashi index 1..12 — standard Vedic mapping. */
fun luckyColorGemForRashi(rashiIndex: Int): Pair<String, String> = when (rashiIndex) {
    1 -> "Red" to "Red Coral (Moonga)"
    2 -> "White / Cream" to "Diamond (Heera)"
    3 -> "Green" to "Emerald (Panna)"
    4 -> "White / Silver" to "Pearl (Moti)"
    5 -> "Golden / Orange" to "Ruby (Manik)"
    6 -> "Green" to "Emerald (Panna)"
    7 -> "White / Pink" to "Diamond (Heera)"
    8 -> "Deep Red / Maroon" to "Red Coral (Moonga)"
    9 -> "Yellow" to "Yellow Sapphire (Pukhraj)"
    10 -> "Dark Blue / Black" to "Blue Sapphire (Neelam)"
    11 -> "Violet / Dark Blue" to "Amethyst (Jamunia)"
    else -> "Yellow / Sea Green" to "Yellow Sapphire (Pukhraj)"
}

fun luckyElementsFor(lagnaRashiIndex: Int, lagnaLord: String): LuckyElements {
    val (color, gem) = luckyColorGemForRashi(lagnaRashiIndex)
    val number = planetLuckyNumber(lagnaLord).takeIf { it > 0 } ?: when (lagnaRashiIndex) {
        1, 8 -> 9
        2, 7 -> 6
        3, 6 -> 5
        4 -> 2
        5 -> 1
        9, 12 -> 3
        10, 11 -> 8
        else -> 1
    }
    return LuckyElements(color, number, gem)
}

/** Stringify a Gson-decoded Any (numbers render without trailing ".0"). */
fun anyToDisplay(value: Any?): String = when (value) {
    null -> ""
    is Double -> if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
    is Float -> if (value % 1f == 0f) value.toLong().toString() else "%.2f".format(value)
    is Boolean -> if (value) "Yes" else "No"
    else -> value.toString()
}
