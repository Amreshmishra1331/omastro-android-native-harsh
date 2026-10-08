package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class KundaliRequest(
    @SerializedName("name") val name: String,
    @SerializedName("dob") val dob: String, // YYYY-MM-DD
    @SerializedName("tob") val tob: String, // HH:mm
    @SerializedName("pob") val pob: String, // City name
    @SerializedName("lat") val lat: Double = 28.6139,
    @SerializedName("lon") val lon: Double = 77.2090,
    @SerializedName("gender") val gender: String = "male"
)

data class PlanetInfo(
    @SerializedName("name") val name: String,
    @SerializedName("sign") val sign: String,
    @SerializedName("sign_number") val signNumber: Int, // 1 (Aries) to 12 (Pisces)
    @SerializedName("degree") val degree: Double,
    @SerializedName("house") val house: Int, // 1 to 12
    @SerializedName("is_retrograde") val isRetrograde: Boolean = false,
    @SerializedName("nakshatra") val nakshatra: String,
    @SerializedName("pada") val pada: Int
)

data class KundaliResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("ascendant_sign") val ascendantSign: Int = 1, // 1 to 12
    @SerializedName("ascendant_sign_name") val ascendantSignName: String = "Aries (Mesha)",
    @SerializedName("nakshatra") val nakshatra: String = "Ashwini",
    @SerializedName("planets") val planets: List<PlanetInfo> = emptyList(),
    // Mapping from house number 1..12 to list of planet short codes ("Su", "Mo", "Ma", "Me", "Ju", "Ve", "Sa", "Ra", "Ke")
    @SerializedName("d1_houses") val d1Houses: Map<String, List<String>> = emptyMap(),
    @SerializedName("d9_houses") val d9Houses: Map<String, List<String>> = emptyMap()
)
