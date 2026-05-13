package com.adhdfocus.app.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import com.adhdfocus.app.domain.auth.AuthResult
import com.adhdfocus.app.domain.preferences.CloudCustomTodoGroupsSyncManager
import com.adhdfocus.app.domain.reminder.CategoryReminderScheduler
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.theme.ThemeManager
import com.adhdfocus.app.util.PinValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val themeManager: ThemeManager,
    private val authManager: com.adhdfocus.app.domain.auth.AuthManager,
    private val audioNotificationManager: AudioNotificationManager,
    private val categoryReminderScheduler: CategoryReminderScheduler,
    private val setupManager: TabletSetupManager,
    private val cloudCustomTodoGroupsSyncManager: CloudCustomTodoGroupsSyncManager
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

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

    private val _settingsPasscodeHash = MutableStateFlow<String?>(null)
    private val _hasSettingsPasscode = MutableStateFlow(false)
    val hasSettingsPasscode: StateFlow<Boolean> = _hasSettingsPasscode

    private val _settingsUnlocked = MutableStateFlow(true)
    val settingsUnlocked: StateFlow<Boolean> = _settingsUnlocked

    private val _allowTodoEditing = MutableStateFlow(false)
    val allowTodoEditing: StateFlow<Boolean> = _allowTodoEditing

    private val _customTodoGroups = MutableStateFlow<List<String>>(emptyList())
    val customTodoGroups: StateFlow<List<String>> = _customTodoGroups

    private val _showPasscodeSetupDialog = MutableStateFlow(false)
    val showPasscodeSetupDialog: StateFlow<Boolean> = _showPasscodeSetupDialog

    private val _recoverySignInIntent = MutableStateFlow<Intent?>(null)
    val recoverySignInIntent: StateFlow<Intent?> = _recoverySignInIntent

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private var currentUserId: String? = null

    init {
        // Auto-initialize with the current user's ID from stored tokens
        val userId = resolveUserId()
        if (userId.isNotBlank()) {
            currentUserId = userId
            loadSettings(userId)
        }
    }

    private fun resolveUserId(): String {
        // Extract sub from stored ID token
        val idToken = authManager.getAccessToken() ?: return "default_user"
        return try {
            val parts = idToken.split(".")
            if (parts.size == 3) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING))
                val json = org.json.JSONObject(payload)
                json.optString("sub").takeIf { it.isNotEmpty() } ?: "default_user"
            } else "default_user"
        } catch (e: Exception) { "default_user" }
    }

    /**
     * Initializes settings for a user.
     */
    fun initialize(userId: String) {
        val effectiveId = userId.ifBlank { resolveUserId() }
        currentUserId = effectiveId
        loadSettings(effectiveId)
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
                _settingsPasscodeHash.value = preferences.settingsPasscodeHash
                _hasSettingsPasscode.value = !preferences.settingsPasscodeHash.isNullOrBlank()
                _settingsUnlocked.value = preferences.settingsPasscodeHash.isNullOrBlank()
                _allowTodoEditing.value = preferences.enableTodoEditing
                _customTodoGroups.value = userPreferencesManager.deserializeCustomTodoGroups(preferences.customTodoGroups)
                _showPasscodeSetupDialog.value = false
                
                // Load theme into ThemeManager
                themeManager.loadThemeForUser(userId)
                syncCustomTodoGroupsWithCloud(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun lockSettings() {
        if (_settingsPasscodeHash.value.isNullOrBlank()) return
        _settingsUnlocked.value = false
    }

    fun unlockSettings(passcode: String) {
        val storedHash = _settingsPasscodeHash.value
        if (storedHash.isNullOrBlank()) {
            _settingsUnlocked.value = true
            return
        }
        if (!isValidSettingsPasscode(passcode)) {
            _errorMessage.value = "Passcode must be exactly 5 digits"
            return
        }
        if (PinValidator.validatePin(passcode, storedHash)) {
            _settingsUnlocked.value = true
            _errorMessage.value = null
        } else {
            _errorMessage.value = "Incorrect passcode"
        }
    }

    fun beginPasscodeSetup() {
        _errorMessage.value = null
        _showPasscodeSetupDialog.value = true
    }

    fun dismissPasscodeSetup() {
        _showPasscodeSetupDialog.value = false
    }

    fun saveSettingsPasscode(passcode: String) {
        if (!isValidSettingsPasscode(passcode)) {
            _errorMessage.value = "Passcode must be exactly 5 digits"
            return
        }
        _settingsPasscodeHash.value = PinValidator.hashPin(passcode)
        _settingsUnlocked.value = true
        _showPasscodeSetupDialog.value = false
        _hasSettingsPasscode.value = true
        saveCurrentSettings()
    }

    fun clearSettingsPasscode() {
        _settingsPasscodeHash.value = null
        _settingsUnlocked.value = true
        _hasSettingsPasscode.value = false
        saveCurrentSettings()
    }

    fun updateTodoEditingEnabled(enabled: Boolean) {
        _allowTodoEditing.value = enabled
        saveCurrentSettings()
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
        saveCurrentSettings()
    }

    fun removeCustomTodoGroup(group: String) {
        _customTodoGroups.value = _customTodoGroups.value.filterNot { it.equals(group, ignoreCase = true) }
        saveCurrentSettings()
    }

    fun startCloudRecoverySignIn() {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                _recoverySignInIntent.value = authManager.buildSignInIntent()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start Kinspace login: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun handleCloudRecoveryResult(data: Intent?) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val response = data?.let { net.openid.appauth.AuthorizationResponse.fromIntent(it) }
                val exception = data?.let { net.openid.appauth.AuthorizationException.fromIntent(it) }
                when (val result = authManager.handleAuthorizationResponse(response, exception)) {
                    is AuthResult.Success -> {
                        _settingsPasscodeHash.value = null
                        _settingsUnlocked.value = true
                        _showPasscodeSetupDialog.value = true
                        saveCurrentSettings()
                    }
                    is AuthResult.Error -> {
                        _errorMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Kinspace login failed: ${e.message}"
            } finally {
                _recoverySignInIntent.value = null
                _isSaving.value = false
            }
        }
    }

    fun clearRecoverySignInIntent() {
        _recoverySignInIntent.value = null
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
     * Plays the currently selected timer alarm sound.
     */
    fun previewTimerAlarm() {
        audioNotificationManager.playTimerCompletionSound(_notificationPreferences.value.timerAlarmSound)
    }

    /**
     * Plays the category reminder sound preview.
     */
    fun previewCategoryReminder() {
        audioNotificationManager.playTimerCompletionSound(_notificationPreferences.value.timerAlarmSound)
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
                    viewModelScope.launch {
                        runCatching { categoryReminderScheduler.rescheduleForCurrentSetup() }
                    }
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
                    customTodoGroups = serializeList(_customTodoGroups.value),
                    settingsPasscodeHash = _settingsPasscodeHash.value,
                    enableTodoEditing = _allowTodoEditing.value,
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
                } else {
                    syncCustomTodoGroupsToCloud()
                    viewModelScope.launch {
                        runCatching { categoryReminderScheduler.rescheduleForCurrentSetup() }
                    }
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
        return json.encodeToString(prefs)
    }

    private fun serializeList(list: List<String>): String {
        return json.encodeToString(list.map { it.trim() }.filter { it.isNotBlank() }.distinct())
    }

    private fun isValidSettingsPasscode(passcode: String): Boolean {
        return passcode.length == 5 && passcode.all { it.isDigit() }
    }

    private fun syncCustomTodoGroupsToCloud() {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                cloudCustomTodoGroupsSyncManager.saveCustomTodoGroups(
                    householdId = householdId,
                    groups = _customTodoGroups.value
                )
            }
        }
    }

    private fun syncCustomTodoGroupsWithCloud(userId: String) {
        val householdId = setupManager.getHouseholdId().orEmpty()
        if (householdId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val snapshot = cloudCustomTodoGroupsSyncManager.fetchCustomTodoGroups(householdId)
                when {
                    snapshot.fromCloud && snapshot.groups != _customTodoGroups.value -> {
                        _customTodoGroups.value = snapshot.groups
                        userPreferencesManager.updateCustomTodoGroups(userId, snapshot.groups)
                    }
                    !snapshot.fromCloud && _customTodoGroups.value.isNotEmpty() -> {
                        cloudCustomTodoGroupsSyncManager.saveCustomTodoGroups(
                            householdId = householdId,
                            groups = _customTodoGroups.value
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}
