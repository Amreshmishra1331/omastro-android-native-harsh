package com.omastro.app.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.CheckoutItem
import com.omastro.app.data.model.CheckoutOrder
import com.omastro.app.data.model.PriceBreakdown
import com.omastro.app.data.repository.CheckoutRepository
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.i18n.W
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Flutter-exact checkout state machine.
 *
 * Mirrors Flutter's _NativeCheckoutBottomSheetState + _initNativePayment:
 *  - openSheet() fetches the price breakdown (validate-coupon, empty coupon).
 *  - applyCoupon()/removeCoupon() re-validate through the same endpoint.
 *  - proceedToPay() has the double-tap guard ([isPaymentInProgress], mirroring
 *    Flutter's _isPaymentInProgress) and issues the order via create-order.
 *  - The UI launches RazorpayPaymentActivity with the issued order; the result
 *    comes back to onRazorpaySuccess()/onRazorpayCancelled()/onExternalWallet().
 *  - Success -> "Verifying payment..." -> POST /checkout/verify -> success:
 *    "Payment Successful!" + [onPaymentFinished] (refresh account/wallet state,
 *    mirroring Flutter's home reload); verify-fail: "Payment verification
 *    failed." + [onPaymentFinished] anyway (balance may have changed).
 *  - Cancel/error -> "Payment was cancelled." / "Payment: <gateway message>".
 *  - External wallet -> info message only, NO verify call (Flutter parity).
 */
class CheckoutViewModel : ViewModel() {

    private val repo: CheckoutRepository = AppContainer.checkoutRepository

    /** Refreshed by the host screen after a payment finishes (balance/plan). */
    var onPaymentFinished: (() -> Unit)? = null

    /** What the sheet is selling; null = sheet closed. */
    data class SheetSelection(
        val item: CheckoutItem,
        /** Shown before the server quote arrives (server title is authoritative). */
        val titleFallback: String,
        val priceFallback: Double,
        /** True for product 704 (coin icon), false for plans (crown icon). */
        val isCoinRecharge: Boolean
    )

    /** A freshly issued order awaiting the Razorpay sheet; consumed once by the UI. */
    data class PendingCheckout(
        val order: CheckoutOrder,
        val item: CheckoutItem
    )

    private val _sheet = MutableStateFlow<SheetSelection?>(null)
    val sheet: StateFlow<SheetSelection?> = _sheet.asStateFlow()

    private val _quote = MutableStateFlow<PriceBreakdown?>(null)
    val quote: StateFlow<PriceBreakdown?> = _quote.asStateFlow()

    private val _isLoadingQuote = MutableStateFlow(false)
    val isLoadingQuote: StateFlow<Boolean> = _isLoadingQuote.asStateFlow()

    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<String?>(null)
    val appliedCoupon: StateFlow<String?> = _appliedCoupon.asStateFlow()

    private val _promoMessage = MutableStateFlow<String?>(null)
    val promoMessage: StateFlow<String?> = _promoMessage.asStateFlow()

    private val _isPromoError = MutableStateFlow(false)
    val isPromoError: StateFlow<Boolean> = _isPromoError.asStateFlow()

    /** Double-tap guard on payment init (Flutter: _isPaymentInProgress). */
    private val _isPaymentInProgress = MutableStateFlow(false)
    val isPaymentInProgress: StateFlow<Boolean> = _isPaymentInProgress.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _pendingCheckout = MutableStateFlow<PendingCheckout?>(null)
    val pendingCheckout: StateFlow<PendingCheckout?> = _pendingCheckout.asStateFlow()

    private val _event = MutableStateFlow<String?>(null)
    val event: StateFlow<String?> = _event.asStateFlow()

    /** Open the sheet and fetch the server quote (empty coupon, like Flutter). */
    fun openSheet(selection: SheetSelection) {
        if (selection.item.planKey.isNullOrBlank()
            && (selection.item.productId == null || selection.item.productId <= 0)
            && (selection.item.customCoins == null || selection.item.customCoins <= 0)
        ) {
            _event.value = W.t("checkout_invalid_plan")
            return
        }
        _sheet.value = selection
        _quote.value = null
        _appliedCoupon.value = null
        _promoMessage.value = null
        _isPromoError.value = false
        fetchQuote(coupon = "")
    }

    fun closeSheet() {
        if (_isPaymentInProgress.value || _isVerifying.value) return
        _sheet.value = null
        _quote.value = null
        _pendingCheckout.value = null
    }

    fun clearPendingCheckout() {
        _pendingCheckout.value = null
    }

    /**
     * Pops the sheet when the Razorpay activity opens (Flutter:
     * Navigator.pop() then _initNativePayment). The payment-in-progress
     * guard stays on until the activity result is handled.
     */
    fun dismissForPayment() {
        _sheet.value = null
    }

    fun clearEvent() {
        _event.value = null
    }

    private fun fetchQuote(coupon: String) {
        val sel = _sheet.value ?: return
        val isCouponApply = coupon.isNotBlank()
        viewModelScope.launch {
            if (isCouponApply) _isValidating.value = true else _isLoadingQuote.value = true
            _promoMessage.value = null
            repo.getQuote(sel.item, coupon)
                .onSuccess { q ->
                    _quote.value = q
                    if (isCouponApply) {
                        _appliedCoupon.value = q.couponCode.ifBlank { coupon }
                        _promoMessage.value = q.message?.takeIf { it.isNotBlank() }
                            ?: W.t("checkout_promo_applied")
                        _isPromoError.value = false
                    } else {
                        _appliedCoupon.value = null
                        _promoMessage.value = null
                        _isPromoError.value = false
                    }
                }
                .onFailure { err ->
                    // Mirror Flutter: a failed promo validation keeps the old
                    // breakdown and only shows the error message.
                    if (isCouponApply) {
                        _promoMessage.value = err.message?.takeIf { it.isNotBlank() }
                            ?: W.t("checkout_promo_invalid")
                        _isPromoError.value = true
                    } else {
                        _event.value = err.message ?: W.t("checkout_quote_fail")
                    }
                }
            _isLoadingQuote.value = false
            _isValidating.value = false
        }
    }

    fun applyCoupon(code: String) {
        if (_isValidating.value) return
        fetchQuote(code)
    }

    fun removeCoupon() {
        fetchQuote("")
    }

    /** Pay button — double-tap guarded, issues the order via create-order. */
    fun proceedToPay() {
        if (_isPaymentInProgress.value || _isLoadingQuote.value) return
        val sel = _sheet.value ?: return
        viewModelScope.launch {
            _isPaymentInProgress.value = true
            _event.value = W.t("checkout_opening")
            repo.createOrder(sel.item, _appliedCoupon.value)
                .onSuccess { order ->
                    _pendingCheckout.value = PendingCheckout(order, sel.item)
                }
                .onFailure { err ->
                    _isPaymentInProgress.value = false
                    _event.value = (err.message ?: "").ifBlank {
                        W.t("checkout_could_not_start")
                    }
                }
        }
    }

    /** Called when RazorpayPaymentActivity returns RESULT_OK. */
    fun onRazorpaySuccess(
        pending: PendingCheckout,
        paymentId: String?,
        rzpOrderId: String?,
        signature: String?
    ) {
        clearPendingCheckout()
        if (paymentId.isNullOrBlank() || signature.isNullOrBlank()) {
            _isPaymentInProgress.value = false
            _event.value = W.t("checkout_verify_missing")
            return
        }
        viewModelScope.launch {
            _isVerifying.value = true
            _event.value = W.t("checkout_verifying")
            repo.verifyPayment(
                orderId = pending.order.orderId,
                paymentId = paymentId,
                rzpOrderId = rzpOrderId.orEmpty(),
                signature = signature
            ).onSuccess { msg ->
                _event.value = msg.ifBlank { W.t("checkout_success") }
                _sheet.value = null
                onPaymentFinished?.invoke()
            }.onFailure {
                // Flutter parity: verification failure still refreshes the
                // home state (the capture may have landed server-side).
                _event.value = W.t("checkout_verify_failed")
                onPaymentFinished?.invoke()
            }
            _isVerifying.value = false
            _isPaymentInProgress.value = false
        }
    }

    /** Called when the user cancels or the gateway errors. */
    fun onRazorpayCancelled(reason: String?) {
        clearPendingCheckout()
        _isPaymentInProgress.value = false
        _event.value = if (!reason.isNullOrBlank()) {
            W.tf("checkout_payment_err", reason)
        } else {
            W.t("checkout_cancelled")
        }
    }

    /** External-wallet selection: info only, NO verify call (Flutter parity). */
    fun onExternalWallet(walletName: String?) {
        clearPendingCheckout()
        _isPaymentInProgress.value = false
        _event.value = W.tf(
            "checkout_external_wallet",
            walletName?.takeIf { it.isNotBlank() } ?: "-"
        )
    }
}
