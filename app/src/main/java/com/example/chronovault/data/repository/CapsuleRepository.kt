package com.example.chronovault.data.repository

import com.example.chronovault.data.local.CapsuleDao
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.remote.firebase.FirestoreCapsuleService
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Capsule data operations
 * Handles interaction between local database, Firebase Firestore, and UI layer
 */
class CapsuleRepository(
    private val capsuleDao: CapsuleDao,
    private val firestoreCapsuleService: FirestoreCapsuleService,
    private val preferencesManager: PreferencesManager
) {

    fun getUserCapsules(userId: String): Flow<List<CapsuleEntity>> {
        return capsuleDao.getUserCapsules(userId)
    }

    fun getLockedCapsules(userId: String): Flow<List<CapsuleEntity>> {
        return capsuleDao.getLockedCapsules(userId)
    }

    fun getUnlockedCapsules(userId: String): Flow<List<CapsuleEntity>> {
        return capsuleDao.getUnlockedCapsules(userId)
    }

    fun getSharedCapsules(userId: String): Flow<List<CapsuleEntity>> {
        return capsuleDao.getSharedCapsules(userId)
    }

    suspend fun insertCapsule(capsule: CapsuleEntity) {
        capsuleDao.insertCapsule(capsule)
    }

    suspend fun updateCapsule(capsule: CapsuleEntity) {
        capsuleDao.updateCapsule(capsule)
    }

    suspend fun deleteCapsule(capsuleId: String) {
        capsuleDao.deleteCapsuleById(capsuleId)
    }

    suspend fun getCapsuleById(capsuleId: String): CapsuleEntity? {
        return capsuleDao.getCapsuleById(capsuleId)
    }

    suspend fun unlockCapsule(capsuleId: String) {
        capsuleDao.unlockCapsule(capsuleId)
    }

    suspend fun getTotalCapsuleCount(userId: String): Int {
        return capsuleDao.getTotalCapsuleCount(userId)
    }

    suspend fun getLockedCapsuleCount(userId: String): Int {
        return capsuleDao.getLockedCapsuleCount(userId)
    }

    suspend fun getUnlockedCapsuleCount(userId: String): Int {
        return capsuleDao.getUnlockedCapsuleCount(userId)
    }

    suspend fun getSharedCapsuleCount(userId: String): Int {
        return capsuleDao.getSharedCapsuleCount(userId)
    }

    suspend fun checkTimeBasedUnlocks(currentTime: Long): List<CapsuleEntity> {
        return capsuleDao.getUnlockedByTime(currentTime)
    }

    suspend fun getLocationBasedCapsules(): List<CapsuleEntity> {
        return capsuleDao.getLocationBasedCapsules()
    }

    // Firebase Firestore methods
    suspend fun createCapsuleOnFirebase(capsuleData: Map<String, Any>): Result<String> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return firestoreCapsuleService.createCapsule(userId, capsuleData)
    }

    suspend fun syncCapsulesToCloud(capsules: List<CapsuleEntity>): Result<Unit> {
        return try {
            val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
            capsules.forEach { capsule ->
                val data = mapOf(
                    "title" to capsule.title,
                    "message" to capsule.message,
                    "imagePath" to (capsule.imagePath ?: ""),
                    "latitude" to capsule.latitude,
                    "longitude" to capsule.longitude,
                    "unlockTime" to (capsule.unlockTime ?: 0),
                    "unlockLatitude" to (capsule.unlockLatitude ?: 0.0),
                    "unlockLongitude" to (capsule.unlockLongitude ?: 0.0),
                    "isLocationBased" to capsule.isLocationBased,
                    "isTimeBased" to capsule.isTimeBased,
                    "canBeShared" to capsule.canBeShared
                )
                firestoreCapsuleService.createCapsule(userId, data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

