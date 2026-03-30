package com.adhdfocus.app.domain.accessibility

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for haptic feedback patterns.
 */
class HapticFeedbackManagerTest {

    @Test
    fun testLightTapPattern() {
        val pattern = HapticPatterns.LIGHT_TAP
        assertEquals(2, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(10, pattern[1])
    }

    @Test
    fun testLightDoubleTapPattern() {
        val pattern = HapticPatterns.LIGHT_DOUBLE_TAP
        assertEquals(4, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(10, pattern[1])
        assertEquals(50, pattern[2])
        assertEquals(10, pattern[3])
    }

    @Test
    fun testMediumPressPattern() {
        val pattern = HapticPatterns.MEDIUM_PRESS
        assertEquals(2, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(20, pattern[1])
    }

    @Test
    fun testMediumDoublePressPattern() {
        val pattern = HapticPatterns.MEDIUM_DOUBLE_PRESS
        assertEquals(4, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(20, pattern[1])
        assertEquals(50, pattern[2])
        assertEquals(20, pattern[3])
    }

    @Test
    fun testStrongPressPattern() {
        val pattern = HapticPatterns.STRONG_PRESS
        assertEquals(2, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(40, pattern[1])
    }

    @Test
    fun testStrongDoublePressPattern() {
        val pattern = HapticPatterns.STRONG_DOUBLE_PRESS
        assertEquals(4, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(40, pattern[1])
        assertEquals(100, pattern[2])
        assertEquals(40, pattern[3])
    }

    @Test
    fun testSuccessShortPattern() {
        val pattern = HapticPatterns.SUCCESS_SHORT
        assertEquals(6, pattern.size)
        // Pattern: 0ms off, 20ms on, 50ms off, 20ms on, 50ms off, 100ms on
        assertEquals(0, pattern[0])
        assertEquals(20, pattern[1])
        assertEquals(50, pattern[2])
        assertEquals(20, pattern[3])
        assertEquals(50, pattern[4])
        assertEquals(100, pattern[5])
    }

    @Test
    fun testSuccessLongPattern() {
        val pattern = HapticPatterns.SUCCESS_LONG
        assertEquals(8, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(30, pattern[1])
    }

    @Test
    fun testWarningSinglePattern() {
        val pattern = HapticPatterns.WARNING_SINGLE
        assertEquals(4, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(30, pattern[1])
        assertEquals(100, pattern[2])
        assertEquals(30, pattern[3])
    }

    @Test
    fun testWarningDoublePattern() {
        val pattern = HapticPatterns.WARNING_DOUBLE
        assertEquals(6, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(30, pattern[1])
    }

    @Test
    fun testErrorSinglePattern() {
        val pattern = HapticPatterns.ERROR_SINGLE
        assertEquals(6, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(50, pattern[1])
        assertEquals(100, pattern[2])
        assertEquals(50, pattern[3])
        assertEquals(100, pattern[4])
        assertEquals(50, pattern[5])
    }

    @Test
    fun testErrorDoublePattern() {
        val pattern = HapticPatterns.ERROR_DOUBLE
        assertEquals(10, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(50, pattern[1])
    }

    @Test
    fun testNotificationAlertPattern() {
        val pattern = HapticPatterns.NOTIFICATION_ALERT
        assertEquals(6, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(20, pattern[1])
    }

    @Test
    fun testNotificationReminderPattern() {
        val pattern = HapticPatterns.NOTIFICATION_REMINDER
        assertEquals(4, pattern.size)
        assertEquals(0, pattern[0])
        assertEquals(30, pattern[1])
        assertEquals(100, pattern[2])
        assertEquals(30, pattern[3])
    }

    @Test
    fun testAllPatternsStartWithZero() {
        // All patterns should start with 0 (no initial vibration)
        assertTrue(HapticPatterns.LIGHT_TAP[0] == 0L)
        assertTrue(HapticPatterns.LIGHT_DOUBLE_TAP[0] == 0L)
        assertTrue(HapticPatterns.MEDIUM_PRESS[0] == 0L)
        assertTrue(HapticPatterns.MEDIUM_DOUBLE_PRESS[0] == 0L)
        assertTrue(HapticPatterns.STRONG_PRESS[0] == 0L)
        assertTrue(HapticPatterns.STRONG_DOUBLE_PRESS[0] == 0L)
        assertTrue(HapticPatterns.SUCCESS_SHORT[0] == 0L)
        assertTrue(HapticPatterns.SUCCESS_LONG[0] == 0L)
        assertTrue(HapticPatterns.WARNING_SINGLE[0] == 0L)
        assertTrue(HapticPatterns.WARNING_DOUBLE[0] == 0L)
        assertTrue(HapticPatterns.ERROR_SINGLE[0] == 0L)
        assertTrue(HapticPatterns.ERROR_DOUBLE[0] == 0L)
        assertTrue(HapticPatterns.NOTIFICATION_ALERT[0] == 0L)
        assertTrue(HapticPatterns.NOTIFICATION_REMINDER[0] == 0L)
    }

    @Test
    fun testAllPatternsHaveEvenLength() {
        // All patterns should have even length (off, on, off, on, ...)
        assertTrue(HapticPatterns.LIGHT_TAP.size % 2 == 0)
        assertTrue(HapticPatterns.LIGHT_DOUBLE_TAP.size % 2 == 0)
        assertTrue(HapticPatterns.MEDIUM_PRESS.size % 2 == 0)
        assertTrue(HapticPatterns.MEDIUM_DOUBLE_PRESS.size % 2 == 0)
        assertTrue(HapticPatterns.STRONG_PRESS.size % 2 == 0)
        assertTrue(HapticPatterns.STRONG_DOUBLE_PRESS.size % 2 == 0)
        assertTrue(HapticPatterns.SUCCESS_SHORT.size % 2 == 0)
        assertTrue(HapticPatterns.SUCCESS_LONG.size % 2 == 0)
        assertTrue(HapticPatterns.WARNING_SINGLE.size % 2 == 0)
        assertTrue(HapticPatterns.WARNING_DOUBLE.size % 2 == 0)
        assertTrue(HapticPatterns.ERROR_SINGLE.size % 2 == 0)
        assertTrue(HapticPatterns.ERROR_DOUBLE.size % 2 == 0)
        assertTrue(HapticPatterns.NOTIFICATION_ALERT.size % 2 == 0)
        assertTrue(HapticPatterns.NOTIFICATION_REMINDER.size % 2 == 0)
    }

    @Test
    fun testPatternDurationsArePositive() {
        // All durations (except first 0) should be positive
        HapticPatterns.LIGHT_TAP.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.LIGHT_DOUBLE_TAP.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.MEDIUM_PRESS.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.MEDIUM_DOUBLE_PRESS.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.STRONG_PRESS.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.STRONG_DOUBLE_PRESS.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.SUCCESS_SHORT.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.SUCCESS_LONG.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.WARNING_SINGLE.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.WARNING_DOUBLE.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.ERROR_SINGLE.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.ERROR_DOUBLE.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.NOTIFICATION_ALERT.drop(1).forEach { assertTrue(it > 0) }
        HapticPatterns.NOTIFICATION_REMINDER.drop(1).forEach { assertTrue(it > 0) }
    }
}
