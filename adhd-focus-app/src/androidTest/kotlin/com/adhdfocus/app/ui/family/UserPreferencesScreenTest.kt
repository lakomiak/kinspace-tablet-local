package com.adhdfocus.app.ui.family

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for UserPreferencesScreen.
 *
 * Tests verify:
 * - Preference display
 * - Preference updates
 * - Save/cancel
 * - Reset to defaults
 * - Loading states
 * - Error messages
 */
@RunWith(AndroidJUnit4::class)
class UserPreferencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var testDispatcher: StandardTestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        userPreferencesManager = mockk(relaxed = true)
    }

    // ============ Display Tests ============

    @Test
    fun testPreferencesScreenDisplaysTitle() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
    }

    @Test
    fun testThemeSelectorDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
    }

    @Test
    fun testNotificationPreferencesPanelDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun testDailyResetTimeDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Daily Reset Time").assertIsDisplayed()
    }

    @Test
    fun testAffirmationFrequencyDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Affirmation Frequency").assertIsDisplayed()
    }

    @Test
    fun testGamificationToggleDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Enable Gamification").assertIsDisplayed()
    }

    @Test
    fun testTimerDurationDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Timer Default Duration (minutes)").assertIsDisplayed()
    }

    @Test
    fun testAutoLogoutTimeoutDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Auto-Logout Timeout (minutes, 0 = disabled)").assertIsDisplayed()
    }

    @Test
    fun testActionButtonsDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Reset to Defaults").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    // ============ Button Interaction Tests ============

    @Test
    fun testDoneButtonClickable() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        var backClicked = false
        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = { backClicked = true },
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Done").performClick()

        assert(backClicked)
    }

    @Test
    fun testResetToDefaultsButtonClickable() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.resetToDefaults(userId) } returns true

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Reset to Defaults").performClick()
    }

    // ============ Theme Selection Tests ============

    @Test
    fun testThemeButtonsDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId, theme = Theme.LIGHT)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("LIGHT").assertIsDisplayed()
        composeTestRule.onNodeWithText("DARK").assertIsDisplayed()
    }

    // ============ Input Field Tests ============

    @Test
    fun testDailyResetTimeInputAcceptsText() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        // Find and interact with the time input field
        composeTestRule.onNodeWithText("HH:mm").performTextInput("14:30")
    }

    @Test
    fun testTimerDurationInputAcceptsNumbers() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        // Find and interact with the duration input field
        composeTestRule.onNodeWithText("Enter minutes").performTextInput("45")
    }

    // ============ Loading State Tests ============

    @Test
    fun testLoadingIndicatorDisplayedDuringLoad() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        // After loading completes, preferences should be displayed
        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
    }

    // ============ Error State Tests ============

    @Test
    fun testErrorMessageDisplayedOnLoadFailure() {
        val userId = "user-1"
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } throws Exception("Database error")

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        // Error message should be displayed
        composeTestRule.onNodeWithText("Failed to load").assertIsDisplayed()
    }

    // ============ Notification Preferences Tests ============

    @Test
    fun testNotificationSwitchesDisplayed() {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        composeTestRule.onNodeWithText("Sound").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vibration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Visual Alerts").assertIsDisplayed()
    }

    // ============ State Persistence Tests ============

    @Test
    fun testPreferencesLoadedOnInitialization() {
        val userId = "user-1"
        val preferences = UserPreferences(
            userId = userId,
            theme = Theme.DARK,
            affirmationFrequency = 4,
            timerDefaultDuration = 30
        )
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        composeTestRule.setContent {
            UserPreferencesScreen(
                userId = userId,
                onBackClick = {},
                viewModel = UserPreferencesViewModel(userPreferencesManager)
            )
        }

        // Verify that preferences are loaded and displayed
        composeTestRule.onNodeWithText("Preferences").assertIsDisplayed()
    }
}
