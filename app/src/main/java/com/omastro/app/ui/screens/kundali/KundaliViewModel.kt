package com.omastro.app.ui.screens.kundali

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.JaiminiData
import com.omastro.app.data.model.KundaliRequest
import com.omastro.app.data.model.KundaliResponse
import com.omastro.app.data.model.ReportData
import com.omastro.app.data.remote.PdfExportRequest
import com.omastro.app.data.repository.MyDataRepository
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.share.renderHtmlToPdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The 23 website-parity Kundali tabs, in website order.
 * Titles are bilingual via [KUNDALI_TAB_KEYS] (`kundali_tab_0..22` in FeatureStrings).
 */

/** Bilingual key per tab (FeatureStrings `kundali_tab_0..22`); titles resolve via W.t. */
val KUNDALI_TAB_KEYS = List(23) { i -> "kundali_tab_$i" }

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
 * Export-to-PDF state for the currently displayed chart.
 *
 * Flow: POST /wp-json/omastro/v1/pdf/export (server computes the official
 * report HTML) → [renderHtmlToPdfFile] converts that HTML to real PDF bytes
 * on-device → Ready(file) → the screen opens the system share sheet.
 */
sealed interface KundaliPdfState {
    data object Idle : KundaliPdfState
    data object Loading : KundaliPdfState
    data class Ready(val file: File, val filename: String) : KundaliPdfState
    data class Error(val message: String) : KundaliPdfState
}

/**
 * Kundali workbench state.
 *
 * No birth details are hardcoded: the user enters their own details in the
 * birth form, which are passed to fetchKundali(). Chart data shown is exactly
 * what POST /kundali returns — nothing is invented client-side.
 */
class KundaliViewModel(application: Application) : AndroidViewModel(application) {
    private val getKundaliUseCase = AppContainer.getKundaliUseCase
    private val myDataRepo = MyDataRepository(application)

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

    // Tab state: index into KUNDALI_TAB_KEYS (23 website-parity tabs)
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
        // Honesty: malformed stored birth data must never open someone else's
        // chart (previously a silent "1990-01-01" fallback).
        val req = chart.toKundaliRequest()
        if (req == null) {
            _formError.value = W.t("honesty_saved_bad_data")
            return
        }
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
            // Parse "HH:MM" (24h) or "HH:MM AM/PM" (12h). AM/PM must convert to
            // 24h: "08:30 PM" -> hr=20 (previously AM/PM was silently dropped).
            // Shared helper keeps every screen's conversion identical.
            val (hr, mn) = com.omastro.app.ui.components.parseTimeTo24(tob) ?: (8 to 30)
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
                    // App storage: auto-save the full chart on-device (My Data).
                    // Fire-and-forget — never blocks the UI, never fails loudly.
                    viewModelScope.launch {
                        try {
                            myDataRepo.saveKundali(req, it)
                        } catch (_: Exception) {
                        }
                    }
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

    // --- Kundali PDF export -------------------------------------------

    private val _pdfState = MutableStateFlow<KundaliPdfState>(KundaliPdfState.Idle)
    val pdfState: StateFlow<KundaliPdfState> = _pdfState.asStateFlow()

    fun resetPdfState() {
        _pdfState.value = KundaliPdfState.Idle
    }

    /**
     * Exports the currently displayed chart to a real PDF.
     *
     * The server (POST /pdf/export) recomputes the official Kundali report
     * HTML from the chart's birth data; the device renders that HTML into PDF
     * bytes via [renderHtmlToPdfFile]. The report language follows the app's
     * global Settings language (hi → Hindi report).
     *
     * tz note: the app's /kundali request never sends a tz, so the server
     * computes the chart with its 5.5 default; the export uses the same 5.5 so
     * the PDF matches the chart on screen.
     */
    fun exportPdf(context: Context) {
        val req = _lastRequest.value
        if (req == null) {
            _pdfState.value = KundaliPdfState.Error(W.t("kundali_pdf_no_chart"))
            return
        }
        if (req.lat.isNaN() || req.lon.isNaN()) {
            _pdfState.value = KundaliPdfState.Error(W.t("kundali_pdf_failed"))
            return
        }
        if (_pdfState.value == KundaliPdfState.Loading) return
        _pdfState.value = KundaliPdfState.Loading
        viewModelScope.launch {
            val lang = if (LangPrefs.language.value == "hi") "hi" else "en"
            val result = withContext(Dispatchers.IO) {
                AppContainer.astroRepository.exportKundaliPdf(
                    PdfExportRequest(
                        name = req.name,
                        gender = req.gender.ifBlank { "male" },
                        dob = req.dob,
                        tobHr = req.tobHr,
                        tobMn = req.tobMn,
                        lat = req.lat,
                        lon = req.lon,
                        tz = 5.5,
                        place = req.pob,
                        language = lang,
                        ayanamsa = req.ayanamsa
                    )
                )
            }
            result.fold(
                onSuccess = { payload ->
                    try {
                        val file = renderHtmlToPdfFile(context, payload.html, payload.filename)
                        _pdfState.value = KundaliPdfState.Ready(file, payload.filename)
                    } catch (e: Exception) {
                        _pdfState.value = KundaliPdfState.Error(e.message ?: W.t("kundali_pdf_failed"))
                    }
                },
                onFailure = { e ->
                    _pdfState.value = KundaliPdfState.Error(e.message ?: W.t("kundali_pdf_failed"))
                }
            )
        }
    }

    /**
     * Re-runs the Kundali fetch with the preserved form values.
     * Used by the "Try Again" button on the error card — actually retries
     * the API call instead of just dismissing the error.
     */
    fun retryLastFetch() {
        _errorMessage.value = null
        submitBirthDetails()
    }

    /**
     * Instant re-open of a locally saved Kundali (from My Data): restores
     * the full chart from stored JSON — no re-computation, no API call.
     * The dedicated reports (Personality/BNN/Lal Kitab/Jaimini) were never
     * stored; they lazy-load on tab select exactly like a fresh chart.
     */
    fun loadStoredKundali(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val saved = myDataRepo.getKundali(id)
                    ?: throw IllegalStateException("Saved chart not found.")
                val req = saved.toRequest()
                val response = saved.toResponse()
                _kundaliData.value = response
                _lastRequest.value = req
                _hasData.value = true
                _selectedTab.value = 0
                _selectedDivisional.value = "D1"
                _chartStyle.value = "North Indian"
                _personalityState.value = ReportUiState()
                _bnnState.value = ReportUiState()
                _lalKitabState.value = ReportUiState()
                _jaiminiState.value = JaiminiUiState()
                _isLoading.value = false
            } catch (_: Exception) {
                _errorMessage.value = W.t("mydata_open_failed")
                _isLoading.value = false
            }
        }
    }

    // ------------------------------------------------------------------
    // Birth profiles (on-device, reusable birth details)
    // ------------------------------------------------------------------

    private val _birthProfiles =
        MutableStateFlow<List<MyDataRepository.BirthProfile>>(emptyList())
    val birthProfiles: StateFlow<List<MyDataRepository.BirthProfile>> =
        _birthProfiles.asStateFlow()

    fun refreshBirthProfiles() {
        viewModelScope.launch {
            try {
                _birthProfiles.value = myDataRepo.listBirthProfiles()
            } catch (_: Exception) {
            }
        }
    }

    /** Picks a saved profile into the birth form (no retyping needed). */
    fun applyBirthProfile(profile: MyDataRepository.BirthProfile) {
        _birthName.value = profile.name
        _birthDob.value = profile.dob
        _birthTob.value = profile.tob
        _birthPlace.value = profile.pob
        _birthLat.value = profile.lat.toString()
        _birthLon.value = profile.lon.toString()
        _birthGender.value = profile.gender
        _formError.value = null
    }

    /**
     * Saves the current form values as a reusable birth profile. [profileName]
     * is the user-chosen label ("Me", "Mother"...). Validation mirrors
     * [submitBirthDetails]; onDone(true) only when the profile was stored.
     */
    fun saveCurrentAsBirthProfile(profileName: String, onDone: (Boolean) -> Unit) {
        val cleanName = profileName.trim()
        if (cleanName.length < 2) {
            _formError.value = W.t("profile_name_short")
            onDone(false)
            return
        }
        val dob = _birthDob.value.trim()
        val tob = _birthTob.value.trim()
        val pob = _birthPlace.value.trim()
        val lat = _birthLat.value.toDoubleOrNull()
        val lon = _birthLon.value.toDoubleOrNull()
        if (!DOB_REGEX.matches(dob) || !TOB_REGEX.matches(tob) || pob.isBlank() ||
            lat == null || lat < -90 || lat > 90 || lon == null || lon < -180 || lon > 180
        ) {
            _formError.value = W.t("profile_invalid_details")
            onDone(false)
            return
        }
        viewModelScope.launch {
            try {
                myDataRepo.saveBirthProfile(
                    name = cleanName,
                    dob = dob,
                    tob = tob,
                    pob = pob,
                    lat = lat,
                    lon = lon,
                    gender = _birthGender.value
                )
                refreshBirthProfiles()
                onDone(true)
            } catch (_: Exception) {
                _formError.value = W.t("profile_save_failed")
                onDone(false)
            }
        }
    }

    fun deleteBirthProfile(id: String) {
        viewModelScope.launch {
            try {
                myDataRepo.deleteBirthProfile(id)
                refreshBirthProfiles()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private val DOB_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
        // Accepts "HH:MM" 24h or "HH:MM AM/PM" 12h (e.g. "08:30", "08:30 AM")
        private val TOB_REGEX = Regex("""((([01]\d|2[0-3]):[0-5]\d)|(0\d|1[0-2]):[0-5]\d\s?(AM|PM))""", RegexOption.IGNORE_CASE)
    }
}
