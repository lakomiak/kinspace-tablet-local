package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationPreferencesManagerImpl implements notification preference management.
 *
 * Features:
 * - Per-member preference isolation
 * - In-memory state via MutableStateFlow
 * - Persistence via UserPreferencesManager
 * - Validation of preferences
 * - Default preference values
 */
@Singleton
class NotificationPreferencesManagerImpl @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : NotificationPreferencesManager {

    private val preferencesCache = mutableMapOf<String, MutableStateFlow<NotificationPreferences>>()

    companion object {
        private val DEFAULT_PREFERENCES = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = true,
            visualAlertsEnabled = true
        )
    }

    override suspend fun getPreferences(userId: String): NotificationPreferences? {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        val userPrefs = userPreferencesManager.getPreferences(userId) ?: return null
        return userPreferencesManager.deserializeNotificationPreferences(userPrefs.notificationPreferences)
    }

    override suspend fun getPreferencesOrDefault(userId: String): NotificationPreferences {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return getPreferences(userId) ?: DEFAULT_PREFERENCES
    }

    override suspend fun setPreferences(
        userId: String,
        preferences: NotificationPreferences
    ): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return try {
            userPreferencesManager.updateNotificationPreferences(userId, preferences)
            updateCache(userId, preferences)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setSoundEnabled(userId: String, enabled: Boolean): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return try {
            val current = getPreferencesOrDefault(userId)
            val updated = current.copy(soundEnabled = enabled)
            setPreferences(userId, updated)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setVibrationEnabled(userId: String, enabled: Boolean): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return try {
            val current = getPreferencesOrDefault(userId)
            val updated = current.copy(vibrationEnabled = enabled)
            setPreferences(userId, updated)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setVisualAlertsEnabled(userId: String, enabled: Boolean): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return try {
            val current = getPreferencesOrDefault(userId)
            val updated = current.copy(visualAlertsEnabled = enabled)
            setPreferences(userId, updated)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun resetToDefaults(userId: String): Boolean {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return setPreferences(userId, DEFAULT_PREFERENCES)
    }

    override fun observePreferences(userId: String): StateFlow<NotificationPreferences> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return preferencesCache.getOrPut(userId) {
            MutableStateFlow(DEFAULT_PREFERENCES)
        }
    }

    private fun updateCache(userId: String, preferences: NotificationPreferences) {
        val flow = preferencesCache.getOrPut(userId) {
            MutableStateFlow(DEFAULT_PREFERENCES)
        }
        flow.value = preferences
    }
}
