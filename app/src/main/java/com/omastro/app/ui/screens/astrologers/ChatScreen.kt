package com.omastro.app.ui.screens.astrologers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.BirthDataAttachment
import com.omastro.app.data.model.GeocodePlace
import com.omastro.app.ui.components.DatePickerField
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.components.TimePickerField
import com.omastro.app.ui.theme.*

/**
 * WhatsApp-style astrologer consultation chat (website /chat/ parity).
 *
 * - Fullscreen chat; header morphs to the astrologer (photo, name, online/typing status)
 * - Floating timer pill (MM:SS, display-only; server owns billing)
 * - Per-sentence astrologer bubbles with humanizing delays (see ViewModel)
 * - 3-dot typing bubble, read ticks on the user's last bubble
 * - Rate consent dialog, birth-data attach gate, Add Money popup, review card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    astrologerSlug: String,
    onBack: () -> Unit,
    onNavigateToWallet: () -> Unit,
    viewModel: ConsultationViewModel = viewModel()
) {
    val astrologers by viewModel.astrologers.collectAsState()
    val isLoadingList by viewModel.isLoading.collectAsState()
    val activeSlug by viewModel.activeSlug.collectAsState()
    val activeAstrologer by viewModel.activeAstrologer.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val sendLocked by viewModel.sendLocked.collectAsState()
    val inputLocked by viewModel.inputLocked.collectAsState()
    val showAddMoney by viewModel.showAddMoney.collectAsState()
    val sessionError by viewModel.sessionError.collectAsState()
    val connecting by viewModel.connecting.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val pendingConsent by viewModel.pendingConsent.collectAsState()
    val showBirthAttach by viewModel.showBirthAttach.collectAsState()
    val showReview by viewModel.showReview.collectAsState()
    val lastFailedText by viewModel.lastFailedText.collectAsState()

    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // Entry: resolve the astrologer and request the chat (consent -> birth -> connect).
    LaunchedEffect(astrologerSlug, astrologers) {
        if (astrologerSlug.isBlank()) return@LaunchedEffect
        if (activeSlug != null) return@LaunchedEffect // already in a chat
        val astro = viewModel.findAstrologer(astrologerSlug) ?: return@LaunchedEffect
        viewModel.requestChat(astro)
    }

    // Auto-scroll on new messages / typing state.
    LaunchedEffect(messages.size, isTyping) {
        val target = messages.size + if (isTyping) 1 else 0
        if (target > 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
    }

    // Restore failed text into the input for one-tap retry.
    LaunchedEffect(lastFailedText) {
        lastFailedText?.let {
            inputText = it
            viewModel.clearLastFailed()
        }
    }

    val astro = activeAstrologer ?: viewModel.findAstrologer(astrologerSlug)
    val timerText = remember(elapsedSeconds) {
        String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    }
    val freeTrial = sessionState?.isFreeTrial == true
    val canSend = !sendLocked && !inputLocked && !connecting && activeSlug != null

    Box(modifier = Modifier.fillMaxSize().background(CosmicDeepBlue)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: astrologer photo + name + status.
            ChatHeader(
                astro = astro,
                isTyping = isTyping,
                connecting = connecting,
                freeTrial = freeTrial,
                onBack = {
                    viewModel.endChat(showReview = activeSlug != null)
                    onBack()
                }
            )

            // Error banner.
            if (sessionError != null) {
                Surface(
                    color = Color(0xFF5C1A1A),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sessionError ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearSessionError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Messages.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    if (connecting && messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = SacredGold, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Connecting…", color = StarlightWhite.copy(alpha = 0.6f), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    items(messages, key = { it.id }) { msg ->
                        when {
                            msg.isSystem -> SystemBubble(msg.text)
                            msg.isUser -> UserBubble(msg.text, read = msg.read)
                            else -> AstroBubble(msg.text)
                        }
                    }
                    if (isTyping) {
                        item { TypingBubble() }
                    }
                }

                // Floating timer pill (middle-right, website parity).
                if (activeSlug != null && !connecting) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CosmicNavy.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SacredGold.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (freeTrial) Color(0xFF4CAF50) else SacredGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timerText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Composer.
            ChatComposer(
                inputText = inputText,
                onInputChange = { inputText = it },
                canSend = canSend && inputText.isNotBlank(),
                inputLocked = inputLocked,
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                onRetry = { viewModel.retryLastFailed() },
                showRetry = lastFailedText == null && false // handled via input restore
            )

            // End Chat button (visible only while a real billing session exists).
            if (sessionState?.sessionUuid != null) {
                OutlinedButton(
                    onClick = { viewModel.endChat(showReview = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8A80).copy(alpha = 0.5f))
                ) {
                    Text("End Chat", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Dialogs.
        pendingConsent?.let { consentAstro ->
            RateConsentDialog(
                astro = consentAstro,
                onConfirm = { viewModel.confirmConsent() },
                onDismiss = { viewModel.dismissConsent(); onBack() }
            )
        }

        if (showBirthAttach) {
            BirthAttachDialog(
                onAttach = { attachment ->
                    viewModel.setBirthAttachment(attachment)
                    viewModel.confirmBirthAttach()
                },
                onSkip = { viewModel.skipBirthAttach() }
            )
        }

        if (showAddMoney) {
            AddMoneyDialog(
                rateLabel = sessionState?.rateLabel ?: astro?.displayRate ?: "",
                onAddMoney = {
                    viewModel.dismissAddMoney()
                    onNavigateToWallet()
                },
                onLater = { viewModel.dismissAddMoney() },
                onRetry = { viewModel.retryAfterTopUp() }
            )
        }

        if (showReview && astro != null) {
            ReviewDialog(
                astroName = astro.name,
                onSubmit = { stars, text -> viewModel.submitReview(stars, text) },
                onLater = { viewModel.skipReview(); onBack() }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun ChatHeader(
    astro: Astrologer?,
    isTyping: Boolean,
    connecting: Boolean,
    freeTrial: Boolean,
    onBack: () -> Unit
) {
    Surface(color = CosmicNavy, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
            }
            Box(modifier = Modifier.size(44.dp)) {
                if (!astro?.img.isNullOrBlank()) {
                    AsyncImage(
                        model = astro!!.img,
                        contentDescription = astro.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(CosmicCharcoal)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(CosmicCharcoal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = SacredGold, modifier = Modifier.size(24.dp))
                    }
                }
                if (astro != null) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(if (astro.online) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                            .border(2.dp, CosmicNavy, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = astro?.name ?: "Astrologer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = when {
                        connecting -> "connecting…"
                        isTyping -> "typing…"
                        astro == null -> ""
                        astro.online -> "online"
                        else -> "offline"
                    },
                    color = if (isTyping) SacredGold else StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            if (freeTrial) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "FREE TRIAL",
                        color = Color(0xFF4CAF50),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bubbles
// ---------------------------------------------------------------------------

@Composable
private fun UserBubble(text: String, read: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            color = SacredGold,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = CosmicDeepBlue,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        // Read ticks on the user's last bubble once the reply arrives (website parity).
        Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = if (read) "Read" else "Sent",
            tint = if (read) Color(0xFF53BDEB) else StarlightWhite.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AstroBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = CosmicCharcoal,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun SystemBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CosmicNavy.copy(alpha = 0.9f)
        ) {
            Text(
                text = text,
                color = StarlightWhite.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = CosmicCharcoal,
            modifier = Modifier.widthIn(max = 120.dp)
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alpha)
                            .background(SacredGold, CircleShape)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Composer
// ---------------------------------------------------------------------------

@Composable
private fun ChatComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    canSend: Boolean,
    inputLocked: Boolean,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    showRetry: Boolean
) {
    Surface(color = CosmicNavy, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        if (inputLocked) "Add Money to chat" else "Type your question…",
                        color = StarlightWhite.copy(alpha = 0.5f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CosmicCharcoal,
                    unfocusedContainerColor = CosmicCharcoal,
                    disabledContainerColor = CosmicCharcoal,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = StarlightWhite.copy(alpha = 0.4f),
                    cursorColor = SacredGold,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                enabled = !inputLocked,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) SacredGold else StarlightWhite.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) CosmicDeepBlue else StarlightWhite.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Rate consent dialog
// ---------------------------------------------------------------------------

@Composable
private fun RateConsentDialog(
    astro: Astrologer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val offline = !astro.online
    val expensive = astro.ratePaise > 500
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start chat with ${astro.name}?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (offline) {
                    Text(
                        "${astro.name} is currently offline. Start the chat with this astrologer anyway?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (expensive) {
                    Text(
                        "Rate: ${astro.displayRate} (higher than the expected ₹5/min).",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SacredSaffron
                    )
                } else {
                    Text("Rate: ${astro.displayRate}.", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes, start", color = SacredGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StarlightWhite.copy(alpha = 0.7f))
            }
        },
        containerColor = CosmicNavy,
        titleContentColor = Color.White,
        textContentColor = StarlightWhite.copy(alpha = 0.85f)
    )
}

// ---------------------------------------------------------------------------
// Birth-data attach dialog (gate on entry — website parity)
// ---------------------------------------------------------------------------

@Composable
private fun BirthAttachDialog(
    onAttach: (BirthDataAttachment) -> Unit,
    onSkip: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var dob by remember { mutableStateOf("") } // YYYY-MM-DD (API format)
    var tob by remember { mutableStateOf("") } // "HH:MM AM/PM"
    var place by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<GeocodePlace?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { /* gate: must attach or skip explicitly */ }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CosmicNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Attach Birth Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "The astrologer gives far better guidance with your birth chart attached.",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Gender toggle.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("M" to "Male", "F" to "Female").forEach { (value, label) ->
                        val selected = gender == value
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) SacredGold else CosmicCharcoal,
                            modifier = Modifier.weight(1f).clickable { gender = value }
                        ) {
                            Text(
                                text = label,
                                color = if (selected) CosmicDeepBlue else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                DatePickerField(
                    apiDate = dob,
                    onDateChange = { dob = it },
                    label = "Date of Birth"
                )
                Spacer(modifier = Modifier.height(10.dp))

                TimePickerField(
                    displayTime = tob,
                    onTimeChange = { tob = it },
                    label = "Time of Birth"
                )
                Spacer(modifier = Modifier.height(10.dp))

                PlaceSearchField(
                    value = place,
                    onValueChange = {
                        place = it
                        selectedPlace = null
                    },
                    onPlaceSelected = {
                        selectedPlace = it
                        place = it.displayName
                    },
                    label = "Place of Birth"
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "", color = Color(0xFFFF8A80), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StarlightWhite.copy(alpha = 0.7f))
                    ) {
                        Text("Skip")
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                error = "Please enter your name."
                                return@Button
                            }
                            if (dob.isBlank()) {
                                error = "Please pick your date of birth."
                                return@Button
                            }
                            if (tob.isBlank()) {
                                error = "Please pick your time of birth."
                                return@Button
                            }
                            if (place.isBlank()) {
                                error = "Please enter your place of birth."
                                return@Button
                            }
                            onAttach(
                                BirthDataAttachment(
                                    name = name.trim(),
                                    gender = gender,
                                    dob = dob,
                                    tob = tob,
                                    place = place.trim(),
                                    lat = selectedPlace?.lat,
                                    lon = selectedPlace?.lon,
                                    tz = selectedPlace?.tz
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                    ) {
                        Text("Attach", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SacredGold,
    unfocusedBorderColor = CosmicBorder,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = SacredGold,
    unfocusedLabelColor = StarlightWhite.copy(alpha = 0.6f),
    cursorColor = SacredGold
)

// ---------------------------------------------------------------------------
// Add Money popup (insufficient balance — website parity)
// ---------------------------------------------------------------------------

@Composable
private fun AddMoneyDialog(
    rateLabel: String,
    onAddMoney: () -> Unit,
    onLater: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("Insufficient Balance", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "This astrologer charges $rateLabel. Please add money to continue the chat.",
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAddMoney) {
                Text("Add Money", color = SacredGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRetry) {
                    Text("Retry", color = StarlightWhite.copy(alpha = 0.7f))
                }
                TextButton(onClick = onLater) {
                    Text("Maybe Later", color = StarlightWhite.copy(alpha = 0.7f))
                }
            }
        },
        containerColor = CosmicNavy,
        titleContentColor = Color.White,
        textContentColor = StarlightWhite.copy(alpha = 0.85f)
    )
}

// ---------------------------------------------------------------------------
// Review card (after close — website parity)
// ---------------------------------------------------------------------------

@Composable
private fun ReviewDialog(
    astroName: String,
    onSubmit: (Int, String) -> Unit,
    onLater: () -> Unit
) {
    var stars by remember { mutableStateOf(5) }
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onLater) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CosmicNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Rate your chat with $astroName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { i ->
                        IconButton(onClick = { stars = i }) {
                            Icon(
                                imageVector = if (i <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$i stars",
                                tint = SacredGold,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    label = { Text("Write a review (optional)") },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    colors = dialogFieldColors(),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onLater,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StarlightWhite.copy(alpha = 0.7f))
                    ) {
                        Text("Later")
                    }
                    Button(
                        onClick = { onSubmit(stars, text.trim()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                    ) {
                        Text("Submit", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
