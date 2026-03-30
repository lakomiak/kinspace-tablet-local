package com.adhdfocus.app.ui.settings

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for daily reset time picker functionality.
 *
 * Tests:
 * - Valid time generation (15-minute increments)
 * - Time validation
 * - Time format validation
 * - Minute increment validation
 * - Hour range validation
 */
class DailyResetTimePickerUnitTest {

    @Test
    fun testGenerateValidTimesReturnsAllValidTimes() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        assertEquals(96, validTimes.size) // 24 hours * 4 increments per hour
        assertTrue(validTimes.contains("00:00"))
        assertTrue(validTimes.contains("00:15"))
        assertTrue(validTimes.contains("00:30"))
        assertTrue(validTimes.contains("00:45"))
        assertTrue(validTimes.contains("23:45"))
    }

    @Test
    fun testGenerateValidTimesHas15MinuteIncrements() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        // Check that all times have valid minutes (0, 15, 30, 45)
        for (time in validTimes) {
            val minute = time.substringAfter(":").toInt()
            assertTrue(minute in listOf(0, 15, 30, 45), "Time $time has invalid minute: $minute")
        }
    }

    @Test
    fun testGenerateValidTimesHasValidHours() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        // Check that all times have valid hours (0-23)
        for (time in validTimes) {
            val hour = time.substringBefore(":").toInt()
            assertTrue(hour in 0..23, "Time $time has invalid hour: $hour")
        }
    }

    @Test
    fun testGenerateValidTimesStartsAtMidnight() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        assertEquals("00:00", validTimes.first())
    }

    @Test
    fun testGenerateValidTimesEndsAt2345() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        assertEquals("23:45", validTimes.last())
    }

    @Test
    fun testGenerateValidTimesNoInvalidTimes() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        // Verify no invalid times are included
        val invalidTimes = listOf("00:01", "00:10", "00:25", "06:59", "12:05", "18:20", "24:00")
        for (invalidTime in invalidTimes) {
            assertFalse(validTimes.contains(invalidTime), "Invalid time $invalidTime should not be in valid times")
        }
    }

    @Test
    fun testGenerateValidTimesIsOrdered() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        // Verify times are in chronological order
        for (i in 0 until validTimes.size - 1) {
            val current = validTimes[i]
            val next = validTimes[i + 1]
            assertTrue(current < next, "Times should be in order: $current should be before $next")
        }
    }

    @Test
    fun testValidTimeFormatMidnight() {
        // Act & Assert
        val validTimes = generateValidTimes()
        assertTrue(validTimes.contains("00:00"))
    }

    @Test
    fun testValidTimeFormatNoon() {
        // Act & Assert
        val validTimes = generateValidTimes()
        assertTrue(validTimes.contains("12:00"))
    }

    @Test
    fun testValidTimeFormatEvening() {
        // Act & Assert
        val validTimes = generateValidTimes()
        assertTrue(validTimes.contains("18:00"))
        assertTrue(validTimes.contains("18:15"))
        assertTrue(validTimes.contains("18:30"))
        assertTrue(validTimes.contains("18:45"))
    }

    @Test
    fun testValidTimeFormatLateNight() {
        // Act & Assert
        val validTimes = generateValidTimes()
        assertTrue(validTimes.contains("23:00"))
        assertTrue(validTimes.contains("23:15"))
        assertTrue(validTimes.contains("23:30"))
        assertTrue(validTimes.contains("23:45"))
    }

    @Test
    fun testValidTimeFormatNoInvalidMinutes() {
        // Act & Assert
        val validTimes = generateValidTimes()
        
        // Verify no times with invalid minutes exist
        for (time in validTimes) {
            val minute = time.substringAfter(":").toInt()
            assertTrue(minute % 15 == 0, "Time $time has minute not divisible by 15")
        }
    }

    @Test
    fun testValidTimeFormatNoInvalidHours() {
        // Act & Assert
        val validTimes = generateValidTimes()
        
        // Verify no times with invalid hours exist
        for (time in validTimes) {
            val hour = time.substringBefore(":").toInt()
            assertTrue(hour >= 0 && hour <= 23, "Time $time has invalid hour: $hour")
        }
    }

    @Test
    fun testValidTimeFormatConsistentFormatting() {
        // Act & Assert
        val validTimes = generateValidTimes()
        
        // Verify all times follow HH:mm format
        for (time in validTimes) {
            assertTrue(time.matches(Regex("^\\d{2}:\\d{2}$")), "Time $time doesn't match HH:mm format")
        }
    }

    @Test
    fun testValidTimeFormatAllQuarterHours() {
        // Act
        val validTimes = generateValidTimes()

        // Assert
        // For each hour, verify all 4 quarter-hour times exist
        for (hour in 0..23) {
            val hourStr = String.format("%02d", hour)
            assertTrue(validTimes.contains("$hourStr:00"), "Missing $hourStr:00")
            assertTrue(validTimes.contains("$hourStr:15"), "Missing $hourStr:15")
            assertTrue(validTimes.contains("$hourStr:30"), "Missing $hourStr:30")
            assertTrue(validTimes.contains("$hourStr:45"), "Missing $hourStr:45")
        }
    }
}
