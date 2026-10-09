package com.omastro.app.domain.usecase

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.GocharRequest
import com.omastro.app.data.remote.GocharResponse
import com.omastro.app.data.repository.AstroRepository

class GetKundaliUseCase(private val repository: AstroRepository) {
    suspend operator fun invoke(request: KundaliRequest): Result<KundaliResponse> =
        repository.getKundali(request)
}

class GetDailyHoroscopeUseCase(private val repository: AstroRepository) {
    suspend operator fun invoke(sign: String, period: String = "today"): Result<RashifalData> =
        repository.getDailyHoroscope(sign, period)
}

class GetPanchangUseCase(private val repository: AstroRepository) {
    suspend operator fun invoke(date: String? = null): Result<PanchangResponse> =
        repository.getPanchang(date)
}

class CalculateMatchmakingUseCase(private val repository: AstroRepository) {
    suspend operator fun invoke(request: MatchmakingRequest): Result<MatchmakingResponse> =
        repository.calculateMatchmaking(request)
}

class GetGocharUseCase(private val repository: AstroRepository) {
    suspend operator fun invoke(request: GocharRequest): Result<GocharResponse> =
        repository.getGochar(request)
}
