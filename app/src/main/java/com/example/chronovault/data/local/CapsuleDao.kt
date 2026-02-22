package com.example.chronovault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chronovault.data.local.entity.CapsuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Capsule entities
 */
@Dao
interface CapsuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapsule(capsule: CapsuleEntity)

    @Update
    suspend fun updateCapsule(capsule: CapsuleEntity)

    @Delete
    suspend fun deleteCapsule(capsule: CapsuleEntity)

    @Query("SELECT * FROM capsules WHERE id = :capsuleId")
    suspend fun getCapsuleById(capsuleId: String): CapsuleEntity?

    @Query("SELECT * FROM capsules WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun getUserCapsules(ownerId: String): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsules WHERE ownerId = :ownerId AND isUnlocked = 0 ORDER BY createdAt DESC")
    fun getLockedCapsules(ownerId: String): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsules WHERE ownerId = :ownerId AND isUnlocked = 1 ORDER BY createdAt DESC")
    fun getUnlockedCapsules(ownerId: String): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsules WHERE isSharedWithMe = 1 AND ownerId != :userId ORDER BY sharedAt DESC")
    fun getSharedCapsules(userId: String): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsules WHERE isUnlocked = 0 AND isTimeBased = 1 AND unlockTime <= :currentTime")
    suspend fun getUnlockedByTime(currentTime: Long): List<CapsuleEntity>

    @Query("SELECT * FROM capsules WHERE isUnlocked = 0 AND isLocationBased = 1")
    suspend fun getLocationBasedCapsules(): List<CapsuleEntity>

    @Query("UPDATE capsules SET isUnlocked = 1 WHERE id = :capsuleId")
    suspend fun unlockCapsule(capsuleId: String)

    @Query("DELETE FROM capsules WHERE id = :capsuleId")
    suspend fun deleteCapsuleById(capsuleId: String)

    @Query("SELECT COUNT(*) FROM capsules WHERE ownerId = :ownerId")
    suspend fun getTotalCapsuleCount(ownerId: String): Int

    @Query("SELECT COUNT(*) FROM capsules WHERE ownerId = :ownerId AND isUnlocked = 0")
    suspend fun getLockedCapsuleCount(ownerId: String): Int

    @Query("SELECT COUNT(*) FROM capsules WHERE ownerId = :ownerId AND isUnlocked = 1")
    suspend fun getUnlockedCapsuleCount(ownerId: String): Int

    @Query("SELECT COUNT(*) FROM capsules WHERE isSharedWithMe = 1 AND ownerId != :userId")
    suspend fun getSharedCapsuleCount(userId: String): Int
}

