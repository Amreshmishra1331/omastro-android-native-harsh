package com.omastro.app.ui.screens.wallet

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.RechargePack
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val lang by LangPrefs.language.collectAsState()
    val walletBalanceInr by viewModel.walletBalanceInr.collectAsState()
    val paidBalanceInr by viewModel.paidBalanceInr.collectAsState()
    val bonusBalanceInr by viewModel.bonusBalanceInr.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()
    val pendingOrder by viewModel.pendingOrder.collectAsState()

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

    // Launches the REAL Razorpay checkout sheet for a freshly issued order.
    val paymentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val order = pendingOrder
        viewModel.clearPendingOrder()
        if (result.resultCode == Activity.RESULT_OK && order != null) {
            val data: Intent? = result.data
            viewModel.verifyPayment(
                orderId = order.order.orderId,
                paymentId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_PAYMENT_ID),
                signature = data?.getStringExtra(RazorpayPaymentActivity.RESULT_SIGNATURE),
                pack = order.pack
            )
        } else {
            val reason = result.data?.getStringExtra(RazorpayPaymentActivity.RESULT_ERROR)
            viewModel.onPaymentAborted(reason)
        }
    }

    val context = LocalContext.current

    // When an order is issued, hand it to the payment activity exactly once.
    LaunchedEffect(pendingOrder) {
        val po = pendingOrder ?: return@LaunchedEffect
        val intent = Intent(context, RazorpayPaymentActivity::class.java).apply {
            putExtra(RazorpayPaymentActivity.EXTRA_KEY_ID, po.order.rzpKeyId)
            putExtra(RazorpayPaymentActivity.EXTRA_ORDER_ID, po.order.orderId)
            putExtra(RazorpayPaymentActivity.EXTRA_AMOUNT_PAISE, po.order.amount)
            putExtra(RazorpayPaymentActivity.EXTRA_CURRENCY, po.order.currency)
            putExtra(RazorpayPaymentActivity.EXTRA_TITLE, "OmAstro AI")
            putExtra(RazorpayPaymentActivity.EXTRA_DESCRIPTION, W.tf("money_credit_desc", "₹${po.pack.creditInr.toInt()}"))
        }
        paymentLauncher.launch(intent)
    }

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

            // Recharge Packs Section
            item {
                Text(text = W.t("money_add_money"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            items(viewModel.rechargePacks) { pack ->
                RechargePackCard(
                    pack = pack,
                    isProcessing = isProcessing,
                    onBuyClick = { viewModel.createOrder(pack) }
                )
            }

            // Bonus slabs + GST note (website Add Money parity).
            item { BonusSlabCard() }

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
    pack: RechargePack,
    isProcessing: Boolean,
    onBuyClick: () -> Unit
) {
    // Website bonus-slab parity: "Pay ₹X → Get ₹Y credit" (+bonus chip).
    // The bonus itself is computed and granted by the server on verify.
    val payLabel = "₹${pack.payInr.toInt()}"
    val creditLabel = "₹${pack.creditInr.toInt()}"
    val bonusLabel = "₹${pack.bonusInr.toInt()}"
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = W.tf("money_pack_pay_get", payLabel, creditLabel),
                        color = StarlightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (pack.bonusInr > 0) {
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
 * Bonus-slab explainer (website Add Money parity): the exact slabs plus the
 * 18% GST note. Informational only — the server owns the real bonus.
 */
@Composable
private fun BonusSlabCard() {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = W.t("money_slab_title"),
                color = RadiantAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                W.t("money_slab_25"),
                W.t("money_slab_50"),
                W.t("money_slab_100"),
                W.t("money_slab_above")
            ).forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "•  ", color = SacredGold, fontSize = 13.sp)
                    Text(text = line, color = StarlightWhite, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
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
