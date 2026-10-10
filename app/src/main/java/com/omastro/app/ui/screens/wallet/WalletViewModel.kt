package com.omastro.app.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.CheckoutItem
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Server-verified wallet credit for one add-money slab.
 * Every number here comes from POST /checkout/validate-coupon — the app
 * never hardcodes a credit amount. (Cycle 4: the old hardcoded
 * `getRechargePacks()` credit list is gone.)
 */
data class PackQuote(
    /** Amount charged before GST (INR) — the offered slab. */
    val payInr: Int,
    /** Wallet credit the server grants for this slab (INR). */
    val creditInr: Double,
    /** Total payable incl. GST (INR), server-computed. */
    val totalPayableInr: Double
) {
    val bonusInr: Double get() = creditInr - payInr
}

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
    private val checkoutRepo = AppContainer.checkoutRepository

    val walletBalanceInr: StateFlow<Double> = walletRepo.walletBalanceInr
    val paidBalanceInr: StateFlow<Double> = walletRepo.paidBalanceInr
    val bonusBalanceInr: StateFlow<Double> = walletRepo.bonusBalanceInr
    val transactions: StateFlow<List<WalletTransaction>> = walletRepo.transactions

    /**
     * Add-money slabs offered (website parity). The CREDIT for each slab is
     * NOT hardcoded — [packQuotes] carries the server-verified figures from
     * POST /checkout/validate-coupon ({product_id:704, coins} -> item_title
     * "OmAstro Wallet Top-up — ₹X Credit", subtotal, total). Until a quote
     * arrives the card shows "Pay ₹X" with a loading note; on failure an
     * honest error replaces the credit line — never a guessed number.
     */
    val packPayAmounts: List<Int> = listOf(25, 50, 100, 200)

    private val _packQuotes = MutableStateFlow<Map<Int, PackQuote>?>(null)
    val packQuotes: StateFlow<Map<Int, PackQuote>?> = _packQuotes.asStateFlow()

    private val _packQuotesFailed = MutableStateFlow(false)
    val packQuotesFailed: StateFlow<Boolean> = _packQuotesFailed.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshBalance()
        loadPackQuotes()
    }

    /** Fetch server-verified credit/total for every offered pack. */
    fun loadPackQuotes() {
        viewModelScope.launch {
            _packQuotesFailed.value = false
            val quotes = packPayAmounts.mapNotNull { pay ->
                checkoutRepo.getQuote(CheckoutItem.walletPack(pay), "")
                    .getOrNull()
                    ?.let { q -> parsePackQuote(pay, q.itemTitle, q.total) }
            }.associateBy { it.payInr }
            _packQuotes.value = quotes.ifEmpty { null }
            _packQuotesFailed.value = quotes.isEmpty()
        }
    }

    /**
     * Extract the server's credit figure from the quote title, e.g.
     * "OmAstro Wallet Top-up — ₹35.00 Credit". Returns null when the title
     * does not carry a parseable credit — the UI then shows the honest
     * "credit confirmed at checkout" fallback instead of a guess.
     */
    private fun parsePackQuote(pay: Int, itemTitle: String, total: Double): PackQuote? {
        val match = Regex("₹\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*Credit").find(itemTitle)
            ?: return null
        val credit = match.groupValues[1].replace(",", "").toDoubleOrNull()
            ?: return null
        return PackQuote(payInr = pay, creditInr = credit, totalPayableInr = total)
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
