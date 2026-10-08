package com.omastro.app.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.RechargePack
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val coinBalance by viewModel.coinBalance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cosmic Coins & Wallet", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
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
                    borderColor = SacredGold
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "AVAILABLE BALANCE", color = RadiantAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$coinBalance", color = SacredGold, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Use coins for Kundali, Horoscope & Live Astrologer consultations", color = StarlightWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }

            // Recharge Packs Section
            item {
                Text(text = "Select Recharge Pack", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            items(viewModel.rechargePacks) { pack ->
                RechargePackCard(
                    pack = pack,
                    isProcessing = isProcessing,
                    onBuyClick = { viewModel.rechargePack(pack) }
                )
            }

            // Recent Transactions Section
            item {
                Text(text = "Recent Transactions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            items(transactions) { tx ->
                TransactionRow(tx)
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
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${pack.coins} Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (pack.bonusCoins > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RadiantAmber
                        ) {
                            Text(
                                text = "+${pack.bonusCoins} FREE",
                                color = CosmicDeepBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (pack.badge != null) {
                    Text(text = pack.badge, color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onBuyClick,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "₹${pack.priceInr.toInt()}", color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            Column {
                Text(text = tx.description, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = tx.date, color = StarlightWhite.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            val isCredit = tx.type == "credit"
            Text(
                text = "${if (isCredit) "+" else "-"}${tx.amount} Coins",
                color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFFF5252),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
