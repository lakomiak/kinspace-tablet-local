package com.adhdfocus.app.domain.progress

import java.time.LocalDate

/**
 * StreakCalculator handles streak calculation logic.
 *
 * Calculates:
 * - Current streak (consecutive days at 100% completion)
 * - Streak increments and resets
 * - Streak milestones
 */
class StreakCalculator {
    /**
     * Determines if a streak should be incremented based on completion status.
     *
     * @param isAllTasksCompleted Whether all tasks were completed today
     * @param lastCompletionDate Last date all tasks were completed
     * @return True if streak should be incremented
     */
    fun shouldIncrementStreak(
        isAllTasksCompleted: Boolean,
        lastCompletionDate: LocalDate?
    ): Boolean {
        if (!isAllTasksCompleted) return false
        if (lastCompletionDate == null) return true

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Only increment if last completion was yesterday or today
        return lastCompletionDate == yesterday || lastCompletionDate == today
    }

    /**
     * Determines if a streak should be reset.
     *
     * @param isAllTasksCompleted Whether all tasks were completed today
     * @param lastCompletionDate Last date all tasks were completed
     * @return True if streak should be reset
     */
    fun shouldResetStreak(
        isAllTasksCompleted: Boolean,
        lastCompletionDate: LocalDate?
    ): Boolean {
        if (isAllTasksCompleted) return false
        if (lastCompletionDate == null) return false

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Reset if last completion was before yesterday
        return lastCompletionDate < yesterday
    }

    /**
     * Checks if a streak count is a milestone.
     *
     * @param streakCount Current streak count
     * @return True if this is a milestone streak
     */
    fun isMilestone(streakCount: Int): Boolean {
        return streakCount in listOf(3, 7, 14, 30, 60, 100)
    }

    /**
     * Gets the next milestone after the current streak.
     *
     * @param streakCount Current streak count
     * @return Next milestone streak count
     */
    fun getNextMilestone(streakCount: Int): Int {
        return when {
            streakCount < 3 -> 3
            streakCount < 7 -> 7
            streakCount < 14 -> 14
            streakCount < 30 -> 30
            streakCount < 60 -> 60
            streakCount < 100 -> 100
            else -> streakCount + 10
        }
    }

    /**
     * Gets the progress toward the next milestone.
     *
     * @param streakCount Current streak count
     * @return Progress as a percentage (0-100)
     */
    fun getProgressToNextMilestone(streakCount: Int): Int {
        val nextMilestone = getNextMilestone(streakCount)
        val previousMilestone = when {
            nextMilestone <= 3 -> 0
            nextMilestone <= 7 -> 3
            nextMilestone <= 14 -> 7
            nextMilestone <= 30 -> 14
            nextMilestone <= 60 -> 30
            nextMilestone <= 100 -> 60
            else -> 100
        }

        val progress = streakCount - previousMilestone
        val range = nextMilestone - previousMilestone
        return if (range > 0) (progress * 100) / range else 100
    }
}
