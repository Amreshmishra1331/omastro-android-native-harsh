package com.omastro.app.ui.screens.kundali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.KundaliRequest
import com.omastro.app.data.model.KundaliResponse
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KundaliViewModel : ViewModel() {
    private val getKundaliUseCase = AppContainer.getKundaliUseCase

    private val _kundaliData = MutableStateFlow<KundaliResponse?>(null)
    val kundaliData: StateFlow<KundaliResponse?> = _kundaliData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Tab state: 0=Charts, 1=Grahas, 2=Dasha, 3=Yogas, 4=Ashtakavarga, 5=LalKitab, 6=JaiminiHealth
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Divisional Chart: D1, D9, D10, D7, D60
    private val _selectedDivisional = MutableStateFlow("D1")
    val selectedDivisional: StateFlow<String> = _selectedDivisional.asStateFlow()

    // Chart Style: "North Indian", "South Indian"
    private val _chartStyle = MutableStateFlow("North Indian")
    val chartStyle: StateFlow<String> = _chartStyle.asStateFlow()

    init {
        // Load default Kundali
        fetchKundali(
            name = "Arjun Sharma",
            dob = "1995-10-15",
            tob = "08:30",
            pob = "New Delhi",
            lat = 28.6139,
            lon = 77.2090
        )
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun selectDivisional(divisional: String) {
        _selectedDivisional.value = divisional
    }

    fun selectChartStyle(style: String) {
        _chartStyle.value = style
    }

    fun fetchKundali(
        name: String,
        dob: String,
        tob: String,
        pob: String,
        lat: Double = 28.6139,
        lon: Double = 77.2090
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val parts = tob.split(":")
            val hr = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val mn = parts.getOrNull(1)?.toIntOrNull() ?: 30
            val req = KundaliRequest(
                name = name,
                dob = dob,
                tobHr = hr,
                tobMn = mn,
                tob = tob,
                pob = pob,
                lat = lat,
                lon = lon,
                ayanamsa = "lahiri"
            )
            getKundaliUseCase(req)
                .onSuccess {
                    _kundaliData.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    _errorMessage.value = it.localizedMessage ?: "Failed to compute Kundali chart"
                    _isLoading.value = false
                }
        }
    }
}
