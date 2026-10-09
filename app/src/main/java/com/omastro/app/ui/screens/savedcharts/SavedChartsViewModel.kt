package com.omastro.app.ui.screens.savedcharts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.KundaliRequest
import com.omastro.app.data.remote.SavedChart
import com.omastro.app.data.repository.SavedChartsRepository
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SavedChartsUiState(
    val charts: List<SavedChart> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteInProgress: Set<String> = emptySet()
)

class SavedChartsViewModel(
    private val repository: SavedChartsRepository = AppContainer.savedChartsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavedChartsUiState(isLoading = true))
    val state: StateFlow<SavedChartsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getSavedCharts()
                .onSuccess { charts ->
                    _state.value = _state.value.copy(
                        charts = charts,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = err.message ?: "Could not load saved charts."
                    )
                }
        }
    }

    fun deleteChart(chart: SavedChart) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                deleteInProgress = _state.value.deleteInProgress + chart.id
            )
            repository.deleteChart(chart.id)
                .onSuccess {
                    _state.value = _state.value.copy(
                        charts = _state.value.charts.filter { it.id != chart.id },
                        deleteInProgress = _state.value.deleteInProgress - chart.id
                    )
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        deleteInProgress = _state.value.deleteInProgress - chart.id,
                        error = err.message ?: "Could not delete chart."
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

/**
 * Shared helper: save the currently-viewed kundali birth details.
 * Returns Result with optional error message for UI display.
 */
class SaveChartHelper(
    private val repository: SavedChartsRepository = AppContainer.savedChartsRepository
) {
    suspend fun save(name: String, request: KundaliRequest): Result<SavedChart?> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter a name for this chart."))
        }
        return repository.saveChart(trimmed, request)
    }
}
