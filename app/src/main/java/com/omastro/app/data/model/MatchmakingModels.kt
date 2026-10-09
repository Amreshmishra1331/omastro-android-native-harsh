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

/**
 * Sentinel defaults (-1 / blank) are DELIBERATE: if the server omits a key,
 * Gson fills the Kotlin default — and a plausible-looking default like 28.5/79%
 * would display as a real verdict. The screen treats any sentinel as
 * "no real data" and shows an error state instead. Never display sentinels.
 */
data class MatchmakingResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("total_score") val totalScore: Double = -1.0,
    @SerializedName("max_score") val maxScore: Double = -1.0,
    @SerializedName("percentage") val percentage: Int = -1,
    @SerializedName("verdict") val verdict: String = "",
    @SerializedName("manglik_boy") val manglikBoy: String = "",
    @SerializedName("manglik_girl") val manglikGirl: String = "",
    @SerializedName("gunas") val gunas: List<GunaDetail> = emptyList(),
    @SerializedName("conclusion") val conclusion: String = ""
) {
    /** True only when the server returned a real, complete score block. */
    val hasRealData: Boolean
        get() = success && totalScore >= 0 && maxScore > 0 && percentage >= 0 && verdict.isNotBlank()
}
