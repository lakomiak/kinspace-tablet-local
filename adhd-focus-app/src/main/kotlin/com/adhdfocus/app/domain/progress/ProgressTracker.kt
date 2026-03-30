package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.repository.StreakRepository
import javax.inject.Inject

/**
 * ProgressTracker calculates and tracks real-time progress metrics.
 *
 * Tracks:
 * - Completion percentage (tasks completed / total tasks)
 * - Task count display (e.g., "5 of 8 complete")
 * - Current streak (consecutive days at 100%)
 * - Best streak (historical maximum)
 * - Day completion status
 */
class ProgressTracker @Inject constructor(
    private val streakRepository: StreakRepository
) {
    /**
     * Calculates the completion percentage for a list of tasks.
     *
     * @param tasks List of tasks to calculate percentage for
     * @return Completion percentage (0-100)
     */
    fun calculateCompletionPercentage(tasks: List<Task>): Int {
        if (tasks.isEmpty()) return 0
        val completedCount = tasks.count { it.status == TaskStatus.COMPLETED }
        return (completedCount * 100) / tasks.size
    }

    /**
     * Gets the task count display string.
     *
     * @param tasks List of tasks
     * @return String like "5 of 8 complete"
     */
    fun getTaskCountDisplay(tasks: List<Task>): String {
        val completedCount = tasks.count { it.status == TaskStatus.COMPLETED }
        val totalCount = tasks.size
        return "$completedCount of $totalCount complete"
    }

    /**
     * Checks if all tasks are completed.
     *
     * @param tasks List of tasks
     * @return True if all tasks are completed
     */
    fun isAllTasksCompleted(tasks: List<Task>): Boolean {
        return tasks.isNotEmpty() && tasks.all { it.status == TaskStatus.COMPLETED }
    }

    /**
     * Gets the current streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return Current streak count
     */
    suspend fun getCurrentStreak(userId: String, householdId: String): Int {
        return streakRepository.getCurrentStreak(userId, householdId)
    }

    /**
     * Gets the best streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return Best streak count
     */
    suspend fun getBestStreak(userId: String, householdId: String): Int {
        return streakRepository.getBestStreak(userId, householdId)
    }

    /**
     * Increments the streak for a user (called when all tasks completed).
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return New streak count
     */
    suspend fun incrementStreak(userId: String, householdId: String): Int {
        return streakRepository.incrementStreak(userId, householdId)
    }

    /**
     * Resets the streak for a user (called when not all tasks completed).
     *
     * @param userId User ID
     * @param householdId Household ID
     */
    suspend fun resetStreak(userId: String, householdId: String) {
        streakRepository.resetStreak(userId, householdId)
    }
}

/**
 * Progress metrics for a user on a given day.
 */
data class ProgressMetrics(
    val completionPercentage: Int,
    val completedCount: Int,
    val totalCount: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val isDayComplete: Boolean
)
