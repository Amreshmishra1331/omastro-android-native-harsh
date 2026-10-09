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

// ---------------------------------------------------------------------------
// Astrologer chat session — real backend contract (verified 2026-10-09).
// POST /astrologers/chat/open {slug} -> {"success":true,"data":{"registered":true,"session_id":"<uuid4>"}}
// ---------------------------------------------------------------------------

/** Request: the server expects `slug` (e.g. "harsh-pandey"), NOT astrologer_id. */
data class OpenChatRequest(
    @SerializedName("slug") val slug: String
)

data class OpenChatData(
    @SerializedName("registered") val registered: Boolean = false,
    @SerializedName("session_id") val sessionId: String? = null
)

data class OpenChatResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: OpenChatData? = null,
    @SerializedName("message") val message: String? = null
) {
    val chatSessionId: String? get() = data?.sessionId
}

/** Request: the server only takes `session_id` for close. */
data class CloseChatRequest(
    @SerializedName("session_id") val sessionId: String
)

// ---------------------------------------------------------------------------
// Billing session — real backend contract (astro-billing.php).
// start/tick/end -> {"success":true,"data":{session_state}} where session_state is:
// {session_uuid, astrologer_slug, rate_paise, billed_seconds, free_seconds_used,
//  free_seconds_left, wallet_total_paise, wallet_covers_sec, status}.
// On insufficient balance the bridge returns success:true with data.insufficient=true
// (the AJAX layer's wp_send_json_error is re-wrapped by kg_rest_astro_bridge).
// ---------------------------------------------------------------------------

data class AstroSessionStartRequest(
    @SerializedName("slug") val slug: String
)

data class AstroSessionTickRequest(
    @SerializedName("session_uuid") val sessionUuid: String
)

data class AstroSessionState(
    @SerializedName("session_uuid") val sessionUuid: String? = null,
    @SerializedName("astrologer_slug") val astrologerSlug: String = "",
    @SerializedName("rate_paise") val ratePaise: Int = 500,
    @SerializedName("billed_seconds") val billedSeconds: Int = 0,
    @SerializedName("free_seconds_used") val freeSecondsUsed: Int = 0,
    @SerializedName("free_seconds_left") val freeSecondsLeft: Int = 0,
    @SerializedName("wallet_total_paise") val walletTotalPaise: Int = 0,
    @SerializedName("wallet_covers_sec") val walletCoversSec: Int = 0,
    @SerializedName("status") val status: String = "",
    /** True when the server blocked the session for insufficient balance. */
    @SerializedName("insufficient") val insufficient: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("resumed") val resumed: Boolean = false
) {
    /** Display rate, e.g. "₹5/min". */
    val rateLabel: String
        get() = if (ratePaise > 0) "₹${ratePaise / 100}/min" else "Free"

    /** True while the free trial covers the chat (no paid debit yet). */
    val isFreeTrial: Boolean
        get() = freeSecondsLeft > 0
}

data class AstroSessionResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: AstroSessionState? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: String? = null
)

// ---------------------------------------------------------------------------
// Review — POST /astrologers/review {slug, stars, review, session_id}.
// One review per session (UNIQUE on session_id, server-enforced).
// ---------------------------------------------------------------------------

data class AstrologerReviewRequest(
    @SerializedName("slug") val slug: String,
    @SerializedName("stars") val stars: Int,
    @SerializedName("review") val review: String = "",
    @SerializedName("session_id") val sessionId: String = ""
)

// ---------------------------------------------------------------------------
// Chat history — GET /astrologers/chat/history?session_id=&astrologer_slug=
// Restores past messages + astrologer + birth context (server-side).
// ---------------------------------------------------------------------------

data class ChatHistoryResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: ChatHistoryData? = null,
    @SerializedName("message") val message: String? = null
)

data class ChatHistoryData(
    @SerializedName("messages") val messages: List<ChatMessage> = emptyList(),
    @SerializedName("astrologer") val astrologer: Astrologer? = null,
    // Birth context keys vary; kept tolerant.
    @SerializedName("birth_context") val birthContext: String? = null,
    @SerializedName("context") val context: String? = null
)
