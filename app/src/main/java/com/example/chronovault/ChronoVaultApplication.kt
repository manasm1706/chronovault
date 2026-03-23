package com.example.chronovault

import android.app.Application
import com.example.chronovault.utils.NotificationHelper
import com.example.chronovault.workers.WorkScheduler

/**
 * ChronoVaultApplication - Custom Application class
 * Initializes WorkManager, notifications, and other services
 */
class ChronoVaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Create notification channels
            NotificationHelper.createNotificationChannel(this)
        } catch (e: Exception) {
            android.util.Log.e("ChronoVault", "Failed to create notification channel", e)
        }

        try {
            // Schedule background workers
            WorkScheduler.scheduleAllWorkers(this)
        } catch (e: Exception) {
            android.util.Log.e("ChronoVault", "Failed to schedule workers", e)
        }
    }
}

