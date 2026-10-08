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

    init {
        loadPanchang()
    }

    fun loadPanchang(date: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            getPanchangUseCase(date)
                .onSuccess {
                    _panchang.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                }
        }
    }
}
