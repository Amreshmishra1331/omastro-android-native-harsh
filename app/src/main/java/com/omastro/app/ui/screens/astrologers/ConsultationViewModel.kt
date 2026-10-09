package com.omastro.app.ui.screens.astrologers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsultationViewModel : ViewModel() {
    private val getAstrologersUseCase = AppContainer.getAstrologersUseCase
    private val openChatSessionUseCase = AppContainer.openChatSessionUseCase
    private val closeChatSessionUseCase = AppContainer.closeChatSessionUseCase
    private val startBillingSessionUseCase = AppContainer.startBillingSessionUseCase
    private val tickBillingSessionUseCase = AppContainer.tickBillingSessionUseCase
    private val endBillingSessionUseCase = AppContainer.endBillingSessionUseCase
    private val consultationRepo = AppContainer.consultationRepository
    private val chatRepository = AppContainer.chatRepository

    private val _astrologers = MutableStateFlow<List<Astrologer>>(emptyList())
    val astrologers: StateFlow<List<Astrologer>> = _astrologers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = consultationRepo.messages

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0)
    val sessionDurationSeconds: StateFlow<Int> = _sessionDurationSeconds.asStateFlow()

    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val aiSessionIds = mutableMapOf<String, String?>()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var billingSessionUuid: String? = null
    private var chatSessionId: String? = null
    private var activeAstrologerId: String? = null

    init {
        loadAstrologers()
    }

    fun loadAstrologers(category: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            getAstrologersUseCase(category)
                .onSuccess {
                    _astrologers.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                }
        }
    }

    fun initChat(astrologerId: String) {
        activeAstrologerId = astrologerId
        viewModelScope.launch {
            _isLoading.value = true
            _sessionError.value = null
            openChatSessionUseCase(astrologerId)
                .onSuccess { response ->
                    chatSessionId = response.sessionId
                    consultationRepo.loadChatHistory(astrologerId)
                    startBillingSession(astrologerId)
                }
                .onFailure { err ->
                    _sessionError.value = err.message ?: "Could not open chat session."
                }
            _isLoading.value = false
        }
    }

    private fun startBillingSession(astrologerId: String) {
        viewModelScope.launch {
            startBillingSessionUseCase(astrologerId)
                .onSuccess { uuid ->
                    billingSessionUuid = uuid
                    _sessionActive.value = true
                    _sessionDurationSeconds.value = 0
                    startTickTimer()
                }
                .onFailure { err ->
                    _sessionError.value = err.message ?: "Could not start billing session."
                }
        }
    }

    private fun startTickTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_sessionActive.value) {
                kotlinx.coroutines.delay(60_000)
                if (!_sessionActive.value) break
                _sessionDurationSeconds.value += 60
                val uuid = billingSessionUuid
                if (uuid != null) {
                    tickBillingSessionUseCase(uuid).onFailure {
                        _sessionError.value = "Billing update failed. Session paused."
                        pauseSession()
                    }
                }
            }
        }
    }

    private fun pauseSession() {
        _sessionActive.value = false
        timerJob?.cancel()
    }

    fun endConsultation() {
        val astroId = activeAstrologerId
        val billingUuid = billingSessionUuid
        pauseSession()
        viewModelScope.launch {
            if (billingUuid != null) {
                endBillingSessionUseCase(billingUuid)
            }
            if (astroId != null) {
                closeChatSessionUseCase(astroId, chatSessionId)
            }
            billingSessionUuid = null
            chatSessionId = null
            activeAstrologerId = null
        }
    }

    fun clearSessionError() {
        _sessionError.value = null
    }

    /**
     * Sends a message in the astrologer consultation via POST /ai/chat
     * (persona "general"), matching the website's chat mechanism. The user
     * message and the AI reply are appended to the visible thread; the
     * server history is refreshed afterwards. Never fabricates replies.
     */
    fun sendMessage(astrologerId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isSending.value) return
        _isSending.value = true
        _sessionError.value = null

        val userMessage = ChatMessage(
            id = "local-${System.currentTimeMillis()}-u",
            senderId = "me",
            isUser = true,
            message = trimmed
        )
        consultationRepo.appendMessages(astrologerId, listOf(userMessage))

        viewModelScope.launch {
            chatRepository.sendAiMessage(trimmed, aiSessionIds[astrologerId], "general")
                .onSuccess { (reply, newSessionId) ->
                    aiSessionIds[astrologerId] = newSessionId
                    val aiMessage = ChatMessage(
                        id = "local-${System.currentTimeMillis()}-a",
                        senderId = "ai",
                        isUser = false,
                        message = reply
                    )
                    consultationRepo.appendMessages(astrologerId, listOf(aiMessage))
                    consultationRepo.loadChatHistory(astrologerId)
                }
                .onFailure { err ->
                    _sessionError.value = err.message ?: "Message could not be sent. Please try again."
                }
            _isSending.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
