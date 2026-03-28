package com.example.chronovault.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import com.example.chronovault.R
import com.example.chronovault.MainActivity

/**
 * Helper class for managing notifications
 */
object NotificationHelper {
    private fun resolveThemePrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            context.getColor(R.color.primary)
        }
    }


    const val CHANNEL_ID = "chronovault_notifications"
    const val CHANNEL_NAME = "ChronoVault Notifications"
    const val NOTIFICATION_ID_UNLOCK = 1001
    const val NOTIFICATION_ID_NEARBY = 1002
    const val NOTIFICATION_ID_SHARED = 1003
    const val NOTIFICATION_ID_CHAT = 1004

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
            .setColor(resolveThemePrimaryColor(context))
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
            .setColor(resolveThemePrimaryColor(context))
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
            .setColor(resolveThemePrimaryColor(context))
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

    /**
     * Send capsule unlocked notification
     */
    fun sendCapsuleUnlockedNotification(context: Context, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setColor(resolveThemePrimaryColor(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500))

        showNotification(context, NOTIFICATION_ID_UNLOCK, builder)
    }

    /**
     * Send location-based unlock notification
     */
    fun sendLocationBasedUnlockNotification(context: Context, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setColor(resolveThemePrimaryColor(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500))

        showNotification(context, NOTIFICATION_ID_NEARBY, builder)
    }

    /**
     * Send shared capsule notification
     */
    fun sendSharedNotification(context: Context, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setColor(resolveThemePrimaryColor(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500))

        showNotification(context, NOTIFICATION_ID_SHARED, builder)
    }

    fun sendChatMessageNotification(
        context: Context,
        chatId: String,
        otherUserId: String,
        senderName: String,
        message: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_CHAT_ID, chatId)
            putExtra(EXTRA_NAV_CHAT_USER_ID, otherUserId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("New message from $senderName")
            .setContentText(message)
            .setAutoCancel(true)
            .setColor(resolveThemePrimaryColor(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        showNotification(context, NOTIFICATION_ID_CHAT, builder)
    }

    const val EXTRA_NAV_CHAT_ID = "extra_nav_chat_id"
    const val EXTRA_NAV_CHAT_USER_ID = "extra_nav_chat_user_id"
}
