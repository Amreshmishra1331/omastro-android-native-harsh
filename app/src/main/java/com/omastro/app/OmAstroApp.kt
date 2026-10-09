package com.omastro.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.omastro.app.data.local.ThemePrefs
import com.omastro.app.di.AppContainer

class OmAstroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePrefs.init(this)
        com.omastro.app.data.local.LangPrefs.init(this)
        // Voice mode (spoken AI replies) — prefs + TTS engine
        com.omastro.app.ui.screens.aichat.VoiceModePrefs.init(this)
        com.omastro.app.ui.screens.aichat.VoiceModeManager.init(this)
        // Initialize Firebase Cloud Messaging token synchronization
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    // Token available for push notifications
                }
            }
        } catch (e: Exception) {
            // Firebase optional in dev/emulator
        }
    }
}
