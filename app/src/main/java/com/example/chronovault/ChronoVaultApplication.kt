package com.example.chronovault

import android.app.Application
import com.example.chronovault.utils.NotificationHelper
import com.example.chronovault.workers.WorkScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * ChronoVaultApplication - Custom Application class
 * Initializes WorkManager, notifications, and other services
 */
class ChronoVaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Create notification channels
            NotificationHelper.createChannel(this)
        } catch (e: Exception) {
            android.util.Log.e("ChronoVault", "Failed to create notification channel", e)
        }

        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .set(mapOf("fcmToken" to token), SetOptions.merge())
                }
                .addOnFailureListener { error ->
                    android.util.Log.e("ChronoVault", "Failed to fetch FCM token", error)
                }
        } catch (e: Exception) {
            android.util.Log.e("ChronoVault", "FCM token bootstrap failed", e)
        }

        try {
            // Schedule background workers
            WorkScheduler.scheduleAllWorkers(this)
        } catch (e: Exception) {
            android.util.Log.e("ChronoVault", "Failed to schedule workers", e)
        }
    }
}

