package com.omastro.app.ui.screens.kundali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.JaiminiData
import com.omastro.app.data.model.KundaliRequest
import com.omastro.app.data.model.KundaliResponse
import com.omastro.app.data.model.ReportData
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The 23 website-parity Kundali tabs, in website order.
 */
val KUNDALI_TABS = listOf(
    "Birth Info", "Birth Chart", "Navamsha", "Karakamsha", "Chandra Lagna",
    "Surya Lagna", "Chalit", "Divisional", "Dasha", "Ashtaka", "Sodasvarga",
    "Yogas", "Doshas", "Personality", "Planets", "Panchang", "Shadbal",
    "Bhrigu Nadi", "Lal Kitab", "Numerology", "Char Dasha", "Faladesh", "Remedies"
)

/** Lazy-load state for a server-generated report (Personality / BNN / Lal Kitab). */
data class ReportUiState(
    val loading: Boolean = false,
    val data: ReportData? = null,
    val error: String? = null
)

/** Lazy-load state for the Jaimini (Char Dasha / Karakamsha) payload. */
data class JaiminiUiState(
    val loading: Boolean = false,
    val data: JaiminiData? = null,
    val error: String? = null
)

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
    private val _birthGender = MutableStateFlow("male") // male | female | other
    val birthGender: StateFlow<String> = _birthGender.asStateFlow()

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
    fun setBirthGender(v: String) { _birthGender.value = v; _formError.value = null }

    // Tab state: index into KUNDALI_TABS (23 website-parity tabs)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Divisional chart backed by real API maps: D1..D60 from allDivisionalCharts
    private val _selectedDivisional = MutableStateFlow("D1")
    val selectedDivisional: StateFlow<String> = _selectedDivisional.asStateFlow()

    // Chart style: "North Indian" or "South Indian" (both genuinely rendered)
    private val _chartStyle = MutableStateFlow("North Indian")
    val chartStyle: StateFlow<String> = _chartStyle.asStateFlow()

    // --- Lazy-loaded dedicated reports (fetched once per chart, on tab select) ---
    private val _personalityState = MutableStateFlow(ReportUiState())
    val personalityState: StateFlow<ReportUiState> = _personalityState.asStateFlow()
    private val _bnnState = MutableStateFlow(ReportUiState())
    val bnnState: StateFlow<ReportUiState> = _bnnState.asStateFlow()
    private val _lalKitabState = MutableStateFlow(ReportUiState())
    val lalKitabState: StateFlow<ReportUiState> = _lalKitabState.asStateFlow()
    private val _jaiminiState = MutableStateFlow(JaiminiUiState())
    val jaiminiState: StateFlow<JaiminiUiState> = _jaiminiState.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        ensureLazyReports(tabIndex)
    }
    fun selectDivisional(divisional: String) { _selectedDivisional.value = divisional }
    fun selectChartStyle(style: String) { _chartStyle.value = style }

    /**
     * Tabs backed by dedicated endpoints load on first select and cache per
     * chart: 13=Personality, 17=Bhrigu Nadi, 18=Lal Kitab, 3/20=Jaimini.
     */
    private fun ensureLazyReports(tabIndex: Int) {
        val req = _lastRequest.value ?: return
        val repo = AppContainer.astroRepository
        when (tabIndex) {
            13 -> if (!_personalityState.value.loading && _personalityState.value.data == null && _personalityState.value.error == null) {
                _personalityState.value = ReportUiState(loading = true)
                viewModelScope.launch {
                    repo.getPersonality(req)
                        .onSuccess { _personalityState.value = ReportUiState(data = it) }
                        .onFailure { _personalityState.value = ReportUiState(error = it.message ?: "Could not load Personality report.") }
                }
            }
            17 -> if (!_bnnState.value.loading && _bnnState.value.data == null && _bnnState.value.error == null) {
                _bnnState.value = ReportUiState(loading = true)
                viewModelScope.launch {
                    repo.getBnnFull(req)
                        .onSuccess { _bnnState.value = ReportUiState(data = it) }
                        .onFailure { _bnnState.value = ReportUiState(error = it.message ?: "Could not load Bhrigu Nadi report.") }
                }
            }
            18 -> if (!_lalKitabState.value.loading && _lalKitabState.value.data == null && _lalKitabState.value.error == null) {
                _lalKitabState.value = ReportUiState(loading = true)
                viewModelScope.launch {
                    repo.getLalKitabFull(req)
                        .onSuccess { _lalKitabState.value = ReportUiState(data = it) }
                        .onFailure { _lalKitabState.value = ReportUiState(error = it.message ?: "Could not load Lal Kitab report.") }
                }
            }
            3, 20 -> if (!_jaiminiState.value.loading && _jaiminiState.value.data == null && _jaiminiState.value.error == null) {
                _jaiminiState.value = JaiminiUiState(loading = true)
                viewModelScope.launch {
                    repo.getJaimini(req)
                        .onSuccess { _jaiminiState.value = JaiminiUiState(data = it) }
                        .onFailure { _jaiminiState.value = JaiminiUiState(error = it.message ?: "Could not load Jaimini data.") }
                }
            }
        }
    }

    /** Retry a failed lazy report (clears the error so ensureLazyReports refires). */
    fun retryReport(tabIndex: Int) {
        when (tabIndex) {
            13 -> _personalityState.value = ReportUiState()
            17 -> _bnnState.value = ReportUiState()
            18 -> _lalKitabState.value = ReportUiState()
            3, 20 -> _jaiminiState.value = JaiminiUiState()
        }
        ensureLazyReports(tabIndex)
    }

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
        _birthGender.value = req.gender.ifBlank { "male" }
        _formError.value = null
        fetchKundali(
            name = req.name,
            dob = req.dob,
            tob = req.tob,
            pob = req.pob,
            lat = req.lat,
            lon = req.lon,
            gender = req.gender.ifBlank { "male" }
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
            _formError.value = "Time of birth must be HH:MM AM/PM (e.g. 08:30 AM)."
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
        fetchKundali(name, dob, tob, pob, lat, lon, _birthGender.value)
    }

    fun fetchKundali(
        name: String,
        dob: String,
        tob: String,
        pob: String,
        lat: Double,
        lon: Double,
        gender: String = "male"
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
                gender = gender,
                ayanamsa = "lahiri"
            )
            getKundaliUseCase(req)
                .onSuccess {
                    _kundaliData.value = it
                    _lastRequest.value = req
                    _hasData.value = true
                    _selectedTab.value = 0
                    _selectedDivisional.value = "D1"
                    // Fresh chart -> drop cached dedicated reports
                    _personalityState.value = ReportUiState()
                    _bnnState.value = ReportUiState()
                    _lalKitabState.value = ReportUiState()
                    _jaiminiState.value = JaiminiUiState()
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
        // Accepts "HH:MM" 24h or "HH:MM AM/PM" 12h (e.g. "08:30", "08:30 AM")
        private val TOB_REGEX = Regex("""((([01]\d|2[0-3]):[0-5]\d)|(0\d|1[0-2]):[0-5]\d\s?(AM|PM))""", RegexOption.IGNORE_CASE)
    }
}
