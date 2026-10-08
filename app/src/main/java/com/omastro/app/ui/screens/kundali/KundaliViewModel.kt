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

    init {
        // Load default Kundali
        fetchKundali(
            name = "Astro Seeker",
            dob = "1995-10-15",
            tob = "08:30",
            pob = "New Delhi",
            lat = 28.6139,
            lon = 77.2090
        )
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
            val req = KundaliRequest(name = name, dob = dob, tob = tob, pob = pob, lat = lat, lon = lon)
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
