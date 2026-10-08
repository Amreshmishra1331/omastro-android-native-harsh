package com.omastro.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.omastro.app.di.AppContainer

class OmAstroApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
