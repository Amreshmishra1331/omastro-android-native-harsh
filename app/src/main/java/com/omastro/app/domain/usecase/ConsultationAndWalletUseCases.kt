package com.omastro.app.domain.usecase

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.omastro.app.data.model.*
import com.omastro.app.data.remote.OpenChatResponse
import com.omastro.app.data.repository.ConsultationRepository
import com.omastro.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class GetAstrologersUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(category: String? = null): Result<List<Astrologer>> =
        repository.getAstrologers(category)
}

class OpenChatSessionUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(astrologerId: String): Result<OpenChatResponse> =
        repository.openChat(astrologerId)
}

class CloseChatSessionUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(astrologerId: String, sessionId: String? = null): Result<Boolean> =
        repository.closeChat(astrologerId, sessionId)
}

class StartBillingSessionUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(astrologerId: String): Result<String?> =
        repository.startBillingSession(astrologerId)
}

class TickBillingSessionUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(sessionUuid: String): Result<Boolean> =
        repository.tickBillingSession(sessionUuid)
}

class EndBillingSessionUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(sessionUuid: String): Result<Boolean> =
        repository.endBillingSession(sessionUuid)
}

class GetWalletBalanceUseCase(private val repository: WalletRepository) {
    val coinBalance: StateFlow<Int> = repository.coinBalance
    val transactions: StateFlow<List<WalletTransaction>> = repository.transactions
}

class RechargeCoinsUseCase(private val repository: WalletRepository) {
    suspend fun createOrder(pack: RechargePack): Result<TopupOrderResponse> =
        repository.createOrder(pack)

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<TopupVerifyResponse> =
        repository.verifyPayment(orderId, paymentId, signature)
}

class RequestInAppReviewUseCase {
    suspend fun launchReview(activity: Activity): Boolean = suspendCancellableCoroutine { continuation ->
        val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    continuation.resume(true)
                }
            } else {
                continuation.resume(false)
            }
        }
    }
}
