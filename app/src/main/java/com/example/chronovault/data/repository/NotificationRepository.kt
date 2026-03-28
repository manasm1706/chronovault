package com.example.chronovault.data.repository

import com.example.chronovault.data.local.NotificationDao
import com.example.chronovault.data.local.entity.NotificationCategory
import com.example.chronovault.data.local.entity.NotificationEntity
import com.example.chronovault.data.local.entity.NotificationType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for local app notifications.
 */
class NotificationRepository(
    private val notificationDao: NotificationDao
) {

    fun observeNotifications(): Flow<List<NotificationEntity>> = notificationDao.observeNotifications()

    fun getUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    suspend fun upsert(notification: NotificationEntity) {
        notificationDao.upsert(notification)
    }

    suspend fun markAsRead(notificationId: String) {
        notificationDao.markAsRead(notificationId)
    }

    suspend fun deleteNotification(notificationId: String) {
        notificationDao.deleteById(notificationId)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }

    suspend fun createUnlockNotification(capsuleId: String, capsuleTitle: String, source: String) {
        val notification = NotificationEntity(
            id = "unlock_${source}_$capsuleId",
            title = "Capsule Unlocked!",
            message = "'$capsuleTitle' is now ready to open",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.UNLOCK,
            typeCategory = NotificationCategory.PERSONAL,
            capsuleId = capsuleId
        )
        upsert(notification)
    }

    suspend fun createNearbyNotification(capsuleId: String, capsuleTitle: String) {
        val notification = NotificationEntity(
            id = "nearby_$capsuleId",
            title = "You're near a memory",
            message = "You are within 50m of '$capsuleTitle'",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.NEARBY,
            typeCategory = NotificationCategory.PERSONAL,
            capsuleId = capsuleId
        )
        upsert(notification)
    }

    suspend fun createSharedNotification(capsuleId: String?, capsuleTitle: String) {
        val safeId = capsuleId ?: "unknown_${System.currentTimeMillis()}"
        val notification = NotificationEntity(
            id = "shared_$safeId",
            title = "New memory shared with you",
            message = "'$capsuleTitle' is now available in your world feed",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.SHARE,
            typeCategory = NotificationCategory.WORLD,
            capsuleId = capsuleId
        )
        upsert(notification)
    }

    suspend fun createChatMessageNotification(chatId: String, senderId: String, preview: String) {
        val notification = NotificationEntity(
            id = "chat_${chatId}_${System.currentTimeMillis()}",
            title = "New message from $senderId",
            message = preview,
            timestamp = System.currentTimeMillis(),
            type = NotificationType.CHAT,
            typeCategory = NotificationCategory.WORLD,
            capsuleId = "$chatId|$senderId"
        )
        upsert(notification)
    }
}

