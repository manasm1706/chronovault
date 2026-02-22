package com.example.chronovault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Time Capsule
 */
@Entity(tableName = "capsules")
data class CapsuleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val message: String,
    val imagePath: String? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val unlockTime: Long? = null,
    val unlockLatitude: Double? = null,
    val unlockLongitude: Double? = null,
    val isUnlocked: Boolean = false,
    val isLocationBased: Boolean = false,
    val isTimeBased: Boolean = false,
    val ownerId: String,
    val sharedWith: List<String> = emptyList(),
    val canBeShared: Boolean = false,
    val isSharedWithMe: Boolean = false,
    val sharedByName: String? = null,
    val sharedAt: Long? = null
)

