package com.omastro.app.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.AiChatQuota
import com.omastro.app.data.model.AiModel
import com.omastro.app.data.model.BirthDataAttachment
import com.omastro.app.data.model.ChatHistoryTurn
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.data.model.SaveChatRequest
import com.omastro.app.data.model.SavedChatSession
import com.omastro.app.data.model.buildBirthDataContext
import com.omastro.app.di.AppContainer
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
    val quota: AiChatQuota = AiChatQuota(),
    val aiModel: AiModel = AiModel.LITE,
    /** "en" | "hi" — sent as `language` with every message (website parity). */
    val language: String = "en",
    val savedSessionId: String? = null,
    val isLoadingSessions: Boolean = false,
    val savedSessions: List<SavedChatSession> = emptyList()
)

class AiChatViewModel : ViewModel() {
    private val chatRepository = AppContainer.chatRepository

    private val _selectedMode = MutableStateFlow(AiChatMode.GENERAL)
    val selectedMode: StateFlow<AiChatMode> = _selectedMode.asStateFlow()

    private val states = AiChatMode.values().associateWith { MutableStateFlow(AiChatUiState()) }
    private val sessionIds = mutableMapOf<AiChatMode, String?>()
    private val chatContexts = mutableMapOf<AiChatMode, String?>()

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
     * attachment blocks + the server-returned chatContext (replayed verbatim).
     */
    private fun buildContext(mode: AiChatMode): String {
        val state = states.getValue(mode).value
        val attachmentCtx = buildBirthDataContext(state.attachments)
        val stored = chatContexts[mode]
        return listOf(attachmentCtx, stored.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    /**
     * Builds the history payload: last 12 turns as {role, text}.
     * Website parity: roles are "user" / "model".
     */
    private fun buildHistory(mode: AiChatMode): List<ChatHistoryTurn> {
        return states.getValue(mode).value.messages
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
        val mode = _selectedMode.value
        val stateFlow = states.getValue(mode)
        val current = stateFlow.value
        if (current.isTyping) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "me",
            isUser = true,
            message = trimmed
        )
        stateFlow.value = current.copy(
            messages = current.messages + userMessage,
            isTyping = true,
            error = null
        )

        val context = buildContext(mode)
        val history = buildHistory(mode)
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
                    quota = response.quota
                )
            }.onFailure { err ->
                val latest = states.getValue(mode).value
                states.getValue(mode).value = latest.copy(
                    isTyping = false,
                    error = err.message ?: "Could not get a reply. Please try again."
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

    fun clearError() {
        val mode = _selectedMode.value
        val stateFlow = states.getValue(mode)
        stateFlow.value = stateFlow.value.copy(error = null)
    }

    /** Starts a fresh chat: clears messages, context, session, attachments. */
    fun clearChat() {
        val mode = _selectedMode.value
        states.getValue(mode).value = AiChatUiState()
        sessionIds.remove(mode)
        chatContexts.remove(mode)
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
                            error = err.message ?: "Could not load saved chats."
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
                    onSaved(id)
                }
                .onFailure { err ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        error = err.message ?: "Could not save chat."
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
                        savedSessionId = detail.id
                    )
                    sessionIds[mode] = detail.sessionId
                    detail.storedContext?.let { chatContexts[mode] = it }
                }
                .onFailure { err ->
                    val flow = states.getValue(mode)
                    flow.value = flow.value.copy(
                        error = err.message ?: "Could not open chat."
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
                        error = err.message ?: "Could not delete chat."
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
                        error = err.message ?: "Could not clear chats."
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
