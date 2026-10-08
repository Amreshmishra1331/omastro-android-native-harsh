package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

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

data class PlanetInfo(
    @SerializedName("name") val name: String,
    @SerializedName("sign") val sign: String,
    @SerializedName("sign_number") val signNumber: Int, // 1 (Aries) to 12 (Pisces)
    @SerializedName("degree") val degree: Double,
    @SerializedName("house") val house: Int, // 1 to 12
    @SerializedName("is_retrograde") val isRetrograde: Boolean = false,
    @SerializedName("dignity") val dignity: String = "Direct",
    @SerializedName("nakshatra") val nakshatra: String,
    @SerializedName("pada") val pada: Int
)

data class VimshottariDashaItem(
    @SerializedName("lord") val lord: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("duration_years") val durationYears: Int,
    @SerializedName("is_current") val isCurrent: Boolean = false
)

data class YogaDoshaItem(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String, // Raja Yoga, Dhana Yoga, Dosha
    @SerializedName("is_auspicious") val isAuspicious: Boolean = true,
    @SerializedName("description") val description: String,
    @SerializedName("remedy") val remedy: String? = null
)

data class AshtakavargaHouseBindu(
    @SerializedName("house") val house: Int,
    @SerializedName("sign") val sign: String,
    @SerializedName("total_bindus") val totalBindus: Int,
    @SerializedName("strength") val strength: String
)

data class LalKitabRemedyItem(
    @SerializedName("planet") val planet: String,
    @SerializedName("house") val house: Int,
    @SerializedName("indication") val indication: String,
    @SerializedName("remedy") val remedy: String
)

data class BNNCombination(
    @SerializedName("title") val title: String,
    @SerializedName("planets_involved") val planetsInvolved: List<String>,
    @SerializedName("life_significance") val lifeSignificance: String
)

data class JaiminiKaraka(
    @SerializedName("role") val role: String, // Atmakaraka, Amatyakaraka, etc.
    @SerializedName("planet") val planet: String,
    @SerializedName("signification") val signification: String
)

data class AstroHealthIndicator(
    @SerializedName("body_system") val bodySystem: String,
    @SerializedName("status") val status: String,
    @SerializedName("astrological_factor") val astrologicalFactor: String
)

data class KundaliResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("ascendant_sign") val ascendantSign: Int = 1, // 1 to 12
    @SerializedName("ascendant_sign_name") val ascendantSignName: String = "Aries (Mesha)",
    @SerializedName("nakshatra") val nakshatra: String = "Ashwini",
    @SerializedName("current_dasha") val currentDasha: String = "Jupiter Mahadasha / Saturn Antardasha",
    @SerializedName("planets") val planets: List<PlanetInfo> = emptyList(),
    @SerializedName("d1_houses") val d1Houses: Map<String, List<String>> = emptyMap(),
    @SerializedName("d9_houses") val d9Houses: Map<String, List<String>> = emptyMap(),
    @SerializedName("d10_houses") val d10Houses: Map<String, List<String>> = emptyMap(),
    @SerializedName("dasha_timeline") val dashaTimeline: List<VimshottariDashaItem> = emptyList(),
    @SerializedName("yogas") val yogas: List<YogaDoshaItem> = emptyList(),
    @SerializedName("ashtakavarga") val ashtakavarga: List<AshtakavargaHouseBindu> = emptyList(),
    @SerializedName("lal_kitab_remedies") val lalKitabRemedies: List<LalKitabRemedyItem> = emptyList(),
    @SerializedName("bnn_combinations") val bnnCombinations: List<BNNCombination> = emptyList(),
    @SerializedName("jaimini_karakas") val jaiminiKarakas: List<JaiminiKaraka> = emptyList(),
    @SerializedName("health_indicators") val healthIndicators: List<AstroHealthIndicator> = emptyList()
)
