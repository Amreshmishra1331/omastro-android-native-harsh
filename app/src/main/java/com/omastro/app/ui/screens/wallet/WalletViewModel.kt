package com.omastro.app.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.RechargePack
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun rechargePack(pack: RechargePack) {
        viewModelScope.launch {
            _isProcessing.value = true
            val orderRes = rechargeCoinsUseCase.createOrder(pack)
            orderRes.onSuccess { order ->
                // Simulate successful Razorpay checkout completion
                val verifyRes = rechargeCoinsUseCase.verifyPayment(
                    orderId = order.orderId,
                    paymentId = "pay_mock_${System.currentTimeMillis()}",
                    signature = "sig_verified",
                    pack = pack
                )
                verifyRes.onSuccess {
                    _message.value = it.message
                    _isProcessing.value = false
                }
            }.onFailure {
                _message.value = "Payment initialization failed"
                _isProcessing.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
