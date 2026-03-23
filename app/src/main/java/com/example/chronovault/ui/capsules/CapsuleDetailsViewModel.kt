package com.example.chronovault.ui.capsules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.CommentDao
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.local.entity.CommentEntity
import com.example.chronovault.data.repository.CapsuleRepository
import com.example.chronovault.data.repository.SharingRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Capsule Details screen
 * Displays full capsule content, handles unlocking, sharing, comments, and privacy
 */
class CapsuleDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val capsuleRepository: CapsuleRepository = ServiceLocator.provideCapsuleRepository(application)
    private val sharingRepository: SharingRepository = ServiceLocator.provideSharingRepository(application)
    private val commentDao: CommentDao = ServiceLocator.provideCommentDao(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    // UI State
    private val _capsule = MutableLiveData<CapsuleEntity?>()
    val capsule: LiveData<CapsuleEntity?> = _capsule

    private val _isOwner = MutableLiveData(false)
    val isOwner: LiveData<Boolean> = _isOwner

    private val _unlockReason = MutableLiveData("")
    val unlockReason: LiveData<String> = _unlockReason

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private val _sharedWithEmails = MutableLiveData<List<String>>(emptyList())
    val sharedWithEmails: LiveData<List<String>> = _sharedWithEmails

    private val _comments = MutableLiveData<List<CommentEntity>>(emptyList())
    val comments: LiveData<List<CommentEntity>> = _comments

    private val _isSharedCapsule = MutableLiveData(false)
    val isSharedCapsule: LiveData<Boolean> = _isSharedCapsule

    private var currentCapsuleId: String? = null

    fun loadCapsule(capsuleId: String) {
        currentCapsuleId = capsuleId
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            try {
                // FIX: 12
                persistExpiredTimeUnlocks()
                val loadedCapsule = capsuleRepository.getCapsuleById(capsuleId)
                if (loadedCapsule != null) {
                    // FIX: 12
                    val normalizedCapsule = if (
                        !loadedCapsule.isUnlocked &&
                        loadedCapsule.isTimeBased &&
                        (loadedCapsule.unlockTime ?: 0L) in 1..System.currentTimeMillis()
                    ) {
                        capsuleRepository.unlockCapsule(loadedCapsule.id)
                        loadedCapsule.copy(isUnlocked = true)
                    } else {
                        loadedCapsule
                    }

                    _capsule.value = normalizedCapsule
                    val userId = preferencesManager.getUserId()
                    _isOwner.value = normalizedCapsule.ownerId == userId
                    _isSharedCapsule.value = normalizedCapsule.canBeShared || normalizedCapsule.sharedWith.isNotEmpty()
                    _sharedWithEmails.value = normalizedCapsule.sharedWith
                    checkUnlockConditions(normalizedCapsule)
                    _loadingState.value = LoadingState.Success
                } else {
                    _loadingState.value = LoadingState.Error("Capsule not found")
                }
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load capsule")
            }
        }
        // Observe comments via Flow
        viewModelScope.launch {
            commentDao.getCommentsForCapsule(capsuleId).collectLatest { commentList ->
                _comments.value = commentList
            }
        }
    }

    private fun checkUnlockConditions(capsule: CapsuleEntity) {
        if (capsule.isUnlocked) {
            _unlockReason.value = "✅ Already unlocked"
            return
        }

        val reasons = mutableListOf<String>()

        if (capsule.isTimeBased && capsule.unlockTime != null) {
            val currentTime = System.currentTimeMillis()
            if (currentTime >= capsule.unlockTime) {
                reasons.add("⏰ Time condition met — ready to unlock")
            } else {
                val diffMs = capsule.unlockTime - currentTime
                val days = diffMs / 86_400_000
                val hours = (diffMs % 86_400_000) / 3_600_000
                reasons.add("⏰ Unlock in ${days}d ${hours}h")
            }
        }

        if (capsule.isLocationBased && capsule.unlockLatitude != null) {
            reasons.add("📍 Location-based unlock — visit the saved place")
        }

        if (reasons.isEmpty()) {
            _unlockReason.value = "🔒 Locked"
        } else {
            _unlockReason.value = reasons.joinToString("\n")
        }
    }

    fun unlockCapsule() {
        val currentCapsule = _capsule.value ?: return
        if (currentCapsule.isUnlocked) {
            _actionState.value = ActionState.Error("Already unlocked")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                capsuleRepository.unlockCapsule(currentCapsule.id)
                _capsule.value = currentCapsule.copy(isUnlocked = true)
                checkUnlockConditions(currentCapsule.copy(isUnlocked = true))
                _actionState.value = ActionState.Success("Capsule unlocked! 🎉")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to unlock")
            }
        }
    }

    fun shareCapsule(userEmail: String) {
        val currentCapsule = _capsule.value ?: return
        if (_isOwner.value != true) {
            _actionState.value = ActionState.Error("Only the owner can share")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                // Enable sharing flag if not already
                if (!currentCapsule.canBeShared) {
                    capsuleRepository.updateSharingEnabled(currentCapsule.id, true)
                }
                sharingRepository.shareCapsuleWithUser(currentCapsule.id, userEmail)
                    .onSuccess {
                        val updated = _sharedWithEmails.value.orEmpty().toMutableList()
                        if (!updated.contains(userEmail)) updated.add(userEmail)
                        _sharedWithEmails.value = updated
                        _isSharedCapsule.value = true
                        _actionState.value = ActionState.Success("Shared with $userEmail")
                    }
                    .onFailure {
                        _actionState.value = ActionState.Error(it.message ?: "Failed to share")
                    }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to share")
            }
        }
    }

    fun unshareCapsule(userEmail: String) {
        val currentCapsule = _capsule.value ?: return
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            sharingRepository.unshareCapsuleWithUser(currentCapsule.id, userEmail)
                .onSuccess {
                    val updated = _sharedWithEmails.value.orEmpty().toMutableList()
                    updated.remove(userEmail)
                    _sharedWithEmails.value = updated
                    _actionState.value = ActionState.Success("Removed $userEmail")
                }
                .onFailure {
                    _actionState.value = ActionState.Error(it.message ?: "Failed to remove")
                }
        }
    }

    fun makeCapsulePrivate() {
        val currentCapsule = _capsule.value ?: return
        if (_isOwner.value != true) return
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                capsuleRepository.makeCapsulePrivate(currentCapsule.id)
                _sharedWithEmails.value = emptyList()
                _isSharedCapsule.value = false
                _capsule.value = currentCapsule.copy(canBeShared = false, sharedWith = emptyList())
                _actionState.value = ActionState.Success("Capsule is now private")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to make private")
            }
        }
    }

    fun addComment(text: String) {
        val capsuleId = currentCapsuleId ?: return
        val userId = preferencesManager.getUserId() ?: return
        val userName = preferencesManager.getUserName() ?: "Anonymous"

        if (text.isBlank()) {
            _actionState.value = ActionState.Error("Comment cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val comment = CommentEntity(
                    id = UUID.randomUUID().toString(),
                    capsuleId = capsuleId,
                    authorId = userId,
                    authorName = userName,
                    text = text.trim(),
                    createdAt = System.currentTimeMillis()
                )
                commentDao.insertComment(comment)
                _actionState.value = ActionState.Success("Comment added")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to add comment")
            }
        }
    }

    fun deleteComment(comment: CommentEntity) {
        val userId = preferencesManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                commentDao.deleteComment(comment.id, userId)
                _actionState.value = ActionState.Success("Comment deleted")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to delete comment")
            }
        }
    }

    fun deleteCapsule() {
        val currentCapsule = _capsule.value ?: return
        if (_isOwner.value != true) {
            _actionState.value = ActionState.Error("Only the owner can delete")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            try {
                commentDao.deleteAllCommentsForCapsule(currentCapsule.id)
                capsuleRepository.deleteCapsule(currentCapsule.id)
                _actionState.value = ActionState.CapsuleDeleted
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Failed to delete")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    fun getCurrentUserId(): String = preferencesManager.getUserId() ?: ""

    // FIX: 12
    private suspend fun persistExpiredTimeUnlocks() {
        val now = System.currentTimeMillis()
        capsuleRepository.checkTimeBasedUnlocks(now).forEach { capsule ->
            if (!capsule.isUnlocked) {
                capsuleRepository.unlockCapsule(capsule.id)
            }
        }
    }
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
    object CapsuleDeleted : ActionState()
}
