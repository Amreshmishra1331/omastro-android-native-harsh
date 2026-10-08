package com.omastro.app.di

import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import com.omastro.app.data.repository.AstroRepository
import com.omastro.app.data.repository.AuthRepository
import com.omastro.app.data.repository.ConsultationRepository
import com.omastro.app.data.repository.WalletRepository
import com.omastro.app.domain.usecase.*

object AppContainer {
    val apiService: OmAstroApiService by lazy { ApiClient.apiService }

    val authRepository: AuthRepository by lazy { AuthRepository(apiService) }
    val astroRepository: AstroRepository by lazy { AstroRepository(apiService) }
    val consultationRepository: ConsultationRepository by lazy { ConsultationRepository(apiService) }
    val walletRepository: WalletRepository by lazy { WalletRepository(apiService) }

    // UseCases
    val getAuthConfigUseCase by lazy { GetAuthConfigUseCase(authRepository) }
    val sendOtpUseCase by lazy { SendOtpUseCase(authRepository) }
    val verifyOtpUseCase by lazy { VerifyOtpUseCase(authRepository) }

    val getKundaliUseCase by lazy { GetKundaliUseCase(astroRepository) }
    val getDailyHoroscopeUseCase by lazy { GetDailyHoroscopeUseCase(astroRepository) }
    val getPanchangUseCase by lazy { GetPanchangUseCase(astroRepository) }
    val calculateMatchmakingUseCase by lazy { CalculateMatchmakingUseCase(astroRepository) }

    val getAstrologersUseCase by lazy { GetAstrologersUseCase(consultationRepository) }
    val sendChatMessageUseCase by lazy { SendChatMessageUseCase(consultationRepository) }

    val getWalletBalanceUseCase by lazy { GetWalletBalanceUseCase(walletRepository) }
    val rechargeCoinsUseCase by lazy { RechargeCoinsUseCase(walletRepository) }
    val requestInAppReviewUseCase by lazy { RequestInAppReviewUseCase() }
}
