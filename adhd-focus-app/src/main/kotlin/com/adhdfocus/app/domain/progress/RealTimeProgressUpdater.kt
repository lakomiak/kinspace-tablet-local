package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * RealTimeProgressUpdater provides real-time progress updates as tasks change.
 *
 * Provides:
 * - Real-time completion percentage updates
 * - Real-time task count updates
 * - Real-time progress metrics
 * - Efficient change detection
 */
class RealTimeProgressUpdater @Inject constructor(
    private val progressTracker: ProgressTracker
) {

    private val _progressMetrics = MutableStateFlow<ProgressMetrics?>(null)
    val progressMetrics: StateFlow<ProgressMetrics?> = _progressMetrics

    private val _completionPercentage = MutableStateFlow(0)
    val completionPercentage: StateFlow<Int> = _completionPercentage

    private val _taskCountDisplay = MutableStateFlow("")
    val taskCountDisplay: StateFlow<String> = _taskCountDisplay

    private val _isDayComplete = MutableStateFlow(false)
    val isDayComplete: StateFlow<Boolean> = _isDayComplete

    /**
     * Updates progress metrics based on current tasks.
     *
     * @param tasks Current list of tasks
     * @param currentStreak Current streak count
     * @param bestStreak Best streak count
     */
    fun updateProgress(
        tasks: List<Task>,
        currentStreak: Int = 0,
        bestStreak: Int = 0
    ) {
        val completedCount = tasks.count { it.status == TaskStatus.COMPLETED }
        val totalCount = tasks.size

        val completionPercentage = progressTracker.calculateCompletionPercentage(tasks)
        val taskCountDisplay = progressTracker.getTaskCountDisplay(tasks)
        val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

        _completionPercentage.value = completionPercentage
        _taskCountDisplay.value = taskCountDisplay
        _isDayComplete.value = isDayComplete

        _progressMetrics.value = ProgressMetrics(
            completionPercentage = completionPercentage,
            completedCount = completedCount,
            totalCount = totalCount,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            isDayComplete = isDayComplete
        )
    }

    /**
     * Gets a flow of completion percentage changes.
     */
    fun getCompletionPercentageFlow(): Flow<Int> {
        return completionPercentage
    }

    /**
     * Gets a flow of task count display changes.
     */
    fun getTaskCountDisplayFlow(): Flow<String> {
        return taskCountDisplay
    }

    /**
     * Gets a flow of day complete status changes.
     */
    fun getDayCompleteFlow(): Flow<Boolean> {
        return isDayComplete
    }

    /**
     * Gets a flow of progress metrics changes.
     */
    fun getProgressMetricsFlow(): Flow<ProgressMetrics?> {
        return progressMetrics
    }

    /**
     * Resets all progress metrics.
     */
    fun resetProgress() {
        _completionPercentage.value = 0
        _taskCountDisplay.value = ""
        _isDayComplete.value = false
        _progressMetrics.value = null
    }

    /**
     * Gets the current progress metrics.
     */
    fun getCurrentMetrics(): ProgressMetrics? = _progressMetrics.value

    /**
     * Checks if progress has changed significantly.
     *
     * @param oldPercentage Previous completion percentage
     * @param newPercentage Current completion percentage
     * @param threshold Threshold for significant change (default 10%)
     * @return True if change is significant
     */
    fun isSignificantChange(
        oldPercentage: Int,
        newPercentage: Int,
        threshold: Int = 10
    ): Boolean {
        return kotlin.math.abs(newPercentage - oldPercentage) >= threshold
    }

    /**
     * Gets the progress change direction.
     *
     * @param oldPercentage Previous completion percentage
     * @param newPercentage Current completion percentage
     * @return ProgressDirection (UP, DOWN, or UNCHANGED)
     */
    fun getProgressDirection(
        oldPercentage: Int,
        newPercentage: Int
    ): ProgressDirection {
        return when {
            newPercentage > oldPercentage -> ProgressDirection.UP
            newPercentage < oldPercentage -> ProgressDirection.DOWN
            else -> ProgressDirection.UNCHANGED
        }
    }
}

/**
 * Progress direction enum.
 */
enum class ProgressDirection {
    UP,
    DOWN,
    UNCHANGED
}
