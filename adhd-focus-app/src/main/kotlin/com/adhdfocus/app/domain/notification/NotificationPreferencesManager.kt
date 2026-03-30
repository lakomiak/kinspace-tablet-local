package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.NotificationPreferences
import kotlinx.coroutines.flow.StateFlow

/**
 * NotificationPreferencesManager handles notification preference management.
 *
 * Responsibilities:
 * - Get notification preferences for a user
 * - Set notification preferences for a user
 * - Update individual preference fields
 * - Observe preference changes
 * - Reset to defaults
 * - Validate preferences
 * - Per-member preference isolation
 *
 * Correctness Properties:
 * - Property 2.8: Notification preferences persist across app sessions
 * - Property 9: Notification preferences are per-member
 * - Property 10: Notification preferences are applied to all notifications
 */
interface NotificationPreferencesManager {
    /**
     * Gets notification preferences for a user.
     *
     * @param userId User ID
     * @return NotificationPreferences or null if not found
     */
    suspend fun getPreferences(userId: String): NotificationPreferences?

    /**
     * Gets notification preferences for a user, or defaults if not found.
     *
     * @param userId User ID
     * @return NotificationPreferences (existing or default)
     */
    suspend fun getPreferencesOrDefault(userId: String): NotificationPreferences

    /**
     * Sets notification preferences for a user.
     *
     * @param userId User ID
     * @param preferences NotificationPreferences to set
     * @return True if successful
     */
    suspend fun setPreferences(userId: String, preferences: NotificationPreferences): Boolean

    /**
     * Updates sound preference for a user.
     *
     * @param userId User ID
     * @param enabled Whether sound is enabled
     * @return True if successful
     */
    suspend fun setSoundEnabled(userId: String, enabled: Boolean): Boolean

    /**
     * Updates vibration preference for a user.
     *
     * @param userId User ID
     * @param enabled Whether vibration is enabled
     * @return True if successful
     */
    suspend fun setVibrationEnabled(userId: String, enabled: Boolean): Boolean

    /**
     * Updates visual alerts preference for a user.
     *
     * @param userId User ID
     * @param enabled Whether visual alerts are enabled
     * @return True if successful
     */
    suspend fun setVisualAlertsEnabled(userId: String, enabled: Boolean): Boolean

    /**
     * Resets notification preferences to defaults for a user.
     *
     * @param userId User ID
     * @return True if successful
     */
    suspend fun resetToDefaults(userId: String): Boolean

    /**
     * Observes notification preferences for a user.
     *
     * @param userId User ID
     * @return StateFlow of NotificationPreferences
     */
    fun observePreferences(userId: String): StateFlow<NotificationPreferences>
}
