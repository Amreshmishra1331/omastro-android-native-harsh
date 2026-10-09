package com.omastro.app.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AiChatMode(val persona: String, val title: String, val subtitle: String) {
    GENERAL("general", "General", "Vedic guidance on any question"),
    DIVINE("divine", "Divine", "Connect with the divine"),
    MUNDANE("mundane", "Mundane", "World events and collective trends")
}

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val error: String? = null
)

class AiChatViewModel : ViewModel() {
    private val chatRepository = AppContainer.chatRepository

    private val _selectedMode = MutableStateFlow(AiChatMode.GENERAL)
    val selectedMode: StateFlow<AiChatMode> = _selectedMode.asStateFlow()

    private val states = AiChatMode.values().associateWith { MutableStateFlow(AiChatUiState()) }
    private val sessionIds = mutableMapOf<AiChatMode, String?>()

    val uiState: StateFlow<AiChatUiState>
        get() = states.getValue(_selectedMode.value)

    fun selectMode(mode: AiChatMode) {
        if (_selectedMode.value != mode) {
            _selectedMode.value = mode
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

        viewModelScope.launch {
            chatRepository.sendAiMessage(trimmed, sessionIds[mode], mode.persona)
                .onSuccess { (reply, newSessionId) ->
                    sessionIds[mode] = newSessionId
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderId = "ai",
                        isUser = false,
                        message = reply
                    )
                    val latest = states.getValue(mode).value
                    states.getValue(mode).value = latest.copy(
                        messages = latest.messages + aiMessage,
                        isTyping = false
                    )
                }
                .onFailure { err ->
                    val latest = states.getValue(mode).value
                    states.getValue(mode).value = latest.copy(
                        isTyping = false,
                        error = err.message ?: "Could not get a reply. Please try again."
                    )
                }
        }
    }

    fun clearError() {
        val mode = _selectedMode.value
        val stateFlow = states.getValue(mode)
        stateFlow.value = stateFlow.value.copy(error = null)
    }

    fun clearChat() {
        val mode = _selectedMode.value
        states.getValue(mode).value = AiChatUiState()
        sessionIds.remove(mode)
    }
}
