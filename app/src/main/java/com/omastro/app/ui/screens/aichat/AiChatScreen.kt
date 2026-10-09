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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.ChatMessage
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

private val modeSuggestions: Map<AiChatMode, List<String>> = mapOf(
    AiChatMode.GENERAL to listOf(
        "What does my horoscope say today?",
        "Tell me about my career prospects",
        "When is a good time to start something new?",
        "What are my lucky numbers and colors?"
    ),
    AiChatMode.DIVINE to listOf(
        "Give me today's spiritual guidance",
        "What message does the universe have for me?",
        "How can I find inner peace right now?",
        "Share a divine blessing for my family"
    ),
    AiChatMode.MUNDANE to listOf(
        "What major world events are unfolding this month?",
        "How will current planetary transits affect the world?",
        "What global trends should I watch?",
        "What does mundane astrology say about the economy?"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    viewModel: AiChatViewModel = viewModel()
) {
    val selectedMode by viewModel.selectedMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun sendCurrent() {
        if (inputText.isNotBlank() && !uiState.isTyping) {
            viewModel.sendMessage(inputText)
            inputText = ""
            focusManager.clearFocus()
        }
    }

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
                            "AI Jyotishi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            selectedMode.subtitle,
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
                                contentDescription = "Menu",
                                tint = SacredGold
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SacredGold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearChat()
                        inputText = ""
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New chat",
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
                if (uiState.messages.isNotEmpty() && !uiState.isTyping) {
                    InlineSuggestionStrip(
                        suggestions = modeSuggestions.getValue(selectedMode).take(3),
                        onTap = { inputText = it }
                    )
                }
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = ::sendCurrent,
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
            ModeSegmentedControl(
                selected = selectedMode,
                onSelect = { viewModel.selectMode(it) }
            )

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
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { msg ->
                        ChatBubble(message = msg)
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
                        text = mode.title,
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
                AiChatMode.GENERAL -> "Welcome to AI Jyotishi"
                AiChatMode.DIVINE -> "Welcome to Divine Guidance"
                AiChatMode.MUNDANE -> "Welcome to Mundane Astrology"
            },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (mode) {
                AiChatMode.GENERAL ->
                    "Ask anything about your life, career, relationships or health. " +
                        "Answers are drawn from Vedic astrology."
                AiChatMode.DIVINE ->
                    "Receive spiritual wisdom and divine messages for your journey. " +
                        "Ask with an open heart."
                AiChatMode.MUNDANE ->
                    "Explore world events, collective trends and global cycles " +
                        "through the lens of mundane astrology."
            },
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Try asking:",
            color = SacredGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(10.dp))
        SuggestionChips(
            suggestions = modeSuggestions.getValue(mode),
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

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Text(
                text = "AI Jyotishi",
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
                text = message.message,
                color = if (isUser) CosmicDeepBlue else Color.White,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
            )
        }
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            color = StarlightWhite.copy(alpha = 0.35f),
            fontSize = 10.5.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
private fun AnimatedTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
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

    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = "AI Jyotishi",
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
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
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
        Text(
            text = "typing...",
            color = StarlightWhite.copy(alpha = 0.35f),
            fontSize = 10.5.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
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
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Ask your question...",
                        color = StarlightWhite.copy(alpha = 0.4f),
                        fontSize = 14.sp
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
                    contentDescription = "Send",
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
