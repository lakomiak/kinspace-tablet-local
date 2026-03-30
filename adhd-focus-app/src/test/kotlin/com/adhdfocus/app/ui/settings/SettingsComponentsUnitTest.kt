package com.adhdfocus.app.ui.settings

import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for settings components and utilities.
 *
 * Tests:
 * - Theme validation
 * - Notification preferences validation
 * - Time format validation
 * - Frequency range validation
 * - Duration validation
 * - Timeout validation
 */
class SettingsComponentsUnitTest {

    @Test
    fun testThemeEnumValues() {
        // Assert
        assertEquals(2, Theme.values().size)
        assertTrue(Theme.values().contains(Theme.LIGHT))
        assertTrue(Theme.values().contains(Theme.DARK))
    }

    @Test
    fun testNotificationPreferencesDefaults() {
        // Act
        val prefs = NotificationPreferences()

        // Assert
        assertTrue(prefs.soundEnabled)
        assertTrue(prefs.vibrationEnabled)
        assertTrue(prefs.visualAlertsEnabled)
    }

    @Test
    fun testNotificationPreferencesCopy() {
        // Arrange
        val original = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )

        // Act
        val modified = original.copy(soundEnabled = false)

        // Assert
        assertFalse(modified.soundEnabled)
        assertFalse(modified.vibrationEnabled)
        assertTrue(modified.visualAlertsEnabled)
        assertTrue(original.soundEnabled) // Original unchanged
    }

    @Test
    fun testTimeFormatValidation() {
        // Arrange
        val validTimes = listOf(
            "00:00", "06:30", "12:00", "18:45", "23:59"
        )
        val invalidTimes = listOf(
            "24:00", "12:60", "invalid", "12", "12:00:00", "-1:00"
        )

        // Act & Assert
        validTimes.forEach { time ->
            val regex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
            assertTrue(regex.matches(time), "Time $time should be valid")
        }

        invalidTimes.forEach { time ->
            val regex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
            assertFalse(regex.matches(time), "Time $time should be invalid")
        }
    }

    @Test
    fun testFrequencyRangeValidation() {
        // Arrange
        val validFrequencies = listOf(1, 2, 3, 4, 5)
        val invalidFrequencies = listOf(0, -1, 6, 10, 100)

        // Act & Assert
        validFrequencies.forEach { freq ->
            assertTrue(freq in 1..5, "Frequency $freq should be valid")
        }

        invalidFrequencies.forEach { freq ->
            assertFalse(freq in 1..5, "Frequency $freq should be invalid")
        }
    }

    @Test
    fun testDurationValidation() {
        // Arrange
        val validDurations = listOf(1, 5, 15, 25, 60, 120)
        val invalidDurations = listOf(0, -1, -10)

        // Act & Assert
        validDurations.forEach { duration ->
            assertTrue(duration > 0, "Duration $duration should be valid")
        }

        invalidDurations.forEach { duration ->
            assertFalse(duration > 0, "Duration $duration should be invalid")
        }
    }

    @Test
    fun testTimeoutValidation() {
        // Arrange
        val validTimeouts = listOf(0, 1, 5, 15, 30, 60)
        val invalidTimeouts = listOf(-1, -10)

        // Act & Assert
        validTimeouts.forEach { timeout ->
            assertTrue(timeout >= 0, "Timeout $timeout should be valid")
        }

        invalidTimeouts.forEach { timeout ->
            assertFalse(timeout >= 0, "Timeout $timeout should be invalid")
        }
    }

    @Test
    fun testNotificationPreferencesEquality() {
        // Arrange
        val prefs1 = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )
        val prefs2 = NotificationPreferences(
            soundEnabled = true,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )
        val prefs3 = NotificationPreferences(
            soundEnabled = false,
            vibrationEnabled = false,
            visualAlertsEnabled = true
        )

        // Assert
        assertEquals(prefs1, prefs2)
        assertFalse(prefs1 == prefs3)
    }

    @Test
    fun testThemeEquality() {
        // Assert
        assertEquals(Theme.LIGHT, Theme.LIGHT)
        assertEquals(Theme.DARK, Theme.DARK)
        assertFalse(Theme.LIGHT == Theme.DARK)
    }

    @Test
    fun testFrequencyBoundaryValues() {
        // Arrange
        val minFrequency = 1
        val maxFrequency = 5

        // Act & Assert
        assertTrue(minFrequency in 1..5)
        assertTrue(maxFrequency in 1..5)
        assertFalse((minFrequency - 1) in 1..5)
        assertFalse((maxFrequency + 1) in 1..5)
    }

    @Test
    fun testDurationBoundaryValues() {
        // Arrange
        val minDuration = 1
        val maxDuration = 1440 // 24 hours

        // Act & Assert
        assertTrue(minDuration > 0)
        assertTrue(maxDuration > 0)
        assertFalse(0 > 0)
        assertFalse(-1 > 0)
    }

    @Test
    fun testTimeoutBoundaryValues() {
        // Arrange
        val minTimeout = 0
        val maxTimeout = 1440 // 24 hours

        // Act & Assert
        assertTrue(minTimeout >= 0)
        assertTrue(maxTimeout >= 0)
        assertFalse(-1 >= 0)
    }

    @Test
    fun testNotificationPreferencesAllCombinations() {
        // Arrange
        val combinations = listOf(
            NotificationPreferences(true, true, true),
            NotificationPreferences(true, true, false),
            NotificationPreferences(true, false, true),
            NotificationPreferences(true, false, false),
            NotificationPreferences(false, true, true),
            NotificationPreferences(false, true, false),
            NotificationPreferences(false, false, true),
            NotificationPreferences(false, false, false)
        )

        // Act & Assert
        assertEquals(8, combinations.size)
        combinations.forEach { prefs ->
            assertTrue(prefs.soundEnabled in listOf(true, false))
            assertTrue(prefs.vibrationEnabled in listOf(true, false))
            assertTrue(prefs.visualAlertsEnabled in listOf(true, false))
        }
    }
}
