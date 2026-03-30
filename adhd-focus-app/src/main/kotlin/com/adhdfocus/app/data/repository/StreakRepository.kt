package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.StreakDao
import com.adhdfocus.app.data.model.Streak
import javax.inject.Inject
import java.time.LocalDate

/**
 * StreakRepository provides data access abstraction for streaks.
 *
 * Handles:
 * - Streak CRUD operations
 * - Streak increment/reset logic
 * - Streak persistence
 */
class StreakRepository @Inject constructor(
    private val streakDao: StreakDao
) {
    /**
     * Gets the current streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return Current streak count
     */
    suspend fun getCurrentStreak(userId: String, householdId: String): Int {
        val streak = streakDao.getStreak(userId, householdId)
        return streak?.currentCount ?: 0
    }

    /**
     * Gets the best streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return Best streak count
     */
    suspend fun getBestStreak(userId: String, householdId: String): Int {
        val streak = streakDao.getStreak(userId, householdId)
        return streak?.bestCount ?: 0
    }

    /**
     * Increments the streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return New streak count
     */
    suspend fun incrementStreak(userId: String, householdId: String): Int {
        var streak = streakDao.getStreak(userId, householdId)
        if (streak == null) {
            streak = Streak(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                householdId = householdId,
                currentCount = 1,
                bestCount = 1,
                lastCompletionDate = LocalDate.now(),
                startDate = LocalDate.now(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            streak = streak.copy(
                currentCount = streak.currentCount + 1,
                bestCount = maxOf(streak.currentCount + 1, streak.bestCount),
                lastCompletionDate = LocalDate.now(),
                updatedAt = System.currentTimeMillis()
            )
        }
        streakDao.upsert(streak)
        return streak.currentCount
    }

    /**
     * Resets the streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     */
    suspend fun resetStreak(userId: String, householdId: String) {
        val streak = streakDao.getStreak(userId, householdId)
        if (streak != null) {
            val resetStreak = streak.copy(
                currentCount = 0,
                updatedAt = System.currentTimeMillis()
            )
            streakDao.upsert(resetStreak)
        }
    }

    /**
     * Gets the streak for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return Streak object or null if not found
     */
    suspend fun getStreak(userId: String, householdId: String): Streak? {
        return streakDao.getStreak(userId, householdId)
    }

    /**
     * Creates or updates a streak.
     *
     * @param streak Streak to save
     */
    suspend fun saveStreak(streak: Streak) {
        streakDao.upsert(streak)
    }
}
