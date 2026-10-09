package com.omastro.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.MeData
import com.omastro.app.data.model.SavedChatSession
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.SavedChart as SavedChartModel
import com.omastro.app.di.AppContainer
import com.omastro.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dashboard ViewModel — website parity (/dashboard/).
 *
 * Loads all dashboard data from real APIs:
 * - User profile (GET /me)
 * - Wallet balance + transactions
 * - Saved kundalis (GET /me/charts)
 * - AI chat sessions (GET /ai/chats)
 */
class DashboardViewModel : ViewModel() {

    val currentUser = AppContainer.authRepository.currentUser
    val walletRepository = AppContainer.walletRepository
    val chatRepository: ChatRepository = AppContainer.chatRepository
    val savedChartsRepository = AppContainer.savedChartsRepository

    private val _profile = MutableStateFlow<MeData?>(null)
    val profile: StateFlow<MeData?> = _profile.asStateFlow()

    private val _savedCharts = MutableStateFlow<List<SavedChartModel>>(emptyList())
    val savedCharts: StateFlow<List<SavedChartModel>> = _savedCharts.asStateFlow()

    private val _aiChats = MutableStateFlow<List<SavedChatSession>>(emptyList())
    val aiChats: StateFlow<List<SavedChatSession>> = _aiChats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Profile
                try {
                    val res = ApiClient.apiService.getMyProfile()
                    if (res.isSuccessful) {
                        _profile.value = res.body()?.data
                    }
                } catch (e: Exception) { /* non-fatal */ }

                // Wallet
                walletRepository.refreshBalance()
                walletRepository.getTransactions(50, 0)

                // Saved kundalis
                savedChartsRepository.getSavedCharts()
                    .onSuccess { _savedCharts.value = it }

                // AI chats
                chatRepository.listSavedChats()
                    .onSuccess { _aiChats.value = it }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAiChat(chatId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            chatRepository.deleteChatSession(chatId)
                .onSuccess {
                    _aiChats.value = _aiChats.value.filter { it.id != chatId }
                    onDone()
                }
        }
    }

    fun clearAllAiChats(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            chatRepository.clearAllChats()
                .onSuccess {
                    _aiChats.value = emptyList()
                    onDone()
                }
        }
    }

    fun deleteSavedChart(chartId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            savedChartsRepository.deleteChart(chartId)
                .onSuccess {
                    _savedCharts.value = _savedCharts.value.filter { it.id != chartId }
                    onDone()
                }
        }
    }
}
