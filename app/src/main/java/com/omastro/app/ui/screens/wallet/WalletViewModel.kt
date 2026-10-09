package com.omastro.app.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.TopupOrderResponse
import com.omastro.app.data.model.RechargePack
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wallet flow (production):
 *  1. User taps a pack -> createOrder() issues a REAL Razorpay order
 *     (POST /wallet/topup-order).
 *  2. UI launches RazorpayPaymentActivity with the order; the genuine
 *     Razorpay sheet collects the payment.
 *  3. On success the activity returns payment_id + signature, which are sent
 *     to POST /wallet/topup-verify. Coins are credited ONLY on verified success.
 * Nothing is ever fabricated: no mock payment ids, no mock signatures.
 */
class WalletViewModel : ViewModel() {

    private val walletRepo = AppContainer.walletRepository
    private val rechargeCoinsUseCase = AppContainer.rechargeCoinsUseCase

    val coinBalance: StateFlow<Int> = walletRepo.coinBalance
    val transactions: StateFlow<List<WalletTransaction>> = walletRepo.transactions

    val rechargePacks: List<RechargePack> = walletRepo.getRechargePacks()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * A freshly issued order awaiting payment, together with the pack being
     * purchased. The UI observes this and launches RazorpayPaymentActivity once.
     */
    data class PendingOrder(
        val order: TopupOrderResponse,
        val pack: RechargePack
    )

    private val _pendingOrder = MutableStateFlow<PendingOrder?>(null)
    val pendingOrder: StateFlow<PendingOrder?> = _pendingOrder.asStateFlow()

    /** Step 1: create a real Razorpay order for the pack. */
    fun createOrder(pack: RechargePack) {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            _message.value = null
            rechargeCoinsUseCase.createOrder(pack)
                .onSuccess { order ->
                    _pendingOrder.value = PendingOrder(order, pack)
                }
                .onFailure { err ->
                    _message.value = err.message ?: "Could not start the payment. Please try again."
                }
            _isProcessing.value = false
        }
    }

    /** Consumed by the UI after launching the payment activity. */
    fun clearPendingOrder() {
        _pendingOrder.value = null
    }

    /**
     * Step 3: verify the genuine Razorpay result with the backend.
     * Called only with the real payment_id / signature returned by Razorpay.
     */
    fun verifyPayment(orderId: String, paymentId: String?, signature: String?, pack: RechargePack) {
        if (paymentId.isNullOrBlank() || signature.isNullOrBlank()) {
            _message.value = "Payment could not be verified (missing payment details)."
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            rechargeCoinsUseCase.verifyPayment(
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            ).onSuccess { res ->
                _message.value = (res.message ?: "").ifBlank { "Recharge successful." }
            }.onFailure { err ->
                _message.value = err.message ?: "Payment verification failed. If money was debited it will be refunded."
            }
            _isProcessing.value = false
        }
    }

    /** Called when the user cancels the sheet or it fails to open. */
    fun onPaymentAborted(reason: String?) {
        _pendingOrder.value = null
        _message.value = reason ?: "Payment was cancelled."
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            // Repository exposes the transaction list; refresh hook for pull-to-refresh.
            // No-op if the repository pushes updates itself.
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
