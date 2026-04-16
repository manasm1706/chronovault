package com.example.chronovault.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure SharedPreferences wrapper for storing user data
 */
class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "chronovault_preferences",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR = "user_avatar_base64"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_NOTIFICATION_VIBRATION = "notification_vibration"
        private const val KEY_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"
        private const val KEY_SELECTED_THEME_MODE = "selected_theme_mode"
        private const val KEY_SELECTED_COLOR_SCHEME = "selected_color_scheme"
        private const val KEY_LAST_QUOTE_TEXT = "last_quote_text"
        private const val KEY_LAST_QUOTE_AUTHOR = "last_quote_author"
        private const val KEY_LAST_QUOTE_TIMESTAMP = "last_quote_timestamp"
    }

    // User Session
    fun setUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun setUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun setUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun setUserAvatar(avatar: String) {
        sharedPreferences.edit().putString(KEY_USER_AVATAR, avatar).apply()
    }

    fun getUserAvatar(): String? {
        return sharedPreferences.getString(KEY_USER_AVATAR, null)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // First Launch
    fun setFirstLaunch(isFirstLaunch: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, isFirstLaunch).apply()
    }

    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_FIRST_LAUNCH, true)
    }

    // Settings
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationSound(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply()
    }

    fun getNotificationSound(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_SOUND, true)
    }

    fun setNotificationVibration(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, enabled).apply()
    }

    fun getNotificationVibration(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_VIBRATION, true)
    }

    fun setLocationTrackingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_LOCATION_TRACKING_ENABLED, enabled).apply()
    }

    fun isLocationTrackingEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOCATION_TRACKING_ENABLED, false)
    }

    fun setSelectedThemeMode(mode: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_THEME_MODE, mode).apply()
    }

    fun getSelectedThemeMode(): String {
        return sharedPreferences.getString(KEY_SELECTED_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun setSelectedColorScheme(scheme: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_COLOR_SCHEME, scheme).apply()
    }

    fun getSelectedColorScheme(): String {
        return sharedPreferences.getString(KEY_SELECTED_COLOR_SCHEME, "GREEN") ?: "GREEN"
    }

    fun setLastQuoteText(value: String) {
        sharedPreferences.edit().putString(KEY_LAST_QUOTE_TEXT, value).apply()
    }

    fun getLastQuoteText(): String? {
        return sharedPreferences.getString(KEY_LAST_QUOTE_TEXT, null)
    }

    fun setLastQuoteAuthor(value: String) {
        sharedPreferences.edit().putString(KEY_LAST_QUOTE_AUTHOR, value).apply()
    }

    fun getLastQuoteAuthor(): String? {
        return sharedPreferences.getString(KEY_LAST_QUOTE_AUTHOR, null)
    }

    fun setLastQuoteTimestamp(value: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_QUOTE_TIMESTAMP, value).apply()
    }

    fun getLastQuoteTimestamp(): Long {
        return sharedPreferences.getLong(KEY_LAST_QUOTE_TIMESTAMP, 0L)
    }

    // Generic String methods for auth tokens and other data
    fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    // Notification read tracking
    fun setNotificationRead(notificationId: String, isRead: Boolean) {
        sharedPreferences.edit().putBoolean("notification_read_$notificationId", isRead).apply()
    }

    fun isNotificationRead(notificationId: String): Boolean {
        return sharedPreferences.getBoolean("notification_read_$notificationId", false)
    }

    fun isOneTimeEventMarked(eventKey: String): Boolean {
        return sharedPreferences.getBoolean("event_once_$eventKey", false)
    }

    fun markOneTimeEvent(eventKey: String) {
        sharedPreferences.edit().putBoolean("event_once_$eventKey", true).apply()
    }

    fun shouldRunCooldownEvent(eventKey: String, cooldownMillis: Long): Boolean {
        val prefKey = "event_cooldown_$eventKey"
        val now = System.currentTimeMillis()
        val lastRun = sharedPreferences.getLong(prefKey, 0L)
        if (now - lastRun < cooldownMillis) {
            return false
        }
        sharedPreferences.edit().putLong(prefKey, now).apply()
        return true
    }

    // Clear all data (logout)
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}

