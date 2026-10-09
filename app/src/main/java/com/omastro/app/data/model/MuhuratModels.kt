package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

// Muhurat (auspicious dates for life events) — POST /muhurat
// Verified live 2026-10-09 against production backend (KG_Muhurat_Engine).
// Request params: event | date (YYYY-MM-DD) | days_ahead (1-365) | lat | lon | tz | language
data class MuhuratRequest(
    @SerializedName("event") val event: String, // vivah | grih_pravesh | vahan | property
    @SerializedName("date") val date: String, // YYYY-MM-DD start date
    @SerializedName("days_ahead") val daysAhead: Int = 30,
    @SerializedName("lat") val lat: Double = 28.6139,
    @SerializedName("lon") val lon: Double = 77.2090,
    @SerializedName("tz") val tz: Double = 5.5,
    @SerializedName("language") val language: String = "en"
)

data class MuhuratCheck(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("value") val value: Int? = null
)

data class MuhuratChecks(
    @SerializedName("tithi") val tithi: MuhuratCheck? = null,
    @SerializedName("avoid_tithi") val avoidTithi: MuhuratCheck? = null,
    @SerializedName("nakshatra") val nakshatra: MuhuratCheck? = null,
    @SerializedName("vara") val vara: MuhuratCheck? = null
)

// One scored day from core_data. vara: 0=Sun..6=Sat; nakshatra/yoga: 1..27;
// tithi: 1..15 (tithi_full 1..30 spans both pakshas).
data class MuhuratDay(
    @SerializedName("date") val date: String,
    @SerializedName("yr") val yr: Int,
    @SerializedName("mo") val mo: Int,
    @SerializedName("dy") val dy: Int,
    @SerializedName("tithi") val tithi: Int,
    @SerializedName("tithi_full") val tithiFull: Int,
    @SerializedName("nakshatra") val nakshatra: Int,
    @SerializedName("vara") val vara: Int,
    @SerializedName("yoga") val yoga: Int,
    @SerializedName("score") val score: Int,
    @SerializedName("max_score") val maxScore: Int,
    @SerializedName("rating") val rating: Int, // 0..100
    @SerializedName("verdict") val verdict: String, // excellent | good | acceptable
    @SerializedName("checks") val checks: MuhuratChecks? = null
)

data class MuhuratData(
    @SerializedName("event") val event: String,
    @SerializedName("from") val from: String,
    @SerializedName("days_ahead") val daysAhead: Int,
    @SerializedName("core_data") val coreData: List<MuhuratDay> = emptyList(),
    @SerializedName("analysis_text") val analysisText: String? = null
)

// Defensive envelope: {success, data{...}}. Never invented values — screen
// renders only keys present in the response.
data class MuhuratResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: MuhuratData? = null,
    @SerializedName("message") val message: String? = null
)

// Supported events (verified against KG_Muhurat_Engine::get_event_rules).
data class MuhuratEvent(
    val key: String,
    val title: String,
    val subtitle: String
)

val MUHURAT_EVENTS = listOf(
    MuhuratEvent("vivah", "Marriage", "Vivah — wedding ceremonies"),
    MuhuratEvent("grih_pravesh", "Housewarming", "Griha Pravesh — entering a new home"),
    MuhuratEvent("vahan", "Vehicle", "Vahan — buying a new vehicle"),
    MuhuratEvent("property", "Property", "Property — land & home purchase")
)

// Classical 27 nakshatras, 1-indexed (verified against engine comments:
// 4=Rohini, 5=Mrigashira, 10=Magha, 27=Revati).
val NAKSHATRA_NAMES = listOf(
    "", "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "P. Phalguni", "U. Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula",
    "P. Ashadha", "U. Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
    "P. Bhadrapada", "U. Bhadrapada", "Revati"
)

// Vara: 0=Sunday..6=Saturday (verified: engine varas [1,3,4,5] = Mon,Wed,Thu,Fri).
val VARA_NAMES = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
)
val VARA_NAMES_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// 27 yogas, 1-indexed classical order.
val YOGA_NAMES = listOf(
    "", "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana", "Atiganda",
    "Sukarma", "Dhriti", "Shula", "Ganda", "Vriddhi", "Dhruva", "Vyaghata",
    "Harshana", "Vajra", "Siddhi", "Vyatipata", "Variyana", "Parigha", "Shiva",
    "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma", "Indra", "Vaidhriti"
)

// Tithi names 1..15 within a paksha.
val TITHI_NAMES = listOf(
    "", "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashthi",
    "Saptami", "Ashtami", "Navami", "Dashami", "Ekadashi", "Dwadashi",
    "Trayodashi", "Chaturdashi", "Purnima"
)

fun muhuratDayTithiName(day: MuhuratDay): String {
    val paksha = if (day.tithiFull <= 15) "Shukla" else "Krishna"
    val name = TITHI_NAMES.getOrElse(day.tithi) { "Tithi ${day.tithi}" }
    val amavasya = if (day.tithiFull == 30) "Amavasya" else name
    return "$paksha $amavasya"
}
