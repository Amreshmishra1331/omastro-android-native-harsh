package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class MatchmakingRequest(
    @SerializedName("boy_name") val boyName: String,
    @SerializedName("boy_dob") val boyDob: String,
    @SerializedName("boy_tob") val boyTob: String,
    @SerializedName("boy_pob") val boyPob: String,
    @SerializedName("girl_name") val girlName: String,
    @SerializedName("girl_dob") val girlDob: String,
    @SerializedName("girl_tob") val girlTob: String,
    @SerializedName("girl_pob") val girlPob: String
)

data class GunaDetail(
    @SerializedName("name") val name: String,
    @SerializedName("obtained") val obtained: Double,
    @SerializedName("maximum") val maximum: Double,
    @SerializedName("description") val description: String
)

data class MatchmakingResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("total_score") val totalScore: Double = 28.5,
    @SerializedName("max_score") val maxScore: Double = 36.0,
    @SerializedName("percentage") val percentage: Int = 79,
    @SerializedName("verdict") val verdict: String = "Highly Auspicious Match",
    @SerializedName("manglik_boy") val manglikBoy: String = "Non-Manglik",
    @SerializedName("manglik_girl") val manglikGirl: String = "Non-Manglik",
    @SerializedName("gunas") val gunas: List<GunaDetail> = emptyList(),
    @SerializedName("conclusion") val conclusion: String = "Planetary harmony indicates excellent spiritual, mental, and physical compatibility."
)
