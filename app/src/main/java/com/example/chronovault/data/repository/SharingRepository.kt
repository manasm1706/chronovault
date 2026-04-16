package com.example.chronovault.data.repository

import android.util.Log
import com.example.chronovault.data.local.CapsuleDao
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.remote.firebase.FirebaseSharingService
import com.google.firebase.firestore.ListenerRegistration
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Repository for Capsule Sharing operations
 * Handles sharing permissions and shared capsule management
 */
class SharingRepository(
    private val firebaseSharingService: FirebaseSharingService,
    private val capsuleDao: CapsuleDao,
    private val preferencesManager: PreferencesManager
) {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sharedListeners = mutableMapOf<String, ListenerRegistration>()

    suspend fun shareCapsuleWithUser(capsuleId: String, userId: String): Result<Unit> {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isBlank()) return Result.failure(Exception("User ID is required"))

        val direct = firebaseSharingService.shareCapsule(capsuleId, normalizedUserId)
        if (direct.isSuccess) return direct

        val ownerId = preferencesManager.getUserId().orEmpty()
        val local = capsuleDao.getCapsuleById(capsuleId)
            ?: return direct

        val resolvedRemoteId = firebaseSharingService.findRemoteCapsuleIdForOwner(
            ownerId = ownerId,
            title = local.title,
            latitude = local.latitude,
            longitude = local.longitude,
            clientId = local.id
        ).getOrNull()

        if (resolvedRemoteId.isNullOrBlank()) return direct
        return firebaseSharingService.shareCapsule(resolvedRemoteId, normalizedUserId)
    }

    suspend fun resolveRemoteCapsuleId(localCapsuleId: String): String {
        val ownerId = preferencesManager.getUserId().orEmpty()
        if (ownerId.isBlank()) return localCapsuleId

        val local = capsuleDao.getCapsuleById(localCapsuleId) ?: return localCapsuleId
        val resolvedRemoteId = firebaseSharingService.findRemoteCapsuleIdForOwner(
            ownerId = ownerId,
            title = local.title,
            latitude = local.latitude,
            longitude = local.longitude,
            clientId = local.id
        ).getOrNull()

        return resolvedRemoteId ?: localCapsuleId
    }

    suspend fun findSharedCapsuleByAnyId(identifier: String): Result<CapsuleEntity?> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return firebaseSharingService.getSharedWithMeCapsules(userId).mapCatching { payload ->
            payload
                .firstOrNull { data ->
                    val remoteId = data["id"] as? String
                    val clientId = data["clientId"] as? String
                    remoteId == identifier || clientId == identifier
                }
                ?.let { mapToSharedEntity(it, userId) }
        }
    }

    suspend fun unshareCapsuleWithUser(capsuleId: String, userId: String): Result<Unit> {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isBlank()) return Result.failure(Exception("User ID is required"))
        return firebaseSharingService.removeCapsuleSharing(capsuleId, normalizedUserId)
    }

    suspend fun setCapsulePublicState(capsuleId: String, isPublic: Boolean): Result<Unit> {
        return firebaseSharingService.setCapsulePublicState(capsuleId, isPublic)
    }

    suspend fun getSharedWithMeCapsules(): Result<List<Map<String, Any>>> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return firebaseSharingService.getSharedWithMeCapsules(userId)
    }

    suspend fun syncSharedCapsules(userId: String): Result<Unit> {
        val identifiers = buildShareIdentifiers(userId)
        if (identifiers.isEmpty()) return Result.success(Unit)

        return firebaseSharingService.getSharedWithMeCapsulesForIdentifiers(identifiers)
            .mapCatching { payload ->
                upsertSharedCapsules(payload, userId)
            }
    }

    fun startSharedCapsulesRealtimeSync(userId: String) {
        stopSharedCapsulesRealtimeSync()

        buildShareIdentifiers(userId).forEach { identifier ->
            val registration = firebaseSharingService.observeSharedWithMeCapsules(
                identifier = identifier,
                onUpdate = { payload ->
                    syncScope.launch {
                        runCatching { upsertSharedCapsules(payload, userId) }
                            .onFailure { error ->
                                Log.e("APP_ERROR", "Shared sync update failed: ${error.message}", error)
                            }
                    }
                },
                onError = { error ->
                    Log.e("APP_ERROR", "Shared sync listener error: ${error.message}", error)
                }
            )
            sharedListeners[identifier] = registration
        }
    }

    fun stopSharedCapsulesRealtimeSync() {
        sharedListeners.values.forEach { it.remove() }
        sharedListeners.clear()
    }

    private suspend fun upsertSharedCapsules(payload: List<Map<String, Any>>, userId: String) {
        val remoteIds = payload.mapNotNull { it["id"] as? String }.toSet()

        payload.forEach { data ->
            val id = data["id"] as? String ?: return@forEach
            val entity = mapToSharedEntity(data, userId)
            capsuleDao.insertCapsule(entity)
        }

        val localSharedIds = capsuleDao.getSharedCapsuleIdsForUser(userId)
        localSharedIds
            .filterNot { it in remoteIds }
            .forEach { staleId -> capsuleDao.deleteCapsuleById(staleId) }
    }

    private fun mapToSharedEntity(data: Map<String, Any>, fallbackOwnerId: String): CapsuleEntity {
        val id = (data["clientId"] as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (data["id"] as? String)
            ?: ""

        return CapsuleEntity(
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
            ownerId = data["ownerId"] as? String ?: fallbackOwnerId,
            sharedWith = (data["sharedWith"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isPublic = data["isPublic"] as? Boolean ?: false,
            canBeShared = data["canBeShared"] as? Boolean ?: false,
            isSharedWithMe = true,
            isDiscovered = data["isDiscovered"] as? Boolean ?: false,
            sharedByName = data["sharedByName"] as? String,
            sharedAt = (data["sharedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun buildShareIdentifiers(userId: String): List<String> {
        return listOf(userId.trim()).filter { it.isNotEmpty() }
    }
}

