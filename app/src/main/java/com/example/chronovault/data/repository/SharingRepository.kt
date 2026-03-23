package com.example.chronovault.data.repository

import com.example.chronovault.data.remote.firebase.FirebaseSharingService
import com.example.chronovault.utils.PreferencesManager

/**
 * Repository for Capsule Sharing operations
 * Handles sharing permissions and shared capsule management
 */
class SharingRepository(
    private val firebaseSharingService: FirebaseSharingService,
    private val preferencesManager: PreferencesManager
) {

    suspend fun shareCapsuleWithUser(capsuleId: String, userEmail: String): Result<Unit> {
        return firebaseSharingService.shareCapsule(capsuleId, userEmail)
    }

    suspend fun unshareCapsuleWithUser(capsuleId: String, userEmail: String): Result<Unit> {
        return firebaseSharingService.removeCapsuleSharing(capsuleId, userEmail)
    }

    suspend fun getSharedWithMeCapsules(): Result<List<Map<String, Any>>> {
        val userEmail = preferencesManager.getUserEmail() ?: return Result.failure(Exception("User not authenticated"))
        return firebaseSharingService.getSharedWithMeCapsules(userEmail)
    }
}

