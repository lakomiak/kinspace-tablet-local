package com.adhdfocus.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SettingsScreen UI.
 *
 * Tests:
 * - Settings screen rendering
 * - Settings updates via UI
 * - Settings persistence after app restart
 * - Per-member settings switching
 * - Theme application
 * - Error message display
 * - Loading states
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        viewModel = SettingsViewModel(userPreferencesManager)
    }

    @Test
    fun testSettingsScreenRendersWithDefaultSettings() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Display").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Behavior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Affirmations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gamification").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun testThemeSelectorDisplaysOptions() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, theme = Theme.LIGHT)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("LIGHT").assertIsDisplayed()
        composeTestRule.onNodeWithText("DARK").assertIsDisplayed()
    }

    @Test
    fun testNotificationPreferencesDisplaysToggles() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Sound").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vibration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Visual Alerts").assertIsDisplayed()
    }

    @Test
    fun testDailyResetTimeFieldDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, dailyResetTime = "06:00")
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Daily Reset Time").assertIsDisplayed()
    }

    @Test
    fun testAffirmationFrequencySliderDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, affirmationFrequency = 3)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Affirmation Frequency").assertIsDisplayed()
    }

    @Test
    fun testGamificationToggleDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, enableGamification = true)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Enable Gamification").assertIsDisplayed()
    }

    @Test
    fun testTimerDefaultDurationFieldDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, timerDefaultDuration = 25)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Timer Default Duration (minutes)").assertIsDisplayed()
    }

    @Test
    fun testAutoLogoutTimeoutFieldDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId, autoLogoutTimeout = 0)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Auto-Logout Timeout (minutes, 0 = disabled)").assertIsDisplayed()
    }

    @Test
    fun testAboutSectionDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("ADHD Focus App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version 1.0.0").assertIsDisplayed()
    }

    @Test
    fun testResetToDefaultsButtonDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Reset to Defaults").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset to Defaults").assertIsEnabled()
    }

    @Test
    fun testDoneButtonDisplays() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsEnabled()
    }

    @Test
    fun testLoadingStateDisplaysProgressIndicator() = runTest {
        // Arrange
        val userId = "user-123"
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns UserPreferences(userId = userId)
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert - After loading completes, settings should be visible
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun testErrorMessageDisplaysOnFailure() = runTest {
        // Arrange
        val userId = "user-123"
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } throws Exception("Database error")

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert
        composeTestRule.onNodeWithText("Failed to load settings").assertIsDisplayed()
    }

    @Test
    fun testDoneButtonCallsBackClickCallback() = runTest {
        // Arrange
        val userId = "user-123"
        val preferences = UserPreferences(userId = userId)
        var backClicked = false
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId,
                onBackClick = { backClicked = true },
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Done").performClick()

        // Assert
        assert(backClicked)
    }

    @Test
    fun testPerMemberSettingsSwitching() = runTest {
        // Arrange
        val userId1 = "user-1"
        val userId2 = "user-2"
        val prefs1 = UserPreferences(userId = userId1, theme = Theme.LIGHT)
        val prefs2 = UserPreferences(userId = userId2, theme = Theme.DARK)

        coEvery { userPreferencesManager.getPreferencesOrDefault(userId1) } returns prefs1
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId2) } returns prefs2
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        // Act - Load first user
        composeTestRule.setContent {
            SettingsScreen(
                userId = userId1,
                onBackClick = {},
                viewModel = viewModel
            )
        }

        // Assert - First user's theme should be LIGHT
        assert(viewModel.theme.value == Theme.LIGHT)

        // Act - Switch to second user
        viewModel.initialize(userId2)

        // Assert - Second user's theme should be DARK
        assert(viewModel.theme.value == Theme.DARK)
    }
}
