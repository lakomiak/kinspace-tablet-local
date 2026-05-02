package com.adhdfocus.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdfocus.app.data.model.TimerAlarmSound
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.sync.CloudSyncManager
import com.adhdfocus.app.domain.task.TaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * TimerViewModel manages the state for the Timer interface.
 *
 * Manages:
 * - Timer duration (in seconds)
 * - Time remaining (in seconds)
 * - Timer running state
 * - Progress (0.0 to 1.0)
 * - Timer completion
 * - Pause/resume functionality
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val audioNotificationManager: AudioNotificationManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val setupManager: TabletSetupManager,
    private val taskManager: TaskManager,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    private val _timerDuration = MutableStateFlow(0)
    val timerDuration: StateFlow<Int> = _timerDuration

    private val _timeRemaining = MutableStateFlow(0)
    val timeRemaining: StateFlow<Int> = _timeRemaining

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _timerCompleted = MutableStateFlow(false)
    val timerCompleted: StateFlow<Boolean> = _timerCompleted

    private var timerJob: Job? = null
    private var configuredDurationSeconds: Int = 0
    private var currentTaskId: String? = null

    /**
     * Starts the timer with the specified duration in seconds.
     *
     * @param durationSeconds Duration in seconds
     */
    fun startTimer(durationSeconds: Int) {
        if (durationSeconds <= 0) return

        configuredDurationSeconds = durationSeconds
        _timerDuration.value = durationSeconds
        _timeRemaining.value = durationSeconds
        _isRunning.value = true
        _isPaused.value = false
        _timerCompleted.value = false
        _progress.value = 0f

        startCountdown()
    }

    fun setTaskId(taskId: String) {
        currentTaskId = taskId.ifBlank { null }
    }

    /**
     * Starts the countdown timer.
     */
    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0 && _isRunning.value) {
                if (!_isPaused.value) {
                    delay(1000) // Wait 1 second
                    _timeRemaining.value = (_timeRemaining.value - 1).coerceAtLeast(0)
                    updateProgress()

                    if (_timeRemaining.value == 0) {
                        completeTimer()
                    }
                } else {
                    delay(100) // Check pause state more frequently
                }
            }
        }
    }

    /**
     * Updates the progress value (0.0 to 1.0).
     */
    private fun updateProgress() {
        if (_timerDuration.value > 0) {
            val elapsed = _timerDuration.value - _timeRemaining.value
            _progress.value = elapsed.toFloat() / _timerDuration.value
        }
    }

    /**
     * Pauses the timer.
     */
    fun pauseTimer() {
        if (_isRunning.value && !_isPaused.value) {
            _isPaused.value = true
        }
    }

    /**
     * Resumes the timer.
     */
    fun resumeTimer() {
        if (_isRunning.value && _isPaused.value) {
            _isPaused.value = false
        }
    }

    /**
     * Cancels the timer.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        audioNotificationManager.stopSound()
        _isRunning.value = false
        _isPaused.value = false
        _timerDuration.value = 0
        _timeRemaining.value = 0
        _progress.value = 0f
        _timerCompleted.value = false
    }

    /**
     * Completes the timer.
     */
    private fun completeTimer() {
        _isRunning.value = false
        _isPaused.value = false
        _timerCompleted.value = true
        timerJob?.cancel()
        viewModelScope.launch {
            playCompletionAlarm()
        }
    }

    /**
     * Extends the timer by the specified number of minutes.
     *
     * @param additionalMinutes Minutes to add
     */
    fun extendTimer(additionalMinutes: Int) {
        if (additionalMinutes <= 0) return

        val additionalSeconds = additionalMinutes * 60
        _timerDuration.value += additionalSeconds
        _timeRemaining.value += additionalSeconds
        updateProgress()
    }

    /**
     * Gets the formatted time display (MM:SS).
     *
     * @return Formatted time string
     */
    fun getFormattedTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    /**
     * Gets the progress percentage (0-100).
     *
     * @return Progress percentage
     */
    fun getProgressPercentage(): Int {
        return (progress.value * 100).toInt()
    }

    /**
     * Gets the visual feedback color based on progress.
     *
     * Returns:
     * - 0-50%: Green
     * - 50-90%: Orange/Yellow
     * - 90-100%: Red
     */
    fun getProgressColor(): TimerProgressColor {
        return when {
            progress.value < 0.5f -> TimerProgressColor.GREEN
            progress.value < 0.9f -> TimerProgressColor.ORANGE
            else -> TimerProgressColor.RED
        }
    }

    /**
     * Checks if timer is at a warning threshold (50% or 90%).
     */
    fun isAtWarningThreshold(): Boolean {
        val percentage = getProgressPercentage()
        return percentage == 50 || percentage == 90
    }

    /**
     * Resets the timer state.
     */
    fun resetTimer() {
        cancelTimer()
        if (configuredDurationSeconds > 0) {
            startTimer(configuredDurationSeconds)
        }
    }

    fun completeCurrentTask(onCompleted: (() -> Unit)? = null) {
        val taskId = currentTaskId ?: return
        viewModelScope.launch {
            try {
                cancelTimer()
                taskManager.completeTask(taskId)
                val householdId = setupManager.getHouseholdId().orEmpty()
                val userId = setupManager.getAssignedMemberId().orEmpty()
                if (householdId.isNotBlank() && userId.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        cloudSyncManager.syncPendingChanges(householdId, userId)
                    }
                }
                onCompleted?.invoke()
            } catch (_: Exception) {
                // Keep the screen open if completion fails.
            }
        }
    }

    private suspend fun playCompletionAlarm() {
        val userId = setupManager.getAssignedMemberId().orEmpty()
        val preferences = if (userId.isNotBlank()) {
            userPreferencesManager.getPreferencesOrDefault(userId)
        } else {
            null
        }
        val notificationPreferences = preferences
            ?.let { userPreferencesManager.deserializeNotificationPreferences(it.notificationPreferences) }
            ?: com.adhdfocus.app.data.model.NotificationPreferences()

        if (!notificationPreferences.soundEnabled) {
            return
        }

        audioNotificationManager.playTimerCompletionSound(notificationPreferences.timerAlarmSound)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

/**
 * Enum for timer progress colors.
 */
enum class TimerProgressColor {
    GREEN,    // 0-50%
    ORANGE,   // 50-90%
    RED       // 90-100%
}
