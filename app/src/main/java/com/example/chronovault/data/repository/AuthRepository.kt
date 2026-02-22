package com.example.chronovault.data.repository

import com.example.chronovault.data.remote.firebase.FirebaseAuthService
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for user authentication using Firebase Auth
 */
class AuthRepository(
    private val firebaseAuthService: FirebaseAuthService,
    private val preferencesManager: PreferencesManager
) {

    fun registerUser(
        email: String,
        password: String,
        name: String
    ): Flow<Result<String>> = flow {
        try {
            firebaseAuthService.registerUser(email, password, name).onSuccess { userId ->
                // Save user data locally
                preferencesManager.setUserId(userId)
                preferencesManager.setUserEmail(email)
                preferencesManager.setUserName(name)
                preferencesManager.setLoggedIn(true)
                emit(Result.success(userId))
            }.onFailure { exception ->
                emit(Result.failure(exception))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun loginUser(email: String, password: String): Flow<Result<String>> = flow {
        try {
            firebaseAuthService.loginUser(email, password).onSuccess { userId ->
                // Save user data locally
                preferencesManager.setUserId(userId)
                preferencesManager.setUserEmail(email)
                preferencesManager.setLoggedIn(true)
                emit(Result.success(userId))
            }.onFailure { exception ->
                emit(Result.failure(exception))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun logoutUser() {
        firebaseAuthService.logoutUser()
        preferencesManager.setLoggedIn(false)
        preferencesManager.clearAll()
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuthService.isUserLoggedIn()
    }

    fun getCurrentUserId(): String? {
        return firebaseAuthService.getCurrentUserId()
    }

    fun getCurrentUserEmail(): String? {
        return firebaseAuthService.getCurrentUserEmail()
    }

    fun getCurrentUserName(): String? {
        return preferencesManager.getUserName()
    }
}

