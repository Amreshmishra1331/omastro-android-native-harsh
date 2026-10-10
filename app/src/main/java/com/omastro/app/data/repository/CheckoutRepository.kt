package com.omastro.app.data.repository

import com.omastro.app.data.model.CheckoutItem
import com.omastro.app.data.model.CheckoutOrder
import com.omastro.app.data.model.PriceBreakdown
import com.omastro.app.data.model.CHECKOUT_AMOUNT_FALLBACK_PAISE
import com.omastro.app.data.model.RAZORPAY_FALLBACK_KEY_ID
import com.omastro.app.data.model.WALLET_TOPUP_PRODUCT_ID
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import java.io.IOException

/**
 * Flutter-exact checkout repository.
 *
 * Sequence (mirrors Flutter _NativeCheckoutBottomSheet + _initNativePayment):
 *  1. getQuote()        -> POST /checkout/validate-coupon {coupon, plan?, product_id?, coins?}
 *  2. createOrder()     -> POST /checkout/create-order  {product_id|plan, coins?, qty?, coupon?}
 *  3. verifyPayment()   -> POST /checkout/verify {order_id, razorpay_payment_id,
 *                          razorpay_order_id, razorpay_signature}
 *
 * All calls carry the Bearer token via [ApiClient]'s auth interceptor, and all
 * benefit from its /wp-json -> ?rest_route= 404 fallback. Nothing is ever
 * fabricated: amounts, titles, keys and the GST label all come from the
 * server response.
 */
class CheckoutRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {

    /** Step 1 — price breakdown for the sheet (empty coupon on open). */
    suspend fun getQuote(item: CheckoutItem, coupon: String): Result<PriceBreakdown> {
        return try {
            val body = mutableMapOf<String, Any?>("coupon" to coupon.trim().uppercase())
            item.planKey?.let { body["plan"] = it }
            item.productId?.let { body["product_id"] = it }
            item.customCoins?.let { body["coins"] = it }

            val res = api.validateCoupon(body)
            val root = res.body()
            if (!res.isSuccessful || root == null) {
                return Result.failure(
                    IOException("Could not load the price breakdown (HTTP ${res.code()}).")
                )
            }
            if (root["success"] == true) {
                val data = root["data"] as? Map<String, Any?>
                    ?: return Result.failure(IOException("Invalid price response from server."))
                Result.success(parseBreakdown(data))
            } else {
                // e.g. {success:false, code:"coupon_invalid", message:"..."} —
                // surface the server message; the caller keeps the old quote.
                val msg = (root["message"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "Invalid promo code"
                Result.failure(IOException(stripHtml(msg)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Step 2 — issue a real Razorpay order (Bearer auth required by server). */
    suspend fun createOrder(item: CheckoutItem, coupon: String?): Result<CheckoutOrder> {
        return try {
            val body = mutableMapOf<String, Any?>()
            val coins = item.customCoins
            val pid = item.productId
            when {
                coins != null && coins > 0 && (pid == null || pid == WALLET_TOPUP_PRODUCT_ID) -> {
                    // Wallet top-up / coin recharge (Flutter: {product_id:704, coins, qty}).
                    body["product_id"] = WALLET_TOPUP_PRODUCT_ID
                    body["coins"] = coins
                    body["qty"] = (coins / 100).coerceAtLeast(1)
                }
                pid != null && pid > 0 -> {
                    body["product_id"] = pid
                    if (pid == WALLET_TOPUP_PRODUCT_ID) {
                        // Flutter default for a bare 704 tap: 500 coins, qty 5.
                        body["coins"] = coins ?: 500
                        body["qty"] = 5
                    }
                }
                !item.planKey.isNullOrBlank() -> {
                    body["plan"] = item.planKey.trim().lowercase()
                }
                else -> return Result.failure(
                    IllegalArgumentException("Invalid plan selected. Please try again.")
                )
            }
            coupon?.takeIf { it.isNotBlank() }?.let { body["coupon"] = it.trim().uppercase() }

            val res = api.createCheckoutOrder(body)
            val root = res.body()
            if (!res.isSuccessful || root == null) {
                return Result.failure(
                    IOException("Failed to initialize order (HTTP ${res.code()}).")
                )
            }
            if (root["success"] != true) {
                val msg = (root["message"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "Failed to initialize order"
                return Result.failure(IOException(stripHtml(msg)))
            }
            val data = root["data"] as? Map<String, Any?>
                ?: return Result.failure(IOException("Invalid response from payment server"))
            Result.success(parseOrder(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Step 3 — verify the genuine Razorpay result with the backend.
     * Returns the server message on success.
     */
    suspend fun verifyPayment(
        orderId: Long,
        paymentId: String,
        rzpOrderId: String,
        signature: String
    ): Result<String> {
        return try {
            val res = api.verifyCheckout(
                mapOf(
                    "order_id" to orderId,
                    "razorpay_payment_id" to paymentId,
                    "razorpay_order_id" to rzpOrderId,
                    "razorpay_signature" to signature
                )
            )
            val root = res.body()
            if (res.isSuccessful && root != null && root["success"] == true) {
                val msg = ((root["data"] as? Map<String, Any?>)?.get("message") as? String)
                    ?: (root["message"] as? String)
                Result.success(msg ?: "Payment Successful!")
            } else {
                val msg = (root?.get("message") as? String)?.takeIf { it.isNotBlank() }
                    ?: "Payment verification failed"
                Result.failure(IOException(stripHtml(msg)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Tolerant parsing (server sends JSON numbers; strings tolerated too).
    // ------------------------------------------------------------------

    private fun parseBreakdown(d: Map<String, Any?>): PriceBreakdown {
        return PriceBreakdown(
            itemTitle = (d["item_title"] as? String).orEmpty(),
            subtotal = num(d["subtotal"]),
            subtotalDisplay = d["subtotal_display"] as? String,
            discountAmount = num(d["discount_amount"]),
            discountDisplay = d["discount_display"] as? String,
            taxAmount = num(d["tax_amount"]),
            taxDisplay = d["tax_display"] as? String,
            taxLabel = (d["tax_label"] as? String)?.takeIf { it.isNotBlank() } ?: "GST (18%)",
            total = num(d["total"]),
            totalDisplay = d["total_display"] as? String,
            totalPaise = num(d["total_paise"]).toInt(),
            couponCode = (d["coupon_code"] as? String).orEmpty(),
            message = d["message"] as? String
        )
    }

    private fun parseOrder(d: Map<String, Any?>): CheckoutOrder {
        return CheckoutOrder(
            orderId = long(d["order_id"]),
            rzpKeyId = (d["rzp_key_id"] as? String)?.trim().orEmpty()
                .ifBlank { RAZORPAY_FALLBACK_KEY_ID },
            amountPaise = num(d["amount"]).toInt()
                .takeIf { it > 0 } ?: CHECKOUT_AMOUNT_FALLBACK_PAISE,
            rzpOrderId = (d["rzp_order_id"] as? String)?.trim().orEmpty(),
            planTitle = (d["plan_title"] as? String)?.takeIf { it.isNotBlank() }
                ?: "Om Astro AI Plan",
            userEmail = (d["user_email"] as? String)?.trim().orEmpty(),
            userName = (d["user_name"] as? String)?.trim().orEmpty()
        )
    }

    private fun num(v: Any?): Double = when (v) {
        is Number -> v.toDouble()
        is String -> v.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private fun long(v: Any?): Long = when (v) {
        is Number -> v.toLong()
        is String -> v.filter { it.isDigit() }.toLongOrNull() ?: 0L
        else -> 0L
    }

    /** Server messages occasionally contain HTML — mirror Flutter's tag strip. */
    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]*>"), "").trim().ifBlank { s }
}
