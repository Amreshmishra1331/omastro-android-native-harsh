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
    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

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

    fun getRechargePacks(): List<RechargePack> {
        return listOf(
            RechargePack("pack_100", 100, 99.0, 0, null),
            RechargePack("pack_250", 250, 199.0, 25, "POPULAR"),
            RechargePack("pack_600", 600, 499.0, 100, "BEST VALUE"),
            RechargePack("pack_1500", 1500, 999.0, 300, "DIVINE SAVER"),
            RechargePack("pack_3500", 3500, 1999.0, 800, "VIP SPECIAL")
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
            val amountPaise = (pack.priceInr * 100).toInt()
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
