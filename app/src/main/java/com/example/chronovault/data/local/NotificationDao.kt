package com.example.chronovault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chronovault.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for local notification feed.
 */
@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteById(notificationId: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

