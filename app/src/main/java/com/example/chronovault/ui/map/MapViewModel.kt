package com.example.chronovault.ui.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.repository.CapsuleRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel for Map screen
 * Handles map markers, user location, and nearby capsules
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val capsuleRepository: CapsuleRepository = ServiceLocator.provideCapsuleRepository(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    // UI State
    private val _userLocation = MutableLiveData<Pair<Double, Double>?>()
    val userLocation: LiveData<Pair<Double, Double>?> = _userLocation

    private val _capsuleMarkers = MutableLiveData<List<CapsuleEntity>>(emptyList())
    // FIX: 13
    // MapFragment observes all map capsules from Room via this stream.
    val allCapsules: LiveData<List<CapsuleEntity>> = _capsuleMarkers

    private val _nearbyCapsules = MutableLiveData<List<CapsuleEntity>>(emptyList())

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private var mapDataJob: Job? = null
    // FIX: 10
    private val currentUserId: String? get() = preferencesManager.getUserId()

    init {
        loadMapData()
    }

    fun loadMapData() {
        _loadingState.value = LoadingState.Loading
        val userId = preferencesManager.getUserId()
        if (userId.isNullOrBlank()) {
            _capsuleMarkers.value = emptyList()
            _loadingState.value = LoadingState.Error("User not authenticated")
            return
        }

        mapDataJob?.cancel()

        mapDataJob = viewModelScope.launch {
            try {
                // FIX: 12
                persistExpiredTimeUnlocks()

                capsuleRepository.getCapsulesForMap(userId).collect { capsules ->
                    _capsuleMarkers.value = capsules
                    _loadingState.value = LoadingState.Success
                    capsules.forEach { capsule ->
                        Log.d("MAP", "Lat: ${capsule.latitude}, Lng: ${capsule.longitude}, id=${capsule.id}")
                    }
                }
            } catch (_: CancellationException) {
                // FIX: 14
                // Lifecycle/job cancellations are expected; don't surface as map errors.
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load map data")
                Log.e("MapViewModel", "Failed to collect map capsules", e)
            }
        }
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
        checkNearbyCapules(latitude, longitude)
    }

    fun onLocationPermissionGranted(granted: Boolean) {
        // FIX: 13
        // Permission state is handled directly by the fragment flow.
        if (!granted) {
            _nearbyCapsules.value = emptyList()
        }
    }

    private fun checkNearbyCapules(userLat: Double, userLon: Double) {
        viewModelScope.launch {
            try {
                val locationBasedCapsules = capsuleRepository.getLocationBasedCapsules()
                val nearby = locationBasedCapsules.filter { capsule ->
                    val distance = calculateDistance(
                        userLat, userLon,
                        capsule.latitude, capsule.longitude
                    )
                    distance <= 100f // 100 meters radius
                }
                _nearbyCapsules.value = nearby
            } catch (_: Exception) {
                // Silently fail on nearby check
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadiusMeters * c).toFloat()
    }

    fun getCapsuleStatus(capsule: CapsuleEntity): String {
        // FIX: 12
        val isTimeUnlocked = capsule.isTimeBased && (capsule.unlockTime ?: 0L) in 1..System.currentTimeMillis()

        return when {
            capsule.isUnlocked || isTimeUnlocked -> "Unlocked"
            capsule.isLocationBased && capsule.unlockLatitude != null -> "Location-Locked"
            capsule.isTimeBased && capsule.unlockTime != null -> "Time-Locked"
            else -> "Locked"
        }
    }

    // FIX: 10
    fun isOwnedByCurrentUser(capsule: CapsuleEntity): Boolean {
        return !currentUserId.isNullOrBlank() && capsule.ownerId == currentUserId
    }

    // FIX: 12
    fun isEffectivelyUnlocked(capsule: CapsuleEntity): Boolean {
        if (capsule.isUnlocked) return true
        val unlockTime = capsule.unlockTime ?: 0L
        return capsule.isTimeBased && unlockTime in 1..System.currentTimeMillis()
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


