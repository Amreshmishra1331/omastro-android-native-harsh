package com.omastro.app.ui.screens.aichat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.util.Locale

// ------------------------------------------------------------------
// Voice typing (SpeechRecognizer → text into the chat input)
// ------------------------------------------------------------------

/**
 * Wraps Android SpeechRecognizer with a simple callback API.
 * Language follows the chat language ("en" | "hi").
 */
class VoiceInputManager(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    var onResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onListeningChange: ((Boolean) -> Unit)? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(language: String) {
        if (!isAvailable) {
            onError?.invoke(W.t("voice_unavailable"))
            return
        }
        stopListening()
        val localeTag = if (language == "hi") "hi-IN" else "en-IN"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningChange?.invoke(true)
                    }
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        onListeningChange?.invoke(false)
                    }
                    override fun onError(error: Int) {
                        onListeningChange?.invoke(false)
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                W.t("voice_no_match")
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                W.t("voice_need_mic_perm")
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                W.t("voice_need_net")
                            SpeechRecognizer.ERROR_CLIENT,
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> null // silent retry path
                            else -> W.t("voice_failed")
                        }
                        msg?.let { onError?.invoke(it) }
                    }
                    override fun onResults(results: Bundle?) {
                        onListeningChange?.invoke(false)
                        val matches =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim()
                        if (!best.isNullOrBlank()) onResult?.invoke(best)
                        else onError?.invoke(W.t("voice_no_match"))
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            onListeningChange?.invoke(false)
            onError?.invoke(W.t("voice_could_not_start"))
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
        onListeningChange?.invoke(false)
    }
}

@Composable
fun rememberVoiceInputManager(): VoiceInputManager {
    val context = LocalContext.current
    val manager = remember { VoiceInputManager(context.applicationContext) }
    DisposableEffect(manager) {
        onDispose { manager.stopListening() }
    }
    return manager
}

// ------------------------------------------------------------------
// Voice mode (TextToSpeech readout of AI replies)
// ------------------------------------------------------------------

/**
 * App-wide TTS manager for the AI-chat voice mode.
 * Speaks AI replies aloud (en/hi); strips markdown before speaking.
 */
object VoiceModeManager {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingLanguage = "en"
    var isSpeaking = mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) applyLanguage(pendingLanguage)
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking.value = false
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking.value = false
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking.value = false
                }
            })
        }
    }

    private fun applyLanguage(language: String) {
        pendingLanguage = language
        val t = tts ?: return
        val locale = if (language == "hi") Locale("hi", "IN") else Locale("en", "IN")
        val res = t.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            t.language = Locale.ENGLISH
        }
    }

    /** Speaks [text] aloud; markdown stripped for clean speech. */
    fun speak(text: String, language: String = "en") {
        val t = tts
        if (t == null || !ready) {
            // Lazy init path: init on first use from an Activity context is done
            // by the caller; if still not ready, silently skip.
            return
        }
        applyLanguage(language)
        t.stop()
        val clean = stripMarkdown(text).take(4000)
        if (clean.isNotBlank()) {
            t.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "omastro_voice_mode")
        }
    }

    fun stop() {
        tts?.stop()
        isSpeaking.value = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        ready = false
        isSpeaking.value = false
    }

    /** Removes markdown so TTS reads natural prose. */
    internal fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("#{1,6}\\s*"), "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("_(.*?)_"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace(Regex("^\\s*[-*+]\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}

// ------------------------------------------------------------------
// Voice mode preference (persisted speaker toggle)
// ------------------------------------------------------------------

/** Persists the AI-chat voice-mode (spoken replies) toggle. */
object VoiceModePrefs {
    private const val PREFS = "omastro_voice_prefs"
    private const val KEY_VOICE_MODE = "voice_mode_on"

    private val _enabled = mutableStateOf(false)
    val enabled: androidx.compose.runtime.State<Boolean> = _enabled

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            _enabled.value = prefs!!.getBoolean(KEY_VOICE_MODE, false)
        }
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        prefs?.edit()?.putBoolean(KEY_VOICE_MODE, on)?.apply()
        if (!on) VoiceModeManager.stop()
    }
}

// ------------------------------------------------------------------
// Runtime permission UX (rationale → request → graceful denial)
// ------------------------------------------------------------------

/**
 * Requests a single runtime permission with a rationale dialog.
 * Usage:
 *   val req = rememberPermissionRequest(Manifest.permission.RECORD_AUDIO,
 *       title = "Microphone access",
 *       rationale = "Voice typing needs the microphone ...")
 *   req.request { granted -> ... }
 */
class PermissionRequest(
    private val permission: String,
    title: String,
    rationale: String
) {
    private var launcher: ((String) -> Unit)? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null
    var title: String = title
        private set
    var rationale: String = rationale
        private set
    var showRationale by mutableStateOf(false)
        private set
    var showDenied by mutableStateOf(false)
        private set

    internal fun bind(launch: (String) -> Unit) {
        launcher = launch
    }

    /** Refreshes title/rationale so language changes apply live. */
    internal fun updateLabels(title: String, rationale: String) {
        this.title = title
        this.rationale = rationale
    }

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    fun request(context: Context, onResult: (Boolean) -> Unit) {
        if (isGranted(context)) {
            onResult(true)
            return
        }
        pendingCallback = onResult
        showRationale = true
    }

    internal fun onRationaleAccepted() {
        showRationale = false
        launcher?.invoke(permission)
    }

    internal fun onRationaleDismissed() {
        showRationale = false
        pendingCallback?.invoke(false)
        pendingCallback = null
    }

    internal fun onPermissionResult(granted: Boolean) {
        if (!granted) showDenied = true
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }

    internal fun dismissDenied() {
        showDenied = false
    }
}

@Composable
fun rememberPermissionRequest(
    permission: String,
    title: String,
    rationale: String
): PermissionRequest {
    val req = remember(permission) { PermissionRequest(permission, title, rationale) }
    // Keep labels live: recomposition (driven by LangPrefs) refreshes them.
    req.updateLabels(title, rationale)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        req.onPermissionResult(granted)
    }
    req.bind { launcher.launch(it) }

    if (req.showRationale) {
        AlertDialog(
            onDismissRequest = { req.onRationaleDismissed() },
            title = {
                Text(req.title, color = StarlightWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        req.rationale,
                        color = StarlightWhite.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        W.t("perm_settings_note"),
                        color = StarlightWhite.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { req.onRationaleAccepted() }) {
                    Text(W.t("perm_allow"), color = SacredGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { req.onRationaleDismissed() }) {
                    Text(
                        W.t("perm_not_now"),
                        color = StarlightWhite.copy(alpha = 0.7f)
                    )
                }
            },
            containerColor = CosmicNavy
        )
    }

    if (req.showDenied) {
        AlertDialog(
            onDismissRequest = { req.dismissDenied() },
            title = {
                Text(req.title, color = StarlightWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    W.t("perm_denied_msg"),
                    color = StarlightWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { req.dismissDenied() }) {
                    Text(W.t("perm_ok"), color = SacredGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicNavy
        )
    }

    return req
}
