package com.adhdfocus.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.adhdfocus.app.data.database.DatabaseBackupInfo
import com.adhdfocus.app.data.database.DatabaseBackupManager
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import com.adhdfocus.app.domain.puzzle.PuzzleAgeBand
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BackupListItem(
    val path: String,
    val displayName: String,
    val subtitle: String,
    val sizeLabel: String
)

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
    private val audioNotificationManager: AudioNotificationManager,
    private val categoryReminderScheduler: CategoryReminderScheduler,
    private val setupManager: TabletSetupManager,
    private val backupManager: DatabaseBackupManager
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

    private val _puzzleAgeBand = MutableStateFlow(PuzzleAgeBand.DEFAULT)
    val puzzleAgeBand: StateFlow<PuzzleAgeBand> = _puzzleAgeBand

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _backupDirectory = MutableStateFlow("")
    val backupDirectory: StateFlow<String> = _backupDirectory

    private val _backups = MutableStateFlow<List<BackupListItem>>(emptyList())
    val backups: StateFlow<List<BackupListItem>> = _backups

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy

    private val _restoreReady = MutableStateFlow(false)
    val restoreReady: StateFlow<Boolean> = _restoreReady

    private val _restoreTargetName = MutableStateFlow<String?>(null)
    val restoreTargetName: StateFlow<String?> = _restoreTargetName

    private var currentUserId: String? = null

    init {
        _backupDirectory.value = backupManager.getBackupDirectoryPath()
        refreshBackupList()
        // Local build: initialize from the tablet's assigned member.
        val userId = resolveUserId()
        if (userId.isNotBlank()) {
            currentUserId = userId
            loadSettings(userId)
        }
    }

    private fun resolveUserId(): String {
        return setupManager.getAssignedMemberId().orEmpty()
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
                val storedTabletPasscodeHash = setupManager.getSettingsPasscodeHash()
                val migratedPasscodeHash = storedTabletPasscodeHash
                    ?: preferences.settingsPasscodeHash?.takeIf { it.isNotBlank() }?.also { legacyHash ->
                        setupManager.setSettingsPasscodeHash(legacyHash)
                    }
                _theme.value = preferences.theme
                _notificationPreferences.value =
                    userPreferencesManager.deserializeNotificationPreferences(preferences.notificationPreferences)
                _dailyResetTime.value = preferences.dailyResetTime
                _affirmationFrequency.value = preferences.affirmationFrequency
                _gamificationEnabled.value = preferences.enableGamification
                _badgesEnabled.value = preferences.enableBadges
                _streaksEnabled.value = preferences.enableStreaks
                _efficiencyMetricsEnabled.value = preferences.enableEfficiencyMetrics
                _puzzleAgeBand.value = PuzzleAgeBand.fromKey(preferences.puzzleAgeBand)
                _timerDefaultDuration.value = preferences.timerDefaultDuration
                _autoLogoutTimeout.value = preferences.autoLogoutTimeout
<<<<<<< HEAD
                _settingsPasscodeHash.value = migratedPasscodeHash
                _hasSettingsPasscode.value = !migratedPasscodeHash.isNullOrBlank()
                _settingsUnlocked.value = migratedPasscodeHash.isNullOrBlank()
=======
                val tabletPasscodeHash = setupManager.getSettingsPasscodeHash()
                if (tabletPasscodeHash.isNullOrBlank() && !preferences.settingsPasscodeHash.isNullOrBlank()) {
                    setupManager.setSettingsPasscodeHash(preferences.settingsPasscodeHash)
                }
                val resolvedPasscodeHash = setupManager.getSettingsPasscodeHash()
                _settingsPasscodeHash.value = resolvedPasscodeHash
                _hasSettingsPasscode.value = !resolvedPasscodeHash.isNullOrBlank()
                _settingsUnlocked.value = resolvedPasscodeHash.isNullOrBlank()
>>>>>>> 4f65714c60489fff1b0bd9e94bbb1557cb4bbd9f
                _allowTodoEditing.value = preferences.enableTodoEditing
                _customTodoGroups.value = userPreferencesManager.deserializeCustomTodoGroups(preferences.customTodoGroups)
                _showPasscodeSetupDialog.value = false
                
                // Load theme into ThemeManager
                themeManager.loadThemeForUser(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshBackupList() {
        viewModelScope.launch {
            _backups.value = backupManager.getAvailableBackups().map { it.toUiModel() }
            _backupDirectory.value = backupManager.getBackupDirectoryPath()
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _backupBusy.value = true
            _backupStatusMessage.value = null
            try {
                val backup = backupManager.createBackup()
                if (backup == null) {
                    _errorMessage.value = "Could not create a backup yet."
                } else {
                    _backupStatusMessage.value = "Backup created: ${backup.displayName}"
                    refreshBackupList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create backup: ${e.message}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun deleteBackup(path: String) {
        viewModelScope.launch {
            _backupBusy.value = true
            _backupStatusMessage.value = null
            try {
                if (backupManager.deleteBackup(path)) {
                    _backupStatusMessage.value = "Backup removed."
                    refreshBackupList()
                } else {
                    _errorMessage.value = "Could not delete that backup."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete backup: ${e.message}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun deleteAllBackups() {
        viewModelScope.launch {
            _backupBusy.value = true
            _backupStatusMessage.value = null
            try {
                val deleted = backupManager.deleteAllBackups()
                _backupStatusMessage.value = if (deleted > 0) {
                    "Deleted $deleted backup${if (deleted == 1) "" else "s"}."
                } else {
                    "No backups were removed."
                }
                refreshBackupList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear backups: ${e.message}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun restoreBackup(path: String) {
        viewModelScope.launch {
            _backupBusy.value = true
            _backupStatusMessage.value = null
            try {
                val restored = backupManager.restoreFromBackup(path)
                if (restored) {
                    _restoreTargetName.value = _backups.value.firstOrNull { it.path == path }?.displayName ?: "backup"
                    _restoreReady.value = true
                    _backupStatusMessage.value = "Backup restored. Restart the app to load the restored household data."
                } else {
                    _errorMessage.value = "Could not restore that backup."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore backup: ${e.message}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            _backupStatusMessage.value = null
            try {
                val backup = backupManager.importBackupFromUri(uri)
                if (backup == null) {
                    _errorMessage.value = "Could not import that backup file."
                } else {
                    _backupStatusMessage.value = "Imported backup: ${backup.displayName}"
                    refreshBackupList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to import backup: ${e.message}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun exportBackupToUri(path: String, uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            _backupStatusMessage.value = null
            try {
                if (backupManager.exportBackupToUri(path, uri)) {
                    _backupStatusMessage.value = "Backup exported successfully."
                } else {
                    _errorMessage.value = "Could not export that backup."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to export backup: ${e.message}"
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun acknowledgeRestoreRestart() {
        _restoreReady.value = false
        _restoreTargetName.value = null
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
        setupManager.setSettingsPasscodeHash(_settingsPasscodeHash.value)
        _settingsUnlocked.value = true
        _showPasscodeSetupDialog.value = false
        _hasSettingsPasscode.value = true
        saveCurrentSettings()
    }

    fun clearSettingsPasscode() {
        _settingsPasscodeHash.value = null
        setupManager.setSettingsPasscodeHash(null)
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
        audioNotificationManager.playCategoryReminderSound(_notificationPreferences.value.timerAlarmSound)
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

    fun updatePuzzleAgeBand(ageBand: PuzzleAgeBand) {
        _puzzleAgeBand.value = ageBand
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
                    settingsPasscodeHash = null,
                    enableTodoEditing = _allowTodoEditing.value,
                    dailyResetTime = _dailyResetTime.value,
                    affirmationFrequency = _affirmationFrequency.value,
                    enableGamification = _gamificationEnabled.value,
                    enableBadges = _badgesEnabled.value,
                    enableStreaks = _streaksEnabled.value,
                    enableEfficiencyMetrics = _efficiencyMetricsEnabled.value,
                    puzzleAgeBand = _puzzleAgeBand.value.key,
                    timerDefaultDuration = _timerDefaultDuration.value,
                    autoLogoutTimeout = _autoLogoutTimeout.value
                )
                val success = userPreferencesManager.savePreferences(preferences)
                if (!success) {
                    _errorMessage.value = "Failed to save settings"
                } else {
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

    fun clearBackupStatus() {
        _backupStatusMessage.value = null
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

    private fun DatabaseBackupInfo.toUiModel(): BackupListItem {
        val formattedDate = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date(lastModifiedAt))
        return BackupListItem(
            path = path,
            displayName = displayName,
            subtitle = formattedDate,
            sizeLabel = formatSize(sizeBytes)
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

}
