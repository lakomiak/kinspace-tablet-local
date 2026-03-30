package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.repository.BadgeRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BadgeSystemUnitTest : BehaviorSpec({
    val badgeRepository = mock<BadgeRepository>()
    val badgeSystem = BadgeSystem(badgeRepository)

    Given("BadgeSystem with valid dependencies") {
        When("checking badges for first task completion") {
            Then("FIRST_TASK_COMPLETE badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE"))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.shouldHaveSize(1)
                result[0].badgeType shouldBe "FIRST_TASK_COMPLETE"
                result[0].name shouldBe "First Task Complete"
                result[0].isLocked shouldBe false
                verify(badgeRepository, times(1)).saveBadge(any())
            }
        }

        When("checking badges for 5-task day") {
            Then("FIVE_TASK_DAY badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE"))
                    .thenReturn(null)
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIVE_TASK_DAY"))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 8,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.shouldHaveSize(2) // First task + 5-task day
                result.map { it.badgeType } shouldContain "FIVE_TASK_DAY"
            }
        }

        When("checking badges for perfect day") {
            Then("PERFECT_DAY badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE"))
                    .thenReturn(null)
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIVE_TASK_DAY"))
                    .thenReturn(null)
                whenever(badgeRepository.getBadgeByType(userId, householdId, "PERFECT_DAY"))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.map { it.badgeType } shouldContain "PERFECT_DAY"
            }
        }

        When("checking badges for 3-day streak") {
            Then("THREE_DAY_STREAK badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 3,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.map { it.badgeType } shouldContain "THREE_DAY_STREAK"
            }
        }

        When("checking badges for 7-day streak") {
            Then("WEEK_WARRIOR badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 7,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.map { it.badgeType } shouldContain "WEEK_WARRIOR"
            }
        }

        When("checking badges for 30-day streak") {
            Then("MONTH_MASTER badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 30,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.map { it.badgeType } shouldContain "MONTH_MASTER"
            }
        }

        When("checking badges for speed demon (20% faster)") {
            Then("SPEED_DEMON badge should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 120f
                )

                // Assert
                result.map { it.badgeType } shouldContain "SPEED_DEMON"
            }
        }

        When("badge already earned") {
            Then("badge should not be earned again") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                val existingBadge = Badge(
                    id = "badge123",
                    householdId = householdId,
                    userId = userId,
                    badgeType = "FIRST_TASK_COMPLETE",
                    name = "First Task Complete",
                    isLocked = false
                )
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE"))
                    .thenReturn(existingBadge)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.shouldBeEmpty()
            }
        }

        When("getting earned badges") {
            Then("should return list of earned badges") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                val earnedBadges = listOf(
                    Badge(
                        id = "badge1",
                        householdId = householdId,
                        userId = userId,
                        badgeType = "FIRST_TASK_COMPLETE",
                        name = "First Task Complete",
                        isLocked = false
                    ),
                    Badge(
                        id = "badge2",
                        householdId = householdId,
                        userId = userId,
                        badgeType = "PERFECT_DAY",
                        name = "Perfect Day",
                        isLocked = false
                    )
                )
                whenever(badgeRepository.getEarnedBadges(userId, householdId))
                    .thenReturn(earnedBadges)

                // Act
                val result = badgeSystem.getEarnedBadges(userId, householdId)

                // Assert
                result.shouldHaveSize(2)
                result shouldContain earnedBadges[0]
                result shouldContain earnedBadges[1]
            }
        }

        When("getting locked badges") {
            Then("should return list of locked badges") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                val lockedBadges = listOf(
                    Badge(
                        id = "badge1",
                        householdId = householdId,
                        userId = userId,
                        badgeType = "WEEK_WARRIOR",
                        name = "Week Warrior",
                        progress = 3,
                        isLocked = true
                    )
                )
                whenever(badgeRepository.getLockedBadges(userId, householdId))
                    .thenReturn(lockedBadges)

                // Act
                val result = badgeSystem.getLockedBadges(userId, householdId)

                // Assert
                result.shouldHaveSize(1)
                result[0].isLocked shouldBe true
                result[0].progress shouldBe 3
            }
        }

        When("getting badge progress") {
            Then("should return progress percentage") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeProgress(userId, householdId, "WEEK_WARRIOR"))
                    .thenReturn(60)

                // Act
                val result = badgeSystem.getBadgeProgress(userId, householdId, "WEEK_WARRIOR")

                // Assert
                result shouldBe 60
            }
        }

        When("multiple badges earned in same check") {
            Then("all eligible badges should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 5,
                    currentStreak = 7,
                    efficiencyPercentage = 125f
                )

                // Assert
                result.shouldHaveSize(5) // First task, 5-task day, perfect day, week warrior, speed demon
                result.map { it.badgeType } shouldContain "FIRST_TASK_COMPLETE"
                result.map { it.badgeType } shouldContain "FIVE_TASK_DAY"
                result.map { it.badgeType } shouldContain "PERFECT_DAY"
                result.map { it.badgeType } shouldContain "WEEK_WARRIOR"
                result.map { it.badgeType } shouldContain "SPEED_DEMON"
            }
        }

        When("checking with zero tasks") {
            Then("no badges should be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 0,
                    totalTasksToday = 0,
                    currentStreak = 0,
                    efficiencyPercentage = 0f
                )

                // Assert
                result.shouldBeEmpty()
            }
        }

        When("checking with efficiency below threshold") {
            Then("SPEED_DEMON badge should not be earned") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE"))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.map { it.badgeType } shouldNotBe "SPEED_DEMON"
            }
        }

        When("badge descriptions are generated") {
            Then("each badge type should have appropriate description") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 50),
                    Arb.string(minSize = 1, maxSize = 50)
                ) { userId, householdId ->
                    whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                        .thenReturn(null)

                    val result = badgeSystem.checkAndEarnBadges(
                        userId = userId,
                        householdId = householdId,
                        completedTasksToday = 5,
                        totalTasksToday = 5,
                        currentStreak = 30,
                        efficiencyPercentage = 120f
                    )

                    result.forEach { badge ->
                        badge.description shouldNotBe null
                        badge.description!!.isNotEmpty() shouldBe true
                    }
                }
            }
        }

        When("badge has correct metadata") {
            Then("all badge fields should be properly set") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                // Assert
                result.forEach { badge ->
                    badge.id.isNotEmpty() shouldBe true
                    badge.householdId shouldBe householdId
                    badge.userId shouldBe userId
                    badge.badgeType.isNotEmpty() shouldBe true
                    badge.name.isNotEmpty() shouldBe true
                    badge.earnedAt shouldNotBe null
                    badge.isLocked shouldBe false
                }
            }
        }

        When("calculating badge progress for FIRST_TASK_COMPLETE") {
            Then("progress should be 100 when 1+ tasks completed") {
                val progress = badgeSystem.calculateBadgeProgress("FIRST_TASK_COMPLETE", 1, 5, 0, 100f)
                progress shouldBe 100
            }

            Then("progress should be 0 when no tasks completed") {
                val progress = badgeSystem.calculateBadgeProgress("FIRST_TASK_COMPLETE", 0, 5, 0, 100f)
                progress shouldBe 0
            }
        }

        When("calculating badge progress for FIVE_TASK_DAY") {
            Then("progress should be proportional to completed tasks") {
                val progress1 = badgeSystem.calculateBadgeProgress("FIVE_TASK_DAY", 2, 8, 0, 100f)
                progress1 shouldBe 40

                val progress2 = badgeSystem.calculateBadgeProgress("FIVE_TASK_DAY", 5, 8, 0, 100f)
                progress2 shouldBe 100
            }
        }

        When("calculating badge progress for PERFECT_DAY") {
            Then("progress should be completion percentage") {
                val progress = badgeSystem.calculateBadgeProgress("PERFECT_DAY", 3, 5, 0, 100f)
                progress shouldBe 60
            }

            Then("progress should be 0 when no tasks") {
                val progress = badgeSystem.calculateBadgeProgress("PERFECT_DAY", 0, 0, 0, 100f)
                progress shouldBe 0
            }
        }

        When("calculating badge progress for THREE_DAY_STREAK") {
            Then("progress should be proportional to streak") {
                val progress = badgeSystem.calculateBadgeProgress("THREE_DAY_STREAK", 0, 0, 2, 100f)
                progress shouldBe 66
            }
        }

        When("calculating badge progress for WEEK_WARRIOR") {
            Then("progress should be proportional to streak") {
                val progress = badgeSystem.calculateBadgeProgress("WEEK_WARRIOR", 0, 0, 5, 100f)
                progress shouldBe 71
            }
        }

        When("calculating badge progress for MONTH_MASTER") {
            Then("progress should be proportional to streak") {
                val progress = badgeSystem.calculateBadgeProgress("MONTH_MASTER", 0, 0, 15, 100f)
                progress shouldBe 50
            }
        }

        When("calculating badge progress for SPEED_DEMON") {
            Then("progress should be proportional to efficiency") {
                val progress = badgeSystem.calculateBadgeProgress("SPEED_DEMON", 0, 0, 0, 60f)
                progress shouldBe 50
            }
        }

        When("updating locked badge progress") {
            Then("locked badges should have progress updated") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                val lockedBadge = Badge(
                    id = "badge1",
                    householdId = householdId,
                    userId = userId,
                    badgeType = "WEEK_WARRIOR",
                    name = "Week Warrior",
                    progress = 0,
                    isLocked = true
                )
                whenever(badgeRepository.getLockedBadges(userId, householdId))
                    .thenReturn(listOf(lockedBadge))

                // Act
                badgeSystem.updateLockedBadgeProgress(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 5,
                    efficiencyPercentage = 100f
                )

                // Assert
                val captor = argumentCaptor<Badge>()
                verify(badgeRepository, times(1)).updateBadge(captor.capture())
                captor.value.progress shouldBe 71
            }
        }

        When("getting all badge milestones") {
            Then("should return all defined milestones") {
                val milestones = badgeSystem.getAllBadgeMilestones()
                milestones.shouldHaveSize(7)
                milestones.map { it.badgeType } shouldContain "FIRST_TASK_COMPLETE"
                milestones.map { it.badgeType } shouldContain "WEEK_WARRIOR"
                milestones.map { it.badgeType } shouldContain "SPEED_DEMON"
            }
        }

        When("getting badge milestones by category") {
            Then("should return only milestones in that category") {
                val dailyMilestones = badgeSystem.getBadgeMilestonesByCategory(BadgeSystem.BadgeCategory.DAILY_MILESTONES)
                dailyMilestones.shouldHaveSize(3)
                dailyMilestones.map { it.badgeType } shouldContain "FIRST_TASK_COMPLETE"
                dailyMilestones.map { it.badgeType } shouldContain "FIVE_TASK_DAY"
                dailyMilestones.map { it.badgeType } shouldContain "PERFECT_DAY"
            }

            Then("should return streak milestones") {
                val streakMilestones = badgeSystem.getBadgeMilestonesByCategory(BadgeSystem.BadgeCategory.STREAK_MILESTONES)
                streakMilestones.shouldHaveSize(3)
                streakMilestones.map { it.badgeType } shouldContain "THREE_DAY_STREAK"
                streakMilestones.map { it.badgeType } shouldContain "WEEK_WARRIOR"
                streakMilestones.map { it.badgeType } shouldContain "MONTH_MASTER"
            }

            Then("should return efficiency milestones") {
                val efficiencyMilestones = badgeSystem.getBadgeMilestonesByCategory(BadgeSystem.BadgeCategory.EFFICIENCY_BADGES)
                efficiencyMilestones.shouldHaveSize(1)
                efficiencyMilestones[0].badgeType shouldBe "SPEED_DEMON"
            }
        }

        When("getting a specific badge milestone") {
            Then("should return the milestone if found") {
                val milestone = badgeSystem.getBadgeMilestone("WEEK_WARRIOR")
                milestone shouldNotBe null
                milestone?.name shouldBe "Week Warrior"
                milestone?.threshold shouldBe 7
            }

            Then("should return null if not found") {
                val milestone = badgeSystem.getBadgeMilestone("NONEXISTENT")
                milestone shouldBe null
            }
        }

        When("badge progress calculation with edge cases") {
            Then("progress should not exceed 100") {
                val progress = badgeSystem.calculateBadgeProgress("FIVE_TASK_DAY", 10, 8, 0, 100f)
                progress shouldBe 100
            }

            Then("progress should handle zero efficiency") {
                val progress = badgeSystem.calculateBadgeProgress("SPEED_DEMON", 0, 0, 0, 0f)
                progress shouldBe 0
            }
        }

        When("milestone tracking for multiple badge categories") {
            Then("should track daily, streak, and efficiency milestones") {
                // Arrange
                val userId = "user123"
                val householdId = "household123"
                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)
                whenever(badgeRepository.getLockedBadges(userId, householdId))
                    .thenReturn(emptyList())

                // Act
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 5,
                    currentStreak = 7,
                    efficiencyPercentage = 125f
                )

                // Assert - should have badges from multiple categories
                result.map { it.badgeType } shouldContain "FIRST_TASK_COMPLETE" // Daily
                result.map { it.badgeType } shouldContain "PERFECT_DAY" // Daily
                result.map { it.badgeType } shouldContain "WEEK_WARRIOR" // Streak
                result.map { it.badgeType } shouldContain "SPEED_DEMON" // Efficiency
            }
        }
    }
})
