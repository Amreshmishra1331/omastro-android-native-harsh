package com.omastro.app.ui.screens.panchang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.PanchangResponse
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PanchangViewModel : ViewModel() {
    private val getPanchangUseCase = AppContainer.getPanchangUseCase

    private val _panchang = MutableStateFlow<PanchangResponse?>(null)
    val panchang: StateFlow<PanchangResponse?> = _panchang.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPanchang()
    }

    fun loadPanchang(date: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            getPanchangUseCase(date)
                .onSuccess {
                    _panchang.value = it
                    _isLoading.value = false
                }
                .onFailure { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.message
                        ?: "Could not load Panchang. Please check your connection and try again."
                }
        }
    }
}
