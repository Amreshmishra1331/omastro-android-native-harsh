package com.omastro.app.ui.screens.wallet

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.CheckoutItem
import com.omastro.app.data.model.WalletTransaction
import com.omastro.app.ui.components.*
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.screens.checkout.CheckoutSheetHost
import com.omastro.app.ui.screens.checkout.CheckoutViewModel
import com.omastro.app.ui.theme.*

/**
 * WalletScreen — Premium OmAstro Wallet & Subscription Experience
 *
 * Features:
 * 1. CosmicTopBar "मेरा वॉलेट" + back
 * 2. Balance Hero PremiumCard: large ₹{balance}, Paid+Bonus sub-values, Add Money button
 * 3. Quick Recharge LazyRow: ₹99/₹199/₹499/₹999/₹1999 cards with bonus badge
 * 4. Membership Cards LazyRow: Pro (violet gradient, ₹299/mo) + Max (gold gradient, ₹599/mo)
 * 5. Transaction History list: credit(green)/debit(red) with icon, description, amount, date
 * 6. Recharge ModalBottomSheet: package amount + coupon + GST breakdown + Razorpay pay button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    viewModel: WalletViewModel = viewModel(),
    checkoutVm: CheckoutViewModel = viewModel()
) {
    val lang by LangPrefs.language.collectAsState()
    val hi = lang == "hi"

    val walletBalanceInr by viewModel.walletBalanceInr.collectAsState()
    val paidBalanceInr by viewModel.paidBalanceInr.collectAsState()
    val bonusBalanceInr by viewModel.bonusBalanceInr.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet state for Recharge
    var showRechargeSheet by remember { mutableStateOf(false) }
    var initialRechargeAmount by remember { mutableIntStateOf(199) }

    // Load initial wallet balance & transactions
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

    // Razorpay Checkout Host integration
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
            CosmicTopBar(
                title = if (hi) "मेरा वॉलेट" else "My Wallet",
                subtitle = if (hi) "बैलेंस, रिचार्ज व सब्सक्रिप्शन" else "Balance & Memberships",
                onBackClick = onBack,
                showLogo = false,
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshBalance()
                            viewModel.refreshTransactions()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SacredGold
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmicBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // -------------------------------------------------------------
            // 2. Balance Hero PremiumCard: large ₹{balance}, Paid+Bonus, Add Money
            // -------------------------------------------------------------
            item {
                BalanceHeroCard(
                    walletBalance = walletBalanceInr,
                    paidBalance = paidBalanceInr,
                    bonusBalance = bonusBalanceInr,
                    isProcessing = isProcessing,
                    onAddMoneyClick = {
                        initialRechargeAmount = 199
                        showRechargeSheet = true
                    },
                    hi = hi
                )
            }

            // -------------------------------------------------------------
            // 3. Quick Recharge LazyRow: ₹99, ₹199, ₹499, ₹999, ₹1999 with bonus badge
            // -------------------------------------------------------------
            item {
                QuickRechargeSection(
                    onSelectAmount = { amount ->
                        initialRechargeAmount = amount
                        showRechargeSheet = true
                    },
                    hi = hi
                )
            }

            // -------------------------------------------------------------
            // 4. Membership Cards LazyRow: Pro (₹299/mo) & Max (₹599/mo)
            // -------------------------------------------------------------
            item {
                MembershipSection(
                    onSelectPro = {
                        checkoutVm.openSheet(
                            CheckoutViewModel.SheetSelection(
                                item = CheckoutItem.membershipPlan("pro_monthly"),
                                titleFallback = if (hi) "OmAstro Pro मासिक प्लान" else "OmAstro Pro Monthly Plan",
                                priceFallback = 299.0,
                                isCoinRecharge = false
                            )
                        )
                    },
                    onSelectMax = {
                        checkoutVm.openSheet(
                            CheckoutViewModel.SheetSelection(
                                item = CheckoutItem.membershipPlan("max_monthly"),
                                titleFallback = if (hi) "OmAstro Max VIP मासिक प्लान" else "OmAstro Max VIP Monthly Plan",
                                priceFallback = 599.0,
                                isCoinRecharge = false
                            )
                        )
                    },
                    hi = hi
                )
            }

            // -------------------------------------------------------------
            // 5. Transaction History List: Credit (green) / Debit (red)
            // -------------------------------------------------------------
            item {
                TransactionHistoryHeader(
                    onRefresh = { viewModel.refreshTransactions() },
                    hi = hi
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(hi = hi)
                }
            } else {
                items(transactions) { tx ->
                    TransactionItemCard(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // -------------------------------------------------------------------------
    // 6. Recharge ModalBottomSheet
    // -------------------------------------------------------------------------
    if (showRechargeSheet) {
        RechargeBottomSheet(
            initialAmount = initialRechargeAmount,
            onDismiss = { showRechargeSheet = false },
            onProceedToPay = { selectedAmount, couponCode ->
                showRechargeSheet = false
                checkoutVm.openSheet(
                    CheckoutViewModel.SheetSelection(
                        item = CheckoutItem.walletPack(selectedAmount),
                        titleFallback = if (hi) "OmAstro वॉलेट टॉप-अप — ₹$selectedAmount" else "OmAstro Wallet Top-up — ₹$selectedAmount",
                        priceFallback = selectedAmount.toDouble(),
                        isCoinRecharge = true
                    )
                )
            },
            hi = hi
        )
    }
}

// -----------------------------------------------------------------------------
// Component 2: Balance Hero PremiumCard
// -----------------------------------------------------------------------------
@Composable
private fun BalanceHeroCard(
    walletBalance: Double,
    paidBalance: Double,
    bonusBalance: Double,
    isProcessing: Boolean,
    onAddMoneyClick: () -> Unit,
    hi: Boolean
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SacredGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (hi) "उपलब्ध वॉलेट बैलेंस" else "AVAILABLE BALANCE",
                        color = SacredGoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AuspiciousGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AuspiciousGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (hi) "सक्रिय" else "Active",
                        color = AuspiciousGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Formatted Balance ₹{balance}
            if (isProcessing) {
                CircularProgressIndicator(
                    color = SacredGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Text(
                    text = "₹${String.format("%.2f", walletBalance)}",
                    color = SacredGoldGlow,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Paid + Bonus sub-values
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CosmicSurface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (hi) "मुख्य राशि (Paid)" else "Paid Balance",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format("%.2f", paidBalance)}",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(CosmicCardBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎁 ", fontSize = 11.sp)
                            Text(
                                text = if (hi) "बोनस (Bonus)" else "Bonus Balance",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${String.format("%.2f", bonusBalance)}",
                            color = AuspiciousGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Money Button
            GradientButton(
                text = if (hi) "+ पैसे जोड़ें (Add Money)" else "+ Add Money to Wallet",
                onClick = onAddMoneyClick,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddCard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component 3: Quick Recharge LazyRow: ₹99 / ₹199 / ₹499 / ₹999 / ₹1999
// -----------------------------------------------------------------------------
private data class QuickRechargePack(
    val amount: Int,
    val bonus: Int,
    val badge: String
)

@Composable
private fun QuickRechargeSection(
    onSelectAmount: (Int) -> Unit,
    hi: Boolean
) {
    val packs = listOf(
        QuickRechargePack(99, 10, if (hi) "+₹10 बोनस" else "+₹10 Bonus"),
        QuickRechargePack(199, 30, if (hi) "लोकप्रिय • +₹30" else "Popular • +₹30"),
        QuickRechargePack(499, 100, if (hi) "सर्वश्रेष्ठ • +₹100" else "Best • +₹100"),
        QuickRechargePack(999, 250, if (hi) "+₹250 बोनस" else "+₹250 Bonus"),
        QuickRechargePack(1999, 600, if (hi) "महा बचत • +₹600" else "Mega • +₹600")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hi) "त्वरित रिचार्ज पैक्स" else "Quick Recharge Packs",
                color = SacredGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (hi) "अतिरिक्त बोनस सहित" else "With Extra Bonus",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(packs) { pack ->
                QuickRechargeCard(pack = pack, onClick = { onSelectAmount(pack.amount) }, hi = hi)
            }
        }
    }
}

@Composable
private fun QuickRechargeCard(
    pack: QuickRechargePack,
    onClick: () -> Unit,
    hi: Boolean
) {
    Box(
        modifier = Modifier
            .width(118.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = SacredGold.copy(alpha = 0.25f),
                spotColor = SacredGold.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CosmicCardBg, CosmicSurface)
                )
            )
            .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Badge at Top
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SacredGold.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = pack.badge,
                    color = SacredGoldLight,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "₹${pack.amount}",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (hi) "पाएं ₹${pack.amount + pack.bonus}" else "Get ₹${pack.amount + pack.bonus}",
                color = AuspiciousGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SacredGold,
                modifier = Modifier.fillMaxWidth().height(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (hi) "रिचार्ज" else "Recharge",
                        color = CosmicBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 4: Membership Cards LazyRow (Pro & Max)
// -----------------------------------------------------------------------------
@Composable
private fun MembershipSection(
    onSelectPro: () -> Unit,
    onSelectMax: () -> Unit,
    hi: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hi) "प्रीमियम सदस्यता योजनाएं" else "VIP Membership Plans",
                color = SacredGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (hi) "असीमित लाभ" else "Unlimited Benefits",
                color = CosmicVioletLight,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            // Pro Plan Card (Violet Gradient, ₹299/mo)
            item {
                MembershipProCard(onClick = onSelectPro, hi = hi)
            }

            // Max VIP Plan Card (Gold Gradient, ₹599/mo)
            item {
                MembershipMaxCard(onClick = onSelectMax, hi = hi)
            }
        }
    }
}

@Composable
private fun MembershipProCard(
    onClick: () -> Unit,
    hi: Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    val proGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF4C1D95),
            Color(0xFF2E1065),
            CosmicCardBg
        )
    )

    Box(
        modifier = Modifier
            .width(260.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = CosmicViolet.copy(alpha = 0.4f),
                spotColor = CosmicVioletLight.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(proGradient)
            .border(1.5.dp, CosmicVioletLight.copy(alpha = 0.7f), shape)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CosmicVioletLight.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, CosmicVioletLight)
                ) {
                    Text(
                        text = "⭐ PRO MEMBER",
                        color = CosmicVioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "₹299",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = if (hi) "/ प्रति माह" else "/ month",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val proFeatures = listOf(
                if (hi) "असीमित AI ज्योतिष वार्तालाप" else "Unlimited AI Consultations",
                if (hi) "दैनिक विस्तृत गोचर व फलकथन" else "Detailed Daily Transit Reports",
                if (hi) "10% अतिरिक्त वॉलेट रिचार्ज बोनस" else "10% Extra Wallet Bonus",
                if (hi) "प्राथमिकता ग्राहक सेवा" else "Priority Customer Support"
            )

            proFeatures.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✓", color = CosmicVioletLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = feature, color = TextPrimary.copy(alpha = 0.9f), fontSize = 11.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicViolet,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (hi) "Pro प्लान लें ➔" else "Get Pro Plan ➔",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MembershipMaxCard(
    onClick: () -> Unit,
    hi: Boolean
) {
    val shape = RoundedCornerShape(18.dp)
    val maxGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF5A3902),
            Color(0xFF2E1C03),
            CosmicCardBg
        )
    )

    Box(
        modifier = Modifier
            .width(260.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = SacredGold.copy(alpha = 0.4f),
                spotColor = SacredSaffron.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(maxGradient)
            .border(1.5.dp, SacredGold, shape)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SacredGold.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, SacredGold)
                ) {
                    Text(
                        text = "👑 MAX VIP",
                        color = SacredGoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "₹599",
                    color = SacredGoldGlow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = if (hi) "/ प्रति माह" else "/ month",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxFeatures = listOf(
                if (hi) "Pro की सभी सुविधाएं शामिल" else "All Pro Features Included",
                if (hi) "ज्योतिषी से 1-on-1 परामर्श सत्र" else "1-on-1 Astrologer Session",
                if (hi) "विस्तृत दोष निवारण व रत्न रिपोर्ट" else "Complete Dosha & Gem Report",
                if (hi) "20% अतिरिक्त वॉलेट रिचार्ज बोनस" else "20% Extra Wallet Bonus"
            )

            maxFeatures.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "★", color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = feature, color = TextPrimary.copy(alpha = 0.9f), fontSize = 11.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = CosmicBackground
                )
            ) {
                Text(
                    text = if (hi) "Max VIP प्लान लें ➔" else "Get Max VIP ➔",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 5: Transaction History Header & List
// -----------------------------------------------------------------------------
@Composable
private fun TransactionHistoryHeader(
    onRefresh: () -> Unit,
    hi: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = SacredGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hi) "हाल के लेनदेन (Transactions)" else "Recent Transactions",
                color = SacredGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TransactionItemCard(tx: WalletTransaction) {
    val isCredit = tx.type == "credit"
    val accentColor = if (isCredit) AuspiciousGreen else InauspiciousRed

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        glowColor = accentColor,
        glowAlpha = 0.2f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = if (isCredit) "Credit" else "Debit",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.description.ifBlank { if (isCredit) "वॉलेट रिचार्ज" else "परामर्श भुगतान" },
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tx.date,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}₹${String.format("%.2f", tx.amount / 100.0)}",
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isCredit) "क्रेडिट" else "डेबिट",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard(hi: Boolean) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.HistoryEdu,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hi) "अभी तक कोई लेनदेन नहीं हुआ है" else "No transactions yet",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                text = if (hi) "वॉलेट रिचार्ज करने पर आपका विवरण यहां दिखाई देगा" else "Your balance recharges will appear here",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component 6: Recharge ModalBottomSheet
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RechargeBottomSheet(
    initialAmount: Int,
    onDismiss: () -> Unit,
    onProceedToPay: (amount: Int, coupon: String) -> Unit,
    hi: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAmount by remember { mutableIntStateOf(initialAmount) }
    var customAmountText by remember { mutableStateOf("") }
    var couponText by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf<String?>(null) }
    var couponDiscount by remember { mutableDoubleStateOf(0.0) }
    var couponMessage by remember { mutableStateOf<String?>(null) }

    val packOptions = listOf(99, 199, 499, 999, 1999)

    // Calculation with 18% GST
    val baseAmount = selectedAmount.toDouble()
    val discountedBase = (baseAmount - couponDiscount).coerceAtLeast(1.0)
    val gstAmount = discountedBase * 0.18
    val totalPayable = discountedBase + gstAmount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CosmicSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = SacredGold.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (hi) "वॉलेट रिचार्ज करें" else "Recharge Wallet",
                        color = SacredGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hi) "राशि चुनें एवं Razorpay से सुरक्षित भुगतान करें" else "Select amount & pay securely via Razorpay",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Quick Amount Selector Chips
            Text(
                text = if (hi) "रिचार्ज राशि चुनें" else "Select Package Amount",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                packOptions.forEach { amt ->
                    val isSelected = selectedAmount == amt && customAmountText.isEmpty()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedAmount = amt
                                customAmountText = ""
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SacredGold else CosmicCardBg,
                        border = BorderStroke(1.dp, if (isSelected) SacredGold else CosmicCardBorder)
                    ) {
                        Text(
                            text = "₹$amt",
                            color = if (isSelected) CosmicBackground else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Custom Amount Input
            OutlinedTextField(
                value = customAmountText,
                onValueChange = {
                    val digits = it.filter { ch -> ch.isDigit() }.take(5)
                    customAmountText = digits
                    val parsed = digits.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        selectedAmount = parsed
                    }
                },
                label = { Text(if (hi) "या कस्टम राशि दर्ज करें (₹)" else "Or enter custom amount (₹)") },
                placeholder = { Text("उदा. 250", color = TextSecondary.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SacredGold,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = SacredGold
                )
            )

            // Coupon Code Field + Apply Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = couponText,
                    onValueChange = { couponText = it.uppercase().trim() },
                    placeholder = { Text(if (hi) "कूपन कोड दर्ज करें" else "Enter Coupon Code", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SacredGold,
                        unfocusedBorderColor = CosmicCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SacredGold
                    )
                )

                Button(
                    onClick = {
                        if (couponText.isNotBlank()) {
                            // Demo coupon logic: "OM50" grants ₹50 off, others grant 10% off
                            appliedCoupon = couponText
                            couponDiscount = if (couponText == "OM50") 50.0 else (selectedAmount * 0.10).coerceAtMost(100.0)
                            couponMessage = if (hi) "कूपन सफलतापूर्वक लागू हुआ!" else "Coupon applied successfully!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = if (hi) "लागू करें" else "Apply",
                        color = CosmicBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            couponMessage?.let {
                Text(
                    text = "✓ $it (-₹${String.format("%.2f", couponDiscount)})",
                    color = AuspiciousGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // GST & Price Breakdown Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CosmicCardBg,
                border = BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (hi) "भुगतान विवरण (Price Breakdown)" else "Price Breakdown",
                        color = SacredGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (hi) "रिचार्ज राशि (Base)" else "Recharge Amount", color = TextSecondary, fontSize = 12.5.sp)
                        Text(text = "₹${String.format("%.2f", baseAmount)}", color = TextPrimary, fontSize = 12.5.sp)
                    }

                    if (couponDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (hi) "कूपन छूट ($appliedCoupon)" else "Coupon Discount ($appliedCoupon)", color = AuspiciousGreen, fontSize = 12.5.sp)
                            Text(text = "-₹${String.format("%.2f", couponDiscount)}", color = AuspiciousGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (hi) "GST (18%)" else "GST (18%)", color = TextSecondary, fontSize = 12.5.sp)
                        Text(text = "₹${String.format("%.2f", gstAmount)}", color = TextPrimary, fontSize = 12.5.sp)
                    }

                    HorizontalDivider(color = CosmicCardBorder, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hi) "कुल देय राशि (Total)" else "Total Payable",
                            color = SacredGoldLight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${String.format("%.2f", totalPayable)}",
                            color = SacredGoldGlow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Razorpay Payment Button
            PulseGlowButton(
                text = if (hi)
                    "Razorpay से भुगतान करें • ₹${String.format("%.2f", totalPayable)}"
                else
                    "Proceed to Pay ₹${String.format("%.2f", totalPayable)}",
                onClick = {
                    onProceedToPay(selectedAmount, appliedCoupon ?: "")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Security assurance badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hi) "100% सुरक्षित भुगतान • Razorpay अधिकृत पार्टनर" else "100% Secure Payment • Razorpay Certified",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
