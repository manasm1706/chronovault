package com.example.chronovault.ui.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.repository.AuthRepository
import com.example.chronovault.data.repository.UserRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.ImageConverter
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * ViewModel for Profile screen
 * Handles user profile management, logout, and account settings
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)
    private val userRepository: UserRepository = ServiceLocator.provideUserRepository(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    // UI State
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userAvatar = MutableLiveData<String?>()
    val userAvatar: LiveData<String?> = _userAvatar

    private val _totalCapsules = MutableLiveData<Int>(0)
    val totalCapsules: LiveData<Int> = _totalCapsules

    private val _totalMemories = MutableLiveData<Int>(0)
    val totalMemories: LiveData<Int> = _totalMemories

    private val _accountState = MutableLiveData<AccountState>(AccountState.Idle)
    val accountState: LiveData<AccountState> = _accountState

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            try {
                _userName.value = preferencesManager.getUserName() ?: "User"
                _userEmail.value = preferencesManager.getUserEmail() ?: ""
                _userAvatar.value = preferencesManager.getUserAvatar()

                // Fetch from Firebase for updated data
                userRepository.getUserProfile().onSuccess { profileData ->
                    _userName.value = profileData["name"] as? String ?: _userName.value
                    _userAvatar.value = profileData["avatarBase64"] as? String

                    // Store in preferences for offline access
                    _userName.value?.let { preferencesManager.setUserName(it) }
                }

                _loadingState.value = LoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun updateName(newName: String) {
        if (newName.isEmpty() || newName.length < 2) {
            _accountState.value = AccountState.Error("Name must be at least 2 characters")
            return
        }

        _accountState.value = AccountState.Loading

        viewModelScope.launch {
            userRepository.updateUserProfile(newName).onSuccess {
                _userName.value = newName
                _accountState.value = AccountState.Success("Name updated successfully")
            }.onFailure { exception ->
                _accountState.value = AccountState.Error(exception.message ?: "Failed to update name")
            }
        }
    }

    fun updateAvatar(context: Context, uri: Uri) {
        _accountState.value = AccountState.Loading

        viewModelScope.launch {
            val base64 = ImageConverter.uriToBase64(context, uri, quality = 80)
            if (base64 != null) {
                val compressed = ImageConverter.compressBase64IfNeeded(base64, maxSizeMB = 1.0)
                userRepository.updateUserProfile(_userName.value ?: "User", compressed)
                    .onSuccess {
                        _userAvatar.value = compressed
                        preferencesManager.setUserAvatar(compressed)
                        _accountState.value = AccountState.Success("Avatar updated successfully")
                    }
                    .onFailure { exception ->
                        _accountState.value = AccountState.Error(exception.message ?: "Failed to update avatar")
                    }
            } else {
                _accountState.value = AccountState.Error("Failed to process image")
            }
        }
    }

    fun logout() {
        _accountState.value = AccountState.Loading

        viewModelScope.launch {
            try {
                authRepository.logoutUser()
                _accountState.value = AccountState.LogoutSuccess
            } catch (e: Exception) {
                _accountState.value = AccountState.Error(e.message ?: "Logout failed")
            }
        }
    }

    fun deleteAccount() {
        _accountState.value = AccountState.ConfirmingDelete

        viewModelScope.launch {
            userRepository.deleteAccount().onSuccess {
                authRepository.logoutUser()
                _accountState.value = AccountState.AccountDeleted
            }.onFailure { exception ->
                _accountState.value = AccountState.Error(exception.message ?: "Failed to delete account")
            }
        }
    }

    fun resetAccountState() {
        _accountState.value = AccountState.Idle
    }
}

sealed class AccountState {
    object Idle : AccountState()
    object Loading : AccountState()
    object ConfirmingDelete : AccountState()
    data class Success(val message: String) : AccountState()
    object LogoutSuccess : AccountState()
    object AccountDeleted : AccountState()
    data class Error(val message: String) : AccountState()
}


