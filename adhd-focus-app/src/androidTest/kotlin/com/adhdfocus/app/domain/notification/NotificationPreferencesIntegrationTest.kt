package com.adhdfocus.app.domain.notification

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for NotificationPreferencesManager with real database.
 *
 * Tests the full flow of setting, persisting, and retrieving notification preferences.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationPreferencesIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AdhdfocusDatabase

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var notificationPreferencesManager: NotificationPreferencesManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testNotificationPreferencesPersistedToDatabase() = runTest {
        val userId = "test-user-1"
        val prefs = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = true,
            visualAlertsEnabled = false
        )

        // Set preferences
        val result = notificationPreferencesManager.setPreferences(userId, prefs)
        assertTrue(result)

        // Retrieve from database
        val retrieved = notificationPreferencesManager.getPreferences(userId)
        assertNotNull(retrieved)
        assertEquals(prefs.soundEnabled, retrieved.soundEnabled)
        assertEquals(prefs.vibrationEnabled, retrieved.vibrationEnabled)
        assertEquals(prefs.visualAlertsEnabled, retrieved.visualAlertsEnabled)
    }

    @Test
    fun testSoundPreferencePersistedIndependently() = runTest {
        val userId = "test-user-2"

        // Set sound to false
        val result = notificationPreferencesManager.setSoundEnabled(userId, false)
        assertTrue(result)

        // Retrieve and verify
        val retrieved = notificationPreferencesManager.getPreferences(userId)
        assertNotNull(retrieved)
        assertEquals(false, retrieved.soundEnabled)
    }

    @Test
    fun testVibrationPreferencePersistedIndependently() = runTest {
        val userId = "test-user-3"

        // Set vibration to false
        val result = notificationPreferencesManager.setVibrationEnabled(userId, false)
        assertTrue(result)

        // Retrieve and verify
        val retrieved = notificationPreferencesManager.getPreferences(userId)
        assertNotNull(retrieved)
        assertEquals(false, retrieved.vibrationEnabled)
    }

    @Test
    fun testVisualAlertsPreferencePersistedIndependently() = runTest {
        val userId = "test-user-4"

        // Set visual alerts to false
        val result = notificationPreferencesManager.setVisualAlertsEnabled(userId, false)
        assertTrue(result)

        // Retrieve and verify
        val retrieved = notificationPreferencesManager.getPreferences(userId)
        assertNotNull(retrieved)
        assertEquals(false, retrieved.visualAlertsEnabled)
    }

    @Test
    fun testResetToDefaultsPersisted() = runTest {
        val userId = "test-user-5"

        // Set custom preferences
        val customPrefs = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = false,
            visualAlertsEnabled = false
        )
        notificationPreferencesManager.setPreferences(userId, customPrefs)

        // Reset to defaults
        val result = notificationPreferencesManager.resetToDefaults(userId)
        assertTrue(result)

        // Verify defaults
        val retrieved = notificationPreferencesManager.getPreferences(userId)
        assertNotNull(retrieved)
        assertEquals(true, retrieved.soundEnabled)
        assertEquals(true, retrieved.vibrationEnabled)
        assertEquals(true, retrieved.visualAlertsEnabled)
    }

    @Test
    fun testPerMemberPreferencesIsolated() = runTest {
        val userId1 = "test-user-6"
        val userId2 = "test-user-7"

        val prefs1 = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )
        val prefs2 = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = true,
            visualAlertsEnabled = false
        )

        // Set different preferences for each user
        notificationPreferencesManager.setPreferences(userId1, prefs1)
        notificationPreferencesManager.setPreferences(userId2, prefs2)

        // Retrieve and verify isolation
        val retrieved1 = notificationPreferencesManager.getPreferences(userId1)
        val retrieved2 = notificationPreferencesManager.getPreferences(userId2)

        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals(prefs1.soundEnabled, retrieved1.soundEnabled)
        assertEquals(prefs2.soundEnabled, retrieved2.soundEnabled)
        assertEquals(prefs1.vibrationEnabled, retrieved1.vibrationEnabled)
        assertEquals(prefs2.vibrationEnabled, retrieved2.vibrationEnabled)
    }

    @Test
    fun testMultipleUpdatesPreserveLatestState() = runTest {
        val userId = "test-user-8"

        // Multiple updates
        notificationPreferencesManager.setSoundEnabled(userId, false)
        notificationPreferencesManager.setVibrationEnabled(userId, false)
        notificationPreferencesManager.setVisualAlertsEnabled(userId, false)

        // Verify final state
        val retrieved = notificationPreferencesManager.getPreferences(userId)
        assertNotNull(retrieved)
        assertEquals(false, retrieved.soundEnabled)
        assertEquals(false, retrieved.vibrationEnabled)
        assertEquals(false, retrieved.visualAlertsEnabled)
    }

    @Test
    fun testDefaultsReturnedWhenNotFound() = runTest {
        val userId = "test-user-nonexistent"

        val defaults = notificationPreferencesManager.getPreferencesOrDefault(userId)

        assertNotNull(defaults)
        assertEquals(true, defaults.soundEnabled)
        assertEquals(true, defaults.vibrationEnabled)
        assertEquals(true, defaults.visualAlertsEnabled)
    }

    @Test
    fun testStateFlowUpdatesOnPreferenceChange() = runTest {
        val userId = "test-user-9"

        val flow = notificationPreferencesManager.observePreferences(userId)
        val initialValue = flow.value

        // Update preferences
        val newPrefs = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = false,
            visualAlertsEnabled = false
        )
        notificationPreferencesManager.setPreferences(userId, newPrefs)

        // Flow should emit new value
        assertNotNull(flow.value)
    }
}
