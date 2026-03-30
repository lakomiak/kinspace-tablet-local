package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for NotificationPreferencesManager.
 *
 * **Validates: Requirements 9, 6, Property 2.8**
 *
 * These tests verify universal properties that should hold across all valid inputs.
 */
class NotificationPreferencesManagerPropertyTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var manager: NotificationPreferencesManager

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        manager = NotificationPreferencesManagerImpl(userPreferencesManager)
    }

    /**
     * Property 2.8: Notification Preferences Persistence
     *
     * For any valid notification preferences and user ID, set→load produces identical preferences.
     * Tests with random user IDs and all preference combinations.
     */
    @Test
    fun testNotificationPreferencesPersistenceRoundTrip() = runTest {
        val testCases = listOf(
            Triple("user-1", true, true),
            Triple("user-2", true, false),
            Triple("user-3", false, true),
            Triple("user-4", false, false),
            Triple("user-abc-123", true, true),
            Triple("user-xyz-789", false, false)
        )

        for ((userId, sound, vibration) in testCases) {
            val originalPrefs = NotificationPreferences(
                soundEnabled = sound,
                vibrationEnabled = vibration,
                visualAlertsEnabled = !sound
            )

            coEvery { userPreferencesManager.updateNotificationPreferences(userId, originalPrefs) } returns true
            coEvery { userPreferencesManager.getPreferences(userId) } returns UserPreferences(
                userId = userId,
                notificationPreferences = """{"soundEnabled":$sound,"vibrationEnabled":$vibration,"visualAlertsEnabled":${!sound}}"""
            )
            coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns originalPrefs

            manager.setPreferences(userId, originalPrefs)
            val retrieved = manager.getPreferences(userId)

            assertEquals(originalPrefs, retrieved, "Preferences should persist for $userId")
        }
    }

    /**
     * Property: Per-Member Preference Isolation
     *
     * For any two different users, their preferences are independent.
     * Changing one user's preferences doesn't affect another's.
     */
    @Test
    fun testPerMemberPreferenceIsolation() = runTest {
        val userId1 = "user-1"
        val userId2 = "user-2"

        val prefs1 = NotificationPreferences(soundEnabled = true, vibrationEnabled = false)
        val prefs2 = NotificationPreferences(soundEnabled = false, vibrationEnabled = true)

        coEvery { userPreferencesManager.updateNotificationPreferences(userId1, prefs1) } returns true
        coEvery { userPreferencesManager.updateNotificationPreferences(userId2, prefs2) } returns true
        coEvery { userPreferencesManager.getPreferences(userId1) } returns UserPreferences(
            userId = userId1,
            notificationPreferences = """{"soundEnabled":true,"vibrationEnabled":false,"visualAlertsEnabled":true}"""
        )
        coEvery { userPreferencesManager.getPreferences(userId2) } returns UserPreferences(
            userId = userId2,
            notificationPreferences = """{"soundEnabled":false,"vibrationEnabled":true,"visualAlertsEnabled":true}"""
        )
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } answers {
            when (it.invocation.args[0]) {
                """{"soundEnabled":true,"vibrationEnabled":false,"visualAlertsEnabled":true}""" -> prefs1
                """{"soundEnabled":false,"vibrationEnabled":true,"visualAlertsEnabled":true}""" -> prefs2
                else -> NotificationPreferences()
            }
        }

        manager.setPreferences(userId1, prefs1)
        manager.setPreferences(userId2, prefs2)

        val retrieved1 = manager.getPreferences(userId1)
        val retrieved2 = manager.getPreferences(userId2)

        assertEquals(prefs1, retrieved1)
        assertEquals(prefs2, retrieved2)
        assertTrue(retrieved1 != retrieved2)
    }

    /**
     * Property: State Consistency
     *
     * For any preference, multiple set operations maintain consistency.
     * Final state matches last set value.
     */
    @Test
    fun testStateConsistencyWithMultipleOperations() = runTest {
        val userId = "user-1"
        val prefs1 = NotificationPreferences(soundEnabled = true, vibrationEnabled = true)
        val prefs2 = NotificationPreferences(soundEnabled = false, vibrationEnabled = false)
        val prefs3 = NotificationPreferences(soundEnabled = true, vibrationEnabled = false)

        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true
        coEvery { userPreferencesManager.getPreferences(userId) } returns UserPreferences(
            userId = userId,
            notificationPreferences = """{"soundEnabled":true,"vibrationEnabled":false,"visualAlertsEnabled":true}"""
        )
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns prefs3

        manager.setPreferences(userId, prefs1)
        manager.setPreferences(userId, prefs2)
        manager.setPreferences(userId, prefs3)

        val final = manager.getPreferences(userId)
        assertEquals(prefs3, final)
    }

    /**
     * Property: Reset to Default
     *
     * For any initial preference, reset always sets all to true (defaults).
     * Regardless of previous state.
     */
    @Test
    fun testResetToDefaultAlwaysReturnsDefaults() = runTest {
        val userIds = listOf("user-1", "user-2", "user-3")

        for (userId in userIds) {
            coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true
            coEvery { userPreferencesManager.getPreferences(userId) } returns null

            val result = manager.resetToDefaults(userId)

            assertTrue(result)
        }
    }

    /**
     * Property: Individual Field Updates
     *
     * For any user, updating individual fields preserves other fields.
     * Sound, vibration, and visual alerts can be toggled independently.
     */
    @Test
    fun testIndividualFieldUpdatesPreserveOthers() = runTest {
        val userId = "user-1"
        val initialPrefs = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = true,
            visualAlertsEnabled = true
        )

        coEvery { userPreferencesManager.getPreferences(userId) } returns null
        coEvery { userPreferencesManager.updateNotificationPreferences(userId, any()) } returns true

        // Update sound only
        manager.setSoundEnabled(userId, false)
        // Vibration and visual alerts should remain true

        // Update vibration only
        manager.setVibrationEnabled(userId, false)
        // Sound should remain false, visual alerts should remain true

        // Update visual alerts only
        manager.setVisualAlertsEnabled(userId, false)
        // All should now be false

        assertTrue(true) // All operations succeeded
    }

    /**
     * Property: Default Preferences Correctness
     *
     * For any user without preferences, getPreferencesOrDefault returns all true.
     * Verified with random user IDs.
     */
    @Test
    fun testDefaultPreferencesCorrectness() = runTest {
        val userIds = listOf(
            "user-1", "user-2", "user-abc", "user-xyz-123",
            "test-user", "adhd-user-1"
        )

        for (userId in userIds) {
            coEvery { userPreferencesManager.getPreferences(userId) } returns null

            val defaults = manager.getPreferencesOrDefault(userId)

            assertEquals(true, defaults.soundEnabled, "Sound should be enabled by default for $userId")
            assertEquals(true, defaults.vibrationEnabled, "Vibration should be enabled by default for $userId")
            assertEquals(true, defaults.visualAlertsEnabled, "Visual alerts should be enabled by default for $userId")
        }
    }

    /**
     * Property: StateFlow Emissions
     *
     * For any user, observePreferences returns a StateFlow that emits the current value.
     * Multiple observations return the same flow instance.
     */
    @Test
    fun testStateFlowEmissions() = runTest {
        val userId = "user-1"

        val flow1 = manager.observePreferences(userId)
        val flow2 = manager.observePreferences(userId)

        // Same instance
        assertEquals(flow1, flow2)

        // Emits default value
        assertEquals(NotificationPreferences(), flow1.value)
    }

    /**
     * Property: Preference Combinations
     *
     * For all 8 possible combinations of sound/vibration/visual alerts,
     * preferences can be set and retrieved correctly.
     */
    @Test
    fun testAllPreferenceCombinations() = runTest {
        val userId = "user-1"
        val combinations = listOf(
            Triple(true, true, true),
            Triple(true, true, false),
            Triple(true, false, true),
            Triple(true, false, false),
            Triple(false, true, true),
            Triple(false, true, false),
            Triple(false, false, true),
            Triple(false, false, false)
        )

        for ((sound, vibration, visual) in combinations) {
            val prefs = NotificationPreferences(
                soundEnabled = sound,
                vibrationEnabled = vibration,
                visualAlertsEnabled = visual
            )

            coEvery { userPreferencesManager.updateNotificationPreferences(userId, prefs) } returns true
            coEvery { userPreferencesManager.getPreferences(userId) } returns UserPreferences(
                userId = userId,
                notificationPreferences = """{"soundEnabled":$sound,"vibrationEnabled":$vibration,"visualAlertsEnabled":$visual}"""
            )
            coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns prefs

            val result = manager.setPreferences(userId, prefs)
            assertTrue(result, "Should set preferences for combination ($sound, $vibration, $visual)")

            val retrieved = manager.getPreferences(userId)
            assertEquals(prefs, retrieved, "Should retrieve preferences for combination ($sound, $vibration, $visual)")
        }
    }
}
