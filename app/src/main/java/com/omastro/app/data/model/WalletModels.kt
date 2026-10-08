package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

data class WalletTransaction(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "credit", "debit"
    @SerializedName("amount") val amount: Int,
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: String
)

data class WalletBalanceResponse(
    @SerializedName("balance") val balance: Int,
    @SerializedName("currency") val currency: String = "Coins",
    @SerializedName("transactions") val transactions: List<WalletTransaction> = emptyList()
)

data class RechargePack(
    val id: String,
    val coins: Int,
    val priceInr: Double,
    val bonusCoins: Int = 0,
    val badge: String? = null
)

data class RazorpayOrderRequest(
    @SerializedName("pack_id") val packId: String,
    @SerializedName("amount_inr") val amountInr: Double
)

data class RazorpayOrderResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("amount") val amount: Int, // in paise
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("key_id") val keyId: String
)

data class PaymentVerifyRequest(
    @SerializedName("razorpay_order_id") val orderId: String,
    @SerializedName("razorpay_payment_id") val paymentId: String,
    @SerializedName("razorpay_signature") val signature: String,
    @SerializedName("pack_id") val packId: String
)

data class PaymentVerifyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("new_balance") val newBalance: Int,
    @SerializedName("message") val message: String
)
