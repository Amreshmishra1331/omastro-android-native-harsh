package com.omastro.app.ui.screens.matching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.GunaDetail
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

    init {
        // Preload default sample matching
        val defaultGunas = listOf(
            GunaDetail("Varna (Spiritual)", 1.0, 1.0, "Work & spiritual compatibility"),
            GunaDetail("Vashya (Dominance)", 2.0, 2.0, "Mutual control & harmony"),
            GunaDetail("Tara (Destiny)", 3.0, 3.0, "Health and well-being"),
            GunaDetail("Yoni (Intimacy)", 3.0, 4.0, "Biological attraction"),
            GunaDetail("Maitri (Friendship)", 5.0, 5.0, "Psychological friendship"),
            GunaDetail("Gana (Temperament)", 5.0, 6.0, "Behavioral traits"),
            GunaDetail("Bhakoot (Love & Family)", 7.0, 7.0, "Family welfare & prosperity"),
            GunaDetail("Nadi (Genetics/Health)", 8.0, 8.0, "Physiological compatibility")
        )
        _result.value = MatchmakingResponse(
            success = true,
            totalScore = 34.0,
            maxScore = 36.0,
            percentage = 94,
            verdict = "Excellent Match (Uttam Vivah)",
            manglikBoy = "Non-Manglik",
            manglikGirl = "Non-Manglik",
            gunas = defaultGunas,
            conclusion = "The 36 Guna Ashtakoota analysis indicates extraordinary harmony, mutual respect, and prosperity."
        )
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
            val req = MatchmakingRequest(boyName, boyDob, boyTob, boyPob, girlName, girlDob, girlTob, girlPob)
            calculateMatchmakingUseCase(req)
                .onSuccess {
                    _result.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                }
        }
    }
}
