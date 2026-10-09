package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Zodiac sign metadata (UI-side; rulers/colors match omastroai.com/rashifal/)
// ---------------------------------------------------------------------------
data class HoroscopeSign(
    val id: String,
    val name: String,
    val hindiName: String,
    val symbol: String,
    val element: String,
    val dates: String,
    val ruler: String,
    /** ARGB color int for the glyph circle, mirroring website sign colors */
    val glyphColor: Long
)

// ---------------------------------------------------------------------------
// Real POST /rashifal response shapes (verified live 2026-10-09).
// Envelope: {"success": true, "data": {...}, "meta": {...}}
// ---------------------------------------------------------------------------
data class RashifalEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: RashifalData? = null
)

data class RashifalData(
    @SerializedName("seed") val seed: Long = 0L,
    @SerializedName("period") val period: String = "daily",
    @SerializedName("rashi_idx") val rashiIdx: Int = 0,
    @SerializedName("rashi_name") val rashiName: String = "",
    @SerializedName("dasha") val dasha: RashifalDasha? = null,
    @SerializedName("summary") val summary: String = "",
    @SerializedName("scores") val scores: Map<String, Int> = emptyMap(),
    @SerializedName("areas") val areas: Map<String, RashifalArea> = emptyMap(),
    @SerializedName("lucky") val lucky: RashifalLucky? = null,
    @SerializedName("remedies") val remedies: List<String> = emptyList(),
    @SerializedName("color") val color: String = "",
    @SerializedName("score_color") val scoreColor: String = "",
    @SerializedName("saturn_transit") val saturnTransit: SaturnTransit? = null
)

data class RashifalDasha(
    @SerializedName("md") val md: String = "",
    @SerializedName("ad") val ad: String = "",
    @SerializedName("pd") val pd: String = "",
    @SerializedName("primary") val primary: String = "",
    @SerializedName("primary_name") val primaryName: String = ""
)

data class RashifalArea(
    @SerializedName("label") val label: String = "",
    // Honesty: null when the server omits the score — the UI renders an
    // honest "—" instead of a fabricated 0/10.
    @SerializedName("score") val score: Int? = null,
    @SerializedName("text") val text: String = ""
)

data class RashifalLucky(
    @SerializedName("number") val number: Int = 0,
    @SerializedName("color") val color: String = "",
    @SerializedName("direction") val direction: String = "",
    @SerializedName("gem") val gem: String = "",
    @SerializedName("time") val time: String = ""
)

data class SaturnTransit(
    @SerializedName("active") val active: Boolean = false,
    @SerializedName("type") val type: String = "",
    @SerializedName("phase") val phase: String = "",
    @SerializedName("name_en") val nameEn: String = "",
    @SerializedName("name_hi") val nameHi: String = "",
    @SerializedName("description_en") val descriptionEn: String = "",
    // Extra keys from POST /kundali's saturnTransit (verified live 2026-10-09).
    // Absent in the rashifal payload — Gson ignores missing keys, so this one
    // class serves both endpoints.
    @SerializedName("house_from_moon") val houseFromMoon: Int = 0,
    @SerializedName("saturn_rashi") val saturnRashi: Int = 0,
    @SerializedName("moon_rashi") val moonRashi: Int = 0,
    @SerializedName("description_hi") val descriptionHi: String = ""
)
