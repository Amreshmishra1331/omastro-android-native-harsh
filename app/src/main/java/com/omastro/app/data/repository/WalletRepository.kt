package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class WalletRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    // Real INR wallet balance (in paise, from /wallet/balance API)
    private val _walletBalancePaise = MutableStateFlow(0)
    val walletBalancePaise: StateFlow<Int> = _walletBalancePaise.asStateFlow()

    private val _paidBalancePaise = MutableStateFlow(0)
    val paidBalancePaise: StateFlow<Int> = _paidBalancePaise.asStateFlow()

    private val _bonusBalancePaise = MutableStateFlow(0)
    val bonusBalancePaise: StateFlow<Int> = _bonusBalancePaise.asStateFlow()

    private val _paidBalanceInr = MutableStateFlow(0.0)
    val paidBalanceInr: StateFlow<Double> = _paidBalanceInr.asStateFlow()

    private val _bonusBalanceInr = MutableStateFlow(0.0)
    val bonusBalanceInr: StateFlow<Double> = _bonusBalanceInr.asStateFlow()

    private val _walletBalanceInr = MutableStateFlow(0.0)
    val walletBalanceInr: StateFlow<Double> = _walletBalanceInr.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    suspend fun refreshBalance(): Result<Double> {
        return try {
            val res = api.getWalletBalance()
            val body = res.body()
            if (res.isSuccessful && body != null && body.success && body.data != null) {
                _walletBalancePaise.value = body.data.totalPaise
                _walletBalanceInr.value = body.data.totalInr
                _paidBalancePaise.value = body.data.paidPaise
                _bonusBalancePaise.value = body.data.bonusPaise
                _paidBalanceInr.value = body.data.paidInr
                _bonusBalanceInr.value = body.data.bonusInr
                Result.success(body.data.totalInr)
            } else {
                Result.failure(IOException("Failed to load wallet balance: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the local balance from a server-returned authoritative figure
     * (e.g. `wallet_balance_paise` on paid vision/photo-AI responses), without
     * a `/wallet/balance` round-trip. Paid/bonus splits stay as last loaded;
     * the next [refreshBalance] restores them.
     */
    fun applyServerBalance(totalPaise: Int) {
        _walletBalancePaise.value = totalPaise
        _walletBalanceInr.value = totalPaise / 100.0
    }

    /** Reset all in-memory wallet state without any server call. */
    fun clearLocal() {
        _walletBalancePaise.value = 0
        _paidBalancePaise.value = 0
        _bonusBalancePaise.value = 0
        _paidBalanceInr.value = 0.0
        _bonusBalanceInr.value = 0.0
        _walletBalanceInr.value = 0.0
        _transactions.value = emptyList()
    }

    /**
     * Cycle 4: the hardcoded add-money slab list (₹25→₹35, ₹50→₹80,
     * ₹100→₹200, ₹200→₹400) is REMOVED. Pack credits are now server-driven:
     * [com.omastro.app.ui.screens.wallet.WalletViewModel.loadPackQuotes]
     * fetches POST /checkout/validate-coupon per slab and parses the
     * authoritative "₹X Credit" figure from the server's item_title. The
     * offered pay amounts live in [com.omastro.app.ui.screens.wallet.WalletViewModel.packPayAmounts]
     * (website parity); only the pay amount is sent to the server — bonus is
     * computed server-side on verify.
     */

    suspend fun getTransactions(limit: Int = 20, offset: Int = 0): Result<List<WalletTransaction>> {
        return try {
            val res = api.getWalletTransactions(limit, offset)
            val body = res.body()
            if (res.isSuccessful && body != null) {
                _transactions.value = body
                Result.success(body)
            } else {
                Result.failure(IOException("Failed to load transactions: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(pack: RechargePack): Result<TopupOrderResponse> {
        return try {
            // Only the pay amount is sent — bonus is server-computed on verify.
            // Backend expects amount in RUPEES (kg_wallet_quote($amount_rupees)).
            val amountRupees = pack.payInr.toInt()
            val res = api.createTopupOrder(TopupOrderRequest(amountRupees))
            val body = res.body()
            if (res.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IOException("Failed to create top-up order: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<TopupVerifyResponse> {
        return try {
            val res = api.verifyTopup(TopupVerifyRequest(orderId, paymentId, signature))
            val body = res.body()
            if (res.isSuccessful && body != null && body.success) {
                Result.success(body)
            } else {
                Result.failure(IOException("Payment verification failed: HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
