package com.omastro.app.domain.usecase

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.omastro.app.data.model.*
import com.omastro.app.data.repository.ConsultationRepository
import com.omastro.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCancellableCoroutine

class GetAstrologersUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(category: String? = null): Result<List<Astrologer>> =
        repository.getAstrologers(category)
}

class SendChatMessageUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(astrologerId: String, text: String): ChatMessage =
        repository.sendChatMessage(astrologerId, text)
}

class GetWalletBalanceUseCase(private val repository: WalletRepository) {
    val coinBalance: StateFlow<Int> = repository.coinBalance
    val transactions: StateFlow<List<WalletTransaction>> = repository.transactions
}

class RechargeCoinsUseCase(private val repository: WalletRepository) {
    suspend fun createOrder(pack: RechargePack): Result<RazorpayOrderResponse> =
        repository.createOrder(pack)

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        pack: RechargePack
    ): Result<PaymentVerifyResponse> =
        repository.verifyPayment(orderId, paymentId, signature, pack)
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
