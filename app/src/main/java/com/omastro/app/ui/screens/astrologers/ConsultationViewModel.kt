package com.omastro.app.ui.screens.astrologers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omastro.app.data.model.AstroSessionState
import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.BirthDataAttachment
import com.omastro.app.data.model.ChatHistoryTurn
import com.omastro.app.data.model.buildBirthDataContext
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Astrologer consultation ViewModel — website parity (researched 2026-10-09).
 *
 * Flow:
 *  1. List astrologers (GET /astrologers)
 *  2. Auto-match ("Finding best astrologers…") -> prefer online ₹5/min
 *  3. Rate consent dialog when offline OR rate > ₹5/min
 *  4. Birth-data attach gate on entry
 *  5. Chat open (POST /astrologers/chat/open, server mints UUID)
 *  6. Billing session start (POST /astrologers/session/start)
 *     - insufficient -> Add Money popup + lock input
 *  7. Greeting: 2-6s delay -> typing 1.2s -> greeting bubble
 *  8. Messages with humanizing delays (website astro-chat.js parity):
 *     - user sends -> 2-6s pre-typing pause -> 3-dot typing bubble
 *     - reply split 1 sentence = 1 bubble
 *     - per bubble: first 2-3.5s, later len/2-3.5 chars-per-sec clamped 3-12s
 *     - 0.7-1.3s between bubbles; send locked until last bubble lands
 *     - read ticks on user's last bubble when reply arrives
 *  9. 30s billing tick heartbeat (server is source of truth for money)
 * 10. Idle: 1-min nudge -> 2-min closing + hard close
 * 11. End chat -> review card (stars + text + Later)
 *
 * No WebSocket — REST only, matching the website.
 */
class ConsultationViewModel : ViewModel() {

    private val getAstrologersUseCase = AppContainer.getAstrologersUseCase
    private val openChatSessionUseCase = AppContainer.openChatSessionUseCase
    private val closeChatSessionUseCase = AppContainer.closeChatSessionUseCase
    private val startBillingSessionUseCase = AppContainer.startBillingSessionUseCase
    private val tickBillingSessionUseCase = AppContainer.tickBillingSessionUseCase
    private val endBillingSessionUseCase = AppContainer.endBillingSessionUseCase
    private val submitReviewUseCase = AppContainer.submitAstrologerReviewUseCase
    private val consultationRepo = AppContainer.consultationRepository
    private val chatRepository = AppContainer.chatRepository

    // ------------------------------------------------------------------
    // Astrologer list
    // ------------------------------------------------------------------

    private val _astrologers = MutableStateFlow<List<Astrologer>>(emptyList())
    val astrologers: StateFlow<List<Astrologer>> = _astrologers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _listError = MutableStateFlow<String?>(null)
    val listError: StateFlow<String?> = _listError.asStateFlow()

    // ------------------------------------------------------------------
    // Auto-match
    // ------------------------------------------------------------------

    enum class AutoMatchPhase { IDLE, SEARCHING }

    private val _autoMatchPhase = MutableStateFlow(AutoMatchPhase.IDLE)
    val autoMatchPhase: StateFlow<AutoMatchPhase> = _autoMatchPhase.asStateFlow()

    /** Astrologer picked by auto-match, awaiting rate consent (null = no consent needed). */
    private val _pendingConsent = MutableStateFlow<Astrologer?>(null)
    val pendingConsent: StateFlow<Astrologer?> = _pendingConsent.asStateFlow()

    // ------------------------------------------------------------------
    // Active chat
    // ------------------------------------------------------------------

    private val _activeSlug = MutableStateFlow<String?>(null)
    val activeSlug: StateFlow<String?> = _activeSlug.asStateFlow()

    private val _activeAstrologer = MutableStateFlow<Astrologer?>(null)
    val activeAstrologer: StateFlow<Astrologer?> = _activeAstrologer.asStateFlow()

    private val _messages = MutableStateFlow<List<AstroChatMessage>>(emptyList())
    val messages: StateFlow<List<AstroChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    /** True while an astrologer reply is being delivered — send is locked. */
    private val _sendLocked = MutableStateFlow(false)
    val sendLocked: StateFlow<Boolean> = _sendLocked.asStateFlow()

    /** True when balance is insufficient — input locked, Add Money shown. */
    private val _inputLocked = MutableStateFlow(false)
    val inputLocked: StateFlow<Boolean> = _inputLocked.asStateFlow()

    private val _showAddMoney = MutableStateFlow(false)
    val showAddMoney: StateFlow<Boolean> = _showAddMoney.asStateFlow()

    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError.asStateFlow()

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    // ------------------------------------------------------------------
    // Billing
    // ------------------------------------------------------------------

    private val _sessionState = MutableStateFlow<AstroSessionState?>(null)
    val sessionState: StateFlow<AstroSessionState?> = _sessionState.asStateFlow()

    /** Display-only elapsed seconds (1s UI timer; server owns the money). */
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    // ------------------------------------------------------------------
    // Birth attach
    // ------------------------------------------------------------------

    private val _birthAttachment = MutableStateFlow<BirthDataAttachment?>(null)
    val birthAttachment: StateFlow<BirthDataAttachment?> = _birthAttachment.asStateFlow()

    private val _showBirthAttach = MutableStateFlow(false)
    val showBirthAttach: StateFlow<Boolean> = _showBirthAttach.asStateFlow()

    // ------------------------------------------------------------------
    // Review
    // ------------------------------------------------------------------

    private val _showReview = MutableStateFlow(false)
    val showReview: StateFlow<Boolean> = _showReview.asStateFlow()

    private val _reviewSubmitted = MutableStateFlow(false)
    val reviewSubmitted: StateFlow<Boolean> = _reviewSubmitted.asStateFlow()

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private var chatSessionId: String? = null
    private var billingSessionUuid: String? = null
    private var aiSessionId: String? = null
    private var chatContext: String? = null

    private var timerJob: Job? = null
    private var tickJob: Job? = null
    private var idleJob: Job? = null
    private var deliveryJob: Job? = null
    private var greetingJob: Job? = null

    /** Generation counter — aborts in-flight reply chains on hard close (website replyGen parity). */
    private var replyGeneration = 0

    private var lastUserMessageAt = 0L

    init {
        loadAstrologers()
    }

    // ------------------------------------------------------------------
    // List
    // ------------------------------------------------------------------

    fun loadAstrologers(category: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _listError.value = null
            getAstrologersUseCase(category)
                .onSuccess { _astrologers.value = it }
                .onFailure { _listError.value = it.message ?: "Could not load astrologers." }
            _isLoading.value = false
        }
    }

    fun findAstrologer(slug: String): Astrologer? =
        _astrologers.value.firstOrNull { it.slug == slug || it.id == slug }

    // ------------------------------------------------------------------
    // Auto-match: "Finding best astrologers…" -> prefer online ₹5/min.
    // ------------------------------------------------------------------

    fun startAutoMatch() {
        if (_autoMatchPhase.value == AutoMatchPhase.SEARCHING) return
        viewModelScope.launch {
            _autoMatchPhase.value = AutoMatchPhase.SEARCHING
            _sessionError.value = null
            // Brief "finding" beat so the UI state is visible (website shows the pill
            // while the join request runs).
            delay(1200)
            val list = _astrologers.value.ifEmpty {
                getAstrologersUseCase().getOrNull().also {
                    if (it != null) _astrologers.value = it
                } ?: emptyList()
            }
            _autoMatchPhase.value = AutoMatchPhase.IDLE
            if (list.isEmpty()) {
                _sessionError.value = "No astrologers available right now. Please try again."
                return@launch
            }
            // Prefer online ₹5/min (rate_paise <= 500), else cheapest online, else first.
            val online = list.filter { it.online }
            val pick = online.firstOrNull { it.ratePaise <= 500 }
                ?: online.minByOrNull { it.ratePaise }
                ?: list.minByOrNull { it.ratePaise }
                ?: list.first()
            requestChat(pick)
        }
    }

    // ------------------------------------------------------------------
    // Chat entry: rate consent -> birth gate -> open -> billing -> greeting
    // ------------------------------------------------------------------

    /** Entry from the directory list or auto-match. */
    fun requestChat(astro: Astrologer) {
        if (_connecting.value || _activeSlug.value != null) return
        // Rate consent: offline OR rate > ₹5/min (website parity).
        if (!astro.online || astro.ratePaise > 500) {
            _pendingConsent.value = astro
            return
        }
        beginChat(astro)
    }

    fun dismissConsent() {
        _pendingConsent.value = null
    }

    fun confirmConsent() {
        val astro = _pendingConsent.value ?: return
        _pendingConsent.value = null
        beginChat(astro)
    }

    private fun beginChat(astro: Astrologer) {
        _activeSlug.value = astro.slug
        _activeAstrologer.value = astro
        _messages.value = emptyList()
        _sessionError.value = null
        _reviewSubmitted.value = false
        _showReview.value = false
        _inputLocked.value = false
        _showAddMoney.value = false
        _elapsedSeconds.value = 0
        replyGeneration++
        // Birth-data gate on entry (website parity).
        if (_birthAttachment.value == null) {
            _showBirthAttach.value = true
        } else {
            connectChat()
        }
    }

    fun setBirthAttachment(attachment: BirthDataAttachment?) {
        _birthAttachment.value = attachment
    }

    fun confirmBirthAttach() {
        _showBirthAttach.value = false
        if (_activeSlug.value != null) connectChat()
    }

    fun skipBirthAttach() {
        _showBirthAttach.value = false
        if (_activeSlug.value != null) connectChat()
    }

    private fun connectChat() {
        val slug = _activeSlug.value ?: return
        val astro = _activeAstrologer.value ?: return
        viewModelScope.launch {
            _connecting.value = true
            _sessionError.value = null
            // System notice: joined (website parity — center bubble).
            appendSystemMessage("${astro.name} joined the chat")
            // 1. Register chat session (server mints UUID).
            val sessionId = openChatSessionUseCase(slug).getOrElse {
                _sessionError.value = it.message ?: "Could not open chat."
                _connecting.value = false
                return@launch
            }
            chatSessionId = sessionId
            // 2. Restore history (if any).
            consultationRepo.loadChatHistory(
                sessionId = sessionId,
                astrologerSlug = slug,
                cacheKey = slug
            ).onSuccess { history ->
                if (history.isNotEmpty()) {
                    val ui = history.map {
                        AstroChatMessage(
                            id = it.id,
                            isUser = it.isUser,
                            text = it.message
                        )
                    }
                    _messages.value = _messages.value + ui
                }
            }
            // 3. Start billing session.
            startBillingSessionUseCase(slug).onSuccess { state ->
                if (state.insufficient) {
                    // Blocked: insufficient balance -> Add Money popup + lock input.
                    _sessionState.value = state
                    _inputLocked.value = true
                    _showAddMoney.value = true
                    _sessionError.value = state.message
                        ?: "Insufficient balance. Please add money to chat."
                    _connecting.value = false
                    return@onSuccess
                }
                _sessionState.value = state
                billingSessionUuid = state.sessionUuid
                _connecting.value = false
                startTimers()
                scheduleGreeting()
            }.onFailure {
                _sessionError.value = it.message ?: "Could not start billing session."
                _connecting.value = false
            }
        }
    }

    // ------------------------------------------------------------------
    // Greeting: 2-6s delay -> typing 1.2s -> greeting bubble.
    // ------------------------------------------------------------------

    private fun scheduleGreeting() {
        greetingJob?.cancel()
        greetingJob = viewModelScope.launch {
            val gen = replyGeneration
            val astro = _activeAstrologer.value ?: return@launch
            delay(Random.nextLong(2000, 6001))
            if (gen != replyGeneration) return@launch
            _isTyping.value = true
            delay(1200)
            if (gen != replyGeneration) return@launch
            _isTyping.value = false
            val birth = _birthAttachment.value
            val greeting = if (birth != null && birth.name.isNotBlank()) {
                "Namaste ${birth.name}, I am ${astro.name}. I have your birth details with me. Please tell me, what would you like guidance on today?"
            } else {
                "Namaste, I am ${astro.name}. Please share your birth details or tell me what guidance you seek today."
            }
            appendAstroMessage(greeting)
            lastUserMessageAt = System.currentTimeMillis()
            scheduleIdle()
        }
    }

    // ------------------------------------------------------------------
    // Sending with humanizing delays (website astro-chat.js parity).
    // ------------------------------------------------------------------

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val slug = _activeSlug.value ?: return
        if (trimmed.isEmpty() || _sendLocked.value || _inputLocked.value) return
        if (billingSessionUuid == null) {
            _sessionError.value = "Chat session is not active."
            return
        }
        _sendLocked.value = true
        _sessionError.value = null
        lastUserMessageAt = System.currentTimeMillis()
        idleJob?.cancel()

        appendUserMessage(trimmed)

        deliveryJob?.cancel()
        deliveryJob = viewModelScope.launch {
            val gen = replyGeneration
            try {
                // 2-6s pre-typing pause.
                delay(Random.nextLong(2000, 6001))
                if (gen != replyGeneration) return@launch
                _isTyping.value = true

                // Build birth context for this turn.
                val birthCtx = buildBirthDataContext(
                    listOfNotNull(_birthAttachment.value)
                )
                val context = buildString {
                    if (birthCtx.isNotBlank()) appendLine(birthCtx)
                    if (!chatContext.isNullOrBlank()) appendLine(chatContext)
                }.ifBlank { null }
                val history = _messages.value
                    .filter { !it.isSystem }
                    .takeLast(10)
                    .map { ChatHistoryTurn(if (it.isUser) "user" else "assistant", it.text.take(4000)) }

                val result = chatRepository.sendAiMessage(
                    message = trimmed,
                    sessionId = aiSessionId,
                    persona = "general",
                    context = context,
                    history = history,
                    billingMode = "astrologer",
                    astroSession = billingSessionUuid,
                    ctxLabel = _birthAttachment.value?.let { "Birth Chart: ${it.name}" }
                )
                if (gen != replyGeneration) return@launch
                result.onSuccess { response ->
                    aiSessionId = response.sessionId ?: aiSessionId
                    if (!response.chatContext.isNullOrBlank()) {
                        chatContext = response.chatContext
                    }
                    // Mark user's last bubble as read when the reply arrives.
                    markLastUserRead()
                    deliverReplySentences(response.reply, gen)
                }.onFailure { err ->
                    if (gen != replyGeneration) return@launch
                    _isTyping.value = false
                    _sendLocked.value = false
                    // Center system notice on failure (never as astrologer bubble);
                    // keep text in input for one-tap retry (handled by UI reading lastFailedText).
                    _lastFailedText.value = trimmed
                    appendSystemMessage("Message could not be sent. Tap retry to send again.")
                    _sessionError.value = err.message
                    scheduleIdle()
                }
            } catch (e: Exception) {
                if (gen == replyGeneration) {
                    _isTyping.value = false
                    _sendLocked.value = false
                }
            }
        }
    }

    private val _lastFailedText = MutableStateFlow<String?>(null)
    val lastFailedText: StateFlow<String?> = _lastFailedText.asStateFlow()

    fun retryLastFailed() {
        val text = _lastFailedText.value ?: return
        _lastFailedText.value = null
        sendMessage(text)
    }

    fun clearLastFailed() {
        _lastFailedText.value = null
    }

    /**
     * Delivers the AI reply sentence-by-sentence with humanizing delays:
     * fresh typing bubble before every chunk; first bubble 2-3.5s, later
     * bubbles len/2-3.5 chars-per-sec clamped 3-12s; 0.7-1.3s between bubbles.
     */
    private suspend fun deliverReplySentences(reply: String, gen: Int) {
        val sentences = splitIntoSentences(reply)
        if (sentences.isEmpty()) {
            _isTyping.value = false
            _sendLocked.value = false
            scheduleIdle()
            return
        }
        sentences.forEachIndexed { index, sentence ->
            if (gen != replyGeneration) return
            // Typing bubble is already showing; wait the per-chunk typing time.
            val typingMs = if (index == 0) {
                Random.nextLong(2000, 3501)
            } else {
                val cps = Random.nextDouble(2.0, 3.5)
                (sentence.length / cps * 1000).toLong().coerceIn(3000, 12000)
            }
            delay(typingMs)
            if (gen != replyGeneration) return
            _isTyping.value = false
            appendAstroMessage(sentence)
            if (index < sentences.lastIndex) {
                // Human beat between bubbles, then typing resumes for next chunk.
                delay(Random.nextLong(700, 1301))
                if (gen != replyGeneration) return
                _isTyping.value = true
            }
        }
        _isTyping.value = false
        _sendLocked.value = false
        lastUserMessageAt = System.currentTimeMillis()
        scheduleIdle()
    }

    /**
     * Splits a reply into sentences — 1 sentence = 1 bubble.
     * Decimals like 3.5 are protected; Hindi danda (।) is a terminator.
     */
    private fun splitIntoSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val protected = text.replace(Regex("(\\d)\\.(\\d)"), "$1§DOT§$2")
        val parts = protected.split(Regex("(?<=[.!?।])\\s+"))
        return parts.map { it.replace("§DOT§", ".").trim() }
            .filter { it.isNotBlank() }
    }

    // ------------------------------------------------------------------
    // Timers: 1s display timer, 30s billing tick, idle nudge/close.
    // ------------------------------------------------------------------

    private fun startTimers() {
        timerJob?.cancel()
        tickJob?.cancel()
        // Display-only 1s timer.
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
        // 30s billing heartbeat (server is source of truth for money).
        tickJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                val uuid = billingSessionUuid ?: break
                tickBillingSessionUseCase(uuid).onSuccess { state ->
                    _sessionState.value = state
                    if (state.insufficient) {
                        // Mid-chat low balance -> hard close.
                        hardClose("Your wallet balance ran out. The chat has ended.")
                    }
                }.onFailure {
                    // Tick failed: pause billing display but keep chatting is unsafe —
                    // website closes the session when debit fails. Hard close here too.
                    hardClose("Billing update failed. The chat has ended.")
                }
            }
        }
    }

    private fun scheduleIdle() {
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            val gen = replyGeneration
            // 1-min nudge.
            delay(60_000)
            if (gen != replyGeneration) return@launch
            if (_activeSlug.value == null) return@launch
            val nudges = listOf(
                "Are you still there? I am here whenever you are ready.",
                "Take your time. I am listening.",
                "Do you have another question for me?"
            )
            appendAstroMessage(nudges.random())
            // 2-min total -> closing + hard close.
            delay(60_000)
            if (gen != replyGeneration) return@launch
            if (_activeSlug.value == null) return@launch
            appendAstroMessage("It seems you may be away. I am closing this chat for now — you can start a new one anytime.")
            delay(1500)
            if (gen != replyGeneration) return@launch
            endChat(showReview = true)
        }
    }

    // ------------------------------------------------------------------
    // End / close / review
    // ------------------------------------------------------------------

    /** User-initiated end: bill trailing period, close chat, show review card. */
    fun endChat(showReview: Boolean = true) {
        val uuid = billingSessionUuid
        val sessionId = chatSessionId
        stopTimers()
        replyGeneration++ // abort in-flight delivery
        _isTyping.value = false
        _sendLocked.value = false
        viewModelScope.launch {
            if (uuid != null) {
                endBillingSessionUseCase(uuid)
            }
            if (sessionId != null) {
                closeChatSessionUseCase(sessionId)
            }
        }
        billingSessionUuid = null
        chatSessionId = null
        _sessionState.value = null
        if (showReview && _activeSlug.value != null) {
            _showReview.value = true
        } else {
            resetChat()
        }
    }

    /** Server-forced close (billing failure / idle timeout). */
    private fun hardClose(reason: String) {
        appendSystemMessage(reason)
        endChat(showReview = true)
    }

    private fun stopTimers() {
        timerJob?.cancel()
        tickJob?.cancel()
        idleJob?.cancel()
        greetingJob?.cancel()
        deliveryJob?.cancel()
    }

    fun submitReview(stars: Int, text: String) {
        val slug = _activeSlug.value ?: return
        val sessionId = chatSessionId
        viewModelScope.launch {
            if (sessionId != null) {
                submitReviewUseCase(slug, stars, text, sessionId)
            }
            _reviewSubmitted.value = true
            _showReview.value = false
            resetChat()
        }
    }

    fun skipReview() {
        _showReview.value = false
        resetChat()
    }

    private fun resetChat() {
        stopTimers()
        replyGeneration++
        _activeSlug.value = null
        _activeAstrologer.value = null
        _messages.value = emptyList()
        _isTyping.value = false
        _sendLocked.value = false
        _inputLocked.value = false
        _showAddMoney.value = false
        _elapsedSeconds.value = 0
        _sessionError.value = null
        billingSessionUuid = null
        chatSessionId = null
        aiSessionId = null
        chatContext = null
    }

    fun dismissAddMoney() {
        _showAddMoney.value = false
    }

    /** Called after the user tops up — retry starting the billing session. */
    fun retryAfterTopUp() {
        _showAddMoney.value = false
        _inputLocked.value = false
        val slug = _activeSlug.value ?: return
        viewModelScope.launch {
            startBillingSessionUseCase(slug).onSuccess { state ->
                if (state.insufficient) {
                    _sessionState.value = state
                    _inputLocked.value = true
                    _showAddMoney.value = true
                    _sessionError.value = state.message ?: "Insufficient balance."
                    return@onSuccess
                }
                _sessionState.value = state
                billingSessionUuid = state.sessionUuid
                startTimers()
            }.onFailure {
                _sessionError.value = it.message ?: "Could not start billing session."
            }
        }
    }

    fun clearSessionError() {
        _sessionError.value = null
    }

    // ------------------------------------------------------------------
    // Message helpers
    // ------------------------------------------------------------------

    private fun appendUserMessage(text: String) {
        val msg = AstroChatMessage(
            id = "u-${System.currentTimeMillis()}",
            isUser = true,
            text = text
        )
        _messages.value = _messages.value + msg
    }

    private fun appendAstroMessage(text: String) {
        val msg = AstroChatMessage(
            id = "a-${System.currentTimeMillis()}-${Random.nextInt(1000)}",
            isUser = false,
            text = text
        )
        _messages.value = _messages.value + msg
    }

    private fun appendSystemMessage(text: String) {
        val msg = AstroChatMessage(
            id = "s-${System.currentTimeMillis()}",
            isUser = false,
            text = text,
            isSystem = true
        )
        _messages.value = _messages.value + msg
    }

    private fun markLastUserRead() {
        val list = _messages.value
        val idx = list.indexOfLast { it.isUser }
        if (idx >= 0 && !list[idx].read) {
            _messages.value = list.toMutableList().also {
                it[idx] = it[idx].copy(read = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimers()
    }
}

/**
 * UI message for the astrologer chat.
 * @param isSystem center system bubble (join notices, failures) — never rendered
 *   as the astrologer's bubble.
 * @param read read ticks on the user's last bubble once the reply arrives.
 */
data class AstroChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isSystem: Boolean = false,
    val read: Boolean = false
)
