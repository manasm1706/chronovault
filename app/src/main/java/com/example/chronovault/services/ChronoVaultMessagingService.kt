package com.example.chronovault.services

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.example.chronovault.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * ChronoVaultMessagingService - Handle Firebase Cloud Messaging notifications
 * Receives notifications from Firebase and displays them to the user
 */
class ChronoVaultMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        try {
            // Create notification channel
            NotificationHelper.createNotificationChannel(this)

            // Handle data messages
            if (remoteMessage.data.isNotEmpty()) {
                handleDataMessage(remoteMessage.data)
            }

            // Handle notification messages
            remoteMessage.notification?.let {
                handleNotificationMessage(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message: ${e.message}")
            // Silently fail - don't crash app
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        try {
            // Send token to backend if needed
            sendTokenToBackend(token)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new token: ${e.message}")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        try {
            val title = data["title"] ?: "ChronoVault"
            val message = data["message"] ?: ""
            val type = data["type"] ?: "other"

            when (type) {
                "unlock" -> {
                    NotificationHelper.sendCapsuleUnlockedNotification(this, title, message)
                }
                "location_unlock" -> {
                    NotificationHelper.sendLocationBasedUnlockNotification(this, title, message)
                }
                "shared" -> {
                    NotificationHelper.sendSharedNotification(this, title, message)
                }
                else -> {
                    NotificationHelper.sendCapsuleUnlockedNotification(this, title, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling data message: ${e.message}")
        }
    }

    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        try {
            val title = notification.title ?: "ChronoVault"
            val message = notification.body ?: ""

            NotificationHelper.sendCapsuleUnlockedNotification(this, title, message)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification message: ${e.message}")
        }
    }

    private fun sendTokenToBackend(token: String) {
        try {
            // Optional: Send FCM token to your backend
            Log.d(TAG, "Sending token to backend: $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending token: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ChronoVaultMessaging"
    }
}

