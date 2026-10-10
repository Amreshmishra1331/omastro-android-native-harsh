package com.omastro.app.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.AiChatQuota
import com.omastro.app.data.model.AiModel
import com.omastro.app.data.model.BirthDataAttachment
import com.omastro.app.data.model.ChatHistoryTurn
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.data.model.PendingPhotoContextHolder
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.model.SaveChatRequest
import com.omastro.app.data.model.SavedChatSession
import com.omastro.app.data.model.buildBirthDataContext
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AiChatMode(val persona: String, val title: String, val subtitle: String) {
    GENERAL("general", "General", "Vedic guidance on any question"),
    DIVINE("divine", "Divine", "Connect with the divine"),
    MUNDANE("mundane", "Mundane", "World events and collective trends")
}

/**
 * Per-mode chat state. Mirrors the website's session model:
 * - messages: the visible conversation
 * - chatContext: server-computed context, replayed as `context` each turn
 * - sessionId: links tool-calculated context across turns
 * - attachments: birth-data profiles attached by the user
 * - quota: questions_used/limit/left from the last response
 */
data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val error: String? = null,
    val attachments: List<BirthDataAttachment> = emptyList(),
    /** Photo/vision parsed-facts attachments (palmistry, photo analysis, vastu). */
    val photoAttachments: List<PhotoContextAttachment> = emptyList(),
    val quota: AiChatQuota = AiChatQuota(),
    val aiModel: AiModel = AiModel.LITE,
    /** "en" | "hi" — sent as `language` with every message (website parity). */
    val language: String = "en",
    val savedSessionId: String? = null,
    /** True when a saved session's stored context was restored (chips can't show it). */
    val hasRestoredContext: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val savedSessions: List<SavedChatSession> = emptyList(),
    // ---- Advanced chat: reply threading / ask-about / send retry ----
    /** Message being replied to (draft): its snapshot renders above the input bar. */
    val replyingTo: ChatMessage? = null,
    /** "Ask AI about this": selected message text sent as context with the next question. */
    val askContext: String? = null,
    /** Last message text that failed to send (for the inline retry card). */
    val failedMessage: String? = null,
    /** Friendly text for the inline send-error card. */
    val sendErrorText: String? = null
)

/** Outcome of a pull-to-refresh history reload. */
sealed interface RefreshResult {
    data object Ok : RefreshResult
    /** No saved session open — nothing on the server to reload. */
    data object NoSavedSession : RefreshResult
    /** Local messages exist that were never saved — confirm before discarding. */
    data object UnsyncedChanges : RefreshResult
    data class Failed(val message: String) : RefreshResult
}

class AiChatViewModel : ViewModel() {
    private val chatRepository = AppContainer.chatRepository

    private val _selectedMode = MutableStateFlow(AiChatMode.GENERAL)
    val selectedMode: StateFlow<AiChatMode> = _selectedMode.asStateFlow()

    private val states = AiChatMode.values().associateWith { MutableStateFlow(AiChatUiState()) }
    private val sessionIds = mutableMapOf<AiChatMode, String?>()
    private val chatContexts = mutableMapOf<AiChatMode, String?>()
    /**
     * Messages known to be on the server per mode (loaded or explicitly
     * saved). Pull-to-refresh only discards local messages with the user's
     * confirmation when messages.size exceeds this count.
     */
    private val serverSyncedCounts = mutableMapOf<AiChatMode, Int>()

    val uiState: StateFlow<AiChatUiState>
        get() = states.getValue(_selectedMode.value)

    fun selectMode(mode: AiChatMode) {
        if (_selectedMode.value != mode) {
            _selectedMode.value = mode
        }
    }

    /** Sets the AI model tier for the current mode (lite/pro/expert). */
    fun setAiModel(model: AiModel) {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(aiModel = model)
    }

    /** Sets the reply language for the current mode ("en" | "hi"). */
    fun setLanguage(language: String) {
        val lang = if (language == "hi") "hi" else "en"
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(language = lang)
    }

    /**
     * Builds the context string for the next message:
     * "Ask AI about this" reference + attachment blocks + photo parsed-facts
     * + the server-returned chatContext (replayed verbatim).
     */
    private fun buildContext(mode: AiChatMode): String {
        val state = states.getValue(mode).value
        val askCtx = state.askContext?.takeIf { it.isNotBlank() }?.let {
            "--- Message the user is asking about ---\n$it"
        } ?: ""
        val attachmentCtx = buildBirthDataContext(state.attachments)
        val photoCtx = state.photoAttachments
            .map { it.contextBlock }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        val stored = chatContexts[mode]
        return listOf(askCtx, attachmentCtx, photoCtx, stored.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    /**
     * Builds the history payload: last 12 turns as {role, text}.
     * Website parity: roles are "user" / "model".
     *
     * Takes the message list explicitly so the caller can build history from
     * the PRE-send list — the message being sent must NOT appear in history
     * (it is already sent as `message`; duplicating it wastes tokens and
     * confuses the model).
     */
    private fun buildHistory(messages: List<ChatMessage>): List<ChatHistoryTurn> {
        return messages
            .takeLast(12)
            .map {
                ChatHistoryTurn(
                    role = if (it.isUser) "user" else "model",
                    text = it.message
                )
            }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        // A fresh send dismisses the previous failure card.
        clearSendError()
        sendInternal(trimmed)
    }

    /** Re-sends the last failed message (inline retry card). */
    fun retryFailedMessage() {
        val mode = _selectedMode.value
        val failed = states.getValue(mode).value.failedMessage ?: return
        sendInternal(failed)
    }

    /** Dismisses the inline send-error card. */
    fun clearSendError() {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        if (flow.value.failedMessage != null || flow.value.sendErrorText != null) {
            flow.value = flow.value.copy(failedMessage = null, sendErrorText = null)
        }
    }

    private fun sendInternal(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val mode = _selectedMode.value
        val stateFlow = states.getValue(mode)
        val current = stateFlow.value
        if (current.isTyping) return

        // History is built BEFORE the new user message is appended, so the
        // outgoing message is never duplicated inside the history payload.
        val history = buildHistory(current.messages)

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "me",
            isUser = true,
            message = trimmed,
            // Reply threading: snapshot the quoted message onto the sent
            // message; the draft is cleared below.
            replyToText = current.replyingTo?.message,
            replyToIsUser = current.replyingTo?.isUser
        )
        stateFlow.value = current.copy(
            messages = current.messages + userMessage,
            isTyping = true,
            error = null,
            // The restored-context notice has served its purpose once the
            // user starts a fresh exchange.
            hasRestoredContext = false,
            // The draft quote is baked into the message above.
            replyingTo = null,
            failedMessage = null,
            sendErrorText = null
        )

        val context = buildContext(mode)
        val aiModel = current.aiModel

        viewModelScope.launch {
            chatRepository.sendAiMessage(
                message = trimmed,
                sessionId = sessionIds[mode],
                persona = mode.persona,
                context = context.ifBlank { null },
                history = history,
                aiModel = aiModel,
                language = current.language
            ).onSuccess { response ->
                sessionIds[mode] = response.sessionId
                // Store chatContext and replay it on the next turn.
                if (!response.chatContext.isNullOrBlank()) {
                    chatContexts[mode] = response.chatContext
                }
                // Auto-register birth data the server extracted (website parity).
                response.autoKundali?.let { ak ->
                    if (!ak.dob.isNullOrBlank()) {
                        val flow2 = states.getValue(mode)
                        val already = flow2.value.attachments.any {
                            it.dob == ak.dob && it.name == (ak.name ?: "")
                        }
                        if (!already) {
                            val attachment = BirthDataAttachment(
                                name = ak.name?.takeIf { it.isNotBlank() } ?: "Seeker",
                                gender = ak.gender?.takeIf { it.isNotBlank() } ?: "M",
                                dob = ak.dob,
                                tob = ak.tob?.takeIf { it.isNotBlank() } ?: "12:00 PM",
                                place = ak.place?.takeIf { it.isNotBlank() } ?: "",
                                label = ak.label
                            )
                            flow2.value = flow2.value.copy(
                                attachments = flow2.value.attachments + attachment
                            )
                        }
                    }
                }
                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = "ai",
                    isUser = false,
                    message = response.reply
                )
                val latest = states.getValue(mode).value
                states.getValue(mode).value = latest.copy(
                    messages = latest.messages + aiMessage,
                    isTyping = false,
                    quota = response.quota,
                    // The "Ask AI about this" reference was consumed by this send.
                    askContext = null
                )
            }.onFailure { err ->
                val latest = states.getValue(mode).value
                // Server-provided messages pass through verbatim; transport or
                // HTTP-only failures get the bilingual fallback (never a raw
                // "HTTP 500" string shown to the user). Send failures surface
                // in the inline retry card (failedMessage), not the snackbar.
                val serverMsg = err.message
                val friendly = if (serverMsg.isNullOrBlank() || serverMsg.startsWith("AI chat request failed:")) {
                    W.t("chat_reply_failed")
                } else {
                    serverMsg
                }
                states.getValue(mode).value = latest.copy(
                    isTyping = false,
                    failedMessage = trimmed,
                    sendErrorText = friendly
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Birth data attachments
    // ------------------------------------------------------------------

    /** Adds a birth-data attachment; context is rebuilt on the next send. */
    fun attachBirthData(attachment: BirthDataAttachment) {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(
            attachments = flow.value.attachments + attachment
        )
    }

    /** Removes an attachment by index. */
    fun removeAttachment(index: Int) {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        val list = flow.value.attachments
        if (index in list.indices) {
            flow.value = flow.value.copy(
                attachments = list.filterIndexed { i, _ -> i != index }
            )
        }
    }

    /** Clears all attachments for the current mode. */
    fun clearAttachments() {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(attachments = emptyList())
    }

    // ------------------------------------------------------------------
    // Photo / vision context attachments
    // ------------------------------------------------------------------

    /**
     * Attaches photo parsed-facts as chat context (same mechanism as
     * Attach Birth Data). Follow-up questions will reference the photo.
     */
    fun attachPhotoContext(attachment: PhotoContextAttachment) {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        val exists = flow.value.photoAttachments.any { it.id == attachment.id }
        if (!exists) {
            flow.value = flow.value.copy(
                photoAttachments = flow.value.photoAttachments + attachment
            )
        }
    }

    /** Removes a photo attachment by id. */
    fun removePhotoAttachment(id: String) {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(
            photoAttachments = flow.value.photoAttachments.filter { it.id != id }
        )
    }

    /**
     * Consumes a pending photo context handed off from another screen
     * (Palmistry, Vastu, photo analysis). Called when the chat opens.
     * @return true if a context was attached.
     */
    fun consumePendingPhotoContext(): Boolean {
        val pending = PendingPhotoContextHolder.consume() ?: return false
        attachPhotoContext(pending)
        return true
    }

    fun clearError() {
        val mode = _selectedMode.value
        val stateFlow = states.getValue(mode)
        stateFlow.value = stateFlow.value.copy(error = null)
    }

    // ------------------------------------------------------------------
    // Reply threading + "Ask AI about this"
    // ------------------------------------------------------------------

    /** Starts a quoted reply to [message]; its snapshot renders above the input. */
    fun setReplyingTo(message: ChatMessage) {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(replyingTo = message)
    }

    /** Cancels the reply draft. */
    fun clearReplyingTo() {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        if (flow.value.replyingTo != null) {
            flow.value = flow.value.copy(replyingTo = null)
        }
    }

    /**
     * "Ask AI about this": [text] (the selected message) is sent as context
     * with the next question, then cleared. A chip above the input shows it
     * until sent or dismissed.
     */
    fun setAskContext(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        flow.value = flow.value.copy(askContext = trimmed)
    }

    /** Dismisses the pending "Ask AI about this" reference. */
    fun clearAskContext() {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        if (flow.value.askContext != null) {
            flow.value = flow.value.copy(askContext = null)
        }
    }

    // ------------------------------------------------------------------
    // Pull-to-refresh: reload the open saved session from the server
    // ------------------------------------------------------------------

    /**
     * Reloads the open saved session's history from the server.
     * Suspends until done so the pull-to-refresh indicator can end exactly
     * when the reload finishes. Never discards unsent local messages
     * silently: [RefreshResult.UnsyncedChanges] asks the UI to confirm first.
     */
    suspend fun refreshHistory(): RefreshResult {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        val chatId = flow.value.savedSessionId ?: return RefreshResult.NoSavedSession
        if (flow.value.messages.size > (serverSyncedCounts[mode] ?: 0)) {
            return RefreshResult.UnsyncedChanges
        }
        return reloadFromServer(mode, chatId)
    }

    /**
     * Same as [refreshHistory] but discards local messages without asking.
     * Called only after the user confirmed the discard dialog.
     */
    suspend fun forceRefreshHistory(): RefreshResult {
        val mode = _selectedMode.value
        val chatId = states.getValue(mode).value.savedSessionId
            ?: return RefreshResult.NoSavedSession
        return reloadFromServer(mode, chatId)
    }

    private suspend fun reloadFromServer(mode: AiChatMode, chatId: String): RefreshResult {
        return chatRepository.loadChatSession(chatId).fold(
            onSuccess = { detail ->
                applyLoadedDetail(mode, detail)
                RefreshResult.Ok
            },
            onFailure = { err ->
                RefreshResult.Failed(err.message ?: W.t("chat_refresh_failed"))
            }
        )
    }

    /**
     * Rebuilds a mode's state from a loaded saved session (shared by
     * [loadSession] and pull-to-refresh). Marks every message server-synced.
     */
    private fun applyLoadedDetail(mode: AiChatMode, detail: SavedChatSession) {
        val messages = detail.messages.mapNotNull { m ->
            val content = m.content
            if (content.isBlank()) null else ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = if (m.userTurn) "me" else "ai",
                isUser = m.userTurn,
                message = content
            )
        }
        val flow = states.getValue(mode)
        flow.value = AiChatUiState(
            messages = messages,
            aiModel = flow.value.aiModel,
            // Keep the session's own reply language (was: reset to "en").
            language = detail.language?.takeIf { it == "hi" } ?: "en",
            savedSessionId = detail.id,
            // The stored context IS restored and sent, but the
            // attachment chips can't be rebuilt from it — flag it
            // so the UI can say so honestly instead of implying
            // "no context attached".
            hasRestoredContext = !detail.storedContext.isNullOrBlank()
        )
        sessionIds[mode] = detail.sessionId
        detail.storedContext?.let { chatContexts[mode] = it }
        serverSyncedCounts[mode] = messages.size
    }

    /** Starts a fresh chat: clears messages, context, session, attachments. */
    fun clearChat() {
        val mode = _selectedMode.value
        states.getValue(mode).value = AiChatUiState()
        sessionIds.remove(mode)
        chatContexts.remove(mode)
        serverSyncedCounts.remove(mode)
    }

    // ------------------------------------------------------------------
    // Saved chat sessions
    // ------------------------------------------------------------------

    /** Loads the saved-session list for the session picker. */
    fun loadSavedSessions() {
        val mode = _selectedMode.value
        val flow = states.getValue(mode)
        if (flow.value.isLoadingSessions) return
        flow.value = flow.value.copy(isLoadingSessions = true)
        viewModelScope.launch {
            chatRepository.listSavedChats()
                .onSuccess { sessions ->
                    states.getValue(mode).value =
                        states.getValue(mode).value.copy(
                            savedSessions = sessions,
                            isLoadingSessions = false
                        )
                }
                .onFailure { err ->
                    states.getValue(mode).value =
                        states.getValue(mode).value.copy(
                            isLoadingSessions = false,
                            error = err.message ?: W.t("chat_load_failed")
                        )
                }
        }
    }

    /**
     * Saves the current conversation to the server with its context.
     * Website parity: kundali_ctx is the combined attachment + chatContext string.
     */
    fun saveCurrentSession(onSaved: (String?) -> Unit = {}) {
        val mode = _selectedMode.value
        val state = states.getValue(mode).value
        if (state.messages.isEmpty()) {
            onSaved(null)
            return
        }
        val sessionId = sessionIds[mode] ?: UUID.randomUUID().toString()
        val title = buildSessionTitle(state.messages)
        val history = state.messages.map {
            ChatHistoryTurn(
                role = if (it.isUser) "user" else "model",
                text = it.message
            )
        }
        val request = SaveChatRequest(
            sessionId = sessionId,
            title = title,
            mode = mode.persona,
            language = states.getValue(mode).value.language,
            kundaliCtx = buildContext(mode).ifBlank { null },
            messages = history
        )
        viewModelScope.launch {
            chatRepository.saveChatSession(request)
                .onSuccess { id ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(savedSessionId = id)
                    sessionIds[mode] = sessionId
                    // Everything on screen is now server-side: a later
                    // pull-to-refresh can reload without a discard warning.
                    serverSyncedCounts[mode] = flow.value.messages.size
                    onSaved(id)
                }
                .onFailure { err ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        error = err.message ?: W.t("chat_save_failed")
                    )
                    onSaved(null)
                }
        }
    }

    /** Loads a saved session: restores messages AND context. */
    fun loadSession(session: SavedChatSession) {
        val mode = _selectedMode.value
        viewModelScope.launch {
            chatRepository.loadChatSession(session.id)
                .onSuccess { detail ->
                    applyLoadedDetail(mode, detail)
                }
                .onFailure { err ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        error = err.message ?: W.t("chat_open_failed")
                    )
                }
        }
    }

    /** Deletes a saved session (also refreshes the list). */
    fun deleteSession(chatId: String) {
        val mode = _selectedMode.value
        viewModelScope.launch {
            chatRepository.deleteChatSession(chatId)
                .onSuccess {
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        savedSessions = flow.value.savedSessions.filter { it.id != chatId }
                    )
                }
                .onFailure { err ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        error = err.message ?: W.t("chat_delete_failed")
                    )
                }
        }
    }

    /** Clear all saved chat sessions (website parity: DELETE /ai/chats). */
    fun clearAllSessions() {
        val mode = _selectedMode.value
        viewModelScope.launch {
            chatRepository.clearAllChats()
                .onSuccess {
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(savedSessions = emptyList())
                }
                .onFailure { err ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        error = err.message ?: W.t("chat_clear_failed")
                    )
                }
        }
    }

    private fun buildSessionTitle(messages: List<ChatMessage>): String {
        val firstUser = messages.firstOrNull { it.isUser }?.message?.trim()
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        return if (!firstUser.isNullOrBlank()) {
            firstUser.take(40) + if (firstUser.length > 40) "..." else ""
        } else {
            "AI Chat $date"
        }
    }
}
