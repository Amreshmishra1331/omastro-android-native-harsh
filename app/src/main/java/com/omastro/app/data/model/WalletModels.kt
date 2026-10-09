package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class WalletTransaction(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "credit", "debit"
    @SerializedName("amount") val amount: Int, // amount in paise
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: String
)

data class WalletBalanceData(
    @SerializedName("paid_paise") val paidPaise: Int = 0,
    @SerializedName("bonus_paise") val bonusPaise: Int = 0,
    @SerializedName("total_paise") val totalPaise: Int = 0,
    @SerializedName("paid_inr") val paidInr: Double = 0.0,
    @SerializedName("bonus_inr") val bonusInr: Double = 0.0,
    @SerializedName("total_inr") val totalInr: Double = 0.0
)

data class WalletBalanceResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: WalletBalanceData? = null,
    @SerializedName("message") val message: String? = null
)

data class RechargePack(
    val id: String,
    val coins: Int,
    val priceInr: Double,
    val bonusCoins: Int = 0,
    val badge: String? = null
)

data class TopupOrderRequest(
    @SerializedName("amount") val amountPaise: Int, // amount in paise
    @SerializedName("currency") val currency: String = "INR"
)

data class TopupOrderResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("rzp_key_id") val rzpKeyId: String,
    @SerializedName("amount") val amount: Int, // amount in paise
    @SerializedName("currency") val currency: String = "INR"
)

data class TopupVerifyRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("razorpay_payment_id") val paymentId: String,
    @SerializedName("razorpay_signature") val signature: String
)

data class TopupVerifyResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null
)
