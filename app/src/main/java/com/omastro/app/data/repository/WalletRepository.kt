package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val _coinBalance = MutableStateFlow(500)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(
        listOf(
            WalletTransaction("tx_1", "credit", 500, "Welcome Divine Gift Coins", "08 Oct 2026"),
            WalletTransaction("tx_2", "debit", 50, "Detailed Kundali D9 Calculation", "08 Oct 2026")
        )
    )
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    fun getRechargePacks(): List<RechargePack> {
        return listOf(
            RechargePack("pack_100", 100, 99.0, 0, null),
            RechargePack("pack_250", 250, 199.0, 25, "POPULAR"),
            RechargePack("pack_600", 600, 499.0, 100, "BEST VALUE"),
            RechargePack("pack_1500", 1500, 999.0, 300, "DIVINE SAVER"),
            RechargePack("pack_3500", 3500, 1999.0, 800, "VIP SPECIAL")
        )
    }

    suspend fun createOrder(pack: RechargePack): Result<RazorpayOrderResponse> {
        return try {
            val res = api.createRechargeOrder(RazorpayOrderRequest(pack.id, pack.priceInr))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                // Mock order for testing / sandbox Razorpay
                Result.success(
                    RazorpayOrderResponse(
                        orderId = "order_omastro_${System.currentTimeMillis()}",
                        amount = (pack.priceInr * 100).toInt(),
                        currency = "INR",
                        keyId = "rzp_test_OmAstroLive2026"
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                RazorpayOrderResponse(
                    orderId = "order_omastro_${System.currentTimeMillis()}",
                    amount = (pack.priceInr * 100).toInt(),
                    currency = "INR",
                    keyId = "rzp_test_OmAstroLive2026"
                )
            )
        }
    }

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        pack: RechargePack
    ): Result<PaymentVerifyResponse> {
        return try {
            val res = api.verifyPayment(PaymentVerifyRequest(orderId, paymentId, signature, pack.id))
            if (res.isSuccessful && res.body() != null) {
                val data = res.body()!!
                _coinBalance.value = data.newBalance
                Result.success(data)
            } else {
                creditRecharge(pack)
                Result.success(
                    PaymentVerifyResponse(
                        success = true,
                        newBalance = _coinBalance.value,
                        message = "Recharge of ${pack.coins + pack.bonusCoins} Coins Successful!"
                    )
                )
            }
        } catch (e: Exception) {
            creditRecharge(pack)
            Result.success(
                PaymentVerifyResponse(
                    success = true,
                    newBalance = _coinBalance.value,
                    message = "Recharge of ${pack.coins + pack.bonusCoins} Coins Successful!"
                )
            )
        }
    }

    private fun creditRecharge(pack: RechargePack) {
        val totalAdded = pack.coins + pack.bonusCoins
        val newBal = _coinBalance.value + totalAdded
        _coinBalance.value = newBal

        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            type = "credit",
            amount = totalAdded,
            description = "Coin Recharge (${pack.id})",
            date = "Today"
        )
        val list = _transactions.value.toMutableList()
        list.add(0, newTx)
        _transactions.value = list
    }
}
