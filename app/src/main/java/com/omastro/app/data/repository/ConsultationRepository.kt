package com.omastro.app.data.repository

import com.omastro.app.data.model.AstroSessionResponse
import com.omastro.app.data.model.AstroSessionStartRequest
import com.omastro.app.data.model.AstroSessionState
import com.omastro.app.data.model.AstroSessionTickRequest
import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.AstrologerReviewRequest
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.data.model.CloseChatRequest
import com.omastro.app.data.model.OpenChatRequest
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Astrologer consultation repository — real backend contracts (verified 2026-10-09).
 *
 * Endpoints (all under /wp-json/omastro/v1):
 *  - GET  /astrologers                    -> {success,data:{astrologers:[...]}}
 *  - POST /astrologers/chat/open {slug}   -> {success,data:{registered,session_id}}
 *  - GET  /astrologers/chat/history?session_id=&astrologer_slug=
 *  - POST /astrologers/chat/close {session_id}
 *  - POST /astrologers/session/start {slug}      -> {success,data:{session state}}
 *  - POST /astrologers/session/tick {session_uuid}
 *  - POST /astrologers/session/end  {session_uuid}
 *  - POST /astrologers/review {slug, stars, review, session_id}
 *
 * NOTE: insufficient balance is signalled as success:true with data.insufficient=true
 * (the AJAX error is re-wrapped by kg_rest_astro_bridge) — callers must check the flag.
 */
class ConsultationRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    suspend fun getAstrologers(category: String? = null): Result<List<Astrologer>> {
        return try {
            val res = api.getAstrologers(category)
            val body = res.body()
            // Real envelope: {"success":true,"data":{"astrologers":[...]}}
            val list = body?.data?.astrologers.orEmpty()
            if (res.isSuccessful && body != null && body.success) {
                Result.success(list)
            } else {
                val msg = body?.message ?: "Failed to load astrologers: HTTP ${res.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Registers a chat session. Server mints the UUID4 session id. */
    suspend fun openChat(slug: String): Result<String> {
        return try {
            val res = api.openChat(OpenChatRequest(slug))
            val body = res.body()
            val sessionId = body?.data?.sessionId
            if (res.isSuccessful && body != null && body.success && !sessionId.isNullOrBlank()) {
                Result.success(sessionId)
            } else {
                val msg = body?.message ?: "Could not register the chat session: HTTP ${res.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Restores past messages + astrologer + birth context from the server. */
    suspend fun loadChatHistory(
        sessionId: String? = null,
        astrologerSlug: String? = null,
        cacheKey: String
    ): Result<List<ChatMessage>> {
        return try {
            val res = api.getChatHistory(sessionId, astrologerSlug)
            val body = res.body()
            val msgs = body?.data?.messages.orEmpty()
            if (res.isSuccessful && body != null && body.success) {
                appendMessages(cacheKey, msgs)
                Result.success(msgs)
            } else {
                val msg = body?.message ?: "Failed to load chat history: HTTP ${res.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun appendMessages(cacheKey: String, newMessages: List<ChatMessage>) {
        if (newMessages.isEmpty()) return
        _messages.value = _messages.value.toMutableMap().apply {
            val existing = get(cacheKey) ?: emptyList()
            val existingIds = existing.map { it.id }.toSet()
            put(cacheKey, existing + newMessages.filter { it.id !in existingIds })
        }
    }

    fun clearMessages(cacheKey: String) {
        _messages.value = _messages.value.toMutableMap().apply { remove(cacheKey) }
    }

    suspend fun closeChat(sessionId: String): Result<Boolean> {
        return try {
            val res = api.closeChat(CloseChatRequest(sessionId))
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("Failed to close chat: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens (or resumes) a billing session. Returns the full session state.
     * Check [AstroSessionState.insufficient] — true means blocked for low balance.
     */
    suspend fun startBillingSession(slug: String): Result<AstroSessionState> {
        return try {
            val res = api.startAstroSession(AstroSessionStartRequest(slug))
            val body = res.body()
            val state = body?.data
            if (res.isSuccessful && body != null && state != null) {
                Result.success(state)
            } else {
                val msg = body?.message ?: "Could not start billing session: HTTP ${res.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 30-second heartbeat. Returns the updated session state. */
    suspend fun tickBillingSession(sessionUuid: String): Result<AstroSessionState> {
        return try {
            val res = api.tickAstroSession(AstroSessionTickRequest(sessionUuid))
            val body = res.body()
            val state = body?.data
            if (res.isSuccessful && body != null && state != null) {
                Result.success(state)
            } else {
                val msg = body?.message ?: "Billing tick failed: HTTP ${res.code()}"
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Final bill + close. Returns the final session state. */
    suspend fun endBillingSession(sessionUuid: String): Result<AstroSessionState?> {
        return try {
            val res = api.endAstroSession(AstroSessionTickRequest(sessionUuid))
            val body = res.body()
            if (res.isSuccessful) {
                Result.success(body?.data)
            } else {
                Result.failure(IOException("Failed to end billing session: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Submits a 1-5 star review with optional text (<=500 chars). One per session. */
    suspend fun submitReview(
        slug: String,
        stars: Int,
        review: String,
        sessionId: String
    ): Result<Boolean> {
        return try {
            val res = api.submitAstrologerReview(
                AstrologerReviewRequest(slug, stars.coerceIn(1, 5), review.take(500), sessionId)
            )
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("Could not submit review: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
