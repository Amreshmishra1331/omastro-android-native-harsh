package com.omastro.app.ui.screens.panchang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.HoraData
import com.omastro.app.data.model.PanchangLookups
import com.omastro.app.data.model.PanchangResponse
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PanchangViewModel : ViewModel() {
    private val getPanchangUseCase = AppContainer.getPanchangUseCase
    private val getHoraUseCase = AppContainer.getHoraUseCase

    private val _panchang = MutableStateFlow<PanchangResponse?>(null)
    val panchang: StateFlow<PanchangResponse?> = _panchang.asStateFlow()

    private val _hora = MutableStateFlow<HoraData?>(null)
    val hora: StateFlow<HoraData?> = _hora.asStateFlow()

    private val _selectedDate = MutableStateFlow(PanchangLookups.todayApiDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPanchang()
    }

    fun loadPanchang(date: String? = null) {
        val dateApi = date ?: _selectedDate.value
        _selectedDate.value = dateApi
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            // Panchang (required)
            val panchangResult = getPanchangUseCase(dateApi)
            // Hora (best-effort; never blocks the panchang)
            val horaResult = getHoraUseCase(dateApi)
            _isLoading.value = false
            panchangResult
                .onSuccess { _panchang.value = it }
                .onFailure { err ->
                    _panchang.value = null
                    _errorMessage.value = err.message
                        ?: "Could not load Panchang. Please check your connection and try again."
                }
            horaResult
                .onSuccess { _hora.value = it }
                .onFailure { _hora.value = null }
        }
    }

    fun retry() = loadPanchang()
}
