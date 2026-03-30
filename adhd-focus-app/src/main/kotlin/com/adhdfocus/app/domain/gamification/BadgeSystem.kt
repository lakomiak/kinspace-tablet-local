package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.repository.BadgeRepository
import javax.inject.Inject

/**
 * BadgeSystem manages badge earning and tracking with milestone tracking.
 *
 * Manages:
 * - Badge earning at milestones
 * - Badge progress tracking for locked badges
 * - Badge categories: Daily Milestones, Weekly Achievements, Streak Milestones, Efficiency Badges
 * - Badge display and notifications
 * - Locked badge hints
 */
class BadgeSystem @Inject constructor(
    private val badgeRepository: BadgeRepository
) {
    /**
     * Badge categories for organization.
     */
    enum class BadgeCategory {
        DAILY_MILESTONES,
        WEEKLY_ACHIEVEMENTS,
        STREAK_MILESTONES,
        EFFICIENCY_BADGES
    }

    /**
     * Milestone definitions for badge earning.
     */
    data class BadgeMilestone(
        val badgeType: String,
        val name: String,
        val category: BadgeCategory,
        val description: String,
        val threshold: Int,
        val metricType: String // "tasks", "streak", "efficiency", "weekly"
    )

    companion object {
        private val BADGE_MILESTONES = listOf(
            // Daily Milestones
            BadgeMilestone("FIRST_TASK_COMPLETE", "First Task Complete", BadgeCategory.DAILY_MILESTONES,
                "Completed your first task", 1, "tasks"),
            BadgeMilestone("FIVE_TASK_DAY", "5-Task Day", BadgeCategory.DAILY_MILESTONES,
                "Completed 5 tasks in one day", 5, "tasks"),
            BadgeMilestone("PERFECT_DAY", "Perfect Day", BadgeCategory.DAILY_MILESTONES,
                "Completed all tasks for the day", 100, "completion_percentage"),

            // Streak Milestones
            BadgeMilestone("THREE_DAY_STREAK", "3-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 3-day streak", 3, "streak"),
            BadgeMilestone("WEEK_WARRIOR", "Week Warrior", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 7-day streak", 7, "streak"),
            BadgeMilestone("MONTH_MASTER", "Month Master", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 30-day streak", 30, "streak"),

            // Efficiency Badges
            BadgeMilestone("SPEED_DEMON", "Speed Demon", BadgeCategory.EFFICIENCY_BADGES,
                "Completed tasks 20% faster than estimated", 120, "efficiency")
        )
    }

    /**
     * Checks if a badge should be earned based on current metrics.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @param completedTasksToday Number of tasks completed today
     * @param totalTasksToday Total tasks for today
     * @param currentStreak Current streak count
     * @param efficiencyPercentage Current efficiency percentage
     * @return List of newly earned badges
     */
    suspend fun checkAndEarnBadges(
        userId: String,
        householdId: String,
        completedTasksToday: Int,
        totalTasksToday: Int,
        currentStreak: Int,
        efficiencyPercentage: Float
    ): List<Badge> {
        val earnedBadges = mutableListOf<Badge>()

        // Daily milestones
        if (completedTasksToday == 1) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "FIRST_TASK_COMPLETE", "First Task Complete"))
            }
        }

        if (completedTasksToday >= 5) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "FIVE_TASK_DAY")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "FIVE_TASK_DAY", "5-Task Day"))
            }
        }

        if (completedTasksToday == totalTasksToday && totalTasksToday > 0) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "PERFECT_DAY")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "PERFECT_DAY", "Perfect Day"))
            }
        }

        // Streak milestones
        if (currentStreak == 3) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "THREE_DAY_STREAK")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "THREE_DAY_STREAK", "3-Day Streak"))
            }
        }

        if (currentStreak == 7) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "WEEK_WARRIOR")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "WEEK_WARRIOR", "Week Warrior"))
            }
        }

        if (currentStreak == 30) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "MONTH_MASTER")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "MONTH_MASTER", "Month Master"))
            }
        }

        // Efficiency badges
        if (efficiencyPercentage >= 120) {
            val badge = badgeRepository.getBadgeByType(userId, householdId, "SPEED_DEMON")
            if (badge == null) {
                earnedBadges.add(createBadge(userId, householdId, "SPEED_DEMON", "Speed Demon"))
            }
        }

        // Save earned badges
        earnedBadges.forEach { badgeRepository.saveBadge(it) }

        // Update progress for locked badges
        updateLockedBadgeProgress(userId, householdId, completedTasksToday, totalTasksToday, currentStreak, efficiencyPercentage)

        return earnedBadges
    }

    /**
     * Updates progress for locked badges based on current metrics.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @param completedTasksToday Number of tasks completed today
     * @param totalTasksToday Total tasks for today
     * @param currentStreak Current streak count
     * @param efficiencyPercentage Current efficiency percentage
     */
    suspend fun updateLockedBadgeProgress(
        userId: String,
        householdId: String,
        completedTasksToday: Int,
        totalTasksToday: Int,
        currentStreak: Int,
        efficiencyPercentage: Float
    ) {
        val lockedBadges = badgeRepository.getLockedBadges(userId, householdId)

        for (badge in lockedBadges) {
            val progress = calculateBadgeProgress(badge.badgeType, completedTasksToday, totalTasksToday, currentStreak, efficiencyPercentage)
            if (progress > 0) {
                val updatedBadge = badge.copy(progress = progress)
                badgeRepository.updateBadge(updatedBadge)
            }
        }
    }

    /**
     * Calculates progress toward a specific badge.
     *
     * @param badgeType Badge type
     * @param completedTasksToday Number of tasks completed today
     * @param totalTasksToday Total tasks for today
     * @param currentStreak Current streak count
     * @param efficiencyPercentage Current efficiency percentage
     * @return Progress as a percentage (0-100)
     */
    fun calculateBadgeProgress(
        badgeType: String,
        completedTasksToday: Int,
        totalTasksToday: Int,
        currentStreak: Int,
        efficiencyPercentage: Float
    ): Int {
        return when (badgeType) {
            "FIRST_TASK_COMPLETE" -> if (completedTasksToday >= 1) 100 else 0
            "FIVE_TASK_DAY" -> minOf(100, (completedTasksToday * 100) / 5)
            "PERFECT_DAY" -> if (totalTasksToday > 0) (completedTasksToday * 100) / totalTasksToday else 0
            "THREE_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 3)
            "WEEK_WARRIOR" -> minOf(100, (currentStreak * 100) / 7)
            "MONTH_MASTER" -> minOf(100, (currentStreak * 100) / 30)
            "SPEED_DEMON" -> minOf(100, (efficiencyPercentage.toInt() * 100) / 120)
            else -> 0
        }
    }

    /**
     * Gets all earned badges for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return List of earned badges
     */
    suspend fun getEarnedBadges(userId: String, householdId: String): List<Badge> {
        return badgeRepository.getEarnedBadges(userId, householdId)
    }

    /**
     * Gets all locked badges for a user.
     *
     * @param userId User ID
     * @param householdId Household ID
     * @return List of locked badges with progress
     */
    suspend fun getLockedBadges(userId: String, householdId: String): List<Badge> {
        return badgeRepository.getLockedBadges(userId, householdId)
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
        return badgeRepository.getBadgeProgress(userId, householdId, badgeType)
    }

    /**
     * Gets all badge milestones.
     *
     * @return List of all badge milestones
     */
    fun getAllBadgeMilestones(): List<BadgeMilestone> {
        return BADGE_MILESTONES
    }

    /**
     * Gets badge milestones by category.
     *
     * @param category Badge category
     * @return List of badge milestones in the category
     */
    fun getBadgeMilestonesByCategory(category: BadgeCategory): List<BadgeMilestone> {
        return BADGE_MILESTONES.filter { it.category == category }
    }

    /**
     * Gets a badge milestone by type.
     *
     * @param badgeType Badge type
     * @return Badge milestone or null if not found
     */
    fun getBadgeMilestone(badgeType: String): BadgeMilestone? {
        return BADGE_MILESTONES.find { it.badgeType == badgeType }
    }

    private fun createBadge(
        userId: String,
        householdId: String,
        badgeType: String,
        name: String
    ): Badge {
        return Badge(
            id = java.util.UUID.randomUUID().toString(),
            householdId = householdId,
            userId = userId,
            badgeType = badgeType,
            name = name,
            description = getBadgeDescription(badgeType),
            earnedAt = System.currentTimeMillis(),
            isLocked = false
        )
    }

    private fun getBadgeDescription(badgeType: String): String {
        return when (badgeType) {
            "FIRST_TASK_COMPLETE" -> "Completed your first task"
            "FIVE_TASK_DAY" -> "Completed 5 tasks in one day"
            "PERFECT_DAY" -> "Completed all tasks for the day"
            "THREE_DAY_STREAK" -> "Maintained a 3-day streak"
            "WEEK_WARRIOR" -> "Maintained a 7-day streak"
            "MONTH_MASTER" -> "Maintained a 30-day streak"
            "SPEED_DEMON" -> "Completed tasks 20% faster than estimated"
            else -> "Achievement unlocked"
        }
    }
}
