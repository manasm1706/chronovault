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
import com.example.chronovault.utils.LocationHelper
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
    private val _visibleCapsules = MutableLiveData<List<CapsuleEntity>>(emptyList())
    // FIX: 15
    // MapFragment observes currently selected map mode capsules via this stream.
    val allCapsules: LiveData<List<CapsuleEntity>> = _visibleCapsules

    // FIX: 15
    private val _mapMode = MutableLiveData(MapMode.PERSONAL)
    val mapMode: LiveData<MapMode> = _mapMode

    private val _nearbyCapsules = MutableLiveData<List<CapsuleEntity>>(emptyList())
    private val _discoveryEvent = MutableLiveData<CapsuleEntity?>(null)
    val discoveryEvent: LiveData<CapsuleEntity?> = _discoveryEvent
    private val discoveredEventIds = mutableSetOf<String>()

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _overlayOptions = MutableLiveData(MapOverlayOptions())
    val overlayOptions: LiveData<MapOverlayOptions> = _overlayOptions

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
                    // FIX: 15
                    updateVisibleCapsules(capsules)
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
        // FIX: 15
        updateVisibleCapsules(_capsuleMarkers.value.orEmpty())
    }

    // FIX: 15
    fun setMapMode(mode: MapMode) {
        if (_mapMode.value == mode) return
        _mapMode.value = mode
        updateVisibleCapsules(_capsuleMarkers.value.orEmpty())
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
                    // FIX: 15
                    distance <= 50f
                }

                nearby.forEach { capsule ->
                    if (!capsule.isDiscovered) {
                        capsuleRepository.markCapsuleDiscovered(capsule.id)
                        if (discoveredEventIds.add(capsule.id)) {
                            _discoveryEvent.value = capsule.copy(isDiscovered = true)
                        }
                    }
                }
                _nearbyCapsules.value = nearby
            } catch (_: Exception) {
                // Silently fail on nearby check
            }
        }
    }

    fun consumeDiscoveryEvent() {
        _discoveryEvent.value = null
    }

    fun setShowClueCircles(enabled: Boolean) {
        val current = _overlayOptions.value ?: MapOverlayOptions()
        _overlayOptions.value = current.copy(showClueCircles = enabled)
    }

    fun setShowNearbyWaves(enabled: Boolean) {
        val current = _overlayOptions.value ?: MapOverlayOptions()
        _overlayOptions.value = current.copy(showNearbyWaves = enabled)
    }

    fun setShowMyLocation(enabled: Boolean) {
        val current = _overlayOptions.value ?: MapOverlayOptions()
        _overlayOptions.value = current.copy(showMyLocation = enabled)
    }

    fun setShowDiscoveryOverlay(enabled: Boolean) {
        val current = _overlayOptions.value ?: MapOverlayOptions()
        _overlayOptions.value = current.copy(showDiscoveryOverlay = enabled)
    }

    // FIX: 15
    private fun updateVisibleCapsules(source: List<CapsuleEntity>) {
        val visible = when (_mapMode.value ?: MapMode.PERSONAL) {
            MapMode.PERSONAL -> source.filter { isOwnedByCurrentUser(it) }
            MapMode.WORLD -> {
                val location = _userLocation.value
                if (location == null) {
                    emptyList()
                } else {
                    source.filter { capsule ->
                        val isWorldShared = !isOwnedByCurrentUser(capsule) &&
                            (capsule.isSharedWithMe || capsule.sharedWith.isNotEmpty() || capsule.canBeShared)
                        if (!isWorldShared) {
                            false
                        } else {
                            val distance = LocationHelper.calculateDistance(
                                location.first,
                                location.second,
                                capsule.latitude,
                                capsule.longitude
                            )
                            distance <= WORLD_RADIUS_METERS
                        }
                    }
                }
            }
        }
        _visibleCapsules.value = visible
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

    // FIX: 15
    enum class MapMode {
        PERSONAL,
        WORLD
    }

    data class MapOverlayOptions(
        val showClueCircles: Boolean = true,
        val showNearbyWaves: Boolean = true,
        val showMyLocation: Boolean = true,
        val showDiscoveryOverlay: Boolean = true
    )

    companion object {
        // FIX: 15
        private const val WORLD_RADIUS_METERS = 10_000f
    }

}


