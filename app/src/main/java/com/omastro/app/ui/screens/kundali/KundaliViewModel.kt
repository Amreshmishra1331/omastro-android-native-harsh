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

/**
 * Kundali workbench state.
 *
 * No birth details are hardcoded: the user enters their own details in the
 * birth form, which are passed to fetchKundali(). Chart data shown is exactly
 * what POST /kundali returns — nothing is invented client-side.
 */
class KundaliViewModel : ViewModel() {
    private val getKundaliUseCase = AppContainer.getKundaliUseCase

    private val _kundaliData = MutableStateFlow<KundaliResponse?>(null)
    val kundaliData: StateFlow<KundaliResponse?> = _kundaliData.asStateFlow()

    /** True once a chart has been computed; drives form-vs-results UI. */
    private val _hasData = MutableStateFlow(false)
    val hasData: StateFlow<Boolean> = _hasData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Birth form state ---
    private val _birthName = MutableStateFlow("")
    val birthName: StateFlow<String> = _birthName.asStateFlow()
    private val _birthDob = MutableStateFlow("") // YYYY-MM-DD
    val birthDob: StateFlow<String> = _birthDob.asStateFlow()
    private val _birthTob = MutableStateFlow("") // HH:MM (24h)
    val birthTob: StateFlow<String> = _birthTob.asStateFlow()
    private val _birthPlace = MutableStateFlow("")
    val birthPlace: StateFlow<String> = _birthPlace.asStateFlow()
    private val _birthLat = MutableStateFlow("")
    val birthLat: StateFlow<String> = _birthLat.asStateFlow()
    private val _birthLon = MutableStateFlow("")
    val birthLon: StateFlow<String> = _birthLon.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    /** The birth details of the currently displayed chart (for save/load). */
    private val _lastRequest = MutableStateFlow<KundaliRequest?>(null)
    val lastRequest: StateFlow<KundaliRequest?> = _lastRequest.asStateFlow()

    fun setBirthName(v: String) { _birthName.value = v; _formError.value = null }
    fun setBirthDob(v: String) { _birthDob.value = v; _formError.value = null }
    fun setBirthTob(v: String) { _birthTob.value = v; _formError.value = null }
    fun setBirthPlace(v: String) { _birthPlace.value = v; _formError.value = null }
    fun setBirthLat(v: String) { _birthLat.value = v.filter { it.isDigit() || it == '.' || it == '-' }; _formError.value = null }
    fun setBirthLon(v: String) { _birthLon.value = v.filter { it.isDigit() || it == '.' || it == '-' }; _formError.value = null }

    // Tab state: 0=Charts, 1=Grahas, 2=Dasha, 3=Yogas, 4=Ashtakavarga, 5=LalKitab, 6=JaiminiHealth
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Divisional chart backed by real API maps: D1, D9, D10
    private val _selectedDivisional = MutableStateFlow("D1")
    val selectedDivisional: StateFlow<String> = _selectedDivisional.asStateFlow()

    // Chart style: "North Indian" or "South Indian" (both genuinely rendered)
    private val _chartStyle = MutableStateFlow("North Indian")
    val chartStyle: StateFlow<String> = _chartStyle.asStateFlow()

    fun selectTab(tabIndex: Int) { _selectedTab.value = tabIndex }
    fun selectDivisional(divisional: String) { _selectedDivisional.value = divisional }
    fun selectChartStyle(style: String) { _chartStyle.value = style }

    /** Return to the birth form to enter different details. */
    fun editBirthDetails() {
        _hasData.value = false
        _errorMessage.value = null
    }

    /** Load a saved chart: populate the form and fetch. */
    fun loadSavedChart(chart: com.omastro.app.data.remote.SavedChart) {
        val req = chart.toKundaliRequest()
        _birthName.value = req.name
        _birthDob.value = req.dob
        _birthTob.value = req.tob
        _birthPlace.value = req.pob
        _birthLat.value = req.lat.toString()
        _birthLon.value = req.lon.toString()
        _formError.value = null
        fetchKundali(
            name = req.name,
            dob = req.dob,
            tob = req.tob,
            pob = req.pob,
            lat = req.lat,
            lon = req.lon
        )
    }

    /** Validate the form and compute the chart for the entered details. */
    fun submitBirthDetails() {
        val name = _birthName.value.trim()
        val dob = _birthDob.value.trim()
        val tob = _birthTob.value.trim()
        val pob = _birthPlace.value.trim()
        val lat = _birthLat.value.toDoubleOrNull()
        val lon = _birthLon.value.toDoubleOrNull()

        if (name.length < 2) {
            _formError.value = "Please enter the full name."
            return
        }
        if (!DOB_REGEX.matches(dob)) {
            _formError.value = "Date of birth must be YYYY-MM-DD (e.g. 1995-08-15)."
            return
        }
        if (!TOB_REGEX.matches(tob)) {
            _formError.value = "Time of birth must be HH:MM in 24-hour format (e.g. 08:30)."
            return
        }
        if (pob.isBlank()) {
            _formError.value = "Please enter the place of birth."
            return
        }
        if (lat == null || lat < -90 || lat > 90) {
            _formError.value = "Latitude must be a number between -90 and 90."
            return
        }
        if (lon == null || lon < -180 || lon > 180) {
            _formError.value = "Longitude must be a number between -180 and 180."
            return
        }
        fetchKundali(name, dob, tob, pob, lat, lon)
    }

    fun fetchKundali(
        name: String,
        dob: String,
        tob: String,
        pob: String,
        lat: Double,
        lon: Double
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
                    _lastRequest.value = req
                    _hasData.value = true
                    _selectedTab.value = 0
                    _isLoading.value = false
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Could not compute the Kundali. Please check your details and try again."
                    _isLoading.value = false
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private val DOB_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
        private val TOB_REGEX = Regex("""([01]\d|2[0-3]):[0-5]\d""")
    }
}
