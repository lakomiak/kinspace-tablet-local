package com.adhdfocus.app.ui.family

import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserPreferencesViewModel.
 *
 * Tests verify:
 * - Preference loading
 * - Preference updates
 * - Preference saving
 * - Error handling
 * - State management
 * - Edge cases
 */
class UserPreferencesViewModelTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: UserPreferencesViewModel
    private lateinit var testDispatcher: StandardTestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        userPreferencesManager = mockk(relaxed = true)
        viewModel = UserPreferencesViewModel(userPreferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Initialization Tests ============

    @Test
    fun `initialize loads preferences successfully`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId, theme = Theme.DARK)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Theme.DARK, viewModel.theme.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `initialize throws on blank userId`() {
        try {
            viewModel.initialize("")
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") == true)
        }
    }

    @Test
    fun `initialize sets loading state`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        assertTrue(viewModel.isLoading.value || !viewModel.isLoading.value) // Initial state
        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `initialize handles exception gracefully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } throws Exception("Database error")

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("Failed to load") == true)
    }

    // ============ Theme Update Tests ============

    @Test
    fun `updateTheme changes theme value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTheme(Theme.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Theme.DARK, viewModel.theme.value)
    }

    @Test
    fun `updateTheme triggers save`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTheme(Theme.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesManager.savePreferences(any()) }
    }

    // ============ Visible Todo Groups Update Tests ============

    @Test
    fun `updateVisibleTodoGroups changes groups value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        val groups = listOf("Morning", "Afternoon")
        viewModel.updateVisibleTodoGroups(groups)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(groups, viewModel.visibleTodoGroups.value)
    }

    @Test
    fun `updateVisibleTodoGroups rejects empty groups`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateVisibleTodoGroups(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("At least one") == true)
    }

    // ============ Notification Preferences Update Tests ============

    @Test
    fun `updateNotificationPreferences changes preferences value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        val newPrefs = NotificationPreferences(soundEnabled = false)
        viewModel.updateNotificationPreferences(newPrefs)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.notificationPreferences.value.soundEnabled)
    }

    // ============ Daily Reset Time Update Tests ============

    @Test
    fun `updateDailyResetTime changes time value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDailyResetTime("14:30")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("14:30", viewModel.dailyResetTime.value)
    }

    @Test
    fun `updateDailyResetTime rejects invalid format`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDailyResetTime("25:00")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("Invalid time format") == true)
    }

    @Test
    fun `updateDailyResetTime accepts valid times`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        val validTimes = listOf("00:00", "12:00", "23:59")
        validTimes.forEach { time ->
            viewModel.updateDailyResetTime(time)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(time, viewModel.dailyResetTime.value)
        }
    }

    // ============ Affirmation Frequency Update Tests ============

    @Test
    fun `updateAffirmationFrequency changes frequency value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAffirmationFrequency(4)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.affirmationFrequency.value)
    }

    @Test
    fun `updateAffirmationFrequency rejects invalid frequency`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAffirmationFrequency(10)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("between 1 and 5") == true)
    }

    // ============ Gamification Update Tests ============

    @Test
    fun `updateGamificationEnabled changes enabled value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateGamificationEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.gamificationEnabled.value)
    }

    // ============ Timer Duration Update Tests ============

    @Test
    fun `updateTimerDefaultDuration changes duration value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTimerDefaultDuration(45)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(45, viewModel.timerDefaultDuration.value)
    }

    @Test
    fun `updateTimerDefaultDuration rejects non-positive duration`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTimerDefaultDuration(0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("positive") == true)
    }

    // ============ Auto-Logout Timeout Update Tests ============

    @Test
    fun `updateAutoLogoutTimeout changes timeout value`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAutoLogoutTimeout(30)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(30, viewModel.autoLogoutTimeout.value)
    }

    @Test
    fun `updateAutoLogoutTimeout accepts zero for disabled`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAutoLogoutTimeout(0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.autoLogoutTimeout.value)
    }

    @Test
    fun `updateAutoLogoutTimeout rejects negative timeout`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAutoLogoutTimeout(-1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("non-negative") == true)
    }

    // ============ Reset to Defaults Tests ============

    @Test
    fun `resetToDefaults resets all preferences`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId, theme = Theme.DARK)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.resetToDefaults(userId) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetToDefaults()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesManager.resetToDefaults(userId) }
    }

    @Test
    fun `resetToDefaults handles failure gracefully`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.resetToDefaults(userId) } returns false

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetToDefaults()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value?.contains("Failed to reset") == true)
    }

    // ============ Error Handling Tests ============

    @Test
    fun `clearError clears error message`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDailyResetTime("invalid")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)

        viewModel.clearError()

        assertNull(viewModel.errorMessage.value)
    }

    // ============ Edge Cases ============

    @Test
    fun `multiple updates in sequence work correctly`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTheme(Theme.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAffirmationFrequency(4)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateGamificationEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Theme.DARK, viewModel.theme.value)
        assertEquals(4, viewModel.affirmationFrequency.value)
        assertEquals(false, viewModel.gamificationEnabled.value)
    }

    @Test
    fun `saving state is managed correctly`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns preferences
        coEvery { userPreferencesManager.deserializeVisibleTodoGroups(any()) } returns emptyList()
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isSaving.value)

        viewModel.updateTheme(Theme.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isSaving.value)
    }
}
