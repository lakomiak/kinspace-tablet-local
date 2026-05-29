package com.adhdfocus.app.domain.preferences

import com.adhdfocus.app.data.dao.UserPreferencesDao
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.puzzle.PuzzleAgeBand
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * UserPreferencesManager handles per-member preferences storage and retrieval.
 *
 * Manages:
 * - Loading preferences for a user
 * - Saving preferences for a user
 * - Updating individual preference fields
 * - Resetting preferences to defaults
 * - Validating preference values
 * - Serializing/deserializing complex preference objects
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val DEFAULT_THEME = "LIGHT"
        private const val DEFAULT_DAILY_RESET_TIME = "00:00"
        private const val DEFAULT_AFFIRMATION_FREQUENCY = 3
        private const val DEFAULT_ENABLE_GAMIFICATION = true
        private const val DEFAULT_ENABLE_TODO_EDITING = false
        private const val DEFAULT_TIMER_DURATION = 25
        private const val DEFAULT_AUTO_LOGOUT_TIMEOUT = 0
        private const val DEFAULT_PUZZLE_AGE_BAND = "5-6"
    }

    /**
     * Gets preferences for a user.
     *
     * @param userId User ID
     * @return UserPreferences or null if not found
     */
    suspend fun getPreferences(userId: String): UserPreferences? {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return userPreferencesDao.getPreferencesByUserId(userId)
    }

    /**
     * Gets preferences for a user, or creates defaults if not found.
     *
     * @param userId User ID
     * @return UserPreferences (existing or default)
     */
    suspend fun getPreferencesOrDefault(userId: String): UserPreferences {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return getPreferences(userId) ?: createDefaultPreferences(userId)
    }

    /**
     * Saves preferences for a user.
     *
     * @param preferences UserPreferences to save
     * @return True if successful
     */
    suspend fun savePreferences(preferences: UserPreferences): Boolean {
        return try {
            validatePreferences(preferences)
            if (userPreferencesDao.preferencesExist(preferences.userId) > 0) {
                userPreferencesDao.update(preferences)
            } else {
                userPreferencesDao.insert(preferences)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates the theme for a user.
     *
     * @param userId User ID
     * @param theme Theme to set
     * @return True if successful
     */
    suspend fun updateTheme(userId: String, theme: Theme): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            userPreferencesDao.updateTheme(userId, theme)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates visible todo groups for a user.
     *
     * @param userId User ID
     * @param groups List of visible todo group names
     * @return True if successful
     */
    suspend fun updateVisibleTodoGroups(userId: String, groups: List<String>): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            require(groups.isNotEmpty()) { "At least one todo group must be visible" }
            val serialized = json.encodeToString(groups)
            userPreferencesDao.updateVisibleTodoGroups(userId, serialized)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns custom todo groups for a user.
     */
    suspend fun getCustomTodoGroups(userId: String): List<String> {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        return getPreferences(userId)?.let { deserializeCustomTodoGroups(it.customTodoGroups) } ?: emptyList()
    }

    /**
     * Updates custom todo groups for a user.
     */
    suspend fun updateCustomTodoGroups(userId: String, groups: List<String>): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            val cleaned = groups.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val serialized = json.encodeToString(cleaned)
            userPreferencesDao.updateCustomTodoGroups(userId, serialized)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates notification preferences for a user.
     *
     * @param userId User ID
     * @param prefs NotificationPreferences to set
     * @return True if successful
     */
    suspend fun updateNotificationPreferences(
        userId: String,
        prefs: NotificationPreferences
    ): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            val serialized = json.encodeToString(prefs)
            userPreferencesDao.updateNotificationPreferences(userId, serialized)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates daily reset time for a user.
     *
     * @param userId User ID
     * @param time Time in HH:mm format with 15-minute increments (00:00 - 23:45)
     * @return True if successful
     */
    suspend fun updateDailyResetTime(userId: String, time: String): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            validateDailyResetTime(time)
            userPreferencesDao.updateDailyResetTime(userId, time)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates affirmation frequency for a user.
     *
     * @param userId User ID
     * @param frequency Frequency (1-5)
     * @return True if successful
     */
    suspend fun updateAffirmationFrequency(userId: String, frequency: Int): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            require(frequency in 1..5) { "Affirmation frequency must be between 1 and 5" }
            userPreferencesDao.updateAffirmationFrequency(userId, frequency)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates gamification enabled state for a user.
     *
     * @param userId User ID
     * @param enabled Whether gamification is enabled
     * @return True if successful
     */
    suspend fun updateGamificationEnabled(userId: String, enabled: Boolean): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            userPreferencesDao.updateGamificationEnabled(userId, enabled)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates timer default duration for a user.
     *
     * @param userId User ID
     * @param duration Duration in minutes (must be positive)
     * @return True if successful
     */
    suspend fun updateTimerDefaultDuration(userId: String, duration: Int): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            require(duration > 0) { "Timer duration must be positive" }
            userPreferencesDao.updateTimerDefaultDuration(userId, duration)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates auto-logout timeout for a user.
     *
     * @param userId User ID
     * @param timeout Timeout in minutes (0 = disabled)
     * @return True if successful
     */
    suspend fun updateAutoLogoutTimeout(userId: String, timeout: Int): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            require(timeout >= 0) { "Auto-logout timeout must be non-negative" }
            userPreferencesDao.updateAutoLogoutTimeout(userId, timeout)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates the puzzle age band for a user.
     */
    suspend fun updatePuzzleAgeBand(userId: String, ageBandKey: String): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            require(PuzzleAgeBand.isValidKey(ageBandKey)) {
                "Puzzle age band is not valid"
            }
            val existing = getPreferences(userId)
            if (existing == null) {
                userPreferencesDao.insert(
                    createDefaultPreferences(userId).copy(puzzleAgeBand = ageBandKey)
                )
            } else {
                userPreferencesDao.updatePuzzleAgeBand(userId, ageBandKey)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates whether the user can edit/delete todos from Home.
     *
     * @param userId User ID
     * @param enabled Whether todo editing is enabled
     * @return True if successful
     */
    suspend fun updateTodoEditingEnabled(userId: String, enabled: Boolean): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            val existing = getPreferences(userId)
            if (existing == null) {
                userPreferencesDao.insert(
                    createDefaultPreferences(userId).copy(enableTodoEditing = enabled)
                )
            } else {
                userPreferencesDao.updateEnableTodoEditing(userId, enabled)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resets preferences to defaults for a user.
     *
     * @param userId User ID
     * @return True if successful
     */
    suspend fun resetToDefaults(userId: String): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            val existing = getPreferences(userId)
            val defaultPrefs = createDefaultPreferences(userId).copy(
                settingsPasscodeHash = existing?.settingsPasscodeHash
            )
            if (existing == null) {
                userPreferencesDao.insert(defaultPrefs)
            } else {
                userPreferencesDao.update(defaultPrefs)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes preferences for a user.
     *
     * @param userId User ID
     * @return True if successful
     */
    suspend fun deletePreferences(userId: String): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            userPreferencesDao.deletePreferencesByUserId(userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if preferences exist for a user.
     *
     * @param userId User ID
     * @return True if preferences exist
     */
    suspend fun preferencesExist(userId: String): Boolean {
        return try {
            require(userId.isNotBlank()) { "userId cannot be blank" }
            userPreferencesDao.preferencesExist(userId) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deserializes visible todo groups from JSON.
     *
     * @param json JSON string
     * @return List of todo group names
     */
    fun deserializeVisibleTodoGroups(json: String): List<String> {
        return if (json.isBlank()) {
            emptyList()
        } else {
            try {
                this.json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Deserializes custom todo groups from JSON.
     */
    fun deserializeCustomTodoGroups(json: String): List<String> {
        return if (json.isBlank()) {
            emptyList()
        } else {
            try {
                this.json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Deserializes notification preferences from JSON.
     *
     * @param json JSON string
     * @return NotificationPreferences
     */
    fun deserializeNotificationPreferences(json: String): NotificationPreferences {
        return if (json.isBlank()) {
            NotificationPreferences()
        } else {
            try {
                this.json.decodeFromString(json)
            } catch (e: Exception) {
                NotificationPreferences()
            }
        }
    }

    /**
     * Creates default preferences for a user.
     *
     * @param userId User ID
     * @return Default UserPreferences
     */
    private fun createDefaultPreferences(userId: String): UserPreferences {
        return UserPreferences(
            userId = userId,
            theme = Theme.LIGHT,
            visibleTodoGroups = json.encodeToString(emptyList<String>()),
            customTodoGroups = json.encodeToString(emptyList<String>()),
            notificationPreferences = json.encodeToString(NotificationPreferences()),
            settingsPasscodeHash = null,
            enableTodoEditing = DEFAULT_ENABLE_TODO_EDITING,
            dailyResetTime = DEFAULT_DAILY_RESET_TIME,
            affirmationFrequency = DEFAULT_AFFIRMATION_FREQUENCY,
            enableGamification = DEFAULT_ENABLE_GAMIFICATION,
            timerDefaultDuration = DEFAULT_TIMER_DURATION,
            autoLogoutTimeout = DEFAULT_AUTO_LOGOUT_TIMEOUT,
            puzzleAgeBand = DEFAULT_PUZZLE_AGE_BAND
        )
    }

    /**
     * Validates preferences.
     *
     * @param preferences UserPreferences to validate
     * @throws IllegalArgumentException if validation fails
     */
    private fun validatePreferences(preferences: UserPreferences) {
        require(preferences.userId.isNotBlank()) { "userId cannot be blank" }
        require(preferences.affirmationFrequency in 1..5) {
            "affirmationFrequency must be between 1 and 5"
        }
        require(preferences.timerDefaultDuration > 0) {
            "timerDefaultDuration must be positive"
        }
        require(preferences.autoLogoutTimeout >= 0) {
            "autoLogoutTimeout must be non-negative"
        }
        require(PuzzleAgeBand.isValidKey(preferences.puzzleAgeBand)) {
            "puzzleAgeBand must be valid"
        }
        validateNotificationPreferences(preferences.notificationPreferences)
        validateDailyResetTime(preferences.dailyResetTime)
    }

    private fun validateNotificationPreferences(notificationPreferences: String) {
        val prefs = deserializeNotificationPreferences(notificationPreferences)
        require(prefs.categoryReminderPreferences.morningLeadMinutes >= 0) {
            "morningLeadMinutes must be non-negative"
        }
        require(prefs.categoryReminderPreferences.afternoonLeadMinutes >= 0) {
            "afternoonLeadMinutes must be non-negative"
        }
        require(prefs.categoryReminderPreferences.eveningLeadMinutes >= 0) {
            "eveningLeadMinutes must be non-negative"
        }
        require(prefs.categoryReminderPreferences.bedtimeLeadMinutes >= 0) {
            "bedtimeLeadMinutes must be non-negative"
        }
    }

    /**
     * Validates time format (HH:mm).
     *
     * @param time Time string to validate
     * @throws IllegalArgumentException if format is invalid
     */
    private fun validateTimeFormat(time: String) {
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        require(timeRegex.matches(time)) {
            "Time must be in HH:mm format (00:00 - 23:59)"
        }
    }

    /**
     * Validates daily reset time format (HH:mm with 15-minute increments).
     * Valid times: 00:00, 00:15, 00:30, 00:45, 01:00, ..., 23:45
     *
     * @param time Time string to validate
     * @throws IllegalArgumentException if format is invalid or not 15-minute increment
     */
    private fun validateDailyResetTime(time: String) {
        // First check basic format
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        require(timeRegex.matches(time)) {
            "Time must be in HH:mm format (00:00 - 23:45)"
        }

        // Check 15-minute increments
        val parts = time.split(":")
        val hour = parts[0].toIntOrNull() ?: throw IllegalArgumentException("Invalid hour")
        val minute = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid minute")

        // Validate hour range (0-23)
        require(hour in 0..23) {
            "Hour must be between 0 and 23"
        }

        // Validate minute is 0, 15, 30, or 45
        require(minute in listOf(0, 15, 30, 45)) {
            "Minutes must be 0, 15, 30, or 45 (15-minute increments)"
        }
    }
}
