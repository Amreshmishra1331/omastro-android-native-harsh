package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class PanchangResponse(
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: String,
    @SerializedName("sunrise") val sunrise: String,
    @SerializedName("sunset") val sunset: String,
    @SerializedName("moonrise") val moonrise: String,
    @SerializedName("tithi") val tithi: String,
    @SerializedName("nakshatra") val nakshatra: String,
    @SerializedName("yoga") val yoga: String,
    @SerializedName("karana") val karana: String,
    @SerializedName("rahu_kaal") val rahuKaal: String,
    @SerializedName("yamaganda") val yamaganda: String,
    @SerializedName("gulika_kaal") val gulikaKaal: String,
    @SerializedName("abhijit_muhurat") val abhijitMuhurat: String,
    @SerializedName("amrit_kaal") val amritKaal: String,
    @SerializedName("is_auspicious") val isAuspicious: Boolean = true
)
