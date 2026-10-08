package com.omastro.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.omastro.app.MainActivity
import com.omastro.app.R
import com.omastro.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OmAstroFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Auto-sync token to WordPress Backend via REST API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authRepo = AppContainer.authRepository
                authRepo.updateFcmToken(token)
            } catch (e: Exception) {
                // Ignore transient network errors on token refresh
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "OmAstro AI"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["message"] 
            ?: remoteMessage.data["body"] 
            ?: "You have a new cosmic insight or astrological reply."
        val channelType = remoteMessage.data["type"] ?: "general"

        showNotification(title, body, channelType)
    }

    private fun showNotification(title: String, message: String, type: String) {
        val channelId = when (type) {
            "ai_chat", "chat" -> "omastro_chat_channel"
            "astrologer" -> "omastro_consultation_channel"
            "panchang", "horoscope" -> "omastro_daily_channel"
            else -> "omastro_general_channel"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when (type) {
                "ai_chat", "chat" -> "AI & Astrologer Replies"
                "astrologer" -> "Live Consultations"
                "panchang", "horoscope" -> "Daily Horoscope & Panchang"
                else -> "OmAstro Announcements"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for OmAstro AI insights and messages"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_om)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
