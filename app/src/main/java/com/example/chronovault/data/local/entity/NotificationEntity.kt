package com.example.chronovault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for in-app notifications.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: NotificationType,
    val typeCategory: NotificationCategory,
    val capsuleId: String? = null
)

enum class NotificationType {
    UNLOCK,
    NEARBY,
    SHARE,
    CHAT
}

enum class NotificationCategory {
    PERSONAL,
    WORLD
}

