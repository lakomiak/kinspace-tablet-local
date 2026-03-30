package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.BadgeDao
import com.adhdfocus.app.data.model.Badge
import javax.inject.Inject

/**
 * BadgeRepository provides data access abstraction for badges.
 *
 * Handles:
 * - Badge CRUD operations
 * - Badge retrieval by user and type
 * - Badge persistence
 * - Badge progress tracking
 */
class BadgeRepository @Inject constructor(
    private val badgeDao: BadgeDao
) {
    /**
     * Gets all earned badges for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return List of earned badges
     */
    suspend fun getEarnedBadges(userId: String, householdId: String): List<Badge> {
        return badgeDao.getEarnedBadgesByUserOnce(userId)
    }

    /**
     * Gets all locked badges for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return List of locked badges
     */
    suspend fun getLockedBadges(userId: String, householdId: String): List<Badge> {
        return badgeDao.getLockedBadgesByUserOnce(userId)
    }

    /**
     * Gets a badge by type for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @param badgeType Badge type
     * @return Badge or null if not found
     */
    suspend fun getBadgeByType(userId: String, householdId: String, badgeType: String): Badge? {
        val badges = badgeDao.getBadgesByUserAndType(userId, badgeType)
        return badges.firstOrNull()
    }

    /**
     * Gets progress toward a specific badge.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @param badgeType Badge type
     * @return Progress as a percentage (0-100)
     */
    suspend fun getBadgeProgress(userId: String, householdId: String, badgeType: String): Int {
        val badge = getBadgeByType(userId, householdId, badgeType)
        return badge?.progress ?: 0
    }

    /**
     * Saves a badge.
     *
     * @param badge Badge to save
     */
    suspend fun saveBadge(badge: Badge) {
        badgeDao.insert(badge)
    }

    /**
     * Saves multiple badges.
     *
     * @param badges List of badges to save
     */
    suspend fun saveBadges(badges: List<Badge>) {
        badges.forEach { badgeDao.insert(it) }
    }

    /**
     * Updates a badge.
     *
     * @param badge Badge to update
     */
    suspend fun updateBadge(badge: Badge) {
        badgeDao.update(badge)
    }

    /**
     * Deletes a badge.
     *
     * @param badgeId Badge ID
     */
    suspend fun deleteBadge(badgeId: String) {
        badgeDao.deleteBadgeById(badgeId)
    }

    /**
     * Gets all badges for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return List of all badges (earned and locked)
     */
    suspend fun getAllBadges(userId: String, householdId: String): List<Badge> {
        return badgeDao.getBadgesByUserOnce(userId)
    }
}
