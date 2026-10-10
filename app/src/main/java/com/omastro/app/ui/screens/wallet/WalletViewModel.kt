package com.omastro.app.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.RechargePack
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wallet screen state: balance + transactions + recharge packs.
 *
 * Money-in now goes through the Flutter-exact checkout flow
 * ([com.omastro.app.ui.screens.checkout.CheckoutViewModel] +
 * [com.omastro.app.ui.screens.checkout.CheckoutSheetHost]):
 * pack tap -> checkout bottom sheet (server price quote) -> Razorpay ->
 * POST /checkout/verify. On payment finish the host calls [refreshBalance]
 * and [refreshTransactions] — the wallet balance is credited ONLY on a
 * verified success, and nothing is ever fabricated.
 */
class WalletViewModel : ViewModel() {

    private val walletRepo = AppContainer.walletRepository

    val walletBalanceInr: StateFlow<Double> = walletRepo.walletBalanceInr
    val paidBalanceInr: StateFlow<Double> = walletRepo.paidBalanceInr
    val bonusBalanceInr: StateFlow<Double> = walletRepo.bonusBalanceInr
    val transactions: StateFlow<List<WalletTransaction>> = walletRepo.transactions

    val rechargePacks: List<RechargePack> = walletRepo.getRechargePacks()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshBalance()
    }

    fun refreshBalance() {
        viewModelScope.launch {
            walletRepo.refreshBalance()
        }
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            _isProcessing.value = true
            walletRepo.getTransactions()
                .onFailure { err ->
                    _message.value = err.message ?: W.t("money_tx_load_fail")
                }
            _isProcessing.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Surfaces checkout-flow messages in the screen snackbar. */
    fun postMessage(msg: String) {
        _message.value = msg
    }
}
