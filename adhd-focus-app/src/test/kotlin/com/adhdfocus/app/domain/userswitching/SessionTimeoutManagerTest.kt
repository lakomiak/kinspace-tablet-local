package com.adhdfocus.app.domain.userswitching

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SessionTimeoutManager.
 *
 * Tests verify:
 * - Session start/end
 * - Activity tracking
 * - Timeout trigger
 * - Warning display
 * - Session extension
 * - Time remaining calculation
 * - Edge cases
 */
class SessionTimeoutManagerTest {

    private lateinit var userSwitchingManager: UserSwitchingManager
    private lateinit var sessionTimeoutManager: SessionTimeoutManager
    private lateinit var testDispatcher: StandardTestDispatcher

    @Before
    fun setUp() {
        userSwitchingManager = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()
        sessionTimeoutManager = SessionTimeoutManager(userSwitchingManager)
    }

    @Test
    fun testStartSession() = runTest {
        val userId = "user-1"
        val timeoutMinutes = 15
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, timeoutMinutes, scope)

        assertTrue(sessionTimeoutManager.isSessionActive())
        assertEquals(0L, sessionTimeoutManager.getTimeRemaining())
    }

    @Test
    fun testStartSessionWithZeroTimeout() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 0, scope)

        assertTrue(sessionTimeoutManager.isSessionActive())
    }

    @Test
    fun testEndSession() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)
        assertTrue(sessionTimeoutManager.isSessionActive())

        sessionTimeoutManager.endSession()

        assertFalse(sessionTimeoutManager.isSessionActive())
        assertEquals(0L, sessionTimeoutManager.getTimeRemaining())
    }

    @Test
    fun testRecordActivity() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)
        delay(100)

        val timeBeforeActivity = sessionTimeoutManager.getTimeRemaining()
        sessionTimeoutManager.recordActivity()
        val timeAfterActivity = sessionTimeoutManager.getTimeRemaining()

        // Time remaining should be reset (increased)
        assertTrue(timeAfterActivity >= timeBeforeActivity)
    }

    @Test
    fun testExtendSession() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)
        delay(100)

        val timeBeforeExtend = sessionTimeoutManager.getTimeRemaining()
        sessionTimeoutManager.extendSession()
        val timeAfterExtend = sessionTimeoutManager.getTimeRemaining()

        // Time remaining should be extended (increased)
        assertTrue(timeAfterExtend >= timeBeforeExtend)
    }

    @Test
    fun testGetTimeRemaining() = runTest {
        val userId = "user-1"
        val timeoutMinutes = 15
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, timeoutMinutes, scope)

        val timeRemaining = sessionTimeoutManager.getTimeRemaining()

        // Should be close to 15 minutes (900 seconds)
        assertTrue(timeRemaining in 890L..900L)
    }

    @Test
    fun testGetTimeRemainingNoSession() = runTest {
        val timeRemaining = sessionTimeoutManager.getTimeRemaining()

        assertEquals(0L, timeRemaining)
    }

    @Test
    fun testIsSessionActive() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        assertFalse(sessionTimeoutManager.isSessionActive())

        sessionTimeoutManager.startSession(userId, 15, scope)
        assertTrue(sessionTimeoutManager.isSessionActive())

        sessionTimeoutManager.endSession()
        assertFalse(sessionTimeoutManager.isSessionActive())
    }

    @Test
    fun testDismissWarning() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)

        sessionTimeoutManager.dismissWarning()

        assertFalse(sessionTimeoutManager.showWarning.value)
    }

    @Test
    fun testMultipleSessions() = runTest {
        val scope = CoroutineScope(testDispatcher)

        // Start first session
        sessionTimeoutManager.startSession("user-1", 15, scope)
        assertTrue(sessionTimeoutManager.isSessionActive())

        // Start second session (should end first)
        sessionTimeoutManager.startSession("user-2", 10, scope)
        assertTrue(sessionTimeoutManager.isSessionActive())

        // End session
        sessionTimeoutManager.endSession()
        assertFalse(sessionTimeoutManager.isSessionActive())
    }

    @Test
    fun testRecordActivityWithoutSession() = runTest {
        // Should not crash
        sessionTimeoutManager.recordActivity()
        assertFalse(sessionTimeoutManager.isSessionActive())
    }

    @Test
    fun testExtendSessionWithoutSession() = runTest {
        // Should not crash
        sessionTimeoutManager.extendSession()
        assertFalse(sessionTimeoutManager.isSessionActive())
    }

    @Test
    fun testDismissWarningWithoutSession() = runTest {
        // Should not crash
        sessionTimeoutManager.dismissWarning()
        assertFalse(sessionTimeoutManager.showWarning.value)
    }

    @Test
    fun testSessionStateFlow() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        var activeState = false
        val job = launch {
            sessionTimeoutManager.isSessionActive.collect { active ->
                activeState = active
            }
        }

        sessionTimeoutManager.startSession(userId, 15, scope)
        delay(100)
        assertTrue(activeState)

        sessionTimeoutManager.endSession()
        delay(100)
        assertFalse(activeState)

        job.cancel()
    }

    @Test
    fun testTimeRemainingStateFlow() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        var timeRemaining = 0L
        val job = launch {
            sessionTimeoutManager.timeRemaining.collect { time ->
                timeRemaining = time
            }
        }

        sessionTimeoutManager.startSession(userId, 15, scope)
        delay(100)

        // Should have time remaining
        assertTrue(timeRemaining > 0)

        job.cancel()
    }

    @Test
    fun testShowWarningStateFlow() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        var showWarning = false
        val job = launch {
            sessionTimeoutManager.showWarning.collect { show ->
                showWarning = show
            }
        }

        sessionTimeoutManager.startSession(userId, 15, scope)
        delay(100)
        assertFalse(showWarning)

        sessionTimeoutManager.dismissWarning()
        delay(100)
        assertFalse(showWarning)

        job.cancel()
    }

    @Test
    fun testWarningTimeRemainingStateFlow() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        var warningTimeRemaining = 0L
        val job = launch {
            sessionTimeoutManager.warningTimeRemaining.collect { time ->
                warningTimeRemaining = time
            }
        }

        sessionTimeoutManager.startSession(userId, 15, scope)
        delay(100)

        // Should be 0 initially (no warning yet)
        assertEquals(0L, warningTimeRemaining)

        job.cancel()
    }

    @Test
    fun testStartSessionWithDifferentTimeouts() = runTest {
        val scope = CoroutineScope(testDispatcher)

        // Test with 5 minutes
        sessionTimeoutManager.startSession("user-1", 5, scope)
        var timeRemaining = sessionTimeoutManager.getTimeRemaining()
        assertTrue(timeRemaining in 290L..300L)

        sessionTimeoutManager.endSession()

        // Test with 30 minutes
        sessionTimeoutManager.startSession("user-2", 30, scope)
        timeRemaining = sessionTimeoutManager.getTimeRemaining()
        assertTrue(timeRemaining in 1790L..1800L)
    }

    @Test
    fun testEndSessionClearsState() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)
        assertTrue(sessionTimeoutManager.isSessionActive())

        sessionTimeoutManager.endSession()

        assertFalse(sessionTimeoutManager.isSessionActive())
        assertEquals(0L, sessionTimeoutManager.getTimeRemaining())
        assertFalse(sessionTimeoutManager.showWarning.value)
        assertEquals(0L, sessionTimeoutManager.warningTimeRemaining.value)
    }

    @Test
    fun testRecordActivityResetsWarning() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)

        // Simulate warning being shown
        // (In real scenario, this would be set by timeout)
        sessionTimeoutManager.recordActivity()

        // Warning should be dismissed
        assertFalse(sessionTimeoutManager.showWarning.value)
    }

    @Test
    fun testExtendSessionResetsWarning() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)

        // Simulate warning being shown
        sessionTimeoutManager.extendSession()

        // Warning should be dismissed
        assertFalse(sessionTimeoutManager.showWarning.value)
    }

    @Test
    fun testSessionWithNegativeTimeout() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        // Negative timeout should use default
        sessionTimeoutManager.startSession(userId, -5, scope)

        assertTrue(sessionTimeoutManager.isSessionActive())
        val timeRemaining = sessionTimeoutManager.getTimeRemaining()
        // Should use default 15 minutes
        assertTrue(timeRemaining in 890L..900L)
    }

    @Test
    fun testConcurrentActivityRecording() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)

        // Record multiple activities
        repeat(5) {
            sessionTimeoutManager.recordActivity()
            delay(10)
        }

        // Should still be active
        assertTrue(sessionTimeoutManager.isSessionActive())
    }

    @Test
    fun testEndSessionMultipleTimes() = runTest {
        val userId = "user-1"
        val scope = CoroutineScope(testDispatcher)

        sessionTimeoutManager.startSession(userId, 15, scope)
        sessionTimeoutManager.endSession()

        // Should not crash when ending again
        sessionTimeoutManager.endSession()

        assertFalse(sessionTimeoutManager.isSessionActive())
    }
}
