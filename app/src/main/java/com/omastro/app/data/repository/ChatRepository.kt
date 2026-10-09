package com.omastro.app.data.repository

import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import java.io.IOException

/**
 * AI chat backed by POST /ai/chat. Personas: general, divine, mundane.
 * Replies are parsed defensively from the response envelope; on any error
 * a failure is returned — replies are never fabricated locally.
 */
class ChatRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    /**
     * Sends a message and returns Pair(reply, sessionId).
     */
    suspend fun sendAiMessage(
        message: String,
        sessionId: String?,
        persona: String
    ): Result<Pair<String, String?>> {
        return try {
            val payload = mutableMapOf<String, Any?>(
                "message" to message,
                "persona" to persona
            )
            sessionId?.let { payload["session_id"] = it }
            val res = api.aiChat(payload)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(
                    IOException("AI chat request failed: HTTP ${res.code()}")
                )
            }
            val data = body["data"] as? Map<*, *>
            val reply = (data?.get("reply") as? String)
                ?: (body["reply"] as? String)
            val newSessionId = (data?.get("session_id") as? String)
                ?: (body["session_id"] as? String)
                ?: sessionId
            if (reply.isNullOrBlank()) {
                return Result.failure(
                    IOException("AI chat returned an empty reply.")
                )
            }
            Result.success(Pair(reply, newSessionId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
