package com.omastro.app.data.repository

import com.google.gson.Gson
import com.omastro.app.data.model.AiChatFullResponse
import com.omastro.app.data.model.AiChatQuota
import com.omastro.app.data.model.AiModel
import com.omastro.app.data.model.AutoKundali
import com.omastro.app.data.model.ChatHistoryTurn
import com.omastro.app.data.model.SaveChatRequest
import com.omastro.app.data.model.SavedChatSession
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import java.io.IOException

/**
 * AI chat backed by POST /ai/chat with the full website-parity context system.
 *
 * Per the website contract (researched 2026-10-09 from production plugin):
 * - Every message sends: message, ai_model, persona, language, context,
 *   history (last 12 turns), session_id
 * - Every response returns: reply, model, persona, questions_used/limit/left,
 *   chatContext, autoKundali, session_id
 * - The app MUST store chatContext and replay it as `context` on the next
 *   turn — this is how the AI "remembers" the chart.
 * - autoKundali (birth data the server extracted) should be auto-registered
 *   into the attachment list when dob is present.
 *
 * Session persistence:
 * - POST /ai/chats — save/update a session (with kundali_ctx)
 * - GET /ai/chats — list saved sessions
 * - GET /ai/chats/{id} — load one session (history + context)
 * - DELETE /ai/chats/{id} — delete one session
 *
 * All intelligence (prompts, tools, calculations) lives server-side. The app
 * is a thin client: send message + context + history, store and replay
 * chatContext / session_id.
 */
class ChatRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val gson = Gson()

    /**
     * Sends a message with full context and returns the parsed full response.
     *
     * @param message user message text (2-2000 chars)
     * @param sessionId session UUID for continuation (null for new chat)
     * @param persona AI persona (general, mundane, divine_ram, ...)
     * @param context combined context string (attachments + stored chatContext), max 28k chars
     * @param history last 12 turns as ChatHistoryTurn(role, text)
     * @param aiModel model tier (lite/pro/expert)
     * @param language "en" | "hi"
     * @param billingMode pass "astrologer" for astrologer consultations — the server
     *   skips the normal per-message AI debit when a live billing session UUID is
     *   present (the per-minute heartbeat owns the charge).
     * @param astroSession billing session UUID from POST /astrologers/session/start
     * @param ctxLabel label for the attached birth context (e.g. "Birth Chart: ...")
     */
    suspend fun sendAiMessage(
        message: String,
        sessionId: String?,
        persona: String,
        context: String? = null,
        history: List<ChatHistoryTurn> = emptyList(),
        aiModel: AiModel = AiModel.LITE,
        language: String = "en",
        billingMode: String? = null,
        astroSession: String? = null,
        ctxLabel: String? = null
    ): Result<AiChatFullResponse> {
        return try {
            val payload = mutableMapOf<String, Any?>(
                "message" to message,
                "ai_model" to aiModel.apiValue,
                "persona" to persona,
                "language" to language
            )
            sessionId?.let { payload["session_id"] = it }
            if (!context.isNullOrBlank()) {
                // Server truncates to 28,000 chars; truncate client-side too
                // to keep payloads predictable.
                payload["context"] = context.take(28000)
            }
            if (!ctxLabel.isNullOrBlank()) {
                payload["ctx_label"] = ctxLabel
            }
            if (!billingMode.isNullOrBlank()) {
                payload["billing_mode"] = billingMode
            }
            if (!astroSession.isNullOrBlank()) {
                payload["astro_session"] = astroSession
            }
            if (history.isNotEmpty()) {
                // Last 12 turns, each text truncated to 4000 chars (website parity)
                payload["history"] = history.takeLast(12).map {
                    mapOf("role" to it.role, "text" to it.text.take(4000))
                }
            }
            val res = api.aiChat(payload)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                // Honest failure: prefer the server's own message (e.g. quota
                // exhausted) over a bare "HTTP 429" string.
                return Result.failure(
                    IOException(serverErrorMessage(res) ?: "AI chat request failed: HTTP ${res.code()}")
                )
            }
            parseAiChatResponse(body, sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts a human-readable message from an unsuccessful response's
     * error body (WordPress REST shape: {message} or {data: {message}}).
     * Returns null when there is nothing usable — the caller falls back to
     * a generic, bilingual message.
     */
    private fun serverErrorMessage(res: retrofit2.Response<*>): String? {
        return try {
            val raw = res.errorBody()?.string()
            if (raw.isNullOrBlank()) return null
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(raw, Map::class.java) as? Map<String, Any>
            val direct = map?.get("message") as? String
            if (!direct.isNullOrBlank()) return direct
            val nested = (map?.get("data") as? Map<String, Any>)?.get("message") as? String
            nested?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Backwards-compatible overload used by existing callers.
     * Sends without context/history (fresh chat, default model).
     */
    suspend fun sendAiMessage(
        message: String,
        sessionId: String?,
        persona: String
    ): Result<Pair<String, String?>> {
        return sendAiMessage(message, sessionId, persona, null, emptyList())
            .map { Pair(it.reply, it.sessionId ?: sessionId) }
    }

    /** Parses the POST /ai/chat envelope defensively. */
    @Suppress("UNCHECKED_CAST")
    private fun parseAiChatResponse(
        body: Map<String, Any>,
        fallbackSessionId: String?
    ): Result<AiChatFullResponse> {
        val data = body["data"] as? Map<String, Any>
        val reply = (data?.get("reply") as? String)
            ?: (body["reply"] as? String)
        if (reply.isNullOrBlank()) {
            val errMsg = (data?.get("message") as? String)
                ?: (body["message"] as? String)
                ?: "AI chat returned an empty reply."
            return Result.failure(IOException(errMsg))
        }
        val newSessionId = (data?.get("session_id") as? String)
            ?: (body["session_id"] as? String)
            ?: fallbackSessionId
        val quota = AiChatQuota(
            used = (data?.get("questions_used") as? Number)?.toInt() ?: 0,
            limit = (data?.get("questions_limit") as? Number)?.toInt() ?: 0,
            left = (data?.get("questions_left") as? Number)?.toInt() ?: 0,
            plan = (data?.get("plan") as? String) ?: "free"
        )
        val chatContext = (data?.get("chatContext") as? String)
            ?: (data?.get("chat_context") as? String)
        val autoKundali = (data?.get("autoKundali") as? Map<String, Any>)?.let {
            AutoKundali(
                name = it["name"] as? String,
                dob = it["dob"] as? String,
                tob = it["tob"] as? String,
                place = it["place"] as? String,
                label = it["label"] as? String,
                gender = it["gender"] as? String
            )
        }
        return Result.success(
            AiChatFullResponse(
                reply = reply,
                model = (data?.get("model") as? String),
                persona = (data?.get("persona") as? String),
                quota = quota,
                chatContext = chatContext,
                autoKundali = autoKundali,
                sessionId = newSessionId
            )
        )
    }

    // ------------------------------------------------------------------
    // Saved chat sessions
    // ------------------------------------------------------------------

    /** Lists saved chat sessions (50 latest). */
    suspend fun listSavedChats(): Result<List<SavedChatSession>> {
        return try {
            val res = api.listAiChats()
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(
                    IOException("Could not load saved chats: HTTP ${res.code()}")
                )
            }
            Result.success(body.allSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Saves (or updates) a chat session with its context. */
    suspend fun saveChatSession(request: SaveChatRequest): Result<String?> {
        return try {
            val res = api.saveAiChat(request)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(
                    IOException("Could not save chat: HTTP ${res.code()}")
                )
            }
            val data = body["data"] as? Map<String, Any>
            val id = (data?.get("id") as? String)
                ?: (data?.get("chat_id") as? String)
                ?: (body["id"] as? String)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Loads one saved chat with its full history and context. */
    suspend fun loadChatSession(chatId: String): Result<SavedChatSession> {
        return try {
            val res = api.getAiChat(chatId)
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return Result.failure(
                    IOException("Could not load chat: HTTP ${res.code()}")
                )
            }
            val session = body.session
                ?: return Result.failure(IOException("Chat not found."))
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Deletes one saved chat. */
    suspend fun deleteChatSession(chatId: String): Result<Unit> {
        return try {
            val res = api.deleteAiChat(chatId)
            if (!res.isSuccessful) {
                return Result.failure(
                    IOException("Could not delete chat: HTTP ${res.code()}")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Clears all saved chats. */
    suspend fun clearAllChats(): Result<Unit> {
        return try {
            val res = api.clearAiChats()
            if (!res.isSuccessful) {
                return Result.failure(
                    IOException("Could not clear chats: HTTP ${res.code()}")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
