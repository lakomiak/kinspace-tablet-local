package com.adhdfocus.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
class TimerViewModel @Inject constructor() : ViewModel() {

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

    /**
     * Starts the timer with the specified duration in minutes.
     *
     * @param durationMinutes Duration in minutes
     */
    fun startTimer(durationMinutes: Int) {
        if (durationMinutes <= 0) return

        _timerDuration.value = durationMinutes * 60 // Convert to seconds
        _timeRemaining.value = durationMinutes * 60
        _isRunning.value = true
        _isPaused.value = false
        _timerCompleted.value = false
        _progress.value = 0f

        startCountdown()
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
