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
import com.example.chronovault.data.repository.NotificationRepository
import com.example.chronovault.data.repository.SharingRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.LocationHelper
import com.example.chronovault.utils.NotificationHelper
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
    private val sharingRepository: SharingRepository = ServiceLocator.provideSharingRepository(application)
    private val notificationRepository: NotificationRepository = ServiceLocator.provideNotificationRepository(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    // UI State
    private val _userLocation = MutableLiveData<Pair<Double, Double>?>()
    val userLocation: LiveData<Pair<Double, Double>?> = _userLocation

    private val _capsuleMarkers = MutableLiveData<List<CapsuleEntity>>(emptyList())
    private val _publicCloudCapsules = MutableLiveData<List<CapsuleEntity>>(emptyList())
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
                syncSharedCapsulesToLocal()
                loadPublicCapsulesFromCloud()

                capsuleRepository.getCapsulesForMap(userId).collect { capsules ->
                    val merged = mergeCapsules(capsules, _publicCloudCapsules.value.orEmpty())
                    _capsuleMarkers.value = merged
                    // FIX: 15
                    updateVisibleCapsules(merged)
                    _loadingState.value = LoadingState.Success
                    merged.forEach { capsule ->
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
        if (mode == MapMode.WORLD) {
            viewModelScope.launch { loadPublicCapsulesFromCloud() }
        }
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
                            if (!preferencesManager.isOneTimeEventMarked("discovered_${capsule.id}")) {
                                preferencesManager.markOneTimeEvent("discovered_${capsule.id}")
                                NotificationHelper.sendMemoryDiscoveredNotification(getApplication())
                                notificationRepository.createNearbyNotification(capsule.id, capsule.title)
                            }
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
            MapMode.PERSONAL -> source.filter { capsule ->
                isOwnedByCurrentUser(capsule) || capsule.isSharedWithMe
            }
            MapMode.WORLD -> {
                val location = _userLocation.value
                source.filter { capsule ->
                    if (!capsule.isPublic) return@filter false
                    if (location == null) {
                        true
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
        _visibleCapsules.value = visible
    }

    private suspend fun loadPublicCapsulesFromCloud() {
        val cloud = capsuleRepository.getPublicCapsulesFromCloud().getOrDefault(emptyList())
        _publicCloudCapsules.value = cloud
        _capsuleMarkers.value = mergeCapsules(_capsuleMarkers.value.orEmpty(), cloud)
        updateVisibleCapsules(_capsuleMarkers.value.orEmpty())
    }

    private fun mergeCapsules(local: List<CapsuleEntity>, remote: List<CapsuleEntity>): List<CapsuleEntity> {
        val byId = linkedMapOf<String, CapsuleEntity>()
        (local + remote).forEach { capsule ->
            val existing = byId[capsule.id]
            byId[capsule.id] = if (existing == null) capsule else pickRicherCapsule(existing, capsule)
        }

        val bySignature = linkedMapOf<String, CapsuleEntity>()
        byId.values.forEach { capsule ->
            val signature = buildCapsuleSignature(capsule)
            val existing = bySignature[signature]
            bySignature[signature] = if (existing == null) capsule else pickRicherCapsule(existing, capsule)
        }

        return bySignature.values.toList()
    }

    private fun buildCapsuleSignature(capsule: CapsuleEntity): String {
        return listOf(
            capsule.ownerId,
            capsule.title.trim().lowercase(),
            capsule.message.trim().lowercase(),
            String.format("%.5f", capsule.latitude),
            String.format("%.5f", capsule.longitude)
        ).joinToString("|")
    }

    private fun pickRicherCapsule(first: CapsuleEntity, second: CapsuleEntity): CapsuleEntity {
        val firstScore = capsuleRichnessScore(first)
        val secondScore = capsuleRichnessScore(second)
        return if (secondScore > firstScore) second else first
    }

    private fun capsuleRichnessScore(capsule: CapsuleEntity): Int {
        var score = 0
        if (capsule.message.isNotBlank()) score += 2
        if (capsule.title.isNotBlank()) score += 2
        if (!capsule.imageBase64.isNullOrBlank()) score += 2
        if (capsule.latitude != 0.0 || capsule.longitude != 0.0) score += 1
        if (capsule.isSharedWithMe) score += 1
        if (capsule.isPublic) score += 1
        return score
    }

    private suspend fun syncSharedCapsulesToLocal() {
        val remoteShared = sharingRepository.getSharedWithMeCapsules().getOrNull() ?: return
        remoteShared.forEach { data ->
            val id = ((data["clientId"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["id"] as? String)) ?: return@forEach
            val sharedCapsule = CapsuleEntity(
                id = id,
                title = data["title"] as? String ?: "",
                message = data["message"] as? String ?: "",
                imageBase64 = (data["imageBase64"] as? String)?.ifBlank { null },
                imageMimeType = data["imageMimeType"] as? String ?: "image/jpeg",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                unlockTime = (data["unlockTime"] as? Number)?.toLong(),
                unlockLatitude = (data["unlockLatitude"] as? Number)?.toDouble(),
                unlockLongitude = (data["unlockLongitude"] as? Number)?.toDouble(),
                isUnlocked = data["isUnlocked"] as? Boolean ?: false,
                isLocationBased = data["isLocationBased"] as? Boolean ?: false,
                isTimeBased = data["isTimeBased"] as? Boolean ?: false,
                ownerId = data["ownerId"] as? String ?: "",
                sharedWith = (data["sharedWith"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isPublic = data["isPublic"] as? Boolean ?: false,
                canBeShared = data["canBeShared"] as? Boolean ?: false,
                isSharedWithMe = true,
                isDiscovered = data["isDiscovered"] as? Boolean ?: false,
                sharedByName = data["sharedByName"] as? String,
                sharedAt = (data["sharedAt"] as? Number)?.toLong()
            )
            capsuleRepository.insertCapsule(sharedCapsule)
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


