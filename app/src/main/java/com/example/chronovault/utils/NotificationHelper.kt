package com.example.chronovault.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

    const val CHANNEL_ID = "chronovault_channel"
    const val CHANNEL_NAME = "ChronoVault Notifications"
    const val CHANNEL_DESCRIPTION = "Updates for memories, discovery, and sharing"
    const val NOTIFICATION_ID_UNLOCK = 1001
    const val NOTIFICATION_ID_NEARBY = 1002
    const val NOTIFICATION_ID_SHARED = 1003
    const val NOTIFICATION_ID_CHAT = 1004
    const val NOTIFICATION_ID_CREATED = 1005
    const val NOTIFICATION_ID_DISCOVERED = 1006

    /**
     * Create notification channel (required for Android 8.0+)
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Backward-compatible alias used by existing code.
    fun createNotificationChannel(context: Context) = createChannel(context)

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        contentIntent: PendingIntent? = null
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(resolveThemePrimaryColor(context))
            .setVibrate(longArrayOf(0, 300))

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
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
        showNotification(context, title, message, NOTIFICATION_ID_UNLOCK)
    }

    /**
     * Send location-based unlock notification
     */
    fun sendLocationBasedUnlockNotification(context: Context, title: String, message: String) {
        showNotification(context, title, message, NOTIFICATION_ID_NEARBY)
    }

    /**
     * Send shared capsule notification
     */
    fun sendSharedNotification(context: Context, title: String, message: String) {
        showNotification(context, title, message, NOTIFICATION_ID_SHARED)
    }

    fun sendMemoryDiscoveredNotification(context: Context) {
        showNotification(
            context = context,
            title = "Memory Discovered \uD83D\uDCCD",
            message = "You found a memory nearby. Tap to explore.",
            notificationId = NOTIFICATION_ID_DISCOVERED
        )
    }

    fun sendCapsuleCreatedNotification(context: Context) {
        showNotification(
            context = context,
            title = "Capsule Created \uD83D\uDCE6",
            message = "Your memory has been safely stored.",
            notificationId = NOTIFICATION_ID_CREATED
        )
    }

    fun sendNearbyCapsuleAlert(context: Context) {
        showNotification(
            context = context,
            title = "You're near a memory \uD83D\uDC40",
            message = "A memory is waiting nearby.",
            notificationId = NOTIFICATION_ID_NEARBY
        )
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
            .setSmallIcon(R.mipmap.ic_launcher)
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
