package com.omastro.app.ui.screens.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.DailyHoroscopeResponse
import com.omastro.app.data.model.HoroscopeSign
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HoroscopeViewModel : ViewModel() {
    private val getDailyHoroscopeUseCase = AppContainer.getDailyHoroscopeUseCase

    val zodiacSigns = listOf(
        HoroscopeSign("aries", "Aries", "मेष", "♈", "Fire", "Mar 21 - Apr 19"),
        HoroscopeSign("taurus", "Taurus", "वृषभ", "♉", "Earth", "Apr 20 - May 20"),
        HoroscopeSign("gemini", "Gemini", "मिथुन", "♊", "Air", "May 21 - Jun 20"),
        HoroscopeSign("cancer", "Cancer", "कर्क", "♋", "Water", "Jun 21 - Jul 22"),
        HoroscopeSign("leo", "Leo", "सिंह", "♌", "Fire", "Jul 23 - Aug 22"),
        HoroscopeSign("virgo", "Virgo", "कन्या", "♍", "Earth", "Aug 23 - Sep 22"),
        HoroscopeSign("libra", "Libra", "तुला", "♎", "Air", "Sep 23 - Oct 22"),
        HoroscopeSign("scorpio", "Scorpio", "वृश्चिक", "♏", "Water", "Oct 23 - Nov 21"),
        HoroscopeSign("sagittarius", "Sagittarius", "धनु", "♐", "Fire", "Nov 22 - Dec 21"),
        HoroscopeSign("capricorn", "Capricorn", "मकर", "♑", "Earth", "Dec 22 - Jan 19"),
        HoroscopeSign("aquarius", "Aquarius", "कुंभ", "♒", "Air", "Jan 20 - Feb 18"),
        HoroscopeSign("pisces", "Pisces", "मीन", "♓", "Water", "Feb 19 - Mar 20")
    )

    private val _selectedSign = MutableStateFlow(zodiacSigns[0])
    val selectedSign: StateFlow<HoroscopeSign> = _selectedSign.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("today")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _horoscope = MutableStateFlow<DailyHoroscopeResponse?>(null)
    val horoscope: StateFlow<DailyHoroscopeResponse?> = _horoscope.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchHoroscope(zodiacSigns[0].id, "today")
    }

    fun selectSign(sign: HoroscopeSign) {
        _selectedSign.value = sign
        fetchHoroscope(sign.id, _selectedPeriod.value)
    }

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
        fetchHoroscope(_selectedSign.value.id, period)
    }

    fun fetchHoroscope(sign: String, period: String) {
        viewModelScope.launch {
            _isLoading.value = true
            getDailyHoroscopeUseCase(sign, period)
                .onSuccess {
                    _horoscope.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                }
        }
    }
}
