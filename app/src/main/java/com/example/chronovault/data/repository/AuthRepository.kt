package com.example.chronovault.data.repository

import com.example.chronovault.data.remote.firebase.FirebaseAuthService
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

/**
 * Repository for user authentication using Firebase Auth
 */
class AuthRepository(
    private val firebaseAuthService: FirebaseAuthService,
    private val preferencesManager: PreferencesManager
) {

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.US)

    fun registerUser(
        email: String,
        password: String,
        name: String
    ): Flow<Result<String>> = flow {
        val normalizedEmail = normalizeEmail(email)
        try {
            firebaseAuthService.registerUser(normalizedEmail, password, name).onSuccess { userId ->
                // Save user data locally
                preferencesManager.setUserId(userId)
                preferencesManager.setUserEmail(normalizedEmail)
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
        val normalizedEmail = normalizeEmail(email)
        try {
            firebaseAuthService.loginUser(normalizedEmail, password).onSuccess { userId ->
                // Save user data locally
                preferencesManager.setUserId(userId)
                preferencesManager.setUserEmail(normalizedEmail)
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

