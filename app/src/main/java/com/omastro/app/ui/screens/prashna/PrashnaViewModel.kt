package com.omastro.app.ui.screens.prashna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.remote.PrashnaRequest
import com.omastro.app.data.remote.PrashnaResponse
import com.omastro.app.di.AppContainer
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Prashna Kundali (horary astrology) — Cycle 4.
 *
 * The seeker asks a question; POST /prashna-kundali casts the chart of the
 * question's moment and returns a verdict + supporting analysis.
 * The screen renders only keys actually present in the payload — never
 * invented values.
 */
class PrashnaViewModel : ViewModel() {

    private val getPrashnaKundaliUseCase = AppContainer.getPrashnaKundaliUseCase

    private val _result = MutableStateFlow<PrashnaResponse?>(null)
    val result: StateFlow<PrashnaResponse?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun ask(question: String, lat: Double?, lon: Double?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _result.value = null
            getPrashnaKundaliUseCase(
                PrashnaRequest(
                    question = question,
                    lat = lat,
                    lon = lon,
                    language = if (LangPrefs.language.value == "hi") "hi" else "en"
                )
            ).onSuccess {
                _isLoading.value = false
                if (it.success && it.data != null) {
                    _result.value = it
                } else {
                    _errorMessage.value = it.message ?: W.t("prashna_no_data")
                }
            }.onFailure { err ->
                _isLoading.value = false
                _errorMessage.value = err.message ?: W.t("prashna_err_load")
            }
        }
    }

    fun clearResult() {
        _result.value = null
        _errorMessage.value = null
    }
}
