package com.omastro.app.ui.screens.numerology

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Numerology analysis states.
 */
sealed interface NumerologyUiState {
    data object Idle : NumerologyUiState
    data object Loading : NumerologyUiState
    data class Success(val analysisText: String) : NumerologyUiState
    data class Error(val message: String) : NumerologyUiState
}

/**
 * Computes the numerology profile from the user's own name and date of birth
 * via POST /numerology. Nothing is hardcoded: the numbers shown come from the
 * backend response (data.analysis_text).
 *
 * NOTE: calls AppContainer.apiService.getNumerology directly because
 * AstroRepository has no numerology wrapper on this branch. If the data branch
 * adds AstroRepository.getNumerology(...), switch this call to the repository.
 */
class NumerologyViewModel : ViewModel() {

    private val api = AppContainer.apiService

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _dob = MutableStateFlow("") // YYYY-MM-DD
    val dob: StateFlow<String> = _dob.asStateFlow()

    private val _uiState = MutableStateFlow<NumerologyUiState>(NumerologyUiState.Idle)
    val uiState: StateFlow<NumerologyUiState> = _uiState.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    fun setName(v: String) { _name.value = v; _formError.value = null }
    fun setDob(v: String) { _dob.value = v; _formError.value = null }

    fun calculate() {
        val name = _name.value.trim()
        val dob = _dob.value.trim()
        if (name.length < 2) {
            _formError.value = "Please enter your full name."
            return
        }
        if (!DOB_REGEX.matches(dob)) {
            _formError.value = "Date of birth must be YYYY-MM-DD (e.g. 1995-08-15)."
            return
        }
        viewModelScope.launch {
            _uiState.value = NumerologyUiState.Loading
            try {
                val response = api.getNumerology(mapOf("name" to name, "dob" to dob))
                if (response.isSuccessful) {
                    val analysisText = extractAnalysisText(response.body())
                    if (!analysisText.isNullOrBlank()) {
                        _uiState.value = NumerologyUiState.Success(analysisText)
                    } else {
                        _uiState.value = NumerologyUiState.Error(
                            "The server returned an empty numerology profile. Please try again."
                        )
                    }
                } else {
                    _uiState.value = NumerologyUiState.Error(
                        "Could not compute the numerology profile (server error ${response.code()}). Please try again."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = NumerologyUiState.Error(
                    e.message ?: "Network error. Please check your connection and try again."
                )
            }
        }
    }

    fun reset() {
        _uiState.value = NumerologyUiState.Idle
        _formError.value = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractAnalysisText(body: Map<String, Any>?): String? {
        val data = body?.get("data") as? Map<String, Any> ?: return null
        return data["analysis_text"] as? String
    }

    companion object {
        private val DOB_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}
