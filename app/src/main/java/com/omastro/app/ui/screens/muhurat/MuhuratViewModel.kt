package com.omastro.app.ui.screens.muhurat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.MUHURAT_EVENTS
import com.omastro.app.data.model.MuhuratData
import com.omastro.app.data.model.MuhuratRequest
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MuhuratViewModel : ViewModel() {
    private val getMuhuratUseCase = AppContainer.getMuhuratUseCase

    // Form state
    private val _event = MutableStateFlow(MUHURAT_EVENTS.first().key)
    val event: StateFlow<String> = _event.asStateFlow()

    private val _startDate = MutableStateFlow(todayApiDate())
    val startDate: StateFlow<String> = _startDate.asStateFlow()

    private val _daysAhead = MutableStateFlow(30)
    val daysAhead: StateFlow<Int> = _daysAhead.asStateFlow()

    private val _place = MutableStateFlow("New Delhi")
    val place: StateFlow<String> = _place.asStateFlow()

    private val _lat = MutableStateFlow(28.6139)
    val lat: StateFlow<Double> = _lat.asStateFlow()

    private val _lon = MutableStateFlow(77.2090)
    val lon: StateFlow<Double> = _lon.asStateFlow()

    // Result state
    private val _muhurat = MutableStateFlow<MuhuratData?>(null)
    val muhurat: StateFlow<MuhuratData?> = _muhurat.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setEvent(key: String) { _event.value = key }
    fun setStartDate(date: String) { _startDate.value = date }
    fun setDaysAhead(days: Int) { _daysAhead.value = days }
    fun setPlace(place: String, lat: Double, lon: Double) {
        _place.value = place
        _lat.value = lat
        _lon.value = lon
    }

    fun clearError() { _errorMessage.value = null }

    fun findMuhurat() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _muhurat.value = null
            getMuhuratUseCase(
                MuhuratRequest(
                    event = _event.value,
                    date = _startDate.value,
                    daysAhead = _daysAhead.value,
                    lat = _lat.value,
                    lon = _lon.value,
                    tz = 5.5,
                    language = "en"
                )
            ).onSuccess {
                _muhurat.value = it
                _isLoading.value = false
            }.onFailure { err ->
                _isLoading.value = false
                _errorMessage.value = err.message
                    ?: "Could not find muhurat. Please check your connection and try again."
            }
        }
    }

    companion object {
        fun todayApiDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        fun displayDate(apiDate: String): String {
            return try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(apiDate)
                SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(parsed!!)
            } catch (e: Exception) {
                apiDate
            }
        }

        fun datePickerParts(apiDate: String): Triple<Int, Int, Int> {
            return try {
                val parts = apiDate.split("-")
                Triple(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (e: Exception) {
                val c = Calendar.getInstance()
                Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            }
        }
    }
}
