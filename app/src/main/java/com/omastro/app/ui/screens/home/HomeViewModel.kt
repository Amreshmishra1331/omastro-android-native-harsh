package com.omastro.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.PanchangResponse
import com.omastro.app.data.model.UserProfile
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val getPanchangUseCase = AppContainer.getPanchangUseCase
    private val getAstrologersUseCase = AppContainer.getAstrologersUseCase

    val currentUser: StateFlow<UserProfile?> = AppContainer.authRepository.currentUser
    val coinBalance: StateFlow<Int> = AppContainer.walletRepository.coinBalance
    val walletBalanceInr: StateFlow<Double> = AppContainer.walletRepository.walletBalanceInr

    private val _panchang = MutableStateFlow<PanchangResponse?>(null)
    val panchang: StateFlow<PanchangResponse?> = _panchang.asStateFlow()

    private val _astrologers = MutableStateFlow<List<Astrologer>>(emptyList())
    val astrologers: StateFlow<List<Astrologer>> = _astrologers.asStateFlow()

    private val _dailyMantra = MutableStateFlow(
        "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि धियो यो नः प्रचोदयात् ॥"
    )
    val dailyMantra: StateFlow<String> = _dailyMantra.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            AppContainer.walletRepository.refreshBalance()

            val panchangRes = getPanchangUseCase()
            panchangRes.onSuccess { _panchang.value = it }

            val astroRes = getAstrologersUseCase()
            astroRes.onSuccess { _astrologers.value = it }
        }
    }
}
