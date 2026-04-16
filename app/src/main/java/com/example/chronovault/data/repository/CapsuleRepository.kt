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

    fun getPublicCapsules(): Flow<List<CapsuleEntity>> {
        return capsuleDao.getPublicCapsules()
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

    suspend fun markCapsuleDiscovered(capsuleId: String) {
        capsuleDao.markCapsuleDiscovered(capsuleId)
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

    fun getCapsulesForMap(userId: String): Flow<List<CapsuleEntity>> {
        return capsuleDao.getCapsulesForMap(userId)
    }

    suspend fun makeCapsulePrivate(capsuleId: String) {
        capsuleDao.updateSharingEnabled(capsuleId, false)
        capsuleDao.clearSharedWith(capsuleId)
    }

    suspend fun updateSharingEnabled(capsuleId: String, enabled: Boolean) {
        capsuleDao.updateSharingEnabled(capsuleId, enabled)
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
                    "imageBase64" to (capsule.imageBase64 ?: ""), // Base64 image stored in Firestore
                    "imageMimeType" to capsule.imageMimeType,
                    "latitude" to capsule.latitude,
                    "longitude" to capsule.longitude,
                    "unlockTime" to (capsule.unlockTime ?: 0),
                    "unlockLatitude" to (capsule.unlockLatitude ?: 0.0),
                    "unlockLongitude" to (capsule.unlockLongitude ?: 0.0),
                    "isLocationBased" to capsule.isLocationBased,
                    "isTimeBased" to capsule.isTimeBased,
                    "isPublic" to capsule.isPublic,
                    "canBeShared" to capsule.canBeShared,
                    "sharedWith" to capsule.sharedWith
                )
                firestoreCapsuleService.createCapsule(userId, data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCapsuleFromCloud(capsuleId: String): Result<CapsuleEntity> {
        return try {
            val result = firestoreCapsuleService.getCapsuleById(capsuleId)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
            val data = result.getOrThrow()
            val normalizedId = (data["clientId"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: (data["id"] as? String)
                ?: capsuleId
            val capsule = CapsuleEntity(
                id = normalizedId,
                title = data["title"] as? String ?: "",
                message = data["message"] as? String ?: "",
                imageBase64 = data["imageBase64"] as? String,
                imageMimeType = data["imageMimeType"] as? String ?: "image/jpeg",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                unlockTime = (data["unlockTime"] as? Number)?.toLong(),
                unlockLatitude = (data["unlockLatitude"] as? Number)?.toDouble(),
                unlockLongitude = (data["unlockLongitude"] as? Number)?.toDouble(),
                isUnlocked = data["isUnlocked"] as? Boolean ?: false,
                isLocationBased = data["isLocationBased"] as? Boolean ?: false,
                isTimeBased = data["isTimeBased"] as? Boolean ?: false,
                ownerId = data["ownerId"] as? String ?: "",
                sharedWith = (data["sharedWith"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isPublic = data["isPublic"] as? Boolean ?: false,
                canBeShared = data["canBeShared"] as? Boolean ?: false
            )
            insertCapsule(capsule)
            Result.success(capsule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreUserCapsulesFromCloudIfLocalEmpty(userId: String): Result<Int> {
        return try {
            val localCount = capsuleDao.getTotalCapsuleCount(userId)
            if (localCount > 0) return Result.success(0)

            val remote = firestoreCapsuleService.getUserCapsules(userId)
            if (remote.isFailure) {
                return Result.failure(remote.exceptionOrNull() ?: Exception("Failed to fetch remote capsules"))
            }

            val remoteCapsules = remote.getOrThrow()
            var restored = 0
            remoteCapsules.forEach { data ->
                val id = ((data["clientId"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (data["id"] as? String)) ?: return@forEach
                val capsule = CapsuleEntity(
                    id = id,
                    title = data["title"] as? String ?: "",
                    message = data["message"] as? String ?: "",
                    imageBase64 = (data["imageBase64"] as? String)?.ifBlank { null },
                    imageMimeType = data["imageMimeType"] as? String ?: "image/jpeg",
                    latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    unlockTime = (data["unlockTime"] as? Number)?.toLong(),
                    unlockLatitude = (data["unlockLatitude"] as? Number)?.toDouble(),
                    unlockLongitude = (data["unlockLongitude"] as? Number)?.toDouble(),
                    isUnlocked = data["isUnlocked"] as? Boolean ?: false,
                    isLocationBased = data["isLocationBased"] as? Boolean ?: false,
                    isTimeBased = data["isTimeBased"] as? Boolean ?: false,
                    ownerId = data["ownerId"] as? String ?: userId,
                    sharedWith = (data["sharedWith"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    isPublic = data["isPublic"] as? Boolean ?: false,
                    canBeShared = data["canBeShared"] as? Boolean ?: false
                )
                capsuleDao.insertCapsule(capsule)
                restored += 1
            }

            Result.success(restored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicCapsulesFromCloud(): Result<List<CapsuleEntity>> {
        return firestoreCapsuleService.getPublicCapsules().mapCatching { payload ->
            payload.mapNotNull { data ->
                val id = ((data["clientId"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (data["id"] as? String)) ?: return@mapNotNull null
                CapsuleEntity(
                    id = id,
                    title = data["title"] as? String ?: "",
                    message = data["message"] as? String ?: "",
                    imageBase64 = (data["imageBase64"] as? String)?.ifBlank { null },
                    imageMimeType = data["imageMimeType"] as? String ?: "image/jpeg",
                    latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    unlockTime = (data["unlockTime"] as? Number)?.toLong(),
                    unlockLatitude = (data["unlockLatitude"] as? Number)?.toDouble(),
                    unlockLongitude = (data["unlockLongitude"] as? Number)?.toDouble(),
                    isUnlocked = data["isUnlocked"] as? Boolean ?: false,
                    isLocationBased = data["isLocationBased"] as? Boolean ?: false,
                    isTimeBased = data["isTimeBased"] as? Boolean ?: false,
                    ownerId = data["ownerId"] as? String ?: "",
                    sharedWith = (data["sharedWith"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    isPublic = data["isPublic"] as? Boolean ?: false,
                    canBeShared = data["canBeShared"] as? Boolean ?: false,
                    isSharedWithMe = false,
                    isDiscovered = data["isDiscovered"] as? Boolean ?: false,
                    sharedByName = data["sharedByName"] as? String,
                    sharedAt = (data["sharedAt"] as? Number)?.toLong()
                )
            }
        }
    }
}

