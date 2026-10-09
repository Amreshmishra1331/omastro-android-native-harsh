package com.omastro.app.ui.screens.gochar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.remote.GocharRequest
import com.omastro.app.data.remote.GocharResponse
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gochar (planetary transits) analysis.
 *
 * Takes the user's natal birth details plus a transit date, calls POST /gochar,
 * and exposes the raw response. The screen renders only keys actually present
 * in the payload — never invented values.
 */
class GocharViewModel : ViewModel() {

    private val getGocharUseCase = AppContainer.getGocharUseCase

    private val _result = MutableStateFlow<GocharResponse?>(null)
    val result: StateFlow<GocharResponse?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Today's date in YYYY-MM-DD for the default transit date. */
    fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun calculate(
        name: String,
        dob: String,
        tob: String,
        pob: String,
        lat: Double,
        lon: Double,
        gender: String,
        transitDate: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _result.value = null
            getGocharUseCase(
                GocharRequest(
                    name = name,
                    dob = dob,
                    tob = tob,
                    pob = pob,
                    lat = lat,
                    lon = lon,
                    gender = gender,
                    transitDate = transitDate
                )
            ).onSuccess {
                _isLoading.value = false
                if (it.success && it.data != null) {
                    _result.value = it
                } else {
                    _errorMessage.value = it.message
                        ?: "Transit analysis returned no data for this date."
                }
            }.onFailure { err ->
                _isLoading.value = false
                _errorMessage.value = err.message
                    ?: "Could not load transit analysis. Please check your connection and try again."
            }
        }
    }

    fun clearResult() {
        _result.value = null
        _errorMessage.value = null
    }
}
