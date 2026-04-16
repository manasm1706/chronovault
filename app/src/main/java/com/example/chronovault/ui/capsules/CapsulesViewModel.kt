package com.example.chronovault.ui.capsules

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.repository.CapsuleRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.ImageConverter
import com.example.chronovault.utils.NotificationHelper
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Capsules management screen
 * Handles capsule creation, deletion, filtering, and Firebase sync
 */
class CapsulesViewModel(application: Application) : AndroidViewModel(application) {

    private val capsuleRepository: CapsuleRepository = ServiceLocator.provideCapsuleRepository(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)
    private val currentUserId: String? get() = preferencesManager.getUserId()
    private var hasAttemptedCloudRestore = false

    // UI State
    private val _filterType = MutableLiveData<FilterType>(FilterType.ALL)
    val filterType: LiveData<FilterType> = _filterType

    private val _capsulesList = MutableLiveData<List<CapsuleEntity>>(emptyList())
    val capsulesList: LiveData<List<CapsuleEntity>> = _capsulesList

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _createCapsuleState = MutableLiveData<CreateCapsuleState>(CreateCapsuleState.Idle)
    val createCapsuleState: LiveData<CreateCapsuleState> = _createCapsuleState

    // Current capsule being created
    private val _capsuleTitle = MutableLiveData<String>()
    val capsuleTitle: LiveData<String> = _capsuleTitle

    private val _capsuleMessage = MutableLiveData<String>()
    val capsuleMessage: LiveData<String> = _capsuleMessage

    private val _capsuleImageBase64 = MutableLiveData<String?>()
    val capsuleImageBase64: LiveData<String?> = _capsuleImageBase64

    private val _capsuleLatitude = MutableLiveData<Double>(0.0)
    val capsuleLatitude: LiveData<Double> = _capsuleLatitude

    private val _capsuleLongitude = MutableLiveData<Double>(0.0)
    val capsuleLongitude: LiveData<Double> = _capsuleLongitude

    private val _unlockDate = MutableLiveData<Long?>(null)
    val unlockDate: LiveData<Long?> = _unlockDate

    private val _isLocationBased = MutableLiveData<Boolean>(false)
    val isLocationBased: LiveData<Boolean> = _isLocationBased

    private val _isTimeBased = MutableLiveData<Boolean>(false)
    val isTimeBased: LiveData<Boolean> = _isTimeBased

    private val _canShare = MutableLiveData<Boolean>(false)
    val canShare: LiveData<Boolean> = _canShare

    private val _isPublic = MutableLiveData<Boolean>(false)
    val isPublic: LiveData<Boolean> = _isPublic

    init {
        loadCapsules()
    }

    fun loadCapsules() {
        _loadingState.value = LoadingState.Loading
        val userId = preferencesManager.getUserId() ?: return

        viewModelScope.launch {
            try {
                if (!hasAttemptedCloudRestore) {
                    hasAttemptedCloudRestore = true
                    capsuleRepository.restoreUserCapsulesFromCloudIfLocalEmpty(userId)
                }

                // FIX: 12
                persistExpiredTimeUnlocks()

                when (_filterType.value) {
                    FilterType.ALL -> {
                        capsuleRepository.getUserCapsules(userId).collect { capsules ->
                            _capsulesList.value = capsules
                            _loadingState.value = LoadingState.Success
                        }
                    }
                    FilterType.LOCKED -> {
                        capsuleRepository.getLockedCapsules(userId).collect { capsules ->
                            _capsulesList.value = capsules
                            _loadingState.value = LoadingState.Success
                        }
                    }
                    FilterType.UNLOCKED -> {
                        capsuleRepository.getUnlockedCapsules(userId).collect { capsules ->
                            _capsulesList.value = capsules
                            _loadingState.value = LoadingState.Success
                        }
                    }
                    FilterType.SHARED -> {
                        capsuleRepository.getSharedCapsules(userId).collect { capsules ->
                            _capsulesList.value = capsules
                            _loadingState.value = LoadingState.Success
                        }
                    }
                    FilterType.PERSONAL -> {
                        capsuleRepository.getUserCapsules(userId).collect { capsules ->
                            _capsulesList.value = capsules.filter { it.ownerId == userId }
                            _loadingState.value = LoadingState.Success
                        }
                    }
                    FilterType.PUBLIC -> {
                        capsuleRepository.getPublicCapsules().collect { capsules ->
                            _capsulesList.value = capsules
                            _loadingState.value = LoadingState.Success
                        }
                    }
                    null -> {}
                }
            } catch (_: CancellationException) {
                // FIX: 14
                // Stream cancellation is expected when lifecycle/filter changes occur.
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load capsules")
            }
        }
    }

    fun setFilter(filter: FilterType) {
        _filterType.value = filter
        loadCapsules()
    }

    // Capsule creation methods
    fun setTitle(title: String) {
        _capsuleTitle.value = title
    }

    fun setMessage(message: String) {
        _capsuleMessage.value = message
    }

    fun setImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val base64 = ImageConverter.uriToBase64(context, uri, quality = 85)
            if (base64 != null) {
                // Compress if too large
                val compressed = ImageConverter.compressBase64IfNeeded(base64, maxSizeMB = 2.0)
                _capsuleImageBase64.value = compressed
            } else {
                _createCapsuleState.value = CreateCapsuleState.Error("Failed to process image")
            }
        }
    }

    fun setLocation(latitude: Double, longitude: Double) {
        _capsuleLatitude.value = latitude
        _capsuleLongitude.value = longitude
        Log.d("MAP", "Captured location -> Lat: $latitude, Lng: $longitude")
    }

    fun setUnlockDate(timestamp: Long) {
        _unlockDate.value = timestamp
        _isTimeBased.value = true
    }

    fun setLocationBased(enabled: Boolean) {
        _isLocationBased.value = enabled
    }

    fun setShareable(enabled: Boolean) {
        _canShare.value = enabled
    }

    fun setPublic(enabled: Boolean) {
        _isPublic.value = enabled
    }

    fun createCapsule() {
        val title = _capsuleTitle.value.orEmpty().trim()
        val message = _capsuleMessage.value.orEmpty().trim()
        val latitude = _capsuleLatitude.value ?: 0.0
        val longitude = _capsuleLongitude.value ?: 0.0
        val userId = preferencesManager.getUserId() ?: return

        // Validation
        if (title.isEmpty()) {
            _createCapsuleState.value = CreateCapsuleState.Error("Title is required")
            return
        }

        if (message.isEmpty()) {
            _createCapsuleState.value = CreateCapsuleState.Error("Message is required")
            return
        }

        if (latitude == 0.0 && longitude == 0.0) {
            _createCapsuleState.value = CreateCapsuleState.Error("Location not captured yet. Please allow location and try again.")
            return
        }

        _createCapsuleState.value = CreateCapsuleState.Loading

        viewModelScope.launch {
            try {
                val capsuleId = UUID.randomUUID().toString()
                val isLocBased = _isLocationBased.value ?: false
                // FIX: 15
                val isTimeBased = _isTimeBased.value ?: false
                val hasUnlockMethod = isLocBased || isTimeBased
                val startsUnlocked = !hasUnlockMethod
                val isPublicCapsule = _isPublic.value ?: false
                val capsule = CapsuleEntity(
                    id = capsuleId,
                    title = title,
                    message = message,
                    imageBase64 = _capsuleImageBase64.value,
                    latitude = latitude,
                    longitude = longitude,
                    createdAt = System.currentTimeMillis(),
                    unlockTime = _unlockDate.value,
                    unlockLatitude = if (isLocBased) latitude else null,
                    unlockLongitude = if (isLocBased) longitude else null,
                    isUnlocked = startsUnlocked,
                    isLocationBased = isLocBased,
                    isTimeBased = isTimeBased,
                    ownerId = userId,
                    isPublic = isPublicCapsule,
                    sharedWith = if (isPublicCapsule) emptyList() else emptyList(),
                    canBeShared = _canShare.value ?: false
                )

                Log.d("MAP", "Saving capsule ${capsule.id} -> Lat: ${capsule.latitude}, Lng: ${capsule.longitude}")

                capsuleRepository.insertCapsule(capsule)

                // Sync to Firebase
                val firebaseData = mapOf(
                    "id" to capsuleId,
                    "title" to title,
                    "message" to message,
                    "imageBase64" to (_capsuleImageBase64.value ?: ""),
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "createdAt" to capsule.createdAt,
                    "unlockTime" to (_unlockDate.value ?: 0),
                    "isLocationBased" to (_isLocationBased.value ?: false),
                    "isTimeBased" to (_isTimeBased.value ?: false),
                    // FIX: 15
                    "isUnlocked" to startsUnlocked,
                    "isPublic" to isPublicCapsule,
                    "sharedWith" to if (isPublicCapsule) emptyList<String>() else emptyList<String>(),
                    "canBeShared" to (_canShare.value ?: false)
                )

                capsuleRepository.createCapsuleOnFirebase(firebaseData).onSuccess {
                    NotificationHelper.sendCapsuleCreatedNotification(getApplication())
                    _createCapsuleState.value = CreateCapsuleState.Success(capsuleId)
                    resetCreateForm()
                    loadCapsules()
                }.onFailure { exception ->
                    _createCapsuleState.value = CreateCapsuleState.Error(exception.message ?: "Failed to create capsule")
                }
            } catch (e: Exception) {
                _createCapsuleState.value = CreateCapsuleState.Error(e.message ?: "Error creating capsule")
            }
        }
    }

    fun deleteCapsule(capsuleId: String) {
        viewModelScope.launch {
            try {
                capsuleRepository.deleteCapsule(capsuleId)
                loadCapsules()
            } catch (e: Exception) {
                _createCapsuleState.value = CreateCapsuleState.Error("Failed to delete capsule")
            }
        }
    }

    fun canOpenCapsule(c: CapsuleEntity): Boolean {
        // FIX: 12
        val now = System.currentTimeMillis()
        val isTimeUnlocked = c.isTimeBased && (c.unlockTime ?: 0L) in 1..now
        // FIX: 15
        val hasNoUnlockMethod = !c.isTimeBased && !c.isLocationBased

        val isOwner = currentUserId != null && c.ownerId == currentUserId
        val isShared = c.isSharedWithMe

        return when {
            c.isUnlocked || isTimeUnlocked || hasNoUnlockMethod -> true
            isOwner && !c.isUnlocked -> false
            isShared && c.isDiscovered -> true
            else -> false
        }
    }

    fun getLockedMessage(c: CapsuleEntity): String {
        // FIX: 12
        val now = System.currentTimeMillis()
        val isOwner = currentUserId != null && c.ownerId == currentUserId
        val isShared = c.isSharedWithMe

        return when {
            c.isTimeBased && (c.unlockTime ?: 0L) > now -> "This memory unlocks on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(c.unlockTime!!))}."
            isOwner && !c.isUnlocked -> "This memory is locked. Unlock time not reached."
            isShared && !c.isDiscovered -> "Discover this location to unlock the memory."
            else -> "This memory is locked."
        }
    }

    private fun resetCreateForm() {
        _capsuleTitle.value = ""
        _capsuleMessage.value = ""
        _capsuleImageBase64.value = null
        _capsuleLatitude.value = 0.0
        _capsuleLongitude.value = 0.0
        _unlockDate.value = null
        _isLocationBased.value = false
        _isTimeBased.value = false
        _isPublic.value = false
        _canShare.value = false
    }

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

enum class FilterType {
    ALL, LOCKED, UNLOCKED, SHARED, PERSONAL, PUBLIC
}


sealed class CreateCapsuleState {
    object Idle : CreateCapsuleState()
    object Loading : CreateCapsuleState()
    data class Success(val capsuleId: String) : CreateCapsuleState()
    data class Error(val message: String) : CreateCapsuleState()
}

