package com.omastro.app.ui.screens.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.razorpay.Checkout
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

/**
 * Hosts the REAL Razorpay checkout sheet.
 *
 * Started with the order details issued by POST /checkout/create-order
 * (order_id = Razorpay order id, key id, amount in paise, plan title,
 * prefill email/name). Launches Checkout.open() and returns the genuine
 * payment result to the caller:
 *  - RESULT_OK      -> extras: RESULT_PAYMENT_ID, RESULT_ORDER_ID, RESULT_SIGNATURE
 *  - RESULT_CANCELED -> extra RESULT_ERROR with a human-readable reason
 *  - External wallet -> extra RESULT_EXTERNAL_WALLET (wallet name); the caller
 *    shows an info message only and must NOT call verify (Flutter parity).
 *
 * Checkout options mirror the Flutter app EXACTLY (main.dart _initNativePayment):
 * key, amount (paise), name 'Om Astro AI', description = plan title,
 * currency INR, order_id (only when non-empty), prefill email/name (only when
 * non-empty), theme #FF6B00, retry enabled max_count 1, send_sms_hash true.
 *
 * No payment identifiers are ever fabricated here.
 */
class RazorpayPaymentActivity : ComponentActivity(),
    PaymentResultWithDataListener,
    ExternalWalletListener {

    companion object {
        const val EXTRA_KEY_ID = "extra_key_id"
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_AMOUNT_PAISE = "extra_amount_paise"
        const val EXTRA_CURRENCY = "extra_currency"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_NAME = "extra_name"

        const val RESULT_PAYMENT_ID = "result_payment_id"
        const val RESULT_ORDER_ID = "result_order_id"
        const val RESULT_SIGNATURE = "result_signature"
        const val RESULT_ERROR = "result_error"
        const val RESULT_EXTERNAL_WALLET = "result_external_wallet"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyId = intent.getStringExtra(EXTRA_KEY_ID)
        val rzpOrderId = intent.getStringExtra(EXTRA_ORDER_ID)
        val amountPaise = intent.getIntExtra(EXTRA_AMOUNT_PAISE, 0)

        if (keyId.isNullOrBlank() || amountPaise <= 0) {
            finishWithError("Invalid payment order. Please try again.")
            return
        }

        // Preload Razorpay resources for a faster sheet open.
        Checkout.preload(applicationContext)

        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject().apply {
                put("name", intent.getStringExtra(EXTRA_TITLE) ?: "Om Astro AI")
                put("description", intent.getStringExtra(EXTRA_DESCRIPTION) ?: "Om Astro AI")
                // Flutter parity: include order_id only when the server gave one.
                if (!rzpOrderId.isNullOrBlank()) put("order_id", rzpOrderId)
                put("currency", intent.getStringExtra(EXTRA_CURRENCY) ?: "INR")
                put("amount", amountPaise)
                val prefill = JSONObject()
                intent.getStringExtra(EXTRA_EMAIL)?.takeIf { it.isNotBlank() }?.let {
                    prefill.put("email", it)
                }
                intent.getStringExtra(EXTRA_NAME)?.takeIf { it.isNotBlank() }?.let {
                    prefill.put("name", it)
                }
                put("prefill", prefill)
                put("theme", JSONObject().put("color", "#FF6B00"))
                put("retry", JSONObject().put("enabled", true).put("max_count", 1))
                put("send_sms_hash", true)
            }
            checkout.open(this, options)
        } catch (e: Exception) {
            finishWithError(e.message ?: "Could not open the payment screen.")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        val result = Intent().apply {
            putExtra(RESULT_PAYMENT_ID, razorpayPaymentId)
            putExtra(RESULT_ORDER_ID, data?.orderId ?: intent.getStringExtra(EXTRA_ORDER_ID))
            putExtra(RESULT_SIGNATURE, data?.signature)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        finishWithError(response ?: "Payment failed (code $code).")
    }

    override fun onExternalWalletSelected(walletName: String?, data: PaymentData?) {
        // Flutter parity: info message only, NO verify call.
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(RESULT_EXTERNAL_WALLET, walletName)
        )
        finish()
    }

    private fun finishWithError(message: String) {
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(RESULT_ERROR, message))
        finish()
    }
}
