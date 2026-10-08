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
    private val sendChatMessageUseCase = AppContainer.sendChatMessageUseCase
    private val consultationRepo = AppContainer.consultationRepository

    private val _astrologers = MutableStateFlow<List<Astrologer>>(emptyList())
    val astrologers: StateFlow<List<Astrologer>> = _astrologers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = consultationRepo.messages

    private val authRepo = AppContainer.authRepository

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0)
    val sessionDurationSeconds: StateFlow<Int> = _sessionDurationSeconds.asStateFlow()

    private val _remainingCoins = MutableStateFlow(authRepo.currentUser.value?.coinBalance ?: 500)
    val remainingCoins: StateFlow<Int> = _remainingCoins.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

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
        viewModelScope.launch {
            consultationRepo.loadChatHistory(astrologerId)
            startConsultationTimer(astrologerId)
        }
    }

    private fun startConsultationTimer(astrologerId: String) {
        if (_sessionActive.value) return
        _sessionActive.value = true
        _sessionDurationSeconds.value = 0

        val astro = _astrologers.value.find { it.id == astrologerId }
        val ratePerMin = astro?.pricePerMin ?: 20

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_sessionActive.value) {
                kotlinx.coroutines.delay(1000)
                _sessionDurationSeconds.value += 1

                // Every 60 seconds, auto-deduct coins for the consultation minute
                if (_sessionDurationSeconds.value % 60 == 0) {
                    val currentBal = authRepo.currentUser.value?.coinBalance ?: 500
                    val updatedBal = (currentBal - ratePerMin).coerceAtLeast(0)
                    authRepo.updateCoinBalance(updatedBal)
                    _remainingCoins.value = updatedBal
                }
            }
        }
    }

    fun endConsultation() {
        _sessionActive.value = false
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun sendMessage(astrologerId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            sendChatMessageUseCase(astrologerId, text)
        }
    }
}
