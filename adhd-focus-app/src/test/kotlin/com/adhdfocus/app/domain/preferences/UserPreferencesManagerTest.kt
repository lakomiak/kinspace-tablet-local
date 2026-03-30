package com.adhdfocus.app.domain.preferences

import com.adhdfocus.app.data.dao.UserPreferencesDao
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserPreferencesManager.
 *
 * Tests verify:
 * - Preference loading
 * - Preference saving
 * - Preference updates
 * - Preference validation
 * - Default values
 * - Error handling
 * - Edge cases
 */
class UserPreferencesManagerTest {

    private lateinit var userPreferencesDao: UserPreferencesDao
    private lateinit var manager: UserPreferencesManager

    @Before
    fun setUp() {
        userPreferencesDao = mockk(relaxed = true)
        manager = UserPreferencesManager(userPreferencesDao)
    }

    // ============ Preference Loading Tests ============

    @Test
    fun `getPreferences returns preferences when found`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId, theme = Theme.DARK)
        coEvery { userPreferencesDao.getPreferencesByUserId(userId) } returns preferences

        val result = manager.getPreferences(userId)

        assertEquals(preferences, result)
    }

    @Test
    fun `getPreferences returns null when not found`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.getPreferencesByUserId(userId) } returns null

        val result = manager.getPreferences(userId)

        assertNull(result)
    }

    @Test
    fun `getPreferencesOrDefault returns existing preferences`() = runTest {
        val userId = "user-1"
        val preferences = UserPreferences(userId = userId, theme = Theme.DARK)
        coEvery { userPreferencesDao.getPreferencesByUserId(userId) } returns preferences

        val result = manager.getPreferencesOrDefault(userId)

        assertEquals(preferences, result)
    }

    @Test
    fun `getPreferencesOrDefault returns defaults when not found`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.getPreferencesByUserId(userId) } returns null

        val result = manager.getPreferencesOrDefault(userId)

        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals(Theme.LIGHT, result.theme)
    }

    @Test
    fun `getPreferences throws on blank userId`() = runTest {
        try {
            manager.getPreferences("")
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") == true)
        }
    }

    // ============ Preference Saving Tests ============

    @Test
    fun `savePreferences updates preferences successfully`() = runTest {
        val preferences = UserPreferences(userId = "user-1")
        coEvery { userPreferencesDao.update(preferences) } returns Unit

        val result = manager.savePreferences(preferences)

        assertTrue(result)
        coVerify { userPreferencesDao.update(preferences) }
    }

    @Test
    fun `savePreferences returns false on exception`() = runTest {
        val preferences = UserPreferences(userId = "user-1")
        coEvery { userPreferencesDao.update(preferences) } throws Exception("Database error")

        val result = manager.savePreferences(preferences)

        assertFalse(result)
    }

    @Test
    fun `savePreferences validates preferences before saving`() = runTest {
        val invalidPreferences = UserPreferences(
            userId = "user-1",
            affirmationFrequency = 10 // Invalid: must be 1-5
        )

        val result = manager.savePreferences(invalidPreferences)

        assertFalse(result)
    }

    // ============ Theme Update Tests ============

    @Test
    fun `updateTheme updates theme successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateTheme(userId, Theme.DARK) } returns Unit

        val result = manager.updateTheme(userId, Theme.DARK)

        assertTrue(result)
        coVerify { userPreferencesDao.updateTheme(userId, Theme.DARK) }
    }

    @Test
    fun `updateTheme returns false on exception`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateTheme(userId, Theme.DARK) } throws Exception("Error")

        val result = manager.updateTheme(userId, Theme.DARK)

        assertFalse(result)
    }

    @Test
    fun `updateTheme throws on blank userId`() = runTest {
        try {
            manager.updateTheme("", Theme.DARK)
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") == true)
        }
    }

    // ============ Visible Todo Groups Update Tests ============

    @Test
    fun `updateVisibleTodoGroups updates groups successfully`() = runTest {
        val userId = "user-1"
        val groups = listOf("Morning", "Afternoon")
        coEvery { userPreferencesDao.updateVisibleTodoGroups(userId, any()) } returns Unit

        val result = manager.updateVisibleTodoGroups(userId, groups)

        assertTrue(result)
        coVerify { userPreferencesDao.updateVisibleTodoGroups(userId, any()) }
    }

    @Test
    fun `updateVisibleTodoGroups returns false on empty groups`() = runTest {
        val userId = "user-1"
        val groups = emptyList<String>()

        val result = manager.updateVisibleTodoGroups(userId, groups)

        assertFalse(result)
    }

    @Test
    fun `updateVisibleTodoGroups returns false on exception`() = runTest {
        val userId = "user-1"
        val groups = listOf("Morning")
        coEvery { userPreferencesDao.updateVisibleTodoGroups(userId, any()) } throws Exception("Error")

        val result = manager.updateVisibleTodoGroups(userId, groups)

        assertFalse(result)
    }

    // ============ Notification Preferences Update Tests ============

    @Test
    fun `updateNotificationPreferences updates preferences successfully`() = runTest {
        val userId = "user-1"
        val prefs = NotificationPreferences(soundEnabled = false)
        coEvery { userPreferencesDao.updateNotificationPreferences(userId, any()) } returns Unit

        val result = manager.updateNotificationPreferences(userId, prefs)

        assertTrue(result)
        coVerify { userPreferencesDao.updateNotificationPreferences(userId, any()) }
    }

    @Test
    fun `updateNotificationPreferences returns false on exception`() = runTest {
        val userId = "user-1"
        val prefs = NotificationPreferences()
        coEvery { userPreferencesDao.updateNotificationPreferences(userId, any()) } throws Exception("Error")

        val result = manager.updateNotificationPreferences(userId, prefs)

        assertFalse(result)
    }

    // ============ Daily Reset Time Update Tests ============

    @Test
    fun `updateDailyResetTime updates time successfully`() = runTest {
        val userId = "user-1"
        val time = "14:30"
        coEvery { userPreferencesDao.updateDailyResetTime(userId, time) } returns Unit

        val result = manager.updateDailyResetTime(userId, time)

        assertTrue(result)
        coVerify { userPreferencesDao.updateDailyResetTime(userId, time) }
    }

    @Test
    fun `updateDailyResetTime returns false on invalid format`() = runTest {
        val userId = "user-1"
        val invalidTime = "25:00" // Invalid hour

        val result = manager.updateDailyResetTime(userId, invalidTime)

        assertFalse(result)
    }

    @Test
    fun `updateDailyResetTime accepts valid times`() = runTest {
        val userId = "user-1"
        val validTimes = listOf("00:00", "12:00", "23:59")
        coEvery { userPreferencesDao.updateDailyResetTime(userId, any()) } returns Unit

        validTimes.forEach { time ->
            val result = manager.updateDailyResetTime(userId, time)
            assertTrue(result, "Should accept valid time: $time")
        }
    }

    // ============ Affirmation Frequency Update Tests ============

    @Test
    fun `updateAffirmationFrequency updates frequency successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateAffirmationFrequency(userId, 3) } returns Unit

        val result = manager.updateAffirmationFrequency(userId, 3)

        assertTrue(result)
        coVerify { userPreferencesDao.updateAffirmationFrequency(userId, 3) }
    }

    @Test
    fun `updateAffirmationFrequency returns false on invalid frequency`() = runTest {
        val userId = "user-1"

        val result1 = manager.updateAffirmationFrequency(userId, 0)
        val result2 = manager.updateAffirmationFrequency(userId, 6)

        assertFalse(result1)
        assertFalse(result2)
    }

    @Test
    fun `updateAffirmationFrequency accepts valid frequencies`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateAffirmationFrequency(userId, any()) } returns Unit

        (1..5).forEach { frequency ->
            val result = manager.updateAffirmationFrequency(userId, frequency)
            assertTrue(result, "Should accept frequency: $frequency")
        }
    }

    // ============ Gamification Update Tests ============

    @Test
    fun `updateGamificationEnabled updates state successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateGamificationEnabled(userId, false) } returns Unit

        val result = manager.updateGamificationEnabled(userId, false)

        assertTrue(result)
        coVerify { userPreferencesDao.updateGamificationEnabled(userId, false) }
    }

    @Test
    fun `updateGamificationEnabled returns false on exception`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateGamificationEnabled(userId, any()) } throws Exception("Error")

        val result = manager.updateGamificationEnabled(userId, true)

        assertFalse(result)
    }

    // ============ Timer Duration Update Tests ============

    @Test
    fun `updateTimerDefaultDuration updates duration successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateTimerDefaultDuration(userId, 30) } returns Unit

        val result = manager.updateTimerDefaultDuration(userId, 30)

        assertTrue(result)
        coVerify { userPreferencesDao.updateTimerDefaultDuration(userId, 30) }
    }

    @Test
    fun `updateTimerDefaultDuration returns false on non-positive duration`() = runTest {
        val userId = "user-1"

        val result1 = manager.updateTimerDefaultDuration(userId, 0)
        val result2 = manager.updateTimerDefaultDuration(userId, -5)

        assertFalse(result1)
        assertFalse(result2)
    }

    @Test
    fun `updateTimerDefaultDuration accepts positive durations`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateTimerDefaultDuration(userId, any()) } returns Unit

        val result = manager.updateTimerDefaultDuration(userId, 45)

        assertTrue(result)
    }

    // ============ Auto-Logout Timeout Update Tests ============

    @Test
    fun `updateAutoLogoutTimeout updates timeout successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateAutoLogoutTimeout(userId, 15) } returns Unit

        val result = manager.updateAutoLogoutTimeout(userId, 15)

        assertTrue(result)
        coVerify { userPreferencesDao.updateAutoLogoutTimeout(userId, 15) }
    }

    @Test
    fun `updateAutoLogoutTimeout accepts zero for disabled`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateAutoLogoutTimeout(userId, 0) } returns Unit

        val result = manager.updateAutoLogoutTimeout(userId, 0)

        assertTrue(result)
    }

    @Test
    fun `updateAutoLogoutTimeout returns false on negative timeout`() = runTest {
        val userId = "user-1"

        val result = manager.updateAutoLogoutTimeout(userId, -1)

        assertFalse(result)
    }

    // ============ Reset to Defaults Tests ============

    @Test
    fun `resetToDefaults resets preferences successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.update(any()) } returns Unit

        val result = manager.resetToDefaults(userId)

        assertTrue(result)
        coVerify { userPreferencesDao.update(any()) }
    }

    @Test
    fun `resetToDefaults returns false on exception`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.update(any()) } throws Exception("Error")

        val result = manager.resetToDefaults(userId)

        assertFalse(result)
    }

    @Test
    fun `resetToDefaults throws on blank userId`() = runTest {
        try {
            manager.resetToDefaults("")
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") == true)
        }
    }

    // ============ Delete Preferences Tests ============

    @Test
    fun `deletePreferences deletes preferences successfully`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.deletePreferencesByUserId(userId) } returns Unit

        val result = manager.deletePreferences(userId)

        assertTrue(result)
        coVerify { userPreferencesDao.deletePreferencesByUserId(userId) }
    }

    @Test
    fun `deletePreferences returns false on exception`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.deletePreferencesByUserId(userId) } throws Exception("Error")

        val result = manager.deletePreferences(userId)

        assertFalse(result)
    }

    // ============ Preferences Exist Tests ============

    @Test
    fun `preferencesExist returns true when preferences exist`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.preferencesExist(userId) } returns 1

        val result = manager.preferencesExist(userId)

        assertTrue(result)
    }

    @Test
    fun `preferencesExist returns false when preferences don't exist`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.preferencesExist(userId) } returns 0

        val result = manager.preferencesExist(userId)

        assertFalse(result)
    }

    @Test
    fun `preferencesExist returns false on exception`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.preferencesExist(userId) } throws Exception("Error")

        val result = manager.preferencesExist(userId)

        assertFalse(result)
    }

    // ============ Deserialization Tests ============

    @Test
    fun `deserializeVisibleTodoGroups deserializes valid JSON`() {
        val json = "[\"Morning\",\"Afternoon\",\"Evening\"]"

        val result = manager.deserializeVisibleTodoGroups(json)

        assertEquals(listOf("Morning", "Afternoon", "Evening"), result)
    }

    @Test
    fun `deserializeVisibleTodoGroups returns empty list on blank JSON`() {
        val result = manager.deserializeVisibleTodoGroups("")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `deserializeVisibleTodoGroups returns empty list on invalid JSON`() {
        val result = manager.deserializeVisibleTodoGroups("invalid json")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `deserializeNotificationPreferences deserializes valid JSON`() {
        val json = "{\"soundEnabled\":false,\"vibrationEnabled\":true,\"visualAlertsEnabled\":true}"

        val result = manager.deserializeNotificationPreferences(json)

        assertEquals(false, result.soundEnabled)
        assertEquals(true, result.vibrationEnabled)
        assertEquals(true, result.visualAlertsEnabled)
    }

    @Test
    fun `deserializeNotificationPreferences returns defaults on blank JSON`() {
        val result = manager.deserializeNotificationPreferences("")

        assertEquals(NotificationPreferences(), result)
    }

    @Test
    fun `deserializeNotificationPreferences returns defaults on invalid JSON`() {
        val result = manager.deserializeNotificationPreferences("invalid json")

        assertEquals(NotificationPreferences(), result)
    }

    // ============ Edge Cases ============

    @Test
    fun `multiple updates to same preference work correctly`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateTheme(userId, any()) } returns Unit

        val result1 = manager.updateTheme(userId, Theme.LIGHT)
        val result2 = manager.updateTheme(userId, Theme.DARK)
        val result3 = manager.updateTheme(userId, Theme.LIGHT)

        assertTrue(result1)
        assertTrue(result2)
        assertTrue(result3)
    }

    @Test
    fun `concurrent preference updates don't interfere`() = runTest {
        val userId = "user-1"
        coEvery { userPreferencesDao.updateTheme(userId, any()) } returns Unit
        coEvery { userPreferencesDao.updateAffirmationFrequency(userId, any()) } returns Unit

        val result1 = manager.updateTheme(userId, Theme.DARK)
        val result2 = manager.updateAffirmationFrequency(userId, 4)

        assertTrue(result1)
        assertTrue(result2)
    }

    @Test
    fun `preferences for different users are independent`() = runTest {
        val user1 = "user-1"
        val user2 = "user-2"
        val prefs1 = UserPreferences(userId = user1, theme = Theme.LIGHT)
        val prefs2 = UserPreferences(userId = user2, theme = Theme.DARK)

        coEvery { userPreferencesDao.getPreferencesByUserId(user1) } returns prefs1
        coEvery { userPreferencesDao.getPreferencesByUserId(user2) } returns prefs2

        val result1 = manager.getPreferences(user1)
        val result2 = manager.getPreferences(user2)

        assertEquals(Theme.LIGHT, result1?.theme)
        assertEquals(Theme.DARK, result2?.theme)
    }
}
