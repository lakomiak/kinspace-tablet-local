package com.adhdfocus.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.os.Build
import android.util.Log
import com.adhdfocus.app.data.model.TimerAlarmSound
import com.adhdfocus.app.data.model.EfficiencyMetric
import com.adhdfocus.app.data.dao.EfficiencyMetricDao
import com.adhdfocus.app.data.dao.TaskSessionMetricDao
import com.adhdfocus.app.data.model.TaskSessionMetric
import com.adhdfocus.app.data.model.TaskSessionOutcome
import com.adhdfocus.app.data.repository.TokenRepository
import com.adhdfocus.app.domain.audio.AudioNotificationManager
import com.adhdfocus.app.domain.completion.TaskDayCompletionRepository
import com.adhdfocus.app.domain.gamification.EfficiencyCalculator
import com.adhdfocus.app.domain.preferences.UserPreferencesManager
import com.adhdfocus.app.domain.setup.TabletSetupManager
import com.adhdfocus.app.domain.task.TaskManager
import com.adhdfocus.app.domain.timer.TaskCompletionSessionMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.math.ceil

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
    private val taskDayCompletionRepository: TaskDayCompletionRepository,
    private val tokenRepository: TokenRepository,
    private val efficiencyMetricDao: EfficiencyMetricDao,
    private val taskSessionMetricDao: TaskSessionMetricDao,
    private val efficiencyCalculator: EfficiencyCalculator
) : ViewModel() {

    private val tag = "TimerViewModel"

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
    private var sessionStartedAt: Instant? = null
    private var pauseStartedAt: Instant? = null
    private var accumulatedPausedSeconds: Long = 0L
    private var activeElapsedSeconds: Long = 0L
    private var pauseCount: Int = 0
    private var resetCount: Int = 0

    /**
     * Starts the timer with the specified duration in seconds.
     *
     * @param durationSeconds Duration in seconds
     */
    fun startTimer(durationSeconds: Int) {
        if (durationSeconds <= 0) return

        configuredDurationSeconds = durationSeconds
        sessionStartedAt = Instant.now()
        pauseStartedAt = null
        accumulatedPausedSeconds = 0L
        activeElapsedSeconds = 0L
        pauseCount = 0
        resetCount = 0
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
                    activeElapsedSeconds += 1L
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
            pauseCount += 1
            pauseStartedAt = Instant.now()
        }
    }

    /**
     * Resumes the timer.
     */
    fun resumeTimer() {
        if (_isRunning.value && _isPaused.value) {
            pauseStartedAt?.let { started ->
                accumulatedPausedSeconds += Duration.between(started, Instant.now()).seconds.coerceAtLeast(0)
            }
            pauseStartedAt = null
            _isPaused.value = false
        }
    }

    /**
     * Cancels the timer.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        audioNotificationManager.stopSound()
        persistSessionOutcome(completedTask = false)
        clearSessionTracking()
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
        if (configuredDurationSeconds > 0) {
            if (_isPaused.value && pauseStartedAt != null) {
                accumulatedPausedSeconds += Duration.between(pauseStartedAt, Instant.now()).seconds.coerceAtLeast(0)
                pauseStartedAt = null
            }
            resetCount += 1
            timerJob?.cancel()
            _isRunning.value = true
            _isPaused.value = false
            _timerDuration.value = configuredDurationSeconds
            _timeRemaining.value = configuredDurationSeconds
            _progress.value = 0f
            _timerCompleted.value = false
            startCountdown()
        } else {
            cancelTimer()
        }
    }

    fun completeCurrentTask(
        taskIdOverride: String? = null,
        onCompleted: (() -> Unit)? = null
    ) {
        val taskId = taskIdOverride
            ?.takeIf { it.isNotBlank() }
            ?: currentTaskId?.takeIf { it.isNotBlank() }
            ?: return
        viewModelScope.launch {
            try {
                val metrics = buildCompletionMetrics(taskId)
                stopTimerForCompletion()
                val completedTask = taskManager.completeTask(taskId, metrics)
                val focusDate = markTaskCompletedForCurrentFocusDate(taskId)
                if (focusDate != null) {
                    tokenRepository.awardTaskTokensForToday(completedTask, focusDate)
                }
                onCompleted?.invoke()

                metrics?.let {
                    runCatching { recordCompletionMetric(it) }
                        .onFailure { error ->
                            Log.w(tag, "Unable to record completion metric for taskId=$taskId", error)
                        }
                }

                runCatching { persistSessionOutcome(completedTask = true) }
                    .onFailure { error ->
                        Log.w(tag, "Unable to persist completion outcome for taskId=$taskId", error)
                    }
            } catch (error: Exception) {
                Log.e(tag, "Unable to complete taskId=$taskId", error)
            } finally {
                clearSessionTracking()
            }
        }
    }

    private suspend fun markTaskCompletedForCurrentFocusDate(taskId: String): LocalDate? {
        val householdId = setupManager.getHouseholdId().orEmpty()
        val userId = setupManager.getAssignedMemberId().orEmpty()
        val focusDate = setupManager.getCurrentFocusDate() ?: LocalDate.now()

        if (householdId.isBlank() || userId.isBlank()) {
            Log.w(
                tag,
                "Skipping date completion for taskId=$taskId because householdId or userId is blank"
            )
            return null
        }

        taskDayCompletionRepository.setCompletionForDate(
            householdId = householdId,
            userId = userId,
            taskId = taskId,
            date = focusDate,
            isCompleted = true
        )
        Log.d(tag, "Marked taskId=$taskId complete for focusDate=$focusDate")
        return focusDate
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

    private fun stopTimerForCompletion() {
        timerJob?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _timerCompleted.value = true
        audioNotificationManager.stopSound()
    }

    private fun clearSessionTracking() {
        sessionStartedAt = null
        pauseStartedAt = null
        accumulatedPausedSeconds = 0L
        activeElapsedSeconds = 0L
        pauseCount = 0
        resetCount = 0
    }

    private fun buildCompletionMetrics(taskId: String): TaskCompletionSessionMetrics? {
        val startedAt = sessionStartedAt ?: return null
        val stoppedAt = Instant.now()
        val totalPausedSeconds = accumulatedPausedSeconds + pauseStartedAt?.let {
            Duration.between(it, stoppedAt).seconds.coerceAtLeast(0)
        }.orZero()
        val actualDurationSeconds = activeElapsedSeconds.coerceAtLeast(1L).toInt()
        val actualDurationMinutes = ceil(actualDurationSeconds / 60.0).toInt().coerceAtLeast(1)
        val householdId = setupManager.getHouseholdId().orEmpty()
        val userId = setupManager.getAssignedMemberId().orEmpty()
        val estimatedDurationMinutes = if (configuredDurationSeconds > 0) {
            ceil(configuredDurationSeconds / 60.0).toInt().coerceAtLeast(1)
        } else {
            null
        }
        return TaskCompletionSessionMetrics(
            taskId = taskId,
            householdId = householdId,
            userId = userId,
            estimatedDurationMinutes = estimatedDurationMinutes,
            configuredDurationSeconds = configuredDurationSeconds.takeIf { it > 0 },
            actualDurationSeconds = actualDurationSeconds,
            actualDurationMinutes = actualDurationMinutes,
            totalPausedSeconds = totalPausedSeconds.toInt(),
            pauseCount = pauseCount,
            resetCount = resetCount,
            timerStartedAt = startedAt,
            timerStoppedAt = stoppedAt
        )
    }

    private suspend fun recordCompletionMetric(metrics: TaskCompletionSessionMetrics) {
        if (metrics.householdId.isBlank() || metrics.userId.isBlank()) return

        val efficiencyPercentage = efficiencyCalculator.calculateEfficiency(
            metrics.estimatedDurationMinutes,
            metrics.actualDurationMinutes
        )

        efficiencyMetricDao.insert(
            EfficiencyMetric(
                taskId = metrics.taskId,
                userId = metrics.userId,
                householdId = metrics.householdId,
                estimatedDurationMinutes = metrics.estimatedDurationMinutes,
                actualDurationMinutes = metrics.actualDurationMinutes,
                efficiencyPercentage = efficiencyPercentage,
                configuredDurationSeconds = metrics.configuredDurationSeconds,
                actualDurationSeconds = metrics.actualDurationSeconds,
                totalPausedSeconds = metrics.totalPausedSeconds,
                pauseCount = metrics.pauseCount,
                resetCount = metrics.resetCount,
                timerStartedAt = metrics.timerStartedAt,
                timerStoppedAt = metrics.timerStoppedAt,
                completedAt = metrics.completedAt
            )
        )
    }

    private fun persistSessionOutcome(completedTask: Boolean) {
        val taskId = currentTaskId ?: return
        val startedAt = sessionStartedAt ?: return
        val endedAt = Instant.now()
        val householdId = setupManager.getHouseholdId().orEmpty()
        val userId = setupManager.getAssignedMemberId().orEmpty()
        if (householdId.isBlank() || userId.isBlank()) return

        val totalPausedSeconds = accumulatedPausedSeconds + pauseStartedAt?.let {
            Duration.between(it, endedAt).seconds.coerceAtLeast(0)
        }.orZero()
        val activeDurationSeconds = activeElapsedSeconds.coerceAtLeast(0L).toInt()
        val completedAfterTimerEnded = completedTask && _timeRemaining.value == 0
        val stoppedBeforeTimerEnded = !completedTask && _timeRemaining.value > 0
        val outcome = when {
            completedTask && completedAfterTimerEnded -> TaskSessionOutcome.COMPLETED_AFTER_TIME_END
            completedTask -> TaskSessionOutcome.COMPLETED_BEFORE_TIME_END
            _timeRemaining.value > 0 -> TaskSessionOutcome.CANCELED_BEFORE_TIME_END
            else -> TaskSessionOutcome.CANCELED_AFTER_TIME_END
        }

        viewModelScope.launch {
            taskSessionMetricDao.insert(
                TaskSessionMetric(
                    taskId = taskId,
                    userId = userId,
                    householdId = householdId,
                    configuredDurationSeconds = configuredDurationSeconds.takeIf { it > 0 },
                    activeDurationSeconds = activeDurationSeconds,
                    totalPausedSeconds = totalPausedSeconds.toInt(),
                    pauseCount = pauseCount,
                    resetCount = resetCount,
                    timerStartedAt = startedAt,
                    endedAt = endedAt,
                    outcome = outcome,
                    completedTask = completedTask,
                    completedAfterTimerEnded = completedAfterTimerEnded,
                    stoppedBeforeTimerEnded = stoppedBeforeTimerEnded
                )
            )
        }
    }
}

private fun Long?.orZero(): Long = this ?: 0L

/**
 * Enum for timer progress colors.
 */
enum class TimerProgressColor {
    GREEN,    // 0-50%
    ORANGE,   // 50-90%
    RED       // 90-100%
}
