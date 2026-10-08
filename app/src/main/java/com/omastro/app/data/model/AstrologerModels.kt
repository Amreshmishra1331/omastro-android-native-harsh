package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class Astrologer(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("title") val title: String,
    @SerializedName("speciality") val speciality: List<String>,
    @SerializedName("languages") val languages: List<String>,
    @SerializedName("experience_years") val experienceYears: Int,
    @SerializedName("price_per_min") val pricePerMin: Int,
    @SerializedName("rating") val rating: Double,
    @SerializedName("total_consultations") val totalConsultations: Int,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("avatar_url") val avatarUrl: String = ""
)

data class ChatMessage(
    @SerializedName("id") val id: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("is_user") val isUser: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class SendMessageRequest(
    @SerializedName("astrologer_id") val astrologerId: String,
    @SerializedName("message") val message: String
)
