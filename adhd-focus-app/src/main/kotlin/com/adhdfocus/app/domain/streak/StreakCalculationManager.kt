package com.adhdfocus.app.domain.streak

import com.adhdfocus.app.data.model.Streak
import com.adhdfocus.app.data.repository.StreakRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * StreakCalculationManager handles streak calculation and management.
 *
 * Provides:
 * - Streak increment on 100% completion
 * - Streak reset on incomplete day
 * - Streak calculation logic
 * - Best streak tracking
 */
class StreakCalculationManager @Inject constructor(
    private val streakRepository: StreakRepository
) {

    /**
     * Calculates if a streak should be incremented.
     *
     * @param currentStreak Current streak count
     * @param lastCompletionDate Last completion date
     * @param isDayComplete Whether all tasks are completed today
     * @return True if streak should be incremented
     */
    fun shouldIncrementStreak(
        currentStreak: Int,
        lastCompletionDate: LocalDate?,
        isDayComplete: Boolean
    ): Boolean {
        if (!isDayComplete) return false

        val today = LocalDate.now()
        
        // If no previous completion, increment
        if (lastCompletionDate == null) return true

        // If last completion was yesterday, increment
        if (lastCompletionDate == today.minusDays(1)) return true

        // If last completion was today, don't increment again
        if (lastCompletionDate == today) return false

        // If gap is more than 1 day, streak is broken
        return false
    }

    /**
     * Calculates if a streak should be reset.
     *
     * @param lastCompletionDate Last completion date
     * @param isDayComplete Whether all tasks are completed today
     * @return True if streak should be reset
     */
    fun shouldResetStreak(
        lastCompletionDate: LocalDate?,
        isDayComplete: Boolean
    ): Boolean {
        if (isDayComplete) return false

        val today = LocalDate.now()

        // If no previous completion, no reset needed
        if (lastCompletionDate == null) return false

        // If last completion was today, no reset needed
        if (lastCompletionDate == today) return false

        // If last completion was yesterday, no reset needed (streak continues)
        if (lastCompletionDate == today.minusDays(1)) return false

        // If gap is more than 1 day and day is incomplete, reset
        return true
    }

    /**
     * Increments the streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return New streak count
     */
    suspend fun incrementStreak(userId: String, householdId: String): Int {
        return streakRepository.incrementStreak(userId, householdId)
    }

    /**
     * Resets the streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     */
    suspend fun resetStreak(userId: String, householdId: String) {
        streakRepository.resetStreak(userId, householdId)
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
     * Gets the streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return Streak object or null
     */
    suspend fun getStreak(userId: String, householdId: String): Streak? {
        return streakRepository.getStreak(userId, householdId)
    }

    /**
     * Checks if a streak is at a milestone.
     *
     * @param streakCount Current streak count
     * @return True if at a milestone (3, 7, 14, 30, etc.)
     */
    fun isAtMilestone(streakCount: Int): Boolean {
        return streakCount in listOf(3, 7, 14, 30, 60, 90, 365)
    }

    /**
     * Gets the next milestone after current streak.
     *
     * @param streakCount Current streak count
     * @return Next milestone count
     */
    fun getNextMilestone(streakCount: Int): Int {
        return when {
            streakCount < 3 -> 3
            streakCount < 7 -> 7
            streakCount < 14 -> 14
            streakCount < 30 -> 30
            streakCount < 60 -> 60
            streakCount < 90 -> 90
            streakCount < 365 -> 365
            else -> streakCount + 365
        }
    }

    /**
     * Gets the days until next milestone.
     *
     * @param streakCount Current streak count
     * @return Days until next milestone
     */
    fun getDaysUntilNextMilestone(streakCount: Int): Int {
        val nextMilestone = getNextMilestone(streakCount)
        return nextMilestone - streakCount
    }

    /**
     * Calculates streak progress to next milestone.
     *
     * @param streakCount Current streak count
     * @return Progress as percentage (0-100)
     */
    fun getStreakProgressToNextMilestone(streakCount: Int): Int {
        val nextMilestone = getNextMilestone(streakCount)
        val previousMilestone = when {
            streakCount < 3 -> 0
            streakCount < 7 -> 3
            streakCount < 14 -> 7
            streakCount < 30 -> 14
            streakCount < 60 -> 30
            streakCount < 90 -> 60
            streakCount < 365 -> 90
            else -> 365
        }

        val progress = streakCount - previousMilestone
        val range = nextMilestone - previousMilestone

        return if (range > 0) (progress * 100) / range else 100
    }

    /**
     * Gets milestone description.
     *
     * @param streakCount Streak count
     * @return Description of the milestone
     */
    fun getMilestoneDescription(streakCount: Int): String {
        return when (streakCount) {
            3 -> "3-Day Streak! 🔥"
            7 -> "Week Warrior! 🏆"
            14 -> "2-Week Champion! 💪"
            30 -> "Month Master! 🌟"
            60 -> "2-Month Legend! ⭐"
            90 -> "3-Month Superstar! 🚀"
            365 -> "Year of Consistency! 👑"
            else -> "$streakCount-Day Streak! 🔥"
        }
    }
}
