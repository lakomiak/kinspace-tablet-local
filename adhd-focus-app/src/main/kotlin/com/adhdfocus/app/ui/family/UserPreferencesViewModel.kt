package com.adhdfocus.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import com.adhdfocus.app.domain.puzzle.PuzzleAgeBand
import com.adhdfocus.app.domain.reminder.CategoryReminderScheduler
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * UserPreferencesViewModel manages per-member preferences UI state.
 *
 * Manages:
 * - Loading preferences for a user
 * - Updating individual preference fields
 * - Saving preferences
 * - Resetting to defaults
 * - Error handling
 * - Loading states
 */
@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val audioNotificationManager: AudioNotificationManager,
    private val categoryReminderScheduler: CategoryReminderScheduler
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val _theme = MutableStateFlow(Theme.LIGHT)
    val theme: StateFlow<Theme> = _theme

    private val _visibleTodoGroups = MutableStateFlow<List<String>>(emptyList())
    val visibleTodoGroups: StateFlow<List<String>> = _visibleTodoGroups

    private val _customTodoGroups = MutableStateFlow<List<String>>(emptyList())
    val customTodoGroups: StateFlow<List<String>> = _customTodoGroups

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences

    private val _dailyResetTime = MutableStateFlow("00:00")
    val dailyResetTime: StateFlow<String> = _dailyResetTime

    private val _affirmationFrequency = MutableStateFlow(3)
    val affirmationFrequency: StateFlow<Int> = _affirmationFrequency

    private val _gamificationEnabled = MutableStateFlow(true)
    val gamificationEnabled: StateFlow<Boolean> = _gamificationEnabled

    private val _puzzleAgeBand = MutableStateFlow(PuzzleAgeBand.DEFAULT)
    val puzzleAgeBand: StateFlow<PuzzleAgeBand> = _puzzleAgeBand

    private val _timerDefaultDuration = MutableStateFlow(25)
    val timerDefaultDuration: StateFlow<Int> = _timerDefaultDuration

    private val _autoLogoutTimeout = MutableStateFlow(0)
    val autoLogoutTimeout: StateFlow<Int> = _autoLogoutTimeout

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private var currentUserId: String? = null

    /**
     * Initializes preferences for a user.
     *
     * @param userId User ID
     */
    fun initialize(userId: String) {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        currentUserId = userId
        loadPreferences(userId)
    }

    /**
     * Loads preferences for a user.
     *
     * @param userId User ID
     */
    private fun loadPreferences(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
                _theme.value = preferences.theme
                _visibleTodoGroups.value =
                    userPreferencesManager.deserializeVisibleTodoGroups(preferences.visibleTodoGroups)
                _customTodoGroups.value =
                    userPreferencesManager.deserializeCustomTodoGroups(preferences.customTodoGroups)
                _notificationPreferences.value =
                    userPreferencesManager.deserializeNotificationPreferences(preferences.notificationPreferences)
                _dailyResetTime.value = preferences.dailyResetTime
                _affirmationFrequency.value = preferences.affirmationFrequency
                _gamificationEnabled.value = preferences.enableGamification
                _puzzleAgeBand.value = PuzzleAgeBand.fromKey(preferences.puzzleAgeBand)
                _timerDefaultDuration.value = preferences.timerDefaultDuration
                _autoLogoutTimeout.value = preferences.autoLogoutTimeout
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load preferences: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the theme.
     *
     * @param theme Theme to set
     */
    fun updateTheme(theme: Theme) {
        _theme.value = theme
        saveCurrentPreferences()
    }

    /**
     * Updates visible todo groups.
     *
     * @param groups List of visible todo group names
     */
    fun updateVisibleTodoGroups(groups: List<String>) {
        if (groups.isEmpty()) {
            _errorMessage.value = "At least one todo group must be visible"
            return
        }
        _visibleTodoGroups.value = groups
        saveCurrentPreferences()
    }

    /**
     * Updates notification preferences.
     *
     * @param prefs NotificationPreferences to set
     */
    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        _notificationPreferences.value = prefs
        saveCurrentPreferences()
    }

    fun addCustomTodoGroup(rawGroup: String) {
        val group = rawGroup.trim()
        if (group.isBlank()) {
            _errorMessage.value = "Category name cannot be blank"
            return
        }
        val normalized = group.lowercase()
        val defaultGroups = listOf("Morning", "Afternoon", "Evening", "Bedtime", "Other")
        if (defaultGroups.any { it.lowercase() == normalized }) {
            _errorMessage.value = "That category already exists"
            return
        }
        if (_customTodoGroups.value.any { it.equals(group, ignoreCase = true) }) {
            _errorMessage.value = "That category already exists"
            return
        }
        _customTodoGroups.value = _customTodoGroups.value + group
        saveCurrentPreferences()
    }

    fun removeCustomTodoGroup(group: String) {
        _customTodoGroups.value = _customTodoGroups.value.filterNot { it.equals(group, ignoreCase = true) }
        saveCurrentPreferences()
    }

    /**
     * Plays the currently selected timer alarm sound.
     */
    fun previewTimerAlarm() {
        audioNotificationManager.playTimerCompletionSound(
            _notificationPreferences.value.timerAlarmSound,
            durationMs = 2_500L
        )
    }

    fun previewCategoryReminder() {
        audioNotificationManager.playCategoryReminderSound(
            _notificationPreferences.value.timerAlarmSound,
            durationMs = 2_500L
        )
    }

    /**
     * Updates daily reset time.
     *
     * @param time Time in HH:mm format
     */
    fun updateDailyResetTime(time: String) {
        if (!isValidTimeFormat(time)) {
            _errorMessage.value = "Invalid time format. Use HH:mm (00:00 - 23:59)"
            return
        }
        _dailyResetTime.value = time
        saveCurrentPreferences()
    }

    /**
     * Updates affirmation frequency.
     *
     * @param frequency Frequency (1-5)
     */
    fun updateAffirmationFrequency(frequency: Int) {
        if (frequency !in 1..5) {
            _errorMessage.value = "Affirmation frequency must be between 1 and 5"
            return
        }
        _affirmationFrequency.value = frequency
        saveCurrentPreferences()
    }

    /**
     * Updates gamification enabled state.
     *
     * @param enabled Whether gamification is enabled
     */
    fun updateGamificationEnabled(enabled: Boolean) {
        _gamificationEnabled.value = enabled
        saveCurrentPreferences()
    }

    fun updatePuzzleAgeBand(ageBand: PuzzleAgeBand) {
        _puzzleAgeBand.value = ageBand
        saveCurrentPreferences()
    }

    /**
     * Updates timer default duration.
     *
     * @param duration Duration in minutes
     */
    fun updateTimerDefaultDuration(duration: Int) {
        if (duration <= 0) {
            _errorMessage.value = "Timer duration must be positive"
            return
        }
        _timerDefaultDuration.value = duration
        saveCurrentPreferences()
    }

    /**
     * Updates auto-logout timeout.
     *
     * @param timeout Timeout in minutes (0 = disabled)
     */
    fun updateAutoLogoutTimeout(timeout: Int) {
        if (timeout < 0) {
            _errorMessage.value = "Auto-logout timeout must be non-negative"
            return
        }
        _autoLogoutTimeout.value = timeout
        saveCurrentPreferences()
    }

    /**
     * Resets preferences to defaults.
     */
    fun resetToDefaults() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val success = userPreferencesManager.resetToDefaults(userId)
                if (success) {
                    loadPreferences(userId)
                    viewModelScope.launch {
                        runCatching { categoryReminderScheduler.rescheduleForCurrentSetup() }
                    }
                } else {
                    _errorMessage.value = "Failed to reset preferences"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error resetting preferences: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Saves current preferences.
     */
    private fun saveCurrentPreferences() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val preferences = UserPreferences(
                    userId = userId,
                    theme = _theme.value,
                    visibleTodoGroups = serializeList(_visibleTodoGroups.value),
                    customTodoGroups = serializeList(_customTodoGroups.value),
                    notificationPreferences = serializeNotificationPreferences(_notificationPreferences.value),
                    dailyResetTime = _dailyResetTime.value,
                    affirmationFrequency = _affirmationFrequency.value,
                    enableGamification = _gamificationEnabled.value,
                    puzzleAgeBand = _puzzleAgeBand.value.key,
                    timerDefaultDuration = _timerDefaultDuration.value,
                    autoLogoutTimeout = _autoLogoutTimeout.value
                )
                val success = userPreferencesManager.savePreferences(preferences)
                if (!success) {
                    _errorMessage.value = "Failed to save preferences"
                } else {
                    viewModelScope.launch {
                        runCatching { categoryReminderScheduler.rescheduleForCurrentSetup() }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving preferences: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Clears the error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Validates time format (HH:mm).
     *
     * @param time Time string to validate
     * @return True if valid
     */
    private fun isValidTimeFormat(time: String): Boolean {
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        return timeRegex.matches(time)
    }

    /**
     * Serializes a list to JSON.
     *
     * @param list List to serialize
     * @return JSON string
     */
    private fun serializeList(list: List<String>): String {
        return if (list.isEmpty()) {
            "[]"
        } else {
            list.joinToString(",", "[\"", "\"]") { "\"$it\"" }
        }
    }

    /**
     * Serializes notification preferences to JSON.
     *
     * @param prefs NotificationPreferences to serialize
     * @return JSON string
     */
    private fun serializeNotificationPreferences(prefs: NotificationPreferences): String {
        return json.encodeToString(prefs)
    }
}
