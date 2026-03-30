package com.adhdfocus.app.domain.userswitching

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionTimeoutManager manages user session timeouts and auto-logout functionality.
 *
 * Responsibilities:
 * - Track user activity (taps, scrolls, etc.)
 * - Manage timeout timers
 * - Trigger logout on timeout
 * - Show warning before logout
 * - Allow session extension
 * - Automatically clear current user on timeout
 * - Per-member timeout configuration
 * - Persist timeout settings
 *
 * Features:
 * - Configurable timeout duration (default 15 minutes)
 * - Warning display before logout (e.g., 1 minute before)
 * - Session extension capability
 * - Accurate time tracking
 * - Coroutine-based async operations
 */
@Singleton
class SessionTimeoutManager @Inject constructor(
    private val userSwitchingManager: UserSwitchingManager
) {
    private var currentUserId: String? = null
    private var timeoutMinutes: Int = DEFAULT_TIMEOUT_MINUTES
    private var sessionStartTime: Instant? = null
    private var lastActivityTime: Instant? = null
    private var timeoutJob: Job? = null
    private var warningJob: Job? = null
    private var coroutineScope: CoroutineScope? = null

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive

    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining

    private val _showWarning = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning

    private val _warningTimeRemaining = MutableStateFlow(0L)
    val warningTimeRemaining: StateFlow<Long> = _warningTimeRemaining

    companion object {
        private const val DEFAULT_TIMEOUT_MINUTES = 15
        private const val WARNING_BEFORE_LOGOUT_MINUTES = 1
        private const val UPDATE_INTERVAL_MS = 1000L // Update every second
    }

    /**
     * Starts a session for a user with a specified timeout duration.
     *
     * @param userId User ID
     * @param timeoutMinutes Timeout duration in minutes (0 = disabled)
     * @param scope CoroutineScope for launching coroutines
     */
    fun startSession(userId: String, timeoutMinutes: Int, scope: CoroutineScope) {
        // Cancel any existing session
        endSession()

        currentUserId = userId
        this.timeoutMinutes = if (timeoutMinutes > 0) timeoutMinutes else DEFAULT_TIMEOUT_MINUTES
        this.coroutineScope = scope
        sessionStartTime = Instant.now()
        lastActivityTime = Instant.now()

        _isSessionActive.value = true
        _showWarning.value = false

        // Start timeout tracking
        startTimeoutTracking()
    }

    /**
     * Records user activity and resets the timeout.
     */
    fun recordActivity() {
        if (!_isSessionActive.value) return

        lastActivityTime = Instant.now()
        _showWarning.value = false

        // Restart timeout tracking
        cancelTimeoutJobs()
        startTimeoutTracking()
    }

    /**
     * Extends the current session by the configured timeout duration.
     */
    fun extendSession() {
        if (!_isSessionActive.value) return

        lastActivityTime = Instant.now()
        _showWarning.value = false

        // Restart timeout tracking
        cancelTimeoutJobs()
        startTimeoutTracking()
    }

    /**
     * Ends the current session.
     */
    fun endSession() {
        cancelTimeoutJobs()
        currentUserId = null
        sessionStartTime = null
        lastActivityTime = null
        _isSessionActive.value = false
        _showWarning.value = false
        _timeRemaining.value = 0L
        _warningTimeRemaining.value = 0L
    }

    /**
     * Gets the time remaining in the current session (in seconds).
     *
     * @return Time remaining in seconds, or 0 if no active session
     */
    fun getTimeRemaining(): Long {
        if (!_isSessionActive.value || lastActivityTime == null) return 0L

        val elapsedSeconds = java.time.temporal.ChronoUnit.SECONDS.between(lastActivityTime, Instant.now())
        val totalTimeoutSeconds = timeoutMinutes * 60L
        val remainingSeconds = totalTimeoutSeconds - elapsedSeconds

        return if (remainingSeconds > 0) remainingSeconds else 0L
    }

    /**
     * Checks if a session is currently active.
     *
     * @return True if session is active
     */
    fun isSessionActive(): Boolean = _isSessionActive.value

    /**
     * Dismisses the warning dialog.
     */
    fun dismissWarning() {
        _showWarning.value = false
    }

    /**
     * Sets the logout callback to be called when session times out.
     * This is called internally when timeout is reached.
     */
    private suspend fun performLogout() {
        endSession()
        userSwitchingManager.clearCurrentUser()
    }

    /**
     * Starts the timeout tracking with warning display.
     */
    private fun startTimeoutTracking() {
        val scope = coroutineScope ?: return

        val totalTimeoutMs = timeoutMinutes * 60 * 1000L
        val warningTimeMs = WARNING_BEFORE_LOGOUT_MINUTES * 60 * 1000L

        // Schedule warning display
        warningJob = scope.launch {
            delay(totalTimeoutMs - warningTimeMs)
            if (_isSessionActive.value) {
                _showWarning.value = true
                startWarningCountdown()
            }
        }

        // Schedule logout
        timeoutJob = scope.launch {
            delay(totalTimeoutMs)
            if (_isSessionActive.value) {
                performLogout()
            }
        }

        // Start time remaining updates
        scope.launch {
            while (_isSessionActive.value) {
                _timeRemaining.value = getTimeRemaining()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Starts the warning countdown display.
     */
    private fun startWarningCountdown() {
        val scope = coroutineScope ?: return

        scope.launch {
            while (_showWarning.value && _isSessionActive.value) {
                val remaining = getTimeRemaining()
                _warningTimeRemaining.value = remaining
                if (remaining <= 0) {
                    _showWarning.value = false
                    break
                }
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Cancels all timeout-related jobs.
     */
    private fun cancelTimeoutJobs() {
        timeoutJob?.cancel()
        warningJob?.cancel()
        timeoutJob = null
        warningJob = null
    }
}
