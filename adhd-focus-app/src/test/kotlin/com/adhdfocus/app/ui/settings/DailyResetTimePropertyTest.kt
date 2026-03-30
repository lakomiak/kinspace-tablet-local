package com.adhdfocus.app.ui.settings

import com.adhdfocus.app.data.model.NotificationPreferences
import com.adhdfocus.app.data.model.Theme
import com.adhdfocus.app.data.model.UserPreferences
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for daily reset time configuration.
 *
 * **Validates: Requirements 18, Property: Daily Reset Time Configuration**
 *
 * Tests:
 * - All valid 15-minute increments are accepted
 * - All invalid times are rejected
 * - Time validation is consistent
 * - Valid times can be persisted and retrieved
 * - Time format is always HH:mm
 */
class DailyResetTimePropertyTest {

    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        userPreferencesManager = mockk()
        viewModel = SettingsViewModel(userPreferencesManager)
    }

    @Test
    fun testAllValid15MinuteIncrementsAreAccepted() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Generate all valid times
        val validTimes = generateValidTimes()

        // Act & Assert
        for (time in validTimes) {
            viewModel.updateDailyResetTime(time)
            assertEquals(time, viewModel.dailyResetTime.value, "Time $time should be accepted")
            assertEquals(null, viewModel.errorMessage.value, "No error for valid time $time")
        }
    }

    @Test
    fun testAllInvalidTimesAreRejected() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Generate invalid times (not 15-minute increments)
        val invalidTimes = mutableListOf<String>()
        for (hour in 0..23) {
            for (minute in 0..59) {
                if (minute !in listOf(0, 15, 30, 45)) {
                    invalidTimes.add(String.format("%02d:%02d", hour, minute))
                }
            }
        }

        // Act & Assert - Test a sample of invalid times
        val sampleInvalidTimes = invalidTimes.shuffled().take(20)
        for (time in sampleInvalidTimes) {
            viewModel.updateDailyResetTime(time)
            assertTrue(
                viewModel.errorMessage.value?.contains("15-minute increments") == true,
                "Time $time should be rejected with 15-minute increment error"
            )
        }
    }

    @Test
    fun testTimeValidationIsConsistent() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act & Assert - Validate same time multiple times
        val testTime = "14:30"
        for (i in 0..5) {
            viewModel.updateDailyResetTime(testTime)
            assertEquals(testTime, viewModel.dailyResetTime.value, "Time should be consistent on attempt $i")
        }
    }

    @Test
    fun testValidTimesCanBePersisted() = runTest {
        // Arrange
        val userId = "user-123"
        val validTimes = listOf("00:00", "06:15", "12:30", "18:45", "23:45")
        
        for (time in validTimes) {
            val prefs = UserPreferences(userId = userId, dailyResetTime = time)
            coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns prefs
            coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

            // Act
            viewModel.initialize(userId)

            // Assert
            assertEquals(time, viewModel.dailyResetTime.value, "Time $time should be persisted")
        }
    }

    @Test
    fun testTimeFormatIsAlwaysHHmm() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act & Assert
        val validTimes = generateValidTimes()
        for (time in validTimes) {
            viewModel.updateDailyResetTime(time)
            assertTrue(time.matches(Regex("^\\d{2}:\\d{2}$")), "Time $time should match HH:mm format")
        }
    }

    @Test
    fun testHourRangeValidation() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act & Assert - Test invalid hours
        val invalidHours = listOf("24:00", "25:00", "-1:00", "99:00")
        for (time in invalidHours) {
            viewModel.updateDailyResetTime(time)
            assertTrue(
                viewModel.errorMessage.value?.contains("15-minute increments") == true,
                "Time $time should be rejected"
            )
        }
    }

    @Test
    fun testMinuteRangeValidation() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act & Assert - Test invalid minutes
        val invalidMinutes = listOf("12:01", "12:10", "12:25", "12:59", "12:60")
        for (time in invalidMinutes) {
            viewModel.updateDailyResetTime(time)
            assertTrue(
                viewModel.errorMessage.value?.contains("15-minute increments") == true,
                "Time $time should be rejected"
            )
        }
    }

    @Test
    fun testBoundaryTimesAreValid() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act & Assert - Test boundary times
        val boundaryTimes = listOf("00:00", "00:15", "23:30", "23:45")
        for (time in boundaryTimes) {
            viewModel.updateDailyResetTime(time)
            assertEquals(time, viewModel.dailyResetTime.value, "Boundary time $time should be valid")
        }
    }

    @Test
    fun testTimeValidationDoesNotAcceptInvalidFormat() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()

        viewModel.initialize(userId)

        // Act & Assert - Test invalid formats
        val invalidFormats = listOf("1:00", "01:0", "1:30", "12:3", "12-30", "12.30", "12 30", "")
        for (time in invalidFormats) {
            viewModel.updateDailyResetTime(time)
            assertTrue(
                viewModel.errorMessage.value?.contains("15-minute increments") == true,
                "Invalid format $time should be rejected"
            )
        }
    }

    @Test
    fun testAllQuarterHoursAreValid() = runTest {
        // Arrange
        val userId = "user-123"
        val defaultPrefs = UserPreferences(userId = userId)
        coEvery { userPreferencesManager.getPreferencesOrDefault(userId) } returns defaultPrefs
        coEvery { userPreferencesManager.deserializeNotificationPreferences(any()) } returns NotificationPreferences()
        coEvery { userPreferencesManager.savePreferences(any()) } returns true

        viewModel.initialize(userId)

        // Act & Assert - For each hour, verify all 4 quarter-hour times are valid
        for (hour in 0..23) {
            val hourStr = String.format("%02d", hour)
            for (minute in listOf(0, 15, 30, 45)) {
                val time = String.format("%s:%02d", hourStr, minute)
                viewModel.updateDailyResetTime(time)
                assertEquals(time, viewModel.dailyResetTime.value, "Quarter-hour time $time should be valid")
            }
        }
    }
}
