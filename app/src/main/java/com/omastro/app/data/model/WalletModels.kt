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

/**
 * Add-money option — website bonus-slab parity (verified 2026-10-02):
 * pay ₹25 → ₹35 credit, pay ₹50 → ₹80, pay ₹100 → ₹200, above ₹100 → double.
 * 18% GST is included in the price the user pays. The bonus slab itself is
 * computed server-side on verify — the app only sends the pay amount.
 */
data class RechargePack(
    val id: String,
    /** Amount charged via Razorpay (INR). */
    val payInr: Double,
    /** Wallet credit the server grants for this slab (INR). */
    val creditInr: Double
) {
    /** Bonus portion of the slab (credit minus pay), in INR. */
    val bonusInr: Double get() = creditInr - payInr
}

data class TopupOrderRequest(
    @SerializedName("amount") val amountRupees: Int, // amount in RUPEES (backend kg_wallet_quote expects rupees)
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
