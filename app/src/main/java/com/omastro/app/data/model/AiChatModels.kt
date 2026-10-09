package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * A birth-data profile attached to an AI chat session.
 *
 * Mirrors the website's "Attach Kundali" form: Name, Gender, DOB, TOB (AM/PM),
 * Place (with autocomplete). Attachments accumulate; the context string sent
 * with each message is rebuilt from all attachments plus the server-returned
 * chatContext.
 */
data class BirthDataAttachment(
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String, // "M" | "F"
    @SerializedName("dob") val dob: String,      // YYYY-MM-DD (API format)
    @SerializedName("tob") val tob: String,      // "HH:MM AM/PM"
    @SerializedName("place") val place: String,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("tz") val tz: Double? = null,
    @SerializedName("label") val label: String? = null
) {
    /** Short chip label shown in the UI. */
    val chipLabel: String
        get() = label?.takeIf { it.isNotBlank() } ?: name.ifBlank { "Birth Data" }

    /** Human-readable summary for the attachment list. */
    val summary: String
        get() {
            val genderLabel = when (gender.uppercase()) {
                "M", "MALE" -> "Male"
                "F", "FEMALE" -> "Female"
                else -> gender
            }
            return "$name ($genderLabel) - ${formatDobDisplay(dob)}, $tob, $place"
        }

    companion object {
        /** YYYY-MM-DD -> DD-MM-YYYY for display. */
        fun formatDobDisplay(apiDate: String): String {
            val parts = apiDate.split("-")
            return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else apiDate
        }
    }
}

/**
 * Builds the context string sent with each AI chat message.
 *
 * Format mirrors the website's _kgRebuildSavedContext(): each attached profile
 * becomes a labeled block. The server computes the full kundali and returns
 * chatContext, which is then replayed on subsequent turns.
 */
fun buildBirthDataContext(attachments: List<BirthDataAttachment>): String {
    if (attachments.isEmpty()) return ""
    return attachments.mapIndexed { index, a ->
        val header = if (attachments.size > 1) {
            "=== ATTACHED BIRTH DATA ${index + 1}: ${a.name} ==="
        } else {
            "=== ATTACHED BIRTH DATA: ${a.name} ==="
        }
        val coords = if (a.lat != null && a.lon != null) {
            " (${a.lat}, ${a.lon}" + (a.tz?.let { ", TZ:$it" } ?: "") + ")"
        } else ""
        val genderLabel = when (a.gender.uppercase()) {
            "M", "MALE" -> "Male"
            "F", "FEMALE" -> "Female"
            else -> a.gender
        }
        """
        $header
        PROFILE: ${a.name}|Gender:$genderLabel|DOB:${a.dob}|TOB:${a.tob}|Place:${a.place}$coords
        """.trimIndent()
    }.joinToString("\n\n")
}

/** Quota info returned with every AI chat response. */
data class AiChatQuota(
    @SerializedName("questions_used") val used: Int = 0,
    @SerializedName("questions_limit") val limit: Int = 0,
    @SerializedName("questions_left") val left: Int = 0,
    @SerializedName("plan") val plan: String = "free"
)

/** Birth data the server extracted or auto-computed from a message. */
data class AutoKundali(
    @SerializedName("name") val name: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("tob") val tob: String? = null,
    @SerializedName("place") val place: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("gender") val gender: String? = null
)

/** Full parsed response from POST /ai/chat. */
data class AiChatFullResponse(
    val reply: String,
    val model: String? = null,
    val persona: String? = null,
    val quota: AiChatQuota = AiChatQuota(),
    val chatContext: String? = null,
    val autoKundali: AutoKundali? = null,
    val sessionId: String? = null
)

/** AI model tiers supported by the backend. */
enum class AiModel(val apiValue: String, val displayName: String) {
    LITE("lite", "Lite"),
    PRO("pro", "Pro"),
    EXPERT("expert", "Expert")
}

/** A single turn in the history payload sent to the server. */
data class ChatHistoryTurn(
    @SerializedName("role") val role: String, // "user" | "model"
    @SerializedName("text") val text: String
)

/** A saved chat session from GET /ai/chats. */
data class SavedChatSession(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("kundali_ctx") val kundaliCtx: String? = null,
    @SerializedName("context") val context: String? = null,
    @SerializedName("messages") val messages: List<SavedChatMessage> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    /** Display title with fallback. */
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "Chat ${id.take(8)}"

    /** The stored context, accepting either field name the server uses. */
    val storedContext: String?
        get() = kundaliCtx?.takeIf { it.isNotBlank() } ?: context?.takeIf { it.isNotBlank() }
}

data class SavedChatMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("is_user") val isUser: Boolean? = null,
    @SerializedName("sender") val sender: String? = null
) {
    /** Normalized: true if this turn is from the user. */
    val userTurn: Boolean
        get() = isUser ?: when (role?.lowercase()) {
            "user" -> true
            "model", "assistant", "ai" -> false
            else -> sender?.equals("user", ignoreCase = true) == true
        }

    /** Normalized message text. */
    val content: String
        get() = text?.takeIf { it.isNotBlank() } ?: message.orEmpty()
}

data class SavedChatListData(
    @SerializedName("chats") val chats: List<SavedChatSession> = emptyList(),
    @SerializedName("sessions") val sessions: List<SavedChatSession> = emptyList()
) {
    val all: List<SavedChatSession>
        get() = if (chats.isNotEmpty()) chats else sessions
}

data class SavedChatListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: SavedChatListData? = null,
    @SerializedName("chats") val chats: List<SavedChatSession> = emptyList(),
    @SerializedName("message") val message: String? = null
) {
    val allSessions: List<SavedChatSession>
        get() = data?.all?.takeIf { it.isNotEmpty() } ?: chats
}

data class SavedChatDetailResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: SavedChatSession? = null,
    @SerializedName("chat") val chat: SavedChatSession? = null,
    @SerializedName("message") val message: String? = null
) {
    val session: SavedChatSession?
        get() = data ?: chat
}

/** Request body for POST /ai/chats (save/update a session). */
data class SaveChatRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("title") val title: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("language") val language: String = "en",
    @SerializedName("kundali_ctx") val kundaliCtx: String? = null,
    @SerializedName("messages") val messages: List<ChatHistoryTurn>
)
