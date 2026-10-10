package com.omastro.app.ui.screens.refer

import com.omastro.app.ui.i18n.W
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Refer & Earn — real wallet data only.
 *
 * No dedicated referral REST route exists on the server (verified against the
 * live wp-json/omastro/v1 index): the referral code / invite flow is NOT
 * exposed by the API. This screen therefore filters the user's real wallet
 * history (GET /wallet/transactions) for referral-credit entries and shows
 * only those. If there are none, it shows an honest empty state — never fake
 * referral numbers.
 */
class ReferEarnViewModel : ViewModel() {
    private val repo = AppContainer.walletRepository

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val referralTransactions: List<WalletTransaction>
        get() = _transactions.value.filter {
            it.description.contains("referral", ignoreCase = true) ||
                it.type.contains("referral", ignoreCase = true)
        }

    val referralEarningsInr: Double
        get() = referralTransactions
            .filter { it.type.equals("credit", ignoreCase = true) }
            .sumOf { it.amount } / 100.0

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getTransactions(limit = 100, offset = 0)
                .onSuccess { _transactions.value = it }
                .onFailure { e -> _error.value = e.message ?: W.t("refer_load_error") }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferEarnScreen(
    onBack: () -> Unit,
    viewModel: ReferEarnViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    // Reading the transactions flow forces recomposition when the list changes.
    val transactions by viewModel.transactions.collectAsState()
    val referralTxs = remember(transactions) {
        transactions.filter {
            it.description.contains("referral", ignoreCase = true) ||
                it.type.contains("referral", ignoreCase = true)
        }
    }
    val earningsInr = remember(referralTxs) {
        referralTxs.filter { it.type.equals("credit", ignoreCase = true) }
            .sumOf { it.amount } / 100.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("Refer & Earn"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = W.t("chat_back_cd"), tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
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

            // ============ EARNINGS CARD (real wallet data only) ============
            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                W.t("refer_total_earnings"),
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                            Text(
                                "₹%.2f".format(earningsInr),
                                color = StarlightWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ============ HOW IT WORKS (generic, verifiable rules only) ============
            item {
                CosmicCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            W.t("refer_how_title"),
                            color = StarlightWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            W.t("refer_how_body"),
                            color = StarlightWhite.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // ============ REAL REFERRAL TRANSACTIONS ============
            item {
                Text(W.t("refer_history_title"), color = StarlightWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            when {
                isLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SacredGold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                W.t("refer_loading"),
                                color = StarlightWhite.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                error != null -> item {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    error ?: W.t("refer_load_error"),
                                    color = StarlightWhite.copy(alpha = 0.75f),
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text(W.t("Retry"), color = SacredGold)
                            }
                        }
                    }
                }
                referralTxs.isEmpty() -> item {
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                tint = StarlightWhite.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                W.t("refer_empty"),
                                color = StarlightWhite.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
                else -> items(referralTxs, key = { it.id }) { tx ->
                    CosmicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tx.description.ifBlank { W.t("refer_tx_fallback") },
                                    color = StarlightWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (tx.date.isNotBlank()) {
                                    Text(
                                        tx.date,
                                        color = StarlightWhite.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            val isCredit = tx.type.equals("credit", ignoreCase = true)
                            Text(
                                "${if (isCredit) "+" else "-"}₹%.2f".format(tx.amount / 100.0),
                                color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
