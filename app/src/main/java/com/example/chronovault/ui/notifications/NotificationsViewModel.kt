package com.example.chronovault.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.R
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.entity.NotificationCategory
import com.example.chronovault.data.local.entity.NotificationEntity
import com.example.chronovault.data.local.entity.NotificationType
import com.example.chronovault.data.repository.NotificationRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * ViewModel for Notifications screen
 * Displays capsule unlock events and sharing notifications
 */
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)
    private val notificationRepository: NotificationRepository = ServiceLocator.provideNotificationRepository(application)
    private val allNotifications: LiveData<List<NotificationEntity>> =
        notificationRepository.observeNotifications().asLiveData()

    // UI State
    private val _selectedCategory = MutableLiveData(NotificationCategory.PERSONAL)
    val selectedCategory: LiveData<NotificationCategory> = _selectedCategory

    private val _notifications = MediatorLiveData<List<AppNotification>>(emptyList())
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _notificationSound = MutableLiveData<Boolean>(true)
    val notificationSound: LiveData<Boolean> = _notificationSound

    private val _notificationVibration = MutableLiveData<Boolean>(true)
    val notificationVibration: LiveData<Boolean> = _notificationVibration

    private val _emptyStateMessage = MediatorLiveData<String>()
    val emptyStateMessage: LiveData<String> = _emptyStateMessage

    init {
        setupNotificationStream()
        loadNotificationPreferences()
    }

    private fun setupNotificationStream() {
        _loadingState.value = LoadingState.Loading

        val recompute: () -> Unit = {
            val selected = _selectedCategory.value ?: NotificationCategory.PERSONAL
            val mapped = allNotifications.value.orEmpty()
                .filter { it.typeCategory == selected }
                .sortedByDescending { it.timestamp }
                .map { it.toUi() }
            _notifications.value = mapped
            _emptyStateMessage.value = when (selected) {
                NotificationCategory.PERSONAL -> getApplication<Application>().getString(R.string.empty_personal_notifications)
                NotificationCategory.WORLD -> getApplication<Application>().getString(R.string.empty_world_notifications)
            }
            _loadingState.value = LoadingState.Success
        }

        _notifications.addSource(allNotifications) { recompute() }
        _notifications.addSource(_selectedCategory) { recompute() }
        _emptyStateMessage.addSource(_selectedCategory) { recompute() }
    }

    fun setCategory(category: NotificationCategory) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
    }

    fun loadNotifications() {
        // Kept for compatibility with existing calls.
        _loadingState.value = LoadingState.Success
    }

    fun loadNotificationPreferences() {
        _notificationSound.value = preferencesManager.getNotificationSound()
        _notificationVibration.value = preferencesManager.getNotificationVibration()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
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
    val typeCategory: NotificationCategory,
    val capsuleId: String?,
    val read: Boolean = false
)

private fun NotificationEntity.toUi(): AppNotification {
    return AppNotification(
        id = id,
        title = title,
        message = message,
        timestamp = timestamp,
        type = type,
        typeCategory = typeCategory,
        capsuleId = capsuleId,
        read = isRead
    )
}

