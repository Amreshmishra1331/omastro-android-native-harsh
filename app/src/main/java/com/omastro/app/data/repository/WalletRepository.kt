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
     * Add-money slabs — website parity (verified 2026-10-02):
     * ₹25 → ₹35 credit, ₹50 → ₹80, ₹100 → ₹200, above ₹100 → double.
     * The server computes and grants the bonus on verify; the app only
     * sends the pay amount. Never invent prices or badges here.
     */
    fun getRechargePacks(): List<RechargePack> {
        return listOf(
            RechargePack("slab_25", 25.0, 35.0),
            RechargePack("slab_50", 50.0, 80.0),
            RechargePack("slab_100", 100.0, 200.0)
        )
    }

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
            val amountPaise = (pack.payInr * 100).toInt()
            val res = api.createTopupOrder(TopupOrderRequest(amountPaise))
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
