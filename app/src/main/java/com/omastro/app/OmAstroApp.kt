package com.omastro.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.omastro.app.data.local.ThemePrefs
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OmAstroApp : Application() {

    companion object {
        /** Process-wide application instance, set in [onCreate]. */
        lateinit var instance: OmAstroApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemePrefs.init(this)
        com.omastro.app.data.local.LangPrefs.init(this)
        // Voice mode (spoken AI replies) — prefs + TTS engine
        com.omastro.app.ui.screens.aichat.VoiceModePrefs.init(this)
        com.omastro.app.ui.screens.aichat.VoiceModeManager.init(this)
        // Initialize Firebase Cloud Messaging token synchronization.
        // Sync on every cold start (not just onNewToken) so fresh installs
        // register for push immediately.
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            AppContainer.authRepository.updateFcmToken(token)
                        } catch (e: Exception) {
                            // Ignore transient network errors on cold-start sync
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Firebase optional in dev/emulator
        }
    }
}
