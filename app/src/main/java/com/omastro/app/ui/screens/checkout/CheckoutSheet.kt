package com.omastro.app.ui.screens.checkout

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.PriceBreakdown
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.screens.wallet.RazorpayPaymentActivity

// Flutter-exact sheet palette (omastro-flutter-app _NativeCheckoutBottomSheet).
private val SheetBg = Color(0xFF1E0A00)
private val SheetOrange = Color(0xFFFF6B00)
private val SheetOrangeLight = Color(0xFFFF8C00)
private val SheetGold = Color(0xFFFFD700)
private val SheetAmber = Color(0xFFFFB74D)
private val SheetGreen = Color(0xFF10B981)

/**
 * Hosts the Flutter-exact checkout bottom sheet plus the Razorpay activity
 * launcher. Embed once per screen that sells something (Wallet, Plan tab).
 *
 * @param onPaymentFinished refresh account/wallet state after a payment
 *        finishes (mirrors Flutter's home reload after success/verify-fail).
 * @param onEvent surfaces transient messages (the host shows them in its snackbar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheetHost(
    vm: CheckoutViewModel,
    onPaymentFinished: () -> Unit,
    onEvent: (String) -> Unit
) {
    val sheet by vm.sheet.collectAsState()
    val pending by vm.pendingCheckout.collectAsState()
    val event by vm.event.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.onPaymentFinished = onPaymentFinished }
    LaunchedEffect(event) {
        event?.let {
            onEvent(it)
            vm.clearEvent()
        }
    }

    val paymentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val po = pending
        vm.clearPendingCheckout()
        when {
            result.resultCode == Activity.RESULT_OK && po != null -> {
                val data: Intent? = result.data
                vm.onRazorpaySuccess(
                    pending = po,
                    paymentId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_PAYMENT_ID),
                    rzpOrderId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_ORDER_ID),
                    signature = data?.getStringExtra(RazorpayPaymentActivity.RESULT_SIGNATURE)
                )
            }
            result.data?.hasExtra(RazorpayPaymentActivity.RESULT_EXTERNAL_WALLET) == true ->
                vm.onExternalWallet(
                    result.data?.getStringExtra(RazorpayPaymentActivity.RESULT_EXTERNAL_WALLET)
                )
            else ->
                vm.onRazorpayCancelled(
                    result.data?.getStringExtra(RazorpayPaymentActivity.RESULT_ERROR)
                )
        }
    }

    // When an order is issued, hand it to the payment activity exactly once.
    // The sheet is popped first (Flutter: Navigator.pop, then _initNativePayment).
    LaunchedEffect(pending) {
        val po = pending ?: return@LaunchedEffect
        vm.dismissForPayment()
        val intent = Intent(context, RazorpayPaymentActivity::class.java).apply {
            putExtra(RazorpayPaymentActivity.EXTRA_KEY_ID, po.order.rzpKeyId)
            putExtra(RazorpayPaymentActivity.EXTRA_ORDER_ID, po.order.rzpOrderId)
            putExtra(RazorpayPaymentActivity.EXTRA_AMOUNT_PAISE, po.order.amountPaise)
            putExtra(RazorpayPaymentActivity.EXTRA_CURRENCY, "INR")
            putExtra(RazorpayPaymentActivity.EXTRA_TITLE, "Om Astro AI")
            putExtra(RazorpayPaymentActivity.EXTRA_DESCRIPTION, po.order.planTitle)
            putExtra(RazorpayPaymentActivity.EXTRA_EMAIL, po.order.userEmail)
            putExtra(RazorpayPaymentActivity.EXTRA_NAME, po.order.userName)
        }
        paymentLauncher.launch(intent)
    }

    val selection = sheet
    if (selection != null) {
        ModalBottomSheet(
            onDismissRequest = { vm.closeSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SheetBg,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(width = 44.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                )
            }
        ) {
            CheckoutSheetContent(vm = vm, selection = selection)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutSheetContent(
    vm: CheckoutViewModel,
    selection: CheckoutViewModel.SheetSelection
) {
    // Recompose on language change (W.t reads the active language).
    val lang by LangPrefs.language.collectAsState()
    @Suppress("UNUSED_VARIABLE") val unusedLang = lang

    val quote by vm.quote.collectAsState()
    val isLoadingQuote by vm.isLoadingQuote.collectAsState()
    val isValidating by vm.isValidating.collectAsState()
    val isPaymentInProgress by vm.isPaymentInProgress.collectAsState()
    val appliedCoupon by vm.appliedCoupon.collectAsState()
    val promoMessage by vm.promoMessage.collectAsState()
    val isPromoError by vm.isPromoError.collectAsState()

    val promoCtrl = remember { mutableStateOf("") }
    val q: PriceBreakdown? = quote

    val displayTitle = q?.itemTitle?.takeIf { it.isNotBlank() } ?: selection.titleFallback
    val baseAmount = q?.subtotal ?: selection.priceFallback
    val baseDisplay = q?.subtotalDisplay ?: "₹${"%.2f".format(baseAmount)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = SheetOrange, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = W.t("checkout_title"),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.closeSheet() }) {
                Icon(Icons.Default.Close, contentDescription = W.t("Close"), tint = Color.White.copy(alpha = 0.6f))
            }
        }

        // Item card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.35f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(SheetOrange, SheetOrangeLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (selection.isCoinRecharge) Icons.Default.MonetizationOn else Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = W.tf("checkout_base", baseDisplay),
                        color = SheetAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Promo code card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (appliedCoupon != null) SheetGreen.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (appliedCoupon != null) SheetGreen.copy(alpha = 0.4f) else SheetOrange.copy(alpha = 0.25f)
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (appliedCoupon == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = SheetOrangeLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = promoCtrl.value,
                            onValueChange = { promoCtrl.value = it },
                            placeholder = {
                                Text(W.t("checkout_promo_hint"), color = Color.White.copy(alpha = 0.38f), fontSize = 12.sp)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Text
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontWeight = FontWeight.Bold, fontSize = 13.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { vm.applyCoupon(promoCtrl.value.trim().uppercase()) },
                            enabled = !isValidating && promoCtrl.value.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SheetOrange),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(W.t("checkout_apply"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SheetGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = W.tf("checkout_promo_code", appliedCoupon!!),
                                color = SheetGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = W.tf(
                                    "checkout_discount",
                                    q?.discountDisplay ?: "₹${"%.2f".format(q?.discountAmount ?: 0.0)}"
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                promoCtrl.value = ""
                                vm.removeCoupon()
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = W.t("checkout_remove_promo"),
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                promoMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        color = if (isPromoError) Color(0xFFFF5252) else SheetGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Price breakdown
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                BreakdownRow(
                    label = W.t("checkout_subtotal"),
                    value = q?.subtotalDisplay ?: "₹${"%.2f".format(q?.subtotal ?: baseAmount)}",
                    labelColor = Color.White.copy(alpha = 0.7f)
                )
                if ((q?.discountAmount ?: 0.0) > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    BreakdownRow(
                        label = W.t("checkout_promo_discount"),
                        value = "-${q?.discountDisplay ?: "₹${"%.2f".format(q?.discountAmount ?: 0.0)}"}",
                        labelColor = SheetGreen,
                        valueColor = SheetGreen,
                        bold = true
                    )
                }
                if ((q?.taxAmount ?: 0.0) > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    BreakdownRow(
                        label = "${q?.taxLabel ?: W.t("checkout_tax_default")}:",
                        value = "+${q?.taxDisplay ?: "₹${"%.2f".format(q?.taxAmount ?: 0.0)}"}",
                        labelColor = Color.White.copy(alpha = 0.7f)
                    )
                }
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.07f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = W.t("checkout_total_payable"),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoadingQuote || q == null) {
                        CircularProgressIndicator(color = SheetGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = q?.totalDisplay ?: "₹${"%.2f".format(q?.total ?: baseAmount)}",
                            color = SheetGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Pay button (double-tap guarded in the ViewModel, like Flutter).
        Button(
            onClick = { vm.proceedToPay() },
            enabled = !isLoadingQuote && !isPaymentInProgress,
            colors = ButtonDefaults.buttonColors(containerColor = SheetOrange),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isPaymentInProgress) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = W.tf(
                        "checkout_pay_via",
                        q?.totalDisplay ?: "₹${"%.2f".format(q?.total ?: baseAmount)}"
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.38f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = W.t("checkout_secure"),
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color = Color.White,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = labelColor, fontSize = 13.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
