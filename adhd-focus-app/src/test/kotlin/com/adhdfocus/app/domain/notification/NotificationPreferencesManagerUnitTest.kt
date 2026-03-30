package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.NotificationPreferences
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationPreferencesManagerUnitTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var manager: NotificationPreferencesManager

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        manager = NotificationPreferencesManagerImpl(userPreferencesManager)
    }

    @Test
    fun testGetPreferencesReturnsNotificationPreferences() = runTest {
        val userId = "user-1"
        val notifPrefs = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )
        val userPrefs = UserPreferences(
            userId = userId,
            notificationPreferences = """{"soundEnabled":true,"vibrationEnabled":false,"visualAlertsEnabled":true}"""
        )

        coEvery { userPreferencesManager.getPreferences(userId) } returns userPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns notifPrefs

        val result = manager.getPreferences(userId)

        assertEquals(notifPrefs, result)
        coVerify { userPreferencesManager.getPreferences(userId) }
    }

    @Test
    fun testGetPreferencesReturnsNullWhenNotFound() = runTest {
        val userId = "user-1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null

        val result = manager.getPreferences(userId)

        assertEquals(null, result)
    }

    @Test
    fun testGetPreferencesOrDefaultReturnsExisting() = runTest {
        val userId = "user-1"
        val notifPrefs = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = true,
            visualAlertsEnabled = false
        )
        val userPrefs = UserPreferences(
            userId = userId,
            notificationPreferences = """{"soundEnabled":false,"vibrationEnabled":true,"visualAlertsEnabled":false}"""
        )

        coEvery { userPreferencesManager.getPreferences(userId) } returns userPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns notifPrefs

        val result = manager.getPreferencesOrDefault(userId)

        assertEquals(notifPrefs, result)
    }

    @Test
    fun testGetPreferencesOrDefaultReturnsDefaultWhenNotFound() = runTest {
        val userId = "user-1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null

        val result = manager.getPreferencesOrDefault(userId)

        assertNotNull(result)
        assertTrue(result.soundEnabled)
        assertTrue(result.vibrationEnabled)
        assertTrue(result.visualAlertsEnabled)
    }

    @Test
    fun testSetPreferencesUpdatesPreferences() = runTest {
        val userId = "user-1"
        val prefs = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )

        coEvery { userPreferencesManager.updateNotificationPreferences(userId, prefs) } returns true

        val result = manager.setPreferences(userId, prefs)

        assertTrue(result)
        coVerify { userPreferencesManager.updateNotificationPreferences(userId, prefs) }
    }

    @Test
    fun testSetPreferencesReturnsFalseOnFailure() = runTest {
        val userId = "user-1"
        val prefs = NotificationPreferences()

        coEvery { userPreferencesManager.updateNotificationPreferences(userId, prefs) } returns false

        val result = manager.setPreferences(userId, prefs)

        assertFalse(result)
    }

    @Test
    fun testSetSoundEnabledUpdatesPreference() = runTest {
        val userId = "user-1"
        val currentPrefs = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = true,
            visualAlertsEnabled = true
        )

        coEvery { userPreferencesManager.getPreferences(userId) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true

        val result = manager.setSoundEnabled(userId, false)

        assertTrue(result)
        coVerify { userPreferencesManager.updateNotificationPreferences(userId, any()) }
    }

    @Test
    fun testSetVibrationEnabledUpdatesPreference() = runTest {
        val userId = "user-1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true

        val result = manager.setVibrationEnabled(userId, false)

        assertTrue(result)
        coVerify { userPreferencesManager.updateNotificationPreferences(userId, any()) }
    }

    @Test
    fun testSetVisualAlertsEnabledUpdatesPreference() = runTest {
        val userId = "user-1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true

        val result = manager.setVisualAlertsEnabled(userId, false)

        assertTrue(result)
        coVerify { userPreferencesManager.updateNotificationPreferences(userId, any()) }
    }

    @Test
    fun testResetToDefaultsResetsAllPreferences() = runTest {
        val userId = "user-1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true

        val result = manager.resetToDefaults(userId)

        assertTrue(result)
        coVerify { userPreferencesManager.updateNotificationPreferences(userId, any()) }
    }

    @Test
    fun testObservePreferencesReturnsStateFlow() = runTest {
        val userId = "user-1"

        val flow = manager.observePreferences(userId)

        assertNotNull(flow)
        assertEquals(NotificationPreferences(), flow.value)
    }

    @Test
    fun testObservePreferencesReturnsSameFlowForSameUser() = runTest {
        val userId = "user-1"

        val flow1 = manager.observePreferences(userId)
        val flow2 = manager.observePreferences(userId)

        assertEquals(flow1, flow2)
    }

    @Test
    fun testGetPreferencesWithBlankUserIdThrowsException() = runTest {
        try {
            manager.getPreferences("")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") ?: false)
        }
    }

    @Test
    fun testSetPreferencesWithBlankUserIdThrowsException() = runTest {
        try {
            manager.setPreferences("", NotificationPreferences())
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("userId cannot be blank") ?: false)
        }
    }

    @Test
    fun testPerMemberPreferenceIsolation() = runTest {
        val userId1 = "user-1"
        val userId2 = "user-2"

        coEvery { userPreferencesManager.getPreferences(any()) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(any(), any()) } returns true

        val prefs1 = NotificationPreferences(soundEnabled = true, vibrationEnabled = false)
        val prefs2 = NotificationPreferences(soundEnabled = false, vibrationEnabled = true)

        manager.setPreferences(userId1, prefs1)
        manager.setPreferences(userId2, prefs2)

        val flow1 = manager.observePreferences(userId1)
        val flow2 = manager.observePreferences(userId2)

        // Flows should be different instances
        assertTrue(flow1 !== flow2)
    }

    @Test
    fun testMultipleSetOperationsUpdateState() = runTest {
        val userId = "user-1"

        coEvery { userPreferencesManager.getPreferences(userId) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true

        manager.setSoundEnabled(userId, false)
        manager.setVibrationEnabled(userId, false)
        manager.setVisualAlertsEnabled(userId, false)

        coVerify(exactly = 3) { userPreferencesManager.updateNotificationPreferences(userId, any()) }
    }
}
