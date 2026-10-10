package com.omastro.app.data.model

/**
 * Flutter-exact payment flow (spec: /home/hatch/workspace/flutter-payment-spec.md,
 * verified against omastro-flutter-app @ 8628ab8 lib/main.dart 2026-10-10).
 *
 * What the user is buying. Mirrors Flutter's _openNativeCheckoutSheet selection:
 *  - membership plan  -> POST {plan: planKey}
 *  - coin / wallet recharge -> POST {product_id: 704, coins, qty}
 *
 * Server-verified 2026-10-10 (live probe of /checkout/validate-coupon):
 *  - product_id 704 + coins = WALLET TOP-UP; coins = pay-rupees x 100, e.g.
 *    coins=2500  -> pay Rs.25 (+GST 4.50)  -> Rs.35 credit
 *    coins=5000  -> pay Rs.50 (+GST 9.00)  -> Rs.80 credit
 *    coins=10000 -> pay Rs.100 (+GST 18.00)-> Rs.200 credit
 *    coins=20000 -> pay Rs.200 (+GST 36.00)-> Rs.400 credit
 *  - product ids 208/209/210/211 = pro_monthly/pro_yearly/max_monthly/max_yearly
 *    (Rs.499 / 4999 / 999 / 9999 base, 18% GST added by the server).
 */
data class CheckoutItem(
    val planKey: String? = null,
    val productId: Int? = null,
    /** Server "coins" for product 704 (wallet top-up). Null for plan purchases. */
    val customCoins: Int? = null
) {
    companion object {
        fun membershipPlan(planKey: String) = CheckoutItem(planKey = planKey)

        /** Wallet top-up pack: pay [payRupees] INR -> server coins = payRupees x 100. */
        fun walletPack(payRupees: Int) =
            CheckoutItem(productId = WALLET_TOPUP_PRODUCT_ID, customCoins = payRupees * 100)

        /** Custom coin recharge (Flutter entry: qty from input, min 100 coins). */
        fun coinRecharge(coins: Int) =
            CheckoutItem(productId = WALLET_TOPUP_PRODUCT_ID, customCoins = coins)
    }
}

/** WooCommerce product id the server uses for wallet top-up / coin recharge. */
const val WALLET_TOPUP_PRODUCT_ID = 704

/** Fallback Razorpay publishable key (Flutter main.dart:848-852) — used only
 *  when the server omits rzp_key_id in the create-order response. */
const val RAZORPAY_FALLBACK_KEY_ID = "rzp_live_T8gzsClejXQi48"

/** Fallback amount (paise) when the server omits it (Flutter default 49900). */
const val CHECKOUT_AMOUNT_FALLBACK_PAISE = 49900

/**
 * Membership plan catalog. [planKey] is what goes in the create-order body
 * (Flutter's primary plan path); [productId] is the equivalent WooCommerce id.
 * [displayPriceInr] is the pre-quote estimate — the SERVER quote is
 * authoritative (e.g. server bills Pro annual at Rs.4999, not the Rs.3999
 * Flutter UI default).
 */
data class MembershipPlan(
    val planKey: String,
    val productId: Int,
    /** i18n key for the card title. */
    val titleKey: String,
    val displayPriceInr: Double,
    val billingKey: String
)

val MEMBERSHIP_PLANS = listOf(
    MembershipPlan("pro_monthly", 208, "checkout_plan_pro_mo", 499.0, "checkout_billing_monthly"),
    MembershipPlan("pro_yearly", 209, "checkout_plan_pro_yr", 4999.0, "checkout_billing_yearly"),
    MembershipPlan("max_monthly", 210, "checkout_plan_max_mo", 999.0, "checkout_billing_monthly"),
    MembershipPlan("max_yearly", 211, "checkout_plan_max_yr", 9999.0, "checkout_billing_yearly"),
)

/**
 * Server price breakdown from POST /checkout/validate-coupon.
 * Real shape: {success, data:{item_title, subtotal, subtotal_display,
 * discount_amount, discount_display, tax_amount, tax_display, tax_label,
 * total, total_display, total_paise, coupon_code, coupon_error,
 * coupon_message, message}}. On an invalid coupon the server returns
 * {success:false, code:"coupon_invalid", message, calculation:{...same...}}.
 * Numbers may arrive as JSON numbers or numeric strings — parse tolerantly.
 */
data class PriceBreakdown(
    val itemTitle: String,
    val subtotal: Double,
    val subtotalDisplay: String?,
    val discountAmount: Double,
    val discountDisplay: String?,
    val taxAmount: Double,
    val taxDisplay: String?,
    val taxLabel: String,
    val total: Double,
    val totalDisplay: String?,
    val totalPaise: Int,
    val couponCode: String,
    val message: String?
)

/**
 * Issued order from POST /checkout/create-order.
 * Real shape: {success, data:{order_id (WC int), rzp_key_id, amount (paise),
 * rzp_order_id, plan_title, user_email, user_name}}.
 */
data class CheckoutOrder(
    /** WooCommerce order id (server sends an int; kept Long for safety). */
    val orderId: Long,
    val rzpKeyId: String,
    val amountPaise: Int,
    /** Razorpay order id — may be empty; omitted from checkout options then. */
    val rzpOrderId: String,
    val planTitle: String,
    val userEmail: String,
    val userName: String
)
