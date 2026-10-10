package com.omastro.app.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.CheckoutItem
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.screens.checkout.CheckoutSheetHost
import com.omastro.app.ui.screens.checkout.CheckoutViewModel
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = viewModel(),
    checkoutVm: CheckoutViewModel = viewModel()
) {
    val lang by LangPrefs.language.collectAsState()
    val walletBalanceInr by viewModel.walletBalanceInr.collectAsState()
    val paidBalanceInr by viewModel.paidBalanceInr.collectAsState()
    val bonusBalanceInr by viewModel.bonusBalanceInr.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()
    val packQuotes by viewModel.packQuotes.collectAsState()
    val packQuotesFailed by viewModel.packQuotesFailed.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Load balance and transactions on screen launch (website parity).
    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
        viewModel.refreshTransactions()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Flutter-exact checkout flow (bottom sheet -> Razorpay -> verify).
    // After a payment finishes, refresh the wallet state (Flutter reloads
    // the home page to reflect the new balance — this is the native
    // equivalent). Wallet-spend logic is untouched.
    CheckoutSheetHost(
        vm = checkoutVm,
        onPaymentFinished = {
            viewModel.refreshBalance()
            viewModel.refreshTransactions()
        },
        onEvent = { viewModel.postMessage(it) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("money_wallet_title"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = W.t("back"), tint = SacredGold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshBalance()
                        viewModel.refreshTransactions()
                        viewModel.loadPackQuotes()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = W.t("money_refresh"), tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmicDeepBlue
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Balance Card
            item {
                CosmicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderGold = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = W.t("money_avail_balance"),
                            color = RadiantAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isProcessing) {
                            CircularProgressIndicator(color = SacredGold, modifier = Modifier.size(36.dp))
                        } else {
                            Text(
                                text = "₹${String.format("%.2f", walletBalanceInr)}",
                                color = SacredGold,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Paid + Bonus breakdown (website parity)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "₹${String.format("%.2f", paidBalanceInr)}",
                                    color = StarlightWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = W.t("money_paid_balance"),
                                    color = StarlightWhite.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "₹${String.format("%.2f", bonusBalanceInr)}",
                                    color = StarlightWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = W.t("money_bonus_balance"),
                                    color = StarlightWhite.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = W.t("money_wallet_hint"),
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Recharge Packs Section — Cycle 4: credit figures are server-driven
            // (POST /checkout/validate-coupon per slab); the old hardcoded
            // "Pay ₹X → Get ₹Y" list is gone.
            item {
                Text(text = W.t("money_add_money"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            items(viewModel.packPayAmounts) { payInr ->
                val quote = packQuotes?.get(payInr)
                RechargePackCard(
                    payInr = payInr,
                    quote = quote,
                    quoteFailed = packQuotesFailed,
                    isProcessing = isProcessing,
                    // Flutter-exact flow: pack -> checkout bottom sheet (server
                    // quote) -> Razorpay -> verify.
                    onBuyClick = {
                        checkoutVm.openSheet(
                            CheckoutViewModel.SheetSelection(
                                item = CheckoutItem.walletPack(payInr),
                                titleFallback = if (quote != null) W.tf(
                                    "money_pack_pay_get",
                                    "₹$payInr",
                                    "₹${quote.creditInr.toInt()}"
                                ) else W.tf("money_pack_pay_only", "₹$payInr"),
                                priceFallback = payInr.toDouble(),
                                isCoinRecharge = true
                            )
                        )
                    }
                )
            }

            // Custom coin recharge (Flutter entry: qty from input, min 100 coins).
            item { CustomCoinsCard(onRecharge = { coins ->
                checkoutVm.openSheet(
                    CheckoutViewModel.SheetSelection(
                        item = CheckoutItem.coinRecharge(coins),
                        titleFallback = W.tf("checkout_coins_card_title", coins.toString()),
                        priceFallback = coins / 100.0,
                        isCoinRecharge = true
                    )
                )
            }) }

            // Bonus slabs + GST note (website Add Money parity) — Cycle 4:
            // rendered from the server quotes, never hardcoded.
            item { BonusSlabCard(quotes = packQuotes, failed = packQuotesFailed) }

            // Recent Transactions Section
            item {
                Text(text = W.t("money_recent_tx"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (transactions.isEmpty()) {
                item {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = W.t("money_tx_empty"),
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(transactions) { tx ->
                    TransactionRow(tx)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RechargePackCard(
    payInr: Int,
    quote: PackQuote?,
    quoteFailed: Boolean,
    isProcessing: Boolean,
    onBuyClick: () -> Unit
) {
    // Cycle 4: credit figures are server-driven (PackQuote from
    // POST /checkout/validate-coupon). Until the quote arrives the card shows
    // "Pay ₹X" with a loading note; on failure an honest note replaces the
    // credit line — the app never invents a credit amount.
    val payLabel = "₹$payInr"
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when {
                    quote != null -> {
                        val creditLabel = "₹${quote.creditInr.toInt()}"
                        val bonusLabel = "₹${quote.bonusInr.toInt()}"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = W.tf("money_pack_pay_get", payLabel, creditLabel),
                                color = StarlightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (quote.bonusInr > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = RadiantAmber
                                ) {
                                    Text(
                                        text = W.tf("money_pack_bonus", bonusLabel),
                                        color = CosmicDeepBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    quoteFailed -> {
                        Text(
                            text = payLabel,
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = W.t("money_pack_quote_fail"),
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    else -> {
                        Text(
                            text = payLabel,
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = W.t("money_pack_loading"),
                            color = StarlightWhite.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onBuyClick,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = CosmicDeepBlue, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = W.tf("money_pack_btn", payLabel),
                        color = CosmicDeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Bonus-slab explainer (website Add Money parity) — Cycle 4: rows are
 * rendered from the server quotes, never hardcoded. If quotes failed, an
 * honest unavailable note shows instead of stale numbers.
 */
@Composable
private fun BonusSlabCard(quotes: Map<Int, PackQuote>?, failed: Boolean) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = W.t("money_slab_title"),
                color = RadiantAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                quotes != null && quotes.isNotEmpty() -> {
                    quotes.entries.sortedBy { it.key }.forEach { (pay, q) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "•  ", color = SacredGold, fontSize = 13.sp)
                            Text(
                                text = W.tf("money_pack_pay_get", "₹$pay", "₹${q.creditInr.toInt()}"),
                                color = StarlightWhite,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                failed -> {
                    Text(
                        text = W.t("money_slabs_unavailable"),
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = W.t("money_pack_loading"),
                        color = StarlightWhite.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = W.t("money_gst_note"),
                color = StarlightWhite.copy(alpha = 0.55f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Custom coin recharge (Flutter entry point: qty from input, min 100 coins).
 * The server quote in the checkout sheet is authoritative — any coins value
 * maps to the wallet top-up slabs (coins = pay-rupees x 100).
 */
@Composable
private fun CustomCoinsCard(onRecharge: (Int) -> Unit) {
    var coinsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = W.t("checkout_coins_title"),
                color = StarlightWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = coinsText,
                    onValueChange = {
                        coinsText = it.filter { ch -> ch.isDigit() }.take(7)
                        error = null
                    },
                    placeholder = {
                        Text(
                            W.t("checkout_coins_hint"),
                            color = StarlightWhite.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite,
                        focusedBorderColor = SacredGold,
                        unfocusedBorderColor = StarlightWhite.copy(alpha = 0.25f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val coins = coinsText.toIntOrNull() ?: 0
                        if (coins < 100) {
                            error = W.t("checkout_coins_min")
                        } else {
                            onRecharge(coins)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = W.t("checkout_coins_recharge"),
                        color = CosmicDeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            error?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, color = Color(0xFFFF5252), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tx.description, color = StarlightWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = tx.date, color = StarlightWhite.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            val isCredit = tx.type == "credit"
            Text(
                text = "${if (isCredit) "+" else "-"}₹${String.format("%.2f", tx.amount / 100.0)}",
                color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFFF5252),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
