package com.omastro.app.ui.screens.aichat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.Manifest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay
import com.omastro.app.data.model.AiModel
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.model.VisionAnalysis
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

/** Bilingual mode titles/subtitles (auto-follow the Settings language). */
private fun modeTitle(mode: AiChatMode): String = when (mode) {
    AiChatMode.GENERAL -> W.t("chat_mode_general")
    AiChatMode.DIVINE -> W.t("chat_mode_divine")
    AiChatMode.MUNDANE -> W.t("chat_mode_mundane")
}

private fun modeSubtitle(mode: AiChatMode): String = when (mode) {
    AiChatMode.GENERAL -> W.t("chat_mode_general_sub")
    AiChatMode.DIVINE -> W.t("chat_mode_divine_sub")
    AiChatMode.MUNDANE -> W.t("chat_mode_mundane_sub")
}

/** Bilingual per-mode suggestion prompts (used as input text, never as AI output). */
private fun suggestionsFor(mode: AiChatMode): List<String> = when (mode) {
    AiChatMode.GENERAL -> listOf(
        W.t("chat_sug_general_1"),
        W.t("chat_sug_general_2"),
        W.t("chat_sug_general_3"),
        W.t("chat_sug_general_4")
    )
    AiChatMode.DIVINE -> listOf(
        W.t("chat_sug_divine_1"),
        W.t("chat_sug_divine_2"),
        W.t("chat_sug_divine_3"),
        W.t("chat_sug_divine_4")
    )
    AiChatMode.MUNDANE -> listOf(
        W.t("chat_sug_mundane_1"),
        W.t("chat_sug_mundane_2"),
        W.t("chat_sug_mundane_3"),
        W.t("chat_sug_mundane_4")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    // Activity-scoped: the conversation (per mode) survives navigation —
    // leaving for Kundali/Palmistry and coming back no longer silently
    // discards the chat. Every entry point opens the SAME conversation.
    viewModel: AiChatViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    ),
    lockedMode: AiChatMode? = null,
    openHistoryOnStart: Boolean = false
) {
    // When locked to a specific mode (e.g. Divine Chat section), pre-select it
    // and hide the mode switcher.
    LaunchedEffect(lockedMode) {
        if (lockedMode != null) viewModel.selectMode(lockedMode)
    }
    val selectedMode by viewModel.selectedMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showAttachDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }
    // Deep link from the drawer ("AI Chat History") opens the real history UI.
    LaunchedEffect(openHistoryOnStart) {
        if (openHistoryOnStart) showSessionsDialog = true
    }
    var showSaveConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Global UI language: voice/photo UI auto-switches EN/हिन्दी with Settings.
    val lang by LangPrefs.language.collectAsState()

    // Honest quota state: the server is the source of truth. When it reports
    // zero questions left, say so plainly instead of failing silently on send.
    val quotaExhausted = uiState.quota.limit > 0 && uiState.quota.left <= 0

    // ---- Voice typing + photo attach + voice mode (world-class additions) ----
    val voiceInput = rememberVoiceInputManager()
    var isListening by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var photoResult by remember { mutableStateOf<Pair<VisionAnalysis, String>?>(null) }
    val voiceModeOn by VoiceModePrefs.enabled
    val ttsSpeaking by VoiceModeManager.isSpeaking
    val micPermission = rememberPermissionRequest(
        permission = Manifest.permission.RECORD_AUDIO,
        title = W.t("chat_mic_title"),
        rationale = W.t("chat_mic_rationale")
    )
    voiceInput.onListeningChange = { isListening = it }
    voiceInput.onResult = { text ->
        inputText = if (inputText.isBlank()) text else "${inputText.trim()} $text"
    }
    voiceInput.onError = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // Handoff from Palmistry / Vastu / photo screens: attach parsed facts.
    LaunchedEffect(Unit) {
        if (viewModel.consumePendingPhotoContext()) {
            snackbarHostState.showSnackbar(W.t("chat_photo_attached"))
        }
    }

    // Voice mode: speak each new AI reply aloud.
    val lastSpokenId = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.messages, voiceModeOn) {
        if (voiceModeOn) {
            val lastAi = uiState.messages.lastOrNull { !it.isUser }
            if (lastAi != null && lastAi.id != lastSpokenId.value && !uiState.isTyping) {
                lastSpokenId.value = lastAi.id
                VoiceModeManager.speak(lastAi.message, uiState.language)
            }
        } else {
            VoiceModeManager.stop()
        }
    }
    // Stop speech when leaving the screen.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { VoiceModeManager.stop() }
    }

    fun sendCurrent() {
        if (inputText.isBlank() || uiState.isTyping) return
        if (quotaExhausted) {
            // Honest refusal: the server already said zero questions are
            // left — don't burn a request just to show its error again.
            scope.launch {
                snackbarHostState.showSnackbar(
                    W.tf("chat_quota_exhausted", uiState.quota.used, uiState.quota.limit)
                )
            }
            return
        }
        viewModel.sendMessage(inputText)
        inputText = ""
        focusManager.clearFocus()
    }

    // Multi-bubble reveal bookkeeping: messages already on screen when the
    // chat opens (fresh history, a restored saved session, or simply coming
    // back to the chat) render all their bubbles at once; only genuinely NEW
    // AI replies animate in bubble-by-bubble.
    val initialMessageIds = remember { uiState.messages.map { it.id }.toSet() }
    val revealedMessageIds = remember { mutableSetOf<String>() }

    LaunchedEffect(uiState.messages.size, uiState.isTyping, selectedMode) {
        if (uiState.messages.isNotEmpty() || uiState.isTyping) {
            val target = uiState.messages.size // typing indicator sits at this index
            scope.launch { listState.animateScrollToItem(target.coerceAtLeast(0)) }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            W.t("chat_ai_name"),
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            modeSubtitle(selectedMode),
                            color = StarlightWhite.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = W.t("chat_menu_cd"),
                                tint = SacredGold
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = W.t("chat_back_cd"),
                                tint = SacredGold
                            )
                        }
                    }
                },
                actions = {
                    // Quota display: questions left
                    if (uiState.quota.limit > 0) {
                        QuotaPill(
                            left = uiState.quota.left,
                            limit = uiState.quota.limit
                        )
                    }
                    // Voice mode: spoken AI replies (persisted toggle)
                    IconButton(onClick = {
                        VoiceModePrefs.setEnabled(!voiceModeOn)
                        if (!voiceModeOn) {
                            scope.launch {
                                snackbarHostState.showSnackbar(W.t("chat_voice_on_snack"))
                            }
                        }
                    }) {
                        Icon(
                            if (ttsSpeaking) Icons.Default.Stop
                            else if (voiceModeOn) Icons.Default.VolumeUp
                            else Icons.Default.VolumeOff,
                            contentDescription = if (ttsSpeaking) W.t("chat_voice_stop_cd")
                                else if (voiceModeOn) W.t("chat_voice_on_cd")
                                else W.t("chat_voice_off_cd"),
                            tint = if (voiceModeOn) SacredGold else StarlightWhite.copy(alpha = 0.55f)
                        )
                    }
                    // Saved chats
                    IconButton(onClick = {
                        viewModel.loadSavedSessions()
                        showSessionsDialog = true
                    }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = W.t("chat_saved_cd"),
                            tint = SacredGold
                        )
                    }
                    // Save current chat
                    IconButton(onClick = {
                        if (uiState.messages.isNotEmpty()) {
                            viewModel.saveCurrentSession { showSaveConfirm = true }
                        }
                    }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = W.t("chat_save_cd"),
                            tint = SacredGold
                        )
                    }
                    // New chat
                    IconButton(onClick = {
                        viewModel.clearChat()
                        inputText = ""
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = W.t("chat_new_cd"),
                            tint = SacredGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Attached birth-data chips
                AttachmentChips(
                    attachments = uiState.attachments,
                    onRemove = { viewModel.removeAttachment(it) }
                )
                // Attached photo-analysis chips (vision / palmistry / vastu)
                PhotoAttachmentChips(
                    attachments = uiState.photoAttachments,
                    onRemove = { viewModel.removePhotoAttachment(it) }
                )
                // Restored session context: the stored kundali_ctx IS active
                // and will be sent, but no chips can be rebuilt from it —
                // say so instead of implying "no context attached".
                if (uiState.hasRestoredContext) {
                    RestoredContextChip()
                }
                if (uiState.messages.isNotEmpty() && !uiState.isTyping) {
                    InlineSuggestionStrip(
                        suggestions = suggestionsFor(selectedMode).take(3),
                        onTap = { inputText = it }
                    )
                }
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = ::sendCurrent,
                    onAttachClick = { showAttachDialog = true },
                    onMicClick = {
                        if (isListening) {
                            voiceInput.stopListening()
                        } else {
                            micPermission.request(context) { granted ->
                                if (granted) voiceInput.startListening(uiState.language)
                            }
                        }
                    },
                    isListening = isListening,
                    onPhotoClick = { showPhotoDialog = true },
                    enabled = !uiState.isTyping
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hide the mode switcher when locked to a single mode
            // (e.g. the dedicated Divine Chat section).
            if (lockedMode == null) {
                ModeSegmentedControl(
                    selected = selectedMode,
                    onSelect = { viewModel.selectMode(it) }
                )
            }

            // Model selector + quota row
            AiModelSelectorRow(
                selected = uiState.aiModel,
                onSelect = { viewModel.setAiModel(it) },
                quotaText = if (uiState.quota.limit > 0) {
                    W.tf("chat_quota_left", uiState.quota.left, uiState.quota.limit)
                } else null
            )

            // Reply language selector (EN / HI) — website parity.
            LanguageSelectorRow(
                selected = uiState.language,
                onSelect = { viewModel.setLanguage(it) }
            )

            // Quota exhausted: plain-language banner. The top-bar pill already
            // turns red at <= 1; this says what it means and what to do.
            if (quotaExhausted) {
                QuotaExhaustedBanner(
                    used = uiState.quota.used,
                    limit = uiState.quota.limit
                )
            }

            if (uiState.messages.isEmpty() && !uiState.isTyping) {
                ChatEmptyState(
                    mode = selectedMode,
                    onSuggestionTap = { inputText = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    itemsIndexed(
                        items = uiState.messages,
                        key = { _, msg -> msg.id }
                    ) { index, msg ->
                        if (msg.isUser) {
                            ChatBubble(message = msg)
                        } else {
                            // Only replies that arrived after this screen
                            // opened animate in bubble-by-bubble.
                            val firstSeen = remember(msg.id) {
                                revealedMessageIds.add(msg.id)
                            }
                            AiMessageBubbles(
                                message = msg,
                                animate = msg.id !in initialMessageIds && firstSeen,
                                onChunkRevealed = {
                                    scope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            )
                        }
                    }
                    if (uiState.isTyping) {
                        item(key = "typing") {
                            AnimatedTypingIndicator()
                        }
                    }
                }
            }
        }
    }

    // Attach Birth Data dialog
    if (showAttachDialog) {
        AttachBirthDataDialog(
            onDismiss = { showAttachDialog = false },
            onAttach = {
                viewModel.attachBirthData(it)
                showAttachDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(W.t("chat_birth_attached"))
                }
            }
        )
    }

    // Photo analysis dialog (camera / gallery / file → AI vision)
    if (showPhotoDialog) {
        PhotoAttachDialog(
            onDismiss = { showPhotoDialog = false },
            onAnalyzed = { analysis, fileName ->
                showPhotoDialog = false
                photoResult = analysis to fileName
            },
            onError = { msg ->
                showPhotoDialog = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    // Photo analysis result: show analysis + offer to attach parsed facts
    val result = photoResult
    if (result != null) {
        PhotoAnalysisResultDialog(
            analysis = result.first,
            fileName = result.second,
            onDismiss = { photoResult = null },
            onAttachToChat = {
                val block = result.first.toContextBlock(result.second)
                if (block.isNotBlank()) {
                    viewModel.attachPhotoContext(
                        PhotoContextAttachment(
                            label = result.second,
                            contextBlock = block
                        )
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar(W.t("chat_photo_attached"))
                    }
                }
                photoResult = null
            },
            onAskNow = {
                // Attach + immediately ask a follow-up in chat
                val block = result.first.toContextBlock(result.second)
                if (block.isNotBlank()) {
                    viewModel.attachPhotoContext(
                        PhotoContextAttachment(
                            label = result.second,
                            contextBlock = block
                        )
                    )
                }
                photoResult = null
                inputText = W.t("chat_photo_ask_hint")
            }
        )
    }

    // Saved sessions dialog
    if (showSessionsDialog) {
        SavedSessionsDialog(
            sessions = uiState.savedSessions,
            isLoading = uiState.isLoadingSessions,
            onDismiss = { showSessionsDialog = false },
            onSelect = {
                viewModel.loadSession(it)
                showSessionsDialog = false
            },
            onDelete = { viewModel.deleteSession(it) },
            onClearAll = { viewModel.clearAllSessions() }
        )
    }

    // Save confirmation feedback
    LaunchedEffect(showSaveConfirm) {
        if (showSaveConfirm) {
            snackbarHostState.showSnackbar(W.t("chat_saved"))
            showSaveConfirm = false
        }
    }
}

@Composable
private fun ModeSegmentedControl(
    selected: AiChatMode,
    onSelect: (AiChatMode) -> Unit
) {
    val modes = AiChatMode.values()
    Surface(
        color = CosmicNavy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            modes.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) SacredGold else Color.Transparent
                        )
                        .clickable { onSelect(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeTitle(mode),
                        color = if (isSelected) CosmicDeepBlue else StarlightWhite.copy(alpha = 0.65f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    mode: AiChatMode,
    onSuggestionTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Decorative emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SacredGold.copy(alpha = 0.12f))
                .border(1.dp, SacredGold.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val modeGlyph = when (mode) {
                AiChatMode.GENERAL -> "G"
                AiChatMode.DIVINE -> "Om"
                AiChatMode.MUNDANE -> "M"
            }
            Text(
                text = modeGlyph,
                color = SacredGold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = when (mode) {
                AiChatMode.GENERAL -> W.t("chat_welcome_general")
                AiChatMode.DIVINE -> W.t("chat_welcome_divine")
                AiChatMode.MUNDANE -> W.t("chat_welcome_mundane")
            },
            color = StarlightWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (mode) {
                AiChatMode.GENERAL -> W.t("chat_welcome_general_desc")
                AiChatMode.DIVINE -> W.t("chat_welcome_divine_desc")
                AiChatMode.MUNDANE -> W.t("chat_welcome_mundane_desc")
            },
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = W.t("chat_try_asking"),
            color = SacredGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(10.dp))
        SuggestionChips(
            suggestions = suggestionsFor(mode),
            onTap = onSuggestionTap
        )
    }
}

@Composable
private fun SuggestionChips(
    suggestions: List<String>,
    onTap: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CosmicCharcoal,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        SacredGold.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTap(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    color = StarlightWhite.copy(alpha = 0.9f),
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Splits an AI reply into sentence-sized bubbles (website parity: 1 sentence
 * = 1 bubble, Hindi danda । included). Paragraph breaks are hard boundaries.
 * Decimals (3.5), common abbreviations (Mr., Dr., e.g.) and URLs are
 * protected from splitting.
 */
private fun splitAiReplyIntoBubbles(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val out = mutableListOf<String>()
    text.split(Regex("\n[ \t]*\n")).forEach { paragraph ->
        splitSentences(paragraph).forEach { sentence ->
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) return@forEach
            // Merge stray fragments (a lone danda, bullet marker) into the
            // previous bubble instead of showing a near-empty bubble.
            if (out.isNotEmpty() && trimmed.length <= 2) {
                out[out.lastIndex] = out.last() + " " + trimmed
            } else {
                out.add(trimmed)
            }
        }
    }
    return out
}

private fun splitSentences(text: String): List<String> {
    val abbreviations = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st",
        "vs", "etc", "eg", "ie", "no"
    )
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        current.append(c)
        if (c == '.' || c == '!' || c == '?' || c == '।') {
            val prev = if (i > 0) text[i - 1] else ' '
            val next = if (i + 1 < text.length) text[i + 1] else ' '
            val atBoundary = next.isWhitespace() || i + 1 >= text.length
            // 3.5 — digit on both sides of the dot.
            val isDecimal = c == '.' && prev.isDigit() && next.isDigit()
            // Mr. / Dr. / e.g. — word before the dot, dots stripped.
            val lastWord = current.dropLast(1).toString()
                .substringAfterLast(' ')
                .substringAfterLast('\n')
                .replace(".", "")
                .lowercase()
            val isAbbreviation = c == '.' && lastWord in abbreviations
            if (atBoundary && !isDecimal && !isAbbreviation) {
                val chunk = current.toString().trim()
                if (chunk.isNotEmpty()) chunks.add(chunk)
                current.clear()
            }
        }
        i++
    }
    val tail = current.toString().trim()
    if (tail.isNotEmpty()) chunks.add(tail)
    return chunks
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    text: String = message.message,
    showNameLabel: Boolean = true,
    showTimestamp: Boolean = true
) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser && showNameLabel) {
            Text(
                text = W.t("chat_ai_name"),
                color = SacredGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 18.dp
            ),
            color = if (isUser) SacredGold else CosmicCharcoal,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                color = if (isUser) CosmicDeepBlue else StarlightWhite,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
            )
        }
        if (showTimestamp) {
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                color = StarlightWhite.copy(alpha = 0.35f),
                fontSize = 10.5.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

/**
 * Renders one AI reply as multiple sentence-bubbles. A newly arrived reply
 * reveals its bubbles one at a time with a short pause (the typing
 * simulation); stored/history replies render all bubbles immediately.
 * The timestamp appears once, under the final bubble.
 */
@Composable
private fun AiMessageBubbles(
    message: ChatMessage,
    animate: Boolean,
    onChunkRevealed: () -> Unit
) {
    val chunks = remember(message.id, message.message) {
        splitAiReplyIntoBubbles(message.message)
    }
    val total = chunks.size.coerceAtLeast(1)
    var visible by remember(message.id) {
        mutableIntStateOf(if (animate && total > 1) 1 else total)
    }
    LaunchedEffect(message.id) {
        if (animate) {
            while (visible < total) {
                delay(900)
                visible++
                onChunkRevealed()
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = W.t("chat_ai_name"),
            color = SacredGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
        chunks.take(visible).forEachIndexed { index, chunk ->
            ChatBubble(
                message = message,
                text = chunk,
                showNameLabel = false,
                // Timestamp only under the final bubble, once fully revealed.
                showTimestamp = index == total - 1 && visible == total
            )
        }
        if (visible < total) {
            Box(modifier = Modifier.padding(start = 14.dp, top = 2.dp, bottom = 2.dp)) {
                TypingDotsRow()
            }
        }
    }
}

/** The three animated dots, reused by the typing indicator and bubble reveal. */
@Composable
private fun TypingDotsRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing-dots")
    val dots = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 450,
                    delayMillis = index * 150,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$index"
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        SacredGold.copy(alpha = alpha.value),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun AnimatedTypingIndicator() {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = W.t("chat_ai_name"),
            color = SacredGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 6.dp,
                bottomEnd = 18.dp
            ),
            color = CosmicCharcoal,
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                TypingDotsRow()
            }
        }
        Text(
            text = W.t("chat_typing"),
            color = StarlightWhite.copy(alpha = 0.35f),
            fontSize = 10.5.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}

/**
 * Quota exhausted: plain-language banner. Shown only when the server reports
 * a real limit with zero questions left — never from a default 0/0 quota.
 */
@Composable
private fun QuotaExhaustedBanner(used: Int, limit: Int) {
    Surface(
        color = Color(0xFFFF8A80).copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                1.dp,
                Color(0xFFFF8A80).copy(alpha = 0.45f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Text(
            text = W.tf("chat_quota_exhausted", used, limit),
            color = Color(0xFFFF8A80),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

/**
 * Shown after a saved session is reopened: its stored context IS active and
 * will be sent with the next message, but no attachment chips can be rebuilt
 * from it — this chip says so honestly.
 */
@Composable
private fun RestoredContextChip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SacredGold.copy(alpha = 0.12f),
            modifier = Modifier.border(
                1.dp,
                SacredGold.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
        ) {
            Text(
                text = W.t("chat_restored_ctx"),
                color = SacredGold,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    onPhotoClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = CosmicNavy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attach Birth Data button (website parity: paperclip)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(StarlightWhite.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        SacredGold.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable(enabled = enabled) { onAttachClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = W.t("chat_attach_cd"),
                    tint = SacredGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        W.t("chat_input_hint"),
                        color = StarlightWhite.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CosmicCharcoal,
                    unfocusedContainerColor = CosmicCharcoal,
                    disabledContainerColor = CosmicCharcoal,
                    focusedTextColor = StarlightWhite,
                    unfocusedTextColor = StarlightWhite,
                    disabledTextColor = StarlightWhite.copy(alpha = 0.4f),
                    cursorColor = SacredGold,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.weight(1f),
                enabled = enabled,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSend() }
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            // Voice typing (mic) — SpeechRecognizer, en/hi
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) SacredGold.copy(alpha = 0.35f)
                        else StarlightWhite.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        SacredGold.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable(enabled = enabled) { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) W.t("chat_stop_listening") else W.t("chat_voice_typing"),
                    tint = if (isListening) SacredGold else StarlightWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Photo analysis (camera / gallery / file → AI vision)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(StarlightWhite.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        SacredGold.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable(enabled = enabled) { onPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = W.t("chat_analyze_photo_cd"),
                    tint = SacredGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val canSend = value.isNotBlank() && enabled
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) SacredGold
                        else StarlightWhite.copy(alpha = 0.12f)
                    )
                    .clickable(enabled = canSend) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = W.t("chat_send_cd"),
                    tint = if (canSend) CosmicDeepBlue else StarlightWhite.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Compact horizontal suggestion strip used above the input bar once a
 * conversation has started, so users can keep exploring topics.
 */
@Composable
private fun InlineSuggestionStrip(
    suggestions: List<String>,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CosmicCharcoal,
                modifier = Modifier
                    .border(
                        1.dp,
                        SacredGold.copy(alpha = 0.3f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onTap(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    color = SacredGold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/**
 * Quota pill shown in the top bar: "3/5 left".
 * Website parity: questions_used/limit/left from every response.
 */
@Composable
private fun QuotaPill(left: Int, limit: Int) {
    val low = left <= 1
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (low) Color(0xFFFF8A80).copy(alpha = 0.15f)
                else SacredGold.copy(alpha = 0.15f)
            )
            .border(
                1.dp,
                if (low) Color(0xFFFF8A80).copy(alpha = 0.5f)
                else SacredGold.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$left/$limit",
            color = if (low) Color(0xFFFF8A80) else SacredGold,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Reply language selector (English / Hindi).
 * Website parity: `language` is sent with every message.
 */
@Composable
private fun LanguageSelectorRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = W.t("chat_reply_in"),
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
        listOf("en" to "English", "hi" to "हिन्दी").forEach { (code, label) ->
            val isSelected = selected == code
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) SacredGold.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isSelected) SacredGold.copy(alpha = 0.6f)
                        else StarlightWhite.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(code) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) SacredGold else StarlightWhite.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * AI model tier selector (Lite / Pro / Expert) with quota hint.
 * Website parity: ai_model is sent with every message.
 */
@Composable
private fun AiModelSelectorRow(
    selected: AiModel,
    onSelect: (AiModel) -> Unit,
    quotaText: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiModel.values().forEach { model ->
                val isSelected = model == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) SacredGold.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isSelected) SacredGold.copy(alpha = 0.6f)
                            else StarlightWhite.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(model) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = model.displayName,
                        color = if (isSelected) SacredGold else StarlightWhite.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        quotaText?.let {
            Text(
                text = it,
                color = StarlightWhite.copy(alpha = 0.5f),
                fontSize = 11.5.sp
            )
        }
    }
}
