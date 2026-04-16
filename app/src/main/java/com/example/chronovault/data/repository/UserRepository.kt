package com.example.chronovault.data.repository

import com.example.chronovault.data.remote.firebase.FirebaseUserService
import com.example.chronovault.utils.PreferencesManager

/**
 * Repository for User Profile operations
 * Handles user data management with Firebase Firestore
 */
class UserRepository(
    private val firebaseUserService: FirebaseUserService,
    private val preferencesManager: PreferencesManager
) {

    suspend fun getUserProfile(): Result<Map<String, Any>> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return firebaseUserService.getUserProfile(userId)
    }

    suspend fun updateUserProfile(name: String, avatarBase64: String? = null): Result<Unit> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))

        val updateData = mutableMapOf<String, Any>("name" to name)
        if (avatarBase64 != null) {
            updateData["avatarBase64"] = avatarBase64
        }

        return firebaseUserService.updateUserProfile(userId, updateData).onSuccess {
            preferencesManager.setUserName(name)
        }
    }

    suspend fun getUserByEmail(email: String): Result<Map<String, Any>> {
        return firebaseUserService.getUserByEmail(email)
    }

    suspend fun searchUsers(query: String): Result<List<Map<String, Any>>> {
        return firebaseUserService.searchUsers(query)
    }

    suspend fun getUsersByIds(userIds: List<String>): Result<List<Map<String, Any>>> {
        return firebaseUserService.getUsersByIds(userIds)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("User not authenticated"))
        return firebaseUserService.deleteUser(userId).onSuccess {
            preferencesManager.clearAll()
        }
    }

    fun getUserEmail(): String? = preferencesManager.getUserEmail()

    fun getUserName(): String? = preferencesManager.getUserName()

    fun getUserId(): String? = preferencesManager.getUserId()
}

