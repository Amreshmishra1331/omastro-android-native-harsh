package com.omastro.app.data.repository

import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.CloseChatRequest
import com.omastro.app.data.remote.OmAstroApiService
import com.omastro.app.data.remote.OpenChatRequest
import com.omastro.app.data.remote.OpenChatResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class ConsultationRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    suspend fun getAstrologers(category: String? = null): Result<List<Astrologer>> {
        return try {
            val res = api.getAstrologers(category)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Failed to load astrologers: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadChatHistory(astrologerId: String): Result<List<ChatMessage>> {
        return try {
            val res = api.getChatHistory(astrologerId)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                appendMessages(astrologerId, body)
                Result.success(body)
            } else {
                Result.failure(IOException("Failed to load chat history: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun appendMessages(astrologerId: String, newMessages: List<ChatMessage>) {
        _messages.value = _messages.value.toMutableMap().apply {
            val existing = get(astrologerId) ?: emptyList()
            val existingIds = existing.map { it.id }.toSet()
            put(astrologerId, existing + newMessages.filter { it.id !in existingIds })
        }
    }

    suspend fun openChat(astrologerId: String): Result<OpenChatResponse> {
        return try {
            val res = api.openChat(OpenChatRequest(astrologerId))
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Failed to open chat: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeChat(astrologerId: String, sessionId: String? = null): Result<Boolean> {
        return try {
            val res = api.closeChat(CloseChatRequest(astrologerId, sessionId))
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("Failed to close chat: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startBillingSession(astrologerId: String): Result<String?> {
        return try {
            val res = api.startAstroSession(mapOf("slug" to astrologerId))
            if (res.isSuccessful) {
                val uuid = res.body()?.get("session_uuid") as? String
                Result.success(uuid)
            } else {
                Result.failure(IOException("Failed to start billing session: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tickBillingSession(sessionUuid: String): Result<Boolean> {
        return try {
            val res = api.tickAstroSession(mapOf("session_uuid" to sessionUuid))
            Result.success(res.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endBillingSession(sessionUuid: String): Result<Boolean> {
        return try {
            val res = api.endAstroSession(mapOf("session_uuid" to sessionUuid))
            Result.success(res.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
