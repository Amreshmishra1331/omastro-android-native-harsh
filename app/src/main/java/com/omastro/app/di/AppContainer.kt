package com.omastro.app.di

import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import com.omastro.app.data.repository.AstroRepository
import com.omastro.app.data.repository.AuthRepository
import com.omastro.app.data.repository.ChatRepository
import com.omastro.app.data.repository.ConsultationRepository
import com.omastro.app.data.repository.NotificationRepository
import com.omastro.app.data.repository.SavedChartsRepository
import com.omastro.app.data.repository.WalletRepository
import com.omastro.app.domain.usecase.*

object AppContainer {
    val apiService: OmAstroApiService by lazy { ApiClient.apiService }

    val authRepository: AuthRepository by lazy { AuthRepository(apiService) }
    val astroRepository: AstroRepository by lazy { AstroRepository(apiService) }
    val consultationRepository: ConsultationRepository by lazy { ConsultationRepository(apiService) }
    val walletRepository: WalletRepository by lazy { WalletRepository(apiService) }
    val chatRepository: ChatRepository by lazy { ChatRepository(apiService) }
    val savedChartsRepository: SavedChartsRepository by lazy { SavedChartsRepository(apiService) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(apiService) }

    // UseCases
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository) }
    val verifySignupOtpUseCase by lazy { VerifySignupOtpUseCase(authRepository) }
    val forgotPasswordUseCase by lazy { ForgotPasswordUseCase(authRepository) }
    val resendSignupOtpUseCase by lazy { ResendSignupOtpUseCase(authRepository) }

    val getKundaliUseCase by lazy { GetKundaliUseCase(astroRepository) }
    val getDailyHoroscopeUseCase by lazy { GetDailyHoroscopeUseCase(astroRepository) }
    val getPanchangUseCase by lazy { GetPanchangUseCase(astroRepository) }
    val getHoraUseCase by lazy { GetHoraUseCase(astroRepository) }
    val calculateMatchmakingUseCase by lazy { CalculateMatchmakingUseCase(astroRepository) }
    val getGocharUseCase by lazy { GetGocharUseCase(astroRepository) }
    val getMuhuratUseCase by lazy { GetMuhuratUseCase(astroRepository) }

    val getAstrologersUseCase by lazy { GetAstrologersUseCase(consultationRepository) }
    val openChatSessionUseCase by lazy { OpenChatSessionUseCase(consultationRepository) }
    val closeChatSessionUseCase by lazy { CloseChatSessionUseCase(consultationRepository) }
    val startBillingSessionUseCase by lazy { StartBillingSessionUseCase(consultationRepository) }
    val tickBillingSessionUseCase by lazy { TickBillingSessionUseCase(consultationRepository) }
    val endBillingSessionUseCase by lazy { EndBillingSessionUseCase(consultationRepository) }
    val submitAstrologerReviewUseCase by lazy { SubmitAstrologerReviewUseCase(consultationRepository) }

    val getWalletBalanceUseCase by lazy { GetWalletBalanceUseCase(walletRepository) }
    val rechargeCoinsUseCase by lazy { RechargeCoinsUseCase(walletRepository) }
    val requestInAppReviewUseCase by lazy { RequestInAppReviewUseCase() }
}
