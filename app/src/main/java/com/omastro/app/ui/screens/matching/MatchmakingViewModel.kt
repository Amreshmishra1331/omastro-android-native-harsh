package com.omastro.app.ui.screens.matching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.MatchmakingRequest
import com.omastro.app.data.model.MatchmakingResponse
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchmakingViewModel : ViewModel() {
    private val calculateMatchmakingUseCase = AppContainer.calculateMatchmakingUseCase

    private val _result = MutableStateFlow<MatchmakingResponse?>(null)
    val result: StateFlow<MatchmakingResponse?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearResult() {
        _result.value = null
        _errorMessage.value = null
    }

    fun calculate(
        boyName: String,
        boyDob: String,
        boyTob: String,
        boyPob: String,
        girlName: String,
        girlDob: String,
        girlTob: String,
        girlPob: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val req = MatchmakingRequest(boyName, boyDob, boyTob, boyPob, girlName, girlDob, girlTob, girlPob)
            calculateMatchmakingUseCase(req)
                .onSuccess {
                    _result.value = it
                    _isLoading.value = false
                }
                .onFailure { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.message
                        ?: "Could not calculate compatibility. Please check your connection and try again."
                }
        }
    }
}
