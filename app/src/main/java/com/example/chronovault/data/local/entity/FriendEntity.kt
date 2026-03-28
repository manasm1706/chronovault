package com.example.chronovault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local friend model used for social layer foundation.
 */
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val friendUserId: String,
    val status: FriendStatus,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class FriendStatus {
    PENDING,
    ACCEPTED
}

