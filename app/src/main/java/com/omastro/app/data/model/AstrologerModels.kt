package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Astrologer model matching the real production API:
 * GET /omastro/v1/astrologers -> {"success":true,"data":{"astrologers":[...]}}
 *
 * Real fields: id, name, slug, img, rating, reviews, experience,
 * specialisations, languages, about, online, rate_paise, rate_label, gender
 */
data class Astrologer(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String = "",
    @SerializedName("img") val img: String = "",
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("reviews") val reviews: Int = 0,
    @SerializedName("experience") val experience: Int = 0,
    @SerializedName("specialisations") val specialisations: List<String> = emptyList(),
    @SerializedName("languages") val languages: List<String> = emptyList(),
    @SerializedName("about") val about: String = "",
    @SerializedName("online") val online: Boolean = false,
    @SerializedName("rate_paise") val ratePaise: Int = 0,
    @SerializedName("rate_label") val rateLabel: String = "",
    @SerializedName("gender") val gender: String = ""
) {
    /** Display price, e.g. "₹5/min" from the server's rate_label. */
    val displayRate: String
        get() = rateLabel.ifBlank {
            if (ratePaise > 0) "₹${ratePaise / 100}/min" else "Free"
        }
}

/** Envelope: {"success":true,"data":{"astrologers":[...]}} */
data class AstrologersResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: AstrologersData? = null,
    @SerializedName("message") val message: String? = null
)

data class AstrologersData(
    @SerializedName("astrologers") val astrologers: List<Astrologer> = emptyList()
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
