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
        }
    }

    fun sendMessage(astrologerId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            sendChatMessageUseCase(astrologerId, text)
        }
    }
}
