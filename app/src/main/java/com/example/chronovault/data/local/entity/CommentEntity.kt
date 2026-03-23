package com.example.chronovault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a comment on a shared capsule
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val id: String,
    val capsuleId: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long
)

