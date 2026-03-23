package com.example.chronovault.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * ViewModel for Notifications screen
 * Displays capsule unlock events and sharing notifications
 */
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    // UI State
    private val _notifications = MutableLiveData<List<AppNotification>>(emptyList())
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _notificationSound = MutableLiveData<Boolean>(true)
    val notificationSound: LiveData<Boolean> = _notificationSound

    private val _notificationVibration = MutableLiveData<Boolean>(true)
    val notificationVibration: LiveData<Boolean> = _notificationVibration

    init {
        loadNotifications()
        loadNotificationPreferences()
    }

    fun loadNotifications() {
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            try {
                // Mock notifications - in real app, fetch from Firestore
                val mockNotifications = listOf(
                    AppNotification(
                        id = "1",
                        title = "You're near a memory!",
                        message = "You're within 100m of 'Summer 2024' capsule",
                        timestamp = System.currentTimeMillis() - 300000, // 5 mins ago
                        type = NotificationType.LOCATION_UNLOCK,
                        capsuleTitle = "Summer 2024",
                        read = false
                    ),
                    AppNotification(
                        id = "2",
                        title = "Capsule Unlocked!",
                        message = "'Graduation Day' capsule has been unlocked",
                        timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                        type = NotificationType.TIME_UNLOCK,
                        capsuleTitle = "Graduation Day",
                        read = false
                    ),
                    AppNotification(
                        id = "3",
                        title = "New Shared Capsule",
                        message = "Sarah shared 'Paris Trip' with you",
                        timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                        type = NotificationType.SHARED,
                        capsuleTitle = "Paris Trip",
                        read = true
                    ),
                    AppNotification(
                        id = "4",
                        title = "Capsule Created",
                        message = "Your capsule 'New Year Wishes' has been saved",
                        timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                        type = NotificationType.CAPSULE_CREATED,
                        capsuleTitle = "New Year Wishes",
                        read = true
                    )
                )

                _notifications.value = mockNotifications
                _loadingState.value = LoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    fun loadNotificationPreferences() {
        _notificationSound.value = preferencesManager.getNotificationSound()
        _notificationVibration.value = preferencesManager.getNotificationVibration()
    }

    fun markAsRead(notificationId: String) {
        val currentList = _notifications.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(read = true)
            _notifications.value = currentList
            preferencesManager.setNotificationRead(notificationId, true)
        }
    }

    fun deleteNotification(notificationId: String) {
        val currentList = _notifications.value?.toMutableList() ?: return
        currentList.removeAll { it.id == notificationId }
        _notifications.value = currentList
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun toggleNotificationSound(enabled: Boolean) {
        _notificationSound.value = enabled
        preferencesManager.setNotificationSound(enabled)
    }

    fun toggleNotificationVibration(enabled: Boolean) {
        _notificationVibration.value = enabled
        preferencesManager.setNotificationVibration(enabled)
    }

    fun getUnreadCount(): Int {
        return _notifications.value?.count { !it.read } ?: 0
    }

    fun getTimeAgoString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffInSeconds = (now - timestamp) / 1000

        return when {
            diffInSeconds < 60 -> "just now"
            diffInSeconds < 3600 -> "${diffInSeconds / 60}m ago"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600}h ago"
            else -> "${diffInSeconds / 86400}d ago"
        }
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val capsuleTitle: String,
    val read: Boolean = false
)

enum class NotificationType {
    TIME_UNLOCK,
    LOCATION_UNLOCK,
    SHARED,
    CAPSULE_CREATED,
    OTHER
}

