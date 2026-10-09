package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// ---------------------------------------------------------------------------
// Network models — match the real POST /panchang contract:
//   {"success": true, "data": {"core_data": {...}, "analysis_text": "..."}}
// ---------------------------------------------------------------------------

data class PanchangEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: PanchangDataPayload? = null
)

data class PanchangDataPayload(
    @SerializedName("core_data") val coreData: PanchangCoreData? = null,
    @SerializedName("analysis_text") val analysisText: String? = null
)

data class PanchangCoreData(
    @SerializedName("date_local") val dateLocal: String? = null,
    @SerializedName("sunrise_jd") val sunriseJd: Double? = null,
    @SerializedName("sunset_jd") val sunsetJd: Double? = null,
    @SerializedName("tithi") val tithi: TithiInfo? = null,
    @SerializedName("nakshatra") val nakshatra: NakshatraInfo? = null,
    @SerializedName("yoga") val yoga: YogaInfo? = null,
    @SerializedName("karana1") val karana1: KaranaInfo? = null,
    @SerializedName("karana2") val karana2: KaranaInfo? = null,
    @SerializedName("paksha") val paksha: String? = null,
    @SerializedName("vara_idx") val varaIdx: Int? = null,
    @SerializedName("sun_rashi") val sunRashi: RashiPosition? = null,
    @SerializedName("moon_rashi") val moonRashi: RashiPosition? = null,
    @SerializedName("amanta_month") val amantaMonth: String? = null,
    @SerializedName("purnimanta_month") val purnimantaMonth: String? = null,
    @SerializedName("pravishte") val pravishte: Int? = null,
    @SerializedName("shaka_samvat") val shakaSamvat: Int? = null,
    @SerializedName("shaka_year_name") val shakaYearName: String? = null,
    @SerializedName("vikram_samvat") val vikramSamvat: Int? = null,
    @SerializedName("vikram_year_name") val vikramYearName: String? = null
)

data class TithiInfo(
    @SerializedName("num") val num: Int? = null,
    @SerializedName("start_jd") val startJd: Double? = null,
    @SerializedName("end_jd") val endJd: Double? = null
)

data class NakshatraInfo(
    @SerializedName("idx") val idx: Int? = null,
    @SerializedName("data") val data: NakshatraData? = null,
    @SerializedName("start_jd") val startJd: Double? = null,
    @SerializedName("end_jd") val endJd: Double? = null
)

data class NakshatraData(
    @SerializedName("n") val name: String? = null,
    @SerializedName("hi") val hindi: String? = null,
    @SerializedName("lord") val lord: String? = null,
    @SerializedName("pada") val pada: Int? = null,
    @SerializedName("idx") val idx: Int? = null
)

data class YogaInfo(
    @SerializedName("idx") val idx: Int? = null,
    @SerializedName("start_jd") val startJd: Double? = null,
    @SerializedName("end_jd") val endJd: Double? = null
)

data class KaranaInfo(
    @SerializedName("idx") val idx: Int? = null,
    @SerializedName("name_idx") val nameIdx: Int? = null,
    @SerializedName("start_jd") val startJd: Double? = null,
    @SerializedName("end_jd") val endJd: Double? = null
)

data class RashiInfo(
    @SerializedName("en") val en: String? = null,
    @SerializedName("hi") val hi: String? = null,
    @SerializedName("lord") val lord: String? = null,
    @SerializedName("sym") val sym: String? = null
)

data class RashiPosition(
    @SerializedName("r") val rashi: RashiInfo? = null,
    @SerializedName("d") val deg: Int? = null,
    @SerializedName("m") val min: Int? = null,
    @SerializedName("s") val sec: Int? = null
)

// ---------------------------------------------------------------------------
// Hora network models — real POST /hora contract:
//   {"success": true, "data": {"day": [...], "night": [...], "day_lord": ..., "sunrise": ..., "sunset": ...}}
// ---------------------------------------------------------------------------

data class HoraEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: HoraData? = null
)

data class HoraData(
    @SerializedName("day") val day: List<HoraHour> = emptyList(),
    @SerializedName("night") val night: List<HoraHour> = emptyList(),
    @SerializedName("day_lord") val dayLord: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("sunrise") val sunrise: String? = null,
    @SerializedName("sunset") val sunset: String? = null
)

data class HoraHour(
    @SerializedName("lord") val lord: String? = null,
    @SerializedName("start") val start: String? = null,
    @SerializedName("end") val end: String? = null,
    @SerializedName("hour") val hour: Int? = null
)

// ---------------------------------------------------------------------------
// UI model — resolved, display-ready values mapped from the real API.
// Field names tithi/nakshatra/yoga/karana/day/sunrise/sunset are kept so
// existing dashboard bindings keep working.
// ---------------------------------------------------------------------------

data class PanchangResponse(
    val dateApi: String = "",
    val dateLabel: String = "",
    val sunrise: String = "",
    val sunset: String = "",
    val tithi: String = "",
    val tithiStart: String? = null,
    val tithiEnd: String? = null,
    val nakshatra: String = "",
    val nakshatraDetail: String? = null,
    val yoga: String = "",
    val yogaEnd: String? = null,
    val karana: String = "",
    val karana1: String = "",
    val karana2: String? = null,
    val paksha: String = "",
    val day: String = "",
    val amantaMonth: String? = null,
    val purnimantaMonth: String? = null,
    val moonSign: String = "",
    val sunSign: String? = null,
    val samvatLine: String? = null,
    val pravishte: Int? = null
)

// ---------------------------------------------------------------------------
// Lookup tables + mapping (verified against live API responses 2026-10-09)
// ---------------------------------------------------------------------------

object PanchangLookups {
    private val TITHI_NAMES = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima"
    )

    private val YOGA_NAMES = listOf(
        "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
        "Atiganda", "Sukarma", "Dhriti", "Shula", "Ganda",
        "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
        "Siddhi", "Vyatipata", "Variyana", "Parigha", "Shiva",
        "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
        "Indra", "Vaidhriti"
    )

    private val KARANA_NAMES = listOf(
        "Bava", "Balava", "Kaulava", "Taitila", "Gara",
        "Vanija", "Vishti", "Shakuni", "Chatushpada", "Naga", "Kimstughna"
    )

    private val VARA_NAMES = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    fun tithiName(num: Int?): String? {
        if (num == null || num !in 1..30) return null
        return if (num <= 15) {
            val base = TITHI_NAMES[num - 1]
            "Shukla $base"
        } else {
            val base = TITHI_NAMES[num - 16]
            if (num == 30) "Krishna Amavasya" else "Krishna $base"
        }
    }

    fun yogaName(idx: Int?): String? =
        if (idx != null && idx in YOGA_NAMES.indices) YOGA_NAMES[idx] else null

    fun karanaName(nameIdx: Int?): String? =
        if (nameIdx != null && nameIdx in KARANA_NAMES.indices) KARANA_NAMES[nameIdx] else null

    fun varaName(idx: Int?): String? =
        if (idx != null && idx in VARA_NAMES.indices) VARA_NAMES[idx] else null

    fun pakshaName(paksha: String?): String? = when (paksha?.lowercase()) {
        "shukla" -> "Shukla Paksha"
        "krishna" -> "Krishna Paksha"
        else -> paksha?.replaceFirstChar { it.uppercase() }
    }

    private val ist: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /** Julian date -> "6:18 AM" in IST. */
    fun jdToIstTime(jd: Double?): String? {
        if (jd == null) return null
        val unixSec = ((jd - 2440587.5) * 86400.0).toLong()
        val cal = Calendar.getInstance(ist).apply { timeInMillis = unixSec * 1000 }
        val fmt = SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { timeZone = ist }
        return fmt.format(cal.time)
    }

    /** Julian date -> "8 Oct, 10:16 PM" in IST. */
    fun jdToIstDateTime(jd: Double?): String? {
        if (jd == null) return null
        val unixSec = ((jd - 2440587.5) * 86400.0).toLong()
        val cal = Calendar.getInstance(ist).apply { timeInMillis = unixSec * 1000 }
        val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).apply { timeZone = ist }
        return fmt.format(cal.time)
    }

    /** "2026-10-09" -> "Friday, October 9, 2026" in IST. */
    fun apiDateToLabel(dateApi: String): String {
        return try {
            val parse = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = ist }
            val out = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH).apply { timeZone = ist }
            out.format(parse.parse(dateApi)!!)
        } catch (e: Exception) {
            dateApi
        }
    }

    fun todayApiDate(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = ist }
        return fmt.format(Calendar.getInstance(ist).time)
    }

    fun rashiLabel(pos: RashiPosition?): String? {
        val name = pos?.rashi?.en ?: return null
        val d = pos.deg
        return if (d != null) "$name ${d}°" else name
    }

    /** Map the network payload to display-ready UI data. Returns null when unusable. */
    fun toUiData(payload: PanchangDataPayload?, dateApi: String): PanchangResponse? {
        val core = payload?.coreData ?: return null
        val tithiName = tithiName(core.tithi?.num) ?: return null

        val nakData = core.nakshatra?.data
        val nakName = nakData?.name?.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ") ?: "—"
        val nakDetail = buildString {
            if (nakData?.pada != null) append("Pada ${nakData.pada}")
            if (nakData?.lord != null) { if (isNotEmpty()) append(" · "); append("Lord ${nakData.lord}") }
            val upto = jdToIstTime(core.nakshatra?.endJd)
            if (upto != null) { if (isNotEmpty()) append(" · "); append("upto $upto") }
        }.ifEmpty { null }

        val yogaName = yogaName(core.yoga?.idx) ?: "—"
        val karana1Name = karanaName(core.karana1?.nameIdx) ?: "—"
        val karana2Name = karanaName(core.karana2?.nameIdx)

        val samvat = buildString {
            if (core.vikramSamvat != null) append("Vikram ${core.vikramSamvat}")
            if (core.shakaSamvat != null) { if (isNotEmpty()) append(" · "); append("Shaka ${core.shakaSamvat}") }
        }.ifEmpty { null }

        return PanchangResponse(
            dateApi = dateApi,
            dateLabel = apiDateToLabel(core.dateLocal ?: dateApi),
            sunrise = jdToIstTime(core.sunriseJd) ?: "—",
            sunset = jdToIstTime(core.sunsetJd) ?: "—",
            tithi = tithiName,
            tithiStart = jdToIstDateTime(core.tithi?.startJd),
            tithiEnd = jdToIstDateTime(core.tithi?.endJd),
            nakshatra = nakName,
            nakshatraDetail = nakDetail,
            yoga = yogaName,
            yogaEnd = jdToIstTime(core.yoga?.endJd),
            karana = if (karana2Name != null) "$karana1Name, $karana2Name" else karana1Name,
            karana1 = karana1Name,
            karana2 = karana2Name,
            paksha = pakshaName(core.paksha) ?: "—",
            day = varaName(core.varaIdx) ?: "—",
            amantaMonth = core.amantaMonth,
            purnimantaMonth = core.purnimantaMonth,
            moonSign = core.moonRashi?.rashi?.en ?: "—",
            sunSign = rashiLabel(core.sunRashi),
            samvatLine = samvat,
            pravishte = core.pravishte
        )
    }
}
