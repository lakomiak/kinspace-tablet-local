package com.adhdfocus.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SettingsViewModel manages settings UI state and persistence.
 *
 * Manages:
 * - Loading settings for a user
 * - Updating individual settings
 * - Saving settings changes
 * - Resetting to defaults
 * - Error handling
 * - Loading states
 * - Per-member settings support
 * - Theme switching with app-wide application
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _theme = MutableStateFlow(Theme.LIGHT)
    val theme: StateFlow<Theme> = _theme

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences

    private val _dailyResetTime = MutableStateFlow("00:00")
    val dailyResetTime: StateFlow<String> = _dailyResetTime

    private val _affirmationFrequency = MutableStateFlow(3)
    val affirmationFrequency: StateFlow<Int> = _affirmationFrequency

    private val _gamificationEnabled = MutableStateFlow(true)
    val gamificationEnabled: StateFlow<Boolean> = _gamificationEnabled

    private val _badgesEnabled = MutableStateFlow(true)
    val badgesEnabled: StateFlow<Boolean> = _badgesEnabled

    private val _streaksEnabled = MutableStateFlow(true)
    val streaksEnabled: StateFlow<Boolean> = _streaksEnabled

    private val _efficiencyMetricsEnabled = MutableStateFlow(true)
    val efficiencyMetricsEnabled: StateFlow<Boolean> = _efficiencyMetricsEnabled

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
     * Initializes settings for a user.
     *
     * @param userId User ID
     */
    fun initialize(userId: String) {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        currentUserId = userId
        loadSettings(userId)
    }

    /**
     * Loads settings for a user.
     *
     * @param userId User ID
     */
    private fun loadSettings(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val preferences = userPreferencesManager.getPreferencesOrDefault(userId)
                _theme.value = preferences.theme
                _notificationPreferences.value =
                    userPreferencesManager.deserializeNotificationPreferences(preferences.notificationPreferences)
                _dailyResetTime.value = preferences.dailyResetTime
                _affirmationFrequency.value = preferences.affirmationFrequency
                _gamificationEnabled.value = preferences.enableGamification
                _badgesEnabled.value = preferences.enableBadges
                _streaksEnabled.value = preferences.enableStreaks
                _efficiencyMetricsEnabled.value = preferences.enableEfficiencyMetrics
                _timerDefaultDuration.value = preferences.timerDefaultDuration
                _autoLogoutTimeout.value = preferences.autoLogoutTimeout
                
                // Load theme into ThemeManager
                themeManager.loadThemeForUser(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the theme and applies it app-wide.
     *
     * @param theme Theme to set
     */
    fun updateTheme(theme: Theme) {
        _theme.value = theme
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                themeManager.setTheme(theme, userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update theme: ${e.message}"
            }
        }
    }

    /**
     * Updates notification preferences.
     *
     * @param prefs NotificationPreferences to set
     */
    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        _notificationPreferences.value = prefs
        saveCurrentSettings()
    }

    /**
     * Updates daily reset time.
     *
     * @param time Time in HH:mm format with 15-minute increments (00:00 - 23:45)
     */
    fun updateDailyResetTime(time: String) {
        if (!isValidDailyResetTime(time)) {
            _errorMessage.value = "Invalid time format. Use HH:mm with 15-minute increments (00:00 - 23:45)"
            return
        }
        _dailyResetTime.value = time
        saveCurrentSettings()
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
        saveCurrentSettings()
    }

    /**
     * Updates gamification enabled state.
     *
     * @param enabled Whether gamification is enabled
     */
    fun updateGamificationEnabled(enabled: Boolean) {
        _gamificationEnabled.value = enabled
        saveCurrentSettings()
    }

    /**
     * Updates badges enabled state.
     *
     * @param enabled Whether badges are enabled
     */
    fun updateBadgesEnabled(enabled: Boolean) {
        _badgesEnabled.value = enabled
        saveCurrentSettings()
    }

    /**
     * Updates streaks enabled state.
     *
     * @param enabled Whether streaks are enabled
     */
    fun updateStreaksEnabled(enabled: Boolean) {
        _streaksEnabled.value = enabled
        saveCurrentSettings()
    }

    /**
     * Updates efficiency metrics enabled state.
     *
     * @param enabled Whether efficiency metrics are enabled
     */
    fun updateEfficiencyMetricsEnabled(enabled: Boolean) {
        _efficiencyMetricsEnabled.value = enabled
        saveCurrentSettings()
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
        saveCurrentSettings()
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
        saveCurrentSettings()
    }

    /**
     * Resets settings to defaults.
     */
    fun resetToDefaults() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val success = userPreferencesManager.resetToDefaults(userId)
                if (success) {
                    loadSettings(userId)
                } else {
                    _errorMessage.value = "Failed to reset settings"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error resetting settings: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Saves current settings.
     */
    private fun saveCurrentSettings() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val preferences = UserPreferences(
                    userId = userId,
                    theme = _theme.value,
                    visibleTodoGroups = "[]",
                    notificationPreferences = serializeNotificationPreferences(_notificationPreferences.value),
                    dailyResetTime = _dailyResetTime.value,
                    affirmationFrequency = _affirmationFrequency.value,
                    enableGamification = _gamificationEnabled.value,
                    enableBadges = _badgesEnabled.value,
                    enableStreaks = _streaksEnabled.value,
                    enableEfficiencyMetrics = _efficiencyMetricsEnabled.value,
                    timerDefaultDuration = _timerDefaultDuration.value,
                    autoLogoutTimeout = _autoLogoutTimeout.value
                )
                val success = userPreferencesManager.savePreferences(preferences)
                if (!success) {
                    _errorMessage.value = "Failed to save settings"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving settings: ${e.message}"
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
     * Validates daily reset time format (HH:mm with 15-minute increments).
     * Valid times: 00:00, 00:15, 00:30, 00:45, 01:00, ..., 23:45
     *
     * @param time Time string to validate
     * @return True if valid
     */
    private fun isValidDailyResetTime(time: String): Boolean {
        // First check basic format
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        if (!timeRegex.matches(time)) {
            return false
        }

        // Check 15-minute increments
        val parts = time.split(":")
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false

        // Validate hour range (0-23)
        if (hour !in 0..23) {
            return false
        }

        // Validate minute is 0, 15, 30, or 45
        if (minute !in listOf(0, 15, 30, 45)) {
            return false
        }

        return true
    }

    /**
     * Serializes notification preferences to JSON.
     *
     * @param prefs NotificationPreferences to serialize
     * @return JSON string
     */
    private fun serializeNotificationPreferences(prefs: NotificationPreferences): String {
        return "{\"soundEnabled\":${prefs.soundEnabled},\"vibrationEnabled\":${prefs.vibrationEnabled},\"visualAlertsEnabled\":${prefs.visualAlertsEnabled}}"
    }
}
