package com.omastro.app.ui.screens.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.HoroscopeSign
import com.omastro.app.data.model.RashifalData
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HoroscopeViewModel : ViewModel() {
    private val getDailyHoroscopeUseCase = AppContainer.getDailyHoroscopeUseCase

    // Sign metadata mirrors omastroai.com/rashifal/ (glyph circle colors + rulers)
    val zodiacSigns = listOf(
        HoroscopeSign("aries", "Aries", "मेष", "♈", "Fire", "Mar 21 - Apr 19", "Mars", 0xFFE53935),
        HoroscopeSign("taurus", "Taurus", "वृषभ", "♉", "Earth", "Apr 20 - May 20", "Venus", 0xFFFF6B00),
        HoroscopeSign("gemini", "Gemini", "मिथुन", "♊", "Air", "May 21 - Jun 20", "Mercury", 0xFFF59E0B),
        HoroscopeSign("cancer", "Cancer", "कर्क", "♋", "Water", "Jun 21 - Jul 22", "Moon", 0xFFFFD54F),
        HoroscopeSign("leo", "Leo", "सिंह", "♌", "Fire", "Jul 23 - Aug 22", "Sun", 0xFFFFC107),
        HoroscopeSign("virgo", "Virgo", "कन्या", "♍", "Earth", "Aug 23 - Sep 22", "Mercury", 0xFF4CAF50),
        HoroscopeSign("libra", "Libra", "तुला", "♎", "Air", "Sep 23 - Oct 22", "Venus", 0xFF66BB6A),
        HoroscopeSign("scorpio", "Scorpio", "वृश्चिक", "♏", "Water", "Oct 23 - Nov 21", "Mars", 0xFF26A69A),
        HoroscopeSign("sagittarius", "Sagittarius", "धनु", "♐", "Fire", "Nov 22 - Dec 21", "Jupiter", 0xFF42A5F5),
        HoroscopeSign("capricorn", "Capricorn", "मकर", "♑", "Earth", "Dec 22 - Jan 19", "Saturn", 0xFF7E57C2),
        HoroscopeSign("aquarius", "Aquarius", "कुंभ", "♒", "Air", "Jan 20 - Feb 18", "Saturn", 0xFF9575CD),
        HoroscopeSign("pisces", "Pisces", "मीन", "♓", "Water", "Feb 19 - Mar 20", "Jupiter", 0xFFF48FB1)
    )

    /** API period values */
    val periods = listOf("daily", "weekly", "monthly", "yearly")
    val periodLabels = mapOf(
        "daily" to "Daily",
        "weekly" to "Weekly",
        "monthly" to "Monthly",
        "yearly" to "Yearly"
    )

    private val _selectedSign = MutableStateFlow<HoroscopeSign?>(null)
    val selectedSign: StateFlow<HoroscopeSign?> = _selectedSign.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("daily")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _horoscope = MutableStateFlow<RashifalData?>(null)
    val horoscope: StateFlow<RashifalData?> = _horoscope.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun selectSign(sign: HoroscopeSign) {
        _selectedSign.value = sign
        _horoscope.value = null
        _errorMessage.value = null
    }

    fun clearSign() {
        _selectedSign.value = null
        _horoscope.value = null
        _errorMessage.value = null
    }

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
        // Reload if a result is already showing
        if (_selectedSign.value != null && _horoscope.value != null) {
            fetchHoroscope()
        }
    }

    fun fetchHoroscope() {
        val sign = _selectedSign.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            getDailyHoroscopeUseCase(sign.id, _selectedPeriod.value)
                .onSuccess {
                    _horoscope.value = it
                    _isLoading.value = false
                }
                .onFailure { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.message
                        ?: "Could not load horoscope. Please check your connection and try again."
                }
        }
    }

    fun retry() = fetchHoroscope()
}
