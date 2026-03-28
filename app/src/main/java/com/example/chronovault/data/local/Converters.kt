package com.example.chronovault.data.local

import androidx.room.TypeConverter
import com.example.chronovault.data.local.entity.FriendStatus
import com.example.chronovault.data.local.entity.NotificationCategory
import com.example.chronovault.data.local.entity.NotificationType

/**
 * Room TypeConverters for ChronoVaultDatabase
 * Handles conversion of non-primitive types to/from storable formats
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType =
        runCatching { NotificationType.valueOf(value) }.getOrDefault(NotificationType.UNLOCK)

    @TypeConverter
    fun fromNotificationCategory(value: NotificationCategory): String = value.name

    @TypeConverter
    fun toNotificationCategory(value: String): NotificationCategory =
        runCatching { NotificationCategory.valueOf(value) }.getOrDefault(NotificationCategory.PERSONAL)

    @TypeConverter
    fun fromFriendStatus(value: FriendStatus): String = value.name

    @TypeConverter
    fun toFriendStatus(value: String): FriendStatus =
        runCatching { FriendStatus.valueOf(value) }.getOrDefault(FriendStatus.PENDING)
}

