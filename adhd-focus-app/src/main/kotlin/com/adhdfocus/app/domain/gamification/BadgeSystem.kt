package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.repository.BadgeRepository
import java.time.LocalDate
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
            BadgeMilestone("ONE_DAY_STREAK", "1-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Completed one full day", 1, "streak"),
            BadgeMilestone("THREE_DAY_STREAK", "3-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 3-day streak", 3, "streak"),
            BadgeMilestone("SEVEN_DAY_STREAK", "7-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 7-day streak", 7, "streak"),
            BadgeMilestone("FOURTEEN_DAY_STREAK", "14-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 14-day streak", 14, "streak"),
            BadgeMilestone("THIRTY_DAY_STREAK", "30-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 30-day streak", 30, "streak"),
            BadgeMilestone("SIXTY_DAY_STREAK", "60-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 60-day streak", 60, "streak"),
            BadgeMilestone("NINETY_DAY_STREAK", "90-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 90-day streak", 90, "streak"),
            BadgeMilestone("ONE_EIGHTY_DAY_STREAK", "180-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 180-day streak", 180, "streak"),
            BadgeMilestone("TWO_SEVENTY_DAY_STREAK", "270-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 270-day streak", 270, "streak"),
            BadgeMilestone("YEAR_STREAK", "365-Day Streak", BadgeCategory.STREAK_MILESTONES,
                "Maintained a 365-day streak", 365, "streak"),

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
        return reconcileBadgeStates(
            userId = userId,
            householdId = householdId,
            completedTasksToday = completedTasksToday,
            totalTasksToday = totalTasksToday,
            currentStreak = currentStreak,
            efficiencyPercentage = efficiencyPercentage
        )
    }

    /**
     * Reconciles the badge catalog against the current metrics.
     *
     * This keeps earned badges, locked badges, and progress indicators aligned
     * when a historical day is completed or later uncompleted.
     */
    suspend fun reconcileBadgeStates(
        userId: String,
        householdId: String,
        completedTasksToday: Int,
        totalTasksToday: Int,
        currentStreak: Int,
        efficiencyPercentage: Float
    ): List<Badge> {
        ensureCurrentSeasonBadgeCatalog(userId, householdId)
        val newlyCreatedBadges = mutableListOf<Badge>()
        val currentYear = currentSeasonYear()

        BADGE_MILESTONES.forEach { milestone ->
            val shouldEarn = shouldEarnBadge(
                milestone = milestone,
                completedTasksToday = completedTasksToday,
                totalTasksToday = totalTasksToday,
                currentStreak = currentStreak,
                efficiencyPercentage = efficiencyPercentage
            )
            val existing = badgeRepository.getBadgeByType(userId, householdId, milestone.badgeType)
            val progress = calculateBadgeProgress(
                badgeType = milestone.badgeType,
                completedTasksToday = completedTasksToday,
                totalTasksToday = totalTasksToday,
                currentStreak = currentStreak,
                efficiencyPercentage = efficiencyPercentage
            )

            when {
                shouldEarn && existing == null -> {
                    newlyCreatedBadges.add(createBadge(userId, householdId, milestone.badgeType, milestone.name))
                }
                shouldEarn && existing != null && existing.isLocked -> {
                    badgeRepository.updateBadge(
                        existing.copy(
                            isLocked = false,
                            progress = null,
                            earnedAt = System.currentTimeMillis(),
                            seasonYear = currentYear
                        )
                    )
                }
                !shouldEarn && existing != null && !existing.isLocked -> {
                    badgeRepository.updateBadge(
                        existing.copy(
                            isLocked = true,
                            progress = progress,
                            earnedAt = 0L,
                            seasonYear = currentYear
                        )
                    )
                }
                !shouldEarn && existing != null && existing.isLocked -> {
                    badgeRepository.updateBadge(
                        existing.copy(
                            progress = progress,
                            seasonYear = currentYear
                        )
                    )
                }
            }
        }

        newlyCreatedBadges.forEach { badgeRepository.saveBadge(it) }
        updateLockedBadgeProgress(userId, householdId, completedTasksToday, totalTasksToday, currentStreak, efficiencyPercentage)
        return newlyCreatedBadges
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
        ensureCurrentSeasonBadgeCatalog(userId, householdId)
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
            "ONE_DAY_STREAK" -> if (currentStreak >= 1) 100 else 0
            "THREE_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 3)
            "SEVEN_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 7)
            "FOURTEEN_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 14)
            "THIRTY_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 30)
            "SIXTY_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 60)
            "NINETY_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 90)
            "ONE_EIGHTY_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 180)
            "TWO_SEVENTY_DAY_STREAK" -> minOf(100, (currentStreak * 100) / 270)
            "YEAR_STREAK" -> minOf(100, (currentStreak * 100) / 365)
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

    suspend fun ensureCurrentSeasonBadgeCatalog(userId: String, householdId: String) {
        val currentYear = currentSeasonYear()
        val allBadges = badgeRepository.getAllBadges(userId, householdId)
        val seasonYears = allBadges.map { it.seasonYear }.toSet()

        if (allBadges.isNotEmpty() && seasonYears.any { it != currentYear }) {
            badgeRepository.deleteUserBadges(userId, householdId)
        }

        val existingTypes = badgeRepository.getAllBadges(userId, householdId)
            .map { it.badgeType }
            .toSet()

        val missingMilestones = BADGE_MILESTONES.filterNot { it.badgeType in existingTypes }
        if (missingMilestones.isEmpty()) return

        badgeRepository.saveBadges(
            missingMilestones.map { milestone ->
                Badge(
                    id = java.util.UUID.randomUUID().toString(),
                    householdId = householdId,
                    userId = userId,
                    badgeType = milestone.badgeType,
                    name = milestone.name,
                    description = milestone.description,
                    earnedAt = 0L,
                    seasonYear = currentYear,
                    progress = 0,
                    isLocked = true
                )
            }
        )
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
            seasonYear = currentSeasonYear(),
            isLocked = false
        )
    }

    private fun getBadgeDescription(badgeType: String): String {
        return when (badgeType) {
            "FIRST_TASK_COMPLETE" -> "Completed your first task"
            "FIVE_TASK_DAY" -> "Completed 5 tasks in one day"
            "PERFECT_DAY" -> "Completed all tasks for the day"
            "ONE_DAY_STREAK" -> "Completed one full day"
            "THREE_DAY_STREAK" -> "Maintained a 3-day streak"
            "SEVEN_DAY_STREAK" -> "Maintained a 7-day streak"
            "FOURTEEN_DAY_STREAK" -> "Maintained a 14-day streak"
            "THIRTY_DAY_STREAK" -> "Maintained a 30-day streak"
            "SIXTY_DAY_STREAK" -> "Maintained a 60-day streak"
            "NINETY_DAY_STREAK" -> "Maintained a 90-day streak"
            "ONE_EIGHTY_DAY_STREAK" -> "Maintained a 180-day streak"
            "TWO_SEVENTY_DAY_STREAK" -> "Maintained a 270-day streak"
            "YEAR_STREAK" -> "Maintained a 365-day streak"
            "SPEED_DEMON" -> "Completed tasks 20% faster than estimated"
            else -> "Achievement unlocked"
        }
    }

    private fun shouldEarnBadge(
        milestone: BadgeMilestone,
        completedTasksToday: Int,
        totalTasksToday: Int,
        currentStreak: Int,
        efficiencyPercentage: Float
    ): Boolean {
        return when (milestone.metricType) {
            "tasks" -> completedTasksToday >= milestone.threshold
            "completion_percentage" -> totalTasksToday > 0 && completedTasksToday == totalTasksToday
            "streak" -> currentStreak >= milestone.threshold
            "efficiency" -> efficiencyPercentage >= milestone.threshold
            else -> false
        }
    }

    private fun currentSeasonYear(): Int = LocalDate.now().year
}
