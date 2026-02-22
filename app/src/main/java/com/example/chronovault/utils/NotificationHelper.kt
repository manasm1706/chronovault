package com.example.chronovault.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.chronovault.R

/**
 * Helper class for managing notifications
 */
object NotificationHelper {

    const val CHANNEL_ID = "chronovault_notifications"
    const val CHANNEL_NAME = "ChronoVault Notifications"
    const val NOTIFICATION_ID_UNLOCK = 1001
    const val NOTIFICATION_ID_NEARBY = 1002
    const val NOTIFICATION_ID_SHARED = 1003

    /**
     * Create notification channel (required for Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build a notification for capsule unlock
     */
    fun buildUnlockNotification(
        context: Context,
        capsuleTitle: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Capsule Unlocked!")
            .setContentText("$capsuleTitle is now ready to open")
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary))
            .setStyle(NotificationCompat.BigTextStyle().bigText("$capsuleTitle is now ready to open"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    /**
     * Build a notification for nearby capsule
     */
    fun buildNearbyNotification(
        context: Context,
        capsuleTitle: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_home_black_24dp)
            .setContentTitle("Memory Location Nearby!")
            .setContentText("You're near: $capsuleTitle")
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    /**
     * Build a notification for shared capsule
     */
    fun buildSharedNotification(
        context: Context,
        fromName: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("New Shared Capsule")
            .setContentText("$fromName shared a memory with you")
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    /**
     * Show notification
     */
    fun showNotification(context: Context, notificationId: Int, builder: NotificationCompat.Builder) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}

