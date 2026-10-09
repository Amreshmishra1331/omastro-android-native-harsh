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
    @SerializedName("pob") val pob: String = "New Delhi",
    @SerializedName("lat") val lat: Double = 28.6139,
    @SerializedName("lon") val lon: Double = 77.2090,
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("ayanamsa") val ayanamsa: String = "lahiri"
)

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
    @SerializedName("divisionalCharts") val divisionalCharts: Map<String, DivisionalChart> = emptyMap()
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
    @SerializedName("savTotal") val savTotal: Int = 0
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

/** Rashi glyphs ♈..♓ for sign index 1..12. */
fun rashiGlyph(index: Int): String = when ((index - 1).mod(12) + 1) {
    1 -> "♈"; 2 -> "♉"; 3 -> "♊"; 4 -> "♋"; 5 -> "♌"; 6 -> "♍"
    7 -> "♎"; 8 -> "♏"; 9 -> "♐"; 10 -> "♑"; 11 -> "♒"; else -> "♓"
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
    return when {
        n.startsWith("sun") || n == "surya" || n == "सूर्य" -> "☉"
        n.startsWith("moon") || n == "chandra" || n == "moon" || n == "चन्द्र" || n == "चंद्र" -> "☽"
        n.startsWith("mars") || n == "mangal" || n == "मंगल" -> "♂"
        n.startsWith("mercury") || n == "budh" || n == "बुध" -> "☿"
        n.startsWith("jupiter") || n == "guru" || n == "गुरु" -> "♃"
        n.startsWith("venus") || n == "shukra" || n == "शुक्र" -> "♀"
        n.startsWith("saturn") || n == "shani" || n == "शनि" -> "♄"
        n.startsWith("rahu") || n == "राहु" -> "☊"
        n.startsWith("ketu") || n == "केतु" -> "☋"
        else -> name.take(2)
    }
}

/** Hindi name for a planet (used when the server omits nameHi). */
fun planetHindiFallback(name: String): String {
    val n = name.lowercase().trim()
    return when {
        n.startsWith("sun") -> "सूर्य"
        n.startsWith("moon") -> "चन्द्र"
        n.startsWith("mars") -> "मंगल"
        n.startsWith("mercury") -> "बुध"
        n.startsWith("jupiter") -> "गुरु"
        n.startsWith("venus") -> "शुक्र"
        n.startsWith("saturn") -> "शनि"
        n.startsWith("rahu") -> "राहु"
        n.startsWith("ketu") -> "केतु"
        else -> ""
    }
}
