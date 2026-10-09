package com.omastro.app.ui.screens.astrohealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Astro-health analysis states.
 */
sealed interface AstroHealthUiState {
    data object Idle : AstroHealthUiState
    data object Loading : AstroHealthUiState
    /** The raw `data` object from POST /astro-health, rendered generically. */
    data class Success(val data: Map<String, Any?>) : AstroHealthUiState
    data class Error(val message: String) : AstroHealthUiState
}

/**
 * Calls POST /astro-health with the user's own birth data through
 * AstroRepository. Nothing is hardcoded: no organ lists, no vitality
 * percentages — only what the backend returns is shown.
 */
class AstroHealthViewModel : ViewModel() {

    private val astroRepository = AppContainer.astroRepository

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    private val _dob = MutableStateFlow("") // YYYY-MM-DD
    val dob: StateFlow<String> = _dob.asStateFlow()
    private val _tob = MutableStateFlow("") // HH:MM 24h
    val tob: StateFlow<String> = _tob.asStateFlow()
    private val _place = MutableStateFlow("")
    val place: StateFlow<String> = _place.asStateFlow()
    private val _lat = MutableStateFlow("")
    val lat: StateFlow<String> = _lat.asStateFlow()
    private val _lon = MutableStateFlow("")
    val lon: StateFlow<String> = _lon.asStateFlow()

    private val _uiState = MutableStateFlow<AstroHealthUiState>(AstroHealthUiState.Idle)
    val uiState: StateFlow<AstroHealthUiState> = _uiState.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    fun setName(v: String) { _name.value = v; _formError.value = null }
    fun setDob(v: String) { _dob.value = v; _formError.value = null }
    fun setTob(v: String) { _tob.value = v; _formError.value = null }
    fun setPlace(v: String) { _place.value = v; _formError.value = null }
    fun setLat(v: String) { _lat.value = v.filter { it.isDigit() || it == '.' || it == '-' }; _formError.value = null }
    fun setLon(v: String) { _lon.value = v.filter { it.isDigit() || it == '.' || it == '-' }; _formError.value = null }

    fun analyze() {
        val name = _name.value.trim()
        val dob = _dob.value.trim()
        val tob = _tob.value.trim()
        val place = _place.value.trim()
        val lat = _lat.value.toDoubleOrNull()
        val lon = _lon.value.toDoubleOrNull()

        if (name.length < 2) { _formError.value = "Please enter the full name."; return }
        if (!DOB_REGEX.matches(dob)) { _formError.value = "Date of birth must be YYYY-MM-DD."; return }
        if (!TOB_REGEX.matches(tob)) { _formError.value = "Time of birth must be HH:MM (24-hour)."; return }
        if (place.isBlank()) { _formError.value = "Please enter the place of birth."; return }
        if (lat == null || lat < -90 || lat > 90) { _formError.value = "Latitude must be between -90 and 90."; return }
        if (lon == null || lon < -180 || lon > 180) { _formError.value = "Longitude must be between -180 and 180."; return }

        viewModelScope.launch {
            _uiState.value = AstroHealthUiState.Loading
            // AM/PM-safe conversion via the shared helper.
            val (tobHr24, tobMn24) = com.omastro.app.ui.components.parseTimeTo24(tob) ?: (0 to 0)
            val request = com.omastro.app.data.model.KundaliRequest(
                name = name,
                dob = dob,
                tobHr = tobHr24,
                tobMn = tobMn24,
                tob = tob,
                pob = place,
                lat = lat,
                lon = lon
            )
            astroRepository.getAstroHealth(request)
                .onSuccess { body ->
                    val data = extractData(body)
                    if (data != null && data.isNotEmpty()) {
                        _uiState.value = AstroHealthUiState.Success(data)
                    } else {
                        _uiState.value = AstroHealthUiState.Error(
                            "The server returned an empty health analysis. Please try again."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = AstroHealthUiState.Error(
                        e.message ?: "Network error. Please check your connection and try again."
                    )
                }
        }
    }

    fun reset() {
        _uiState.value = AstroHealthUiState.Idle
        _formError.value = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractData(body: Map<String, Any>?): Map<String, Any?>? {
        if (body == null) return null
        val success = body["success"] as? Boolean ?: false
        if (!success) return null
        return body["data"] as? Map<String, Any?>
    }

    companion object {
        private val DOB_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
        private val TOB_REGEX = Regex("""([01]\d|2[0-3]):[0-5]\d""")
    }
}
