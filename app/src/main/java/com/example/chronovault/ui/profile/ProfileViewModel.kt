package com.example.chronovault.ui.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.entity.FriendEntity
import com.example.chronovault.data.repository.AuthRepository
import com.example.chronovault.data.repository.FriendRepository
import com.example.chronovault.data.repository.UserRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.ImageConverter
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for Profile screen
 * Handles user profile management, logout, and account settings
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)
    private val userRepository: UserRepository = ServiceLocator.provideUserRepository(application)
    private val friendRepository: FriendRepository = ServiceLocator.provideFriendRepository(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    // UI State
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userAvatar = MutableLiveData<String?>()
    val userAvatar: LiveData<String?> = _userAvatar

    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    private val _friends = MutableLiveData<List<FriendEntity>>(emptyList())
    val friends: LiveData<List<FriendEntity>> = _friends

    private val _friendRequests = MutableLiveData<List<FriendRepository.FriendRequest>>(emptyList())
    val friendRequests: LiveData<List<FriendRepository.FriendRequest>> = _friendRequests

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
        observeFriends()
        observeFriendRequests()
    }

    fun loadUserProfile() {
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            try {
                _userName.value = preferencesManager.getUserName() ?: "User"
                _userEmail.value = preferencesManager.getUserEmail() ?: ""
                _userAvatar.value = preferencesManager.getUserAvatar()
                _userId.value = userRepository.getUserId().orEmpty()

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

    fun getNotificationSoundEnabled(): Boolean = preferencesManager.getNotificationSound()

    fun setNotificationSoundEnabled(enabled: Boolean) {
        preferencesManager.setNotificationSound(enabled)
    }

    fun getNotificationVibrationEnabled(): Boolean = preferencesManager.getNotificationVibration()

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        preferencesManager.setNotificationVibration(enabled)
    }

    fun getSelectedThemeMode(): String = preferencesManager.getSelectedThemeMode()

    fun setSelectedThemeMode(mode: String) {
        preferencesManager.setSelectedThemeMode(mode)
    }

    fun getSelectedColorScheme(): String = preferencesManager.getSelectedColorScheme()

    fun setSelectedColorScheme(scheme: String) {
        preferencesManager.setSelectedColorScheme(scheme)
    }

    private fun observeFriends() {
        viewModelScope.launch {
            friendRepository.observeFriends().collectLatest { friendList ->
                _friends.value = friendList
            }
        }
    }

    fun sendFriendRequest(friendUserId: String) {
        viewModelScope.launch {
            _accountState.value = AccountState.Loading
            friendRepository.sendFriendRequest(friendUserId.trim())
                .onSuccess {
                    _accountState.value = AccountState.Success("Friend request sent")
                }
                .onFailure { error ->
                    _accountState.value = AccountState.Error(error.message ?: "Failed to send request")
                }
        }
    }

    fun acceptFriend(friendUserId: String) {
        viewModelScope.launch {
            friendRepository.acceptFriend(friendUserId)
                .onSuccess {
                    _accountState.value = AccountState.Success("Friend request accepted")
                }
                .onFailure { error ->
                    _accountState.value = AccountState.Error(error.message ?: "Failed to accept friend")
                }
        }
    }

    fun acceptFriendRequest(request: FriendRepository.FriendRequest) {
        viewModelScope.launch {
            friendRepository.acceptFriendRequest(request.requestId, request.fromUserId)
                .onSuccess {
                    _accountState.value = AccountState.Success("Friend request accepted")
                }
                .onFailure { error ->
                    _accountState.value = AccountState.Error(error.message ?: "Failed to accept request")
                }
        }
    }

    fun rejectFriendRequest(request: FriendRepository.FriendRequest) {
        viewModelScope.launch {
            friendRepository.rejectFriendRequest(request.requestId)
                .onSuccess {
                    _accountState.value = AccountState.Success("Friend request rejected")
                }
                .onFailure { error ->
                    _accountState.value = AccountState.Error(error.message ?: "Failed to reject request")
                }
        }
    }

    private fun observeFriendRequests() {
        viewModelScope.launch {
            friendRepository.observeIncomingRequests().collectLatest { requests ->
                _friendRequests.value = requests
            }
        }
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


