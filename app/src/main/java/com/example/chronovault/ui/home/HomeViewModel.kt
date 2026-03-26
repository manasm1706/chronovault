package com.example.chronovault.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.R
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.remote.RetrofitClient
import com.example.chronovault.data.repository.CapsuleRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.LocationHelper
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Home/Dashboard screen
 * Displays capsule statistics and motivational quotes
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val capsuleRepository: CapsuleRepository = ServiceLocator.provideCapsuleRepository(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)
    private var dashboardJob: Job? = null
    private var userLocation: Pair<Double, Double>? = null

    // UI State
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _totalCapsules = MutableLiveData<Int>(0)
    val totalCapsules: LiveData<Int> = _totalCapsules

    private val _lockedCapsules = MutableLiveData<Int>(0)
    val lockedCapsules: LiveData<Int> = _lockedCapsules

    private val _unlockedCapsules = MutableLiveData<Int>(0)
    val unlockedCapsules: LiveData<Int> = _unlockedCapsules

    private val _sharedCapsules = MutableLiveData<Int>(0)
    val sharedCapsules: LiveData<Int> = _sharedCapsules

    private val _capsuleList = MutableLiveData<List<CapsuleEntity>>(emptyList())
    val capsuleList: LiveData<List<CapsuleEntity>> = _capsuleList

    private val _recentCapsules = MutableLiveData<List<CapsuleEntity>>(emptyList())
    val recentCapsules: LiveData<List<CapsuleEntity>> = _recentCapsules

    private val _nearbyCapsule = MutableLiveData<CapsuleEntity?>(null)
    val nearbyCapsule: LiveData<CapsuleEntity?> = _nearbyCapsule

    private val _greetingSubtitle = MutableLiveData<String>()
    val greetingSubtitle: LiveData<String> = _greetingSubtitle

    private val _dailyQuote = MutableLiveData<String>("Preserve moments. Rediscover yourself.")
    val dailyQuote: LiveData<String> = _dailyQuote

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    init {
        loadDashboardData()
        refreshQuote()
    }

    fun refreshQuote() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.quoteApi.getRandomQuote()
                _dailyQuote.value = "${response.content} — ${response.author}"
            } catch (_: Exception) {
                // Keep the default quote on failure
            }
        }
    }

    fun loadDashboardData() {
        _loadingState.value = LoadingState.Loading
        val userId = preferencesManager.getUserId()
        if (userId == null) {
            _loadingState.value = LoadingState.Error("User not found")
            return
        }

        _userName.value = preferencesManager.getUserName() ?: "User"
        _greetingSubtitle.value = getGreetingSubtitle(0)

        dashboardJob?.cancel()
        dashboardJob = viewModelScope.launch {
            try {
                // FIX: 12
                persistExpiredTimeUnlocks()

                combine(
                    capsuleRepository.getUserCapsules(userId),
                    capsuleRepository.getSharedCapsules(userId)
                ) { userCapsules, sharedCapsules ->
                    val merged = (userCapsules + sharedCapsules)
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt }

                    _capsuleList.value = merged
                    _recentCapsules.value = merged.take(3)

                    _totalCapsules.value = userCapsules.size
                    _lockedCapsules.value = userCapsules.count { !it.isUnlocked }
                    _unlockedCapsules.value = userCapsules.count { it.isUnlocked }
                    _sharedCapsules.value = sharedCapsules.size
                    _greetingSubtitle.value = getGreetingSubtitle(userCapsules.size)

                    updateNearbyCapsule(merged)
                    _loadingState.value = LoadingState.Success
                }.collect { }
            } catch (_: CancellationException) {
                // FIX: 14
                // Dashboard reload/filter cancellations are expected during lifecycle changes.
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load dashboard")
            }
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        userLocation = latitude to longitude
        updateNearbyCapsule(_capsuleList.value.orEmpty())
    }

    private fun updateNearbyCapsule(capsules: List<CapsuleEntity>) {
        val currentLocation = userLocation ?: run {
            _nearbyCapsule.value = null
            return
        }

        val closest = capsules
            .asSequence()
            .filterNot { it.latitude == 0.0 && it.longitude == 0.0 }
            .map { capsule ->
                val distance = LocationHelper.calculateDistance(
                    currentLocation.first,
                    currentLocation.second,
                    capsule.latitude,
                    capsule.longitude
                )
                capsule to distance
            }
            // FIX: 15
            .filter { (_, distance) -> distance <= 50f }
            .minByOrNull { (_, distance) -> distance }
            ?.first

        _nearbyCapsule.value = closest
    }

    private fun getGreetingSubtitle(totalOwnedCapsules: Int): String {
        return when {
            totalOwnedCapsules == 0 -> getApplication<Application>().getString(R.string.home_subtitle_welcome)
            else -> getApplication<Application>().getString(R.string.home_subtitle_rediscover)
        }
    }

    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
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

