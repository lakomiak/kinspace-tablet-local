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
 * Unit tests for settings persistence functionality.
 *
 * Tests:
 * - Settings are persisted immediately on change
 * - Settings survive app restart (loaded from database)
 * - Per-member settings are isolated
 * - Settings reset to defaults works correctly
 * - Concurrent updates don't cause conflicts
 * - Settings validation before persistence
 */
class SettingsPersistenceUnitTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        viewModel = SettingsViewModel(userPreferencesManager)
    }

    @Test
    fun testSettingsPersistedImmediatelyOnThemeChange() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId, theme = Theme.LIGHT)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateTheme(Theme.DARK)

        // Assert
        coVerify(exactly = 1) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsPersistedImmediatelyOnNotificationChange() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        val newPrefs = NotificationPreferences(soundEnabled = false)
        viewModel.updateNotificationPreferences(newPrefs)

        // Assert
        coVerify(exactly = 1) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsPersistedImmediatelyOnFrequencyChange() = runTest {
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
        coVerify(exactly = 1) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsPersistedImmediatelyOnGamificationChange() = runTest {
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
        coVerify(exactly = 1) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsSurviveAppRestart() = runTest {
        // Arrange
        val userId = "user-123"
        val savedPrefs = UserPreferences(
            userId = userId,
            theme = Theme.DARK,
            dailyResetTime = "06:00",
            affirmationFrequency = 4,
            enableGamification = false,
            timerDefaultDuration = 30,
            autoLogoutTimeout = 15
        )

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns savedPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act - Simulate app restart by creating new ViewModel and loading settings
        val newViewModel = SettingsViewModel(userPreferencesManager)
        newViewModel.initialize(userId)

        // Assert
        assertEquals(Theme.DARK, newViewModel.theme.value)
        assertEquals("06:00", newViewModel.dailyResetTime.value)
        assertEquals(4, newViewModel.affirmationFrequency.value)
        assertFalse(newViewModel.gamificationEnabled.value)
        assertEquals(30, newViewModel.timerDefaultDuration.value)
        assertEquals(15, newViewModel.autoLogoutTimeout.value)
    }

    @Test
    fun testPerMemberSettingsAreIsolated() = runTest {
        // Arrange
        val userId1 = "user-1"
        val userId2 = "user-2"
        val prefs1 = UserPreferences(
            userId = userId1,
            theme = Theme.LIGHT,
            affirmationFrequency = 2
        )
        val prefs2 = UserPreferences(
            userId = userId2,
            theme = Theme.DARK,
            affirmationFrequency = 5
        )

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId1) } returns prefs1
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId2) } returns prefs2
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        // Act
        viewModel.initialize(userId1)
        val user1Theme = viewModel.theme.value
        val user1Frequency = viewModel.affirmationFrequency.value

        viewModel.initialize(userId2)
        val user2Theme = viewModel.theme.value
        val user2Frequency = viewModel.affirmationFrequency.value

        // Assert
        assertEquals(Theme.LIGHT, user1Theme)
        assertEquals(2, user1Frequency)
        assertEquals(Theme.DARK, user2Theme)
        assertEquals(5, user2Frequency)
    }

    @Test
    fun testResetToDefaultsPersistsDefaults() = runTest {
        // Arrange
        val userId = "user-123"
        val modifiedPrefs = UserPreferences(
            userId = userId,
            theme = Theme.DARK,
            affirmationFrequency = 5,
            enableGamification = false
        )
        val defaultPrefs = UserPreferences(userId = userId)

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns modifiedPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.resetToDefaults(userId) } returns true
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs

        viewModel.initialize(userId)

        // Act
        viewModel.resetToDefaults()

        // Assert
        coVerify { userPreferencesManager.resetToDefaults(userId) }
    }

    @Test
    fun testMultipleSettingChangesArePersisted() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act
        viewModel.updateTheme(Theme.DARK)
        viewModel.updateAffirmationFrequency(4)
        viewModel.updateGamificationEnabled(false)
        viewModel.updateTimerDefaultDuration(30)

        // Assert
        coVerify(exactly = 4) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testInvalidSettingsAreNotPersisted() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act
        viewModel.updateAffirmationFrequency(10) // Invalid
        viewModel.updateTimerDefaultDuration(0) // Invalid
        viewModel.updateAutoLogoutTimeout(-1) // Invalid

        // Assert
        coVerify(exactly = 0) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsPersistenceHandlesErrors() = runTest {
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
    fun testSettingsPersistenceWithConcurrentUpdates() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act - Simulate rapid consecutive updates
        viewModel.updateTheme(Theme.DARK)
        viewModel.updateAffirmationFrequency(5)
        viewModel.updateGamificationEnabled(false)

        // Assert
        assertEquals(Theme.DARK, viewModel.theme.value)
        assertEquals(5, viewModel.affirmationFrequency.value)
        assertFalse(viewModel.gamificationEnabled.value)
        coVerify(exactly = 3) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsPersistenceValidatesBeforeSaving() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act - Try to set invalid values
        viewModel.updateDailyResetTime("25:00") // Invalid hour
        viewModel.updateAffirmationFrequency(0) // Below range
        viewModel.updateTimerDefaultDuration(-5) // Negative

        // Assert
        assertTrue(viewModel.errorMessage.value != null)
        coVerify(exactly = 0) { userPreferencesManager.savePreferences(any()) }
    }

    @Test
    fun testSettingsPersistencePreservesOtherSettings() = runTest {
        // Arrange
        val userId = "user-123"
        val originalPrefs = UserPreferences(
            userId = userId,
            theme = Theme.LIGHT,
            dailyResetTime = "06:00",
            affirmationFrequency = 3,
            enableGamification = true,
            timerDefaultDuration = 25,
            autoLogoutTimeout = 0
        )

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns originalPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act - Change only one setting
        viewModel.updateTheme(Theme.DARK)

        // Assert - Other settings should remain unchanged
        assertEquals("06:00", viewModel.dailyResetTime.value)
        assertEquals(3, viewModel.affirmationFrequency.value)
        assertTrue(viewModel.gamificationEnabled.value)
        assertEquals(25, viewModel.timerDefaultDuration.value)
        assertEquals(0, viewModel.autoLogoutTimeout.value)
    }
}
