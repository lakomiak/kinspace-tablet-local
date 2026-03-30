package com.adhdfocus.app.ui.settings

import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel.
 *
 * Tests:
 * - Settings state management
 * - Settings persistence
 * - Per-member settings isolation
 * - Settings reset to defaults
 * - Theme switching
 * - Notification preference updates
 * - Validation of settings values
 * - Error handling
 */
class SettingsViewModelUnitTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        viewModel = SettingsViewModel(userPreferencesManager)
    }

    @Test
    fun testInitializeLoadsSettingsForUser() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(
            userId = userId,
            theme = Theme.DARK,
            dailyResetTime = "06:00",
            affirmationFrequency = 4,
            enableGamification = false,
            timerDefaultDuration = 30,
            autoLogoutTimeout = 15
        )

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        viewModel.initialize(userId)

        // Assert
        assertEquals(Theme.DARK, viewModel.theme.value)
        assertEquals("06:00", viewModel.dailyResetTime.value)
        assertEquals(4, viewModel.affirmationFrequency.value)
        assertFalse(viewModel.gamificationEnabled.value)
        assertEquals(30, viewModel.timerDefaultDuration.value)
        assertEquals(15, viewModel.autoLogoutTimeout.value)
    }

    @Test
    fun testUpdateThemePersistsChange() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateTheme(Theme.DARK)

        // Assert
        assertEquals(Theme.DARK, viewModel.theme.value)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testUpdateNotificationPreferencesPersistsChange() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        val newPrefs = NotificationPreferences(soundEnabled = false, vibrationEnabled = true)
        viewModel.updateNotificationPreferences(newPrefs)

        // Assert
        assertEquals(false, viewModel.notificationPreferences.value.soundEnabled)
        assertEquals(true, viewModel.notificationPreferences.value.vibrationEnabled)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testUpdateDailyResetTimeValidatesFormat() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateDailyResetTime("invalid")

        // Assert
        assertEquals("Invalid time format. Use HH:mm with 15-minute increments (00:00 - 23:45)", viewModel.errorMessage.value)
    }

    @Test
    fun testUpdateDailyResetTimeAcceptsValidFormat() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateDailyResetTime("14:30")

        // Assert
        assertEquals("14:30", viewModel.dailyResetTime.value)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testUpdateDailyResetTimeRejectsInvalidMinutes() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateDailyResetTime("14:25")

        // Assert
        assertEquals("Invalid time format. Use HH:mm with 15-minute increments (00:00 - 23:45)", viewModel.errorMessage.value)
    }

    @Test
    fun testUpdateDailyResetTimeAccepts15MinuteIncrements() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act & Assert - Test all valid 15-minute increments
        val validTimes = listOf("00:00", "00:15", "00:30", "00:45", "06:00", "12:15", "18:30", "23:45")
        for (time in validTimes) {
            viewModel.updateDailyResetTime(time)
            assertEquals(time, viewModel.dailyResetTime.value)
        }
    }

    @Test
    fun testUpdateDailyResetTimeRejectsInvalidHour() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateDailyResetTime("24:00")

        // Assert
        assertEquals("Invalid time format. Use HH:mm with 15-minute increments (00:00 - 23:45)", viewModel.errorMessage.value)
    }

    @Test
    fun testUpdateAffirmationFrequencyValidatesRange() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateAffirmationFrequency(10)

        // Assert
        assertEquals("Affirmation frequency must be between 1 and 5", viewModel.errorMessage.value)
    }

    @Test
    fun testUpdateAffirmationFrequencyAcceptsValidValue() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateAffirmationFrequency(5)

        // Assert
        assertEquals(5, viewModel.affirmationFrequency.value)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testUpdateTimerDefaultDurationValidatesPositive() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateTimerDefaultDuration(0)

        // Assert
        assertEquals("Timer duration must be positive", viewModel.errorMessage.value)
    }

    @Test
    fun testUpdateTimerDefaultDurationAcceptsValidValue() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateTimerDefaultDuration(45)

        // Assert
        assertEquals(45, viewModel.timerDefaultDuration.value)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testUpdateAutoLogoutTimeoutValidatesNonNegative() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateAutoLogoutTimeout(-1)

        // Assert
        assertEquals("Auto-logout timeout must be non-negative", viewModel.errorMessage.value)
    }

    @Test
    fun testUpdateAutoLogoutTimeoutAcceptsZero() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateAutoLogoutTimeout(0)

        // Assert
        assertEquals(0, viewModel.autoLogoutTimeout.value)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testUpdateGamificationEnabledPersistsChange() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId, enableGamification = true)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateGamificationEnabled(false)

        // Assert
        assertFalse(viewModel.gamificationEnabled.value)
        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testResetToDefaultsCallsManager() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.resetToDefaults(userId) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.resetToDefaults()

        // Assert
        coVerify { userPreferencesManager.resetToDefaults(userId) }
    }

    @Test
    fun testPerMemberSettingsIsolation() = runTest {
        // Arrange
        val userId1 = "user-1"
        val userId2 = "user-2"
        val prefs1 = UserPreferences(userId = userId1, theme = Theme.LIGHT)
        val prefs2 = UserPreferences(userId = userId2, theme = Theme.DARK)

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId1) } returns prefs1
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId2) } returns prefs2
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        viewModel.initialize(userId1)
        val theme1 = viewModel.theme.value

        viewModel.initialize(userId2)
        val theme2 = viewModel.theme.value

        // Assert
        assertEquals(Theme.LIGHT, theme1)
        assertEquals(Theme.DARK, theme2)
    }

    @Test
    fun testErrorHandlingOnLoadFailure() = runTest {
        // Arrange
        val userId = "user-123"
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } throws Exception("Database error")

        // Act
        viewModel.initialize(userId)

        // Assert
        assertTrue(viewModel.errorMessage.value?.contains("Failed to load settings") == true)
    }

    @Test
    fun testErrorHandlingOnSaveFailure() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns false

        viewModel.initialize(userId)

        // Act
        viewModel.updateTheme(Theme.DARK)

        // Assert
        assertEquals("Failed to save settings", viewModel.errorMessage.value)
    }

    @Test
    fun testClearErrorMessage() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        viewModel.updateDailyResetTime("invalid")

        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.errorMessage.value)
    }
}
