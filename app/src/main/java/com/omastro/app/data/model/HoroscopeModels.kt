package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class HoroscopeSign(
    val id: String,
    val name: String,
    val hindiName: String,
    val symbol: String,
    val element: String,
    val dates: String
)

data class DailyHoroscopeResponse(
    @SerializedName("sign") val sign: String,
    @SerializedName("date") val date: String,
    @SerializedName("period") val period: String = "today", // today, tomorrow, weekly
    @SerializedName("prediction") val prediction: String,
    @SerializedName("love_score") val loveScore: Int = 85,
    @SerializedName("career_score") val careerScore: Int = 78,
    @SerializedName("health_score") val healthScore: Int = 92,
    @SerializedName("lucky_number") val luckyNumber: Int = 7,
    @SerializedName("lucky_color") val luckyColor: String = "Saffron Gold",
    @SerializedName("remedy") val remedy: String = "Chant Gayatri Mantra 11 times in the morning."
)
