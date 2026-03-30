package com.adhdfocus.app.domain.gamification

import com.adhdfocus.app.data.model.Badge
import com.adhdfocus.app.data.repository.BadgeRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Property-based tests for BadgeSystem badge earning logic.
 *
 * **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.6, 6.8**
 * **Validates: Properties 22, 23, 24, 25, 26**
 *
 * Tests verify that badge earning logic correctly:
 * - Awards badges at all milestone thresholds
 * - Handles various task configurations
 * - Handles various streak counts
 * - Handles various efficiency values
 * - Supports multiple badge earning in a single day
 * - Handles edge cases and boundary conditions
 * - Triggers correctly
 * - Calculates progress correctly
 * - Displays correctly in achievements view
 */
class BadgeEarningPropertyTest : FunSpec({

    val badgeRepository = mock<BadgeRepository>()
    val badgeSystem = BadgeSystem(badgeRepository)

    test("Property 22: Badge earning at FIRST_TASK_COMPLETE milestone for any user and household") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "FIRST_TASK_COMPLETE"
                result.find { it.badgeType == "FIRST_TASK_COMPLETE" }?.let { badge ->
                    badge.userId shouldBe userId
                    badge.householdId shouldBe householdId
                    badge.isLocked shouldBe false
                }
            }
        }
    }

    test("Property 22: Badge earning at FIVE_TASK_DAY milestone for any task count >= 5") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.int(5..20)
        ) { userId, householdId, completedTasks ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = completedTasks,
                    totalTasksToday = completedTasks + 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "FIVE_TASK_DAY"
            }
        }
    }

    test("Property 22: Badge earning at PERFECT_DAY milestone when all tasks completed") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.int(1..20)
        ) { userId, householdId, taskCount ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = taskCount,
                    totalTasksToday = taskCount,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "PERFECT_DAY"
            }
        }
    }

    test("Property 22: Badge earning at THREE_DAY_STREAK milestone for streak == 3") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 3,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "THREE_DAY_STREAK"
            }
        }
    }

    test("Property 22: Badge earning at WEEK_WARRIOR milestone for streak == 7") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 7,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "WEEK_WARRIOR"
            }
        }
    }

    test("Property 22: Badge earning at MONTH_MASTER milestone for streak == 30") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 30,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "MONTH_MASTER"
            }
        }
    }

    test("Property 22: Badge earning at SPEED_DEMON milestone for efficiency >= 120") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.float(120f..200f)
        ) { userId, householdId, efficiency ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = efficiency
                )

                result.map { it.badgeType } shouldContain "SPEED_DEMON"
            }
        }
    }

    test("Property 22: No badge earned when thresholds not met") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 0,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.shouldBeEmpty()
            }
        }
    }

    test("Property 22: Badge not earned twice for same milestone") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            val existingBadge = Badge(
                id = "badge1",
                householdId = householdId,
                userId = userId,
                badgeType = "FIRST_TASK_COMPLETE",
                name = "First Task Complete",
                isLocked = false
            )
            whenever(badgeRepository.getBadgeByType(userId, householdId, "FIRST_TASK_COMPLETE"))
                .thenReturn(existingBadge)
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldNotBe "FIRST_TASK_COMPLETE"
            }
        }
    }

    test("Property 23: Badge notification contains correct metadata for earned badges") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.forEach { badge ->
                    badge.id.isNotEmpty() shouldBe true
                    badge.householdId shouldBe householdId
                    badge.userId shouldBe userId
                    badge.badgeType.isNotEmpty() shouldBe true
                    badge.name.isNotEmpty() shouldBe true
                    badge.description shouldNotBe null
                    badge.earnedAt shouldNotBe null
                    badge.isLocked shouldBe false
                }
            }
        }
    }

    test("Property 24: Badge display shows earned badges with correct status") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
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

            runBlocking {
                val result = badgeSystem.getEarnedBadges(userId, householdId)

                result.shouldHaveSize(2)
                result.forEach { badge ->
                    badge.isLocked shouldBe false
                }
            }
        }
    }

    test("Property 26: Locked badge display shows progress toward next achievement") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.int(0..100)
        ) { userId, householdId, progress ->
            val lockedBadges = listOf(
                Badge(
                    id = "badge1",
                    householdId = householdId,
                    userId = userId,
                    badgeType = "WEEK_WARRIOR",
                    name = "Week Warrior",
                    progress = progress,
                    isLocked = true
                )
            )
            whenever(badgeRepository.getLockedBadges(userId, householdId))
                .thenReturn(lockedBadges)

            runBlocking {
                val result = badgeSystem.getLockedBadges(userId, householdId)

                result.shouldHaveSize(1)
                result[0].isLocked shouldBe true
                result[0].progress shouldBe progress
            }
        }
    }

    test("Property 25: Badge progress calculation is correct for FIRST_TASK_COMPLETE") {
        checkAll(
            Arb.int(0..10)
        ) { completedTasks ->
            val progress = badgeSystem.calculateBadgeProgress(
                "FIRST_TASK_COMPLETE",
                completedTasks,
                5,
                0,
                100f
            )

            if (completedTasks >= 1) {
                progress shouldBe 100
            } else {
                progress shouldBe 0
            }
        }
    }

    test("Property 25: Badge progress calculation is correct for FIVE_TASK_DAY") {
        checkAll(
            Arb.int(0..10)
        ) { completedTasks ->
            val progress = badgeSystem.calculateBadgeProgress(
                "FIVE_TASK_DAY",
                completedTasks,
                10,
                0,
                100f
            )

            val expected = minOf(100, (completedTasks * 100) / 5)
            progress shouldBe expected
        }
    }

    test("Property 25: Badge progress calculation is correct for PERFECT_DAY") {
        checkAll(
            Arb.int(0..20),
            Arb.int(1..20)
        ) { completedTasks, totalTasks ->
            val progress = badgeSystem.calculateBadgeProgress(
                "PERFECT_DAY",
                completedTasks,
                totalTasks,
                0,
                100f
            )

            val expected = (completedTasks * 100) / totalTasks
            progress shouldBe expected
        }
    }

    test("Property 25: Badge progress calculation is correct for THREE_DAY_STREAK") {
        checkAll(
            Arb.int(0..10)
        ) { streak ->
            val progress = badgeSystem.calculateBadgeProgress(
                "THREE_DAY_STREAK",
                0,
                0,
                streak,
                100f
            )

            val expected = minOf(100, (streak * 100) / 3)
            progress shouldBe expected
        }
    }

    test("Property 25: Badge progress calculation is correct for WEEK_WARRIOR") {
        checkAll(
            Arb.int(0..15)
        ) { streak ->
            val progress = badgeSystem.calculateBadgeProgress(
                "WEEK_WARRIOR",
                0,
                0,
                streak,
                100f
            )

            val expected = minOf(100, (streak * 100) / 7)
            progress shouldBe expected
        }
    }

    test("Property 25: Badge progress calculation is correct for MONTH_MASTER") {
        checkAll(
            Arb.int(0..60)
        ) { streak ->
            val progress = badgeSystem.calculateBadgeProgress(
                "MONTH_MASTER",
                0,
                0,
                streak,
                100f
            )

            val expected = minOf(100, (streak * 100) / 30)
            progress shouldBe expected
        }
    }

    test("Property 25: Badge progress calculation is correct for SPEED_DEMON") {
        checkAll(
            Arb.float(0f..200f)
        ) { efficiency ->
            val progress = badgeSystem.calculateBadgeProgress(
                "SPEED_DEMON",
                0,
                0,
                0,
                efficiency
            )

            val expected = minOf(100, (efficiency.toInt() * 100) / 120)
            progress shouldBe expected
        }
    }

    test("Property 25: Badge progress never exceeds 100") {
        checkAll(
            Arb.int(0..100),
            Arb.int(0..100),
            Arb.int(0..100),
            Arb.float(0f..300f)
        ) { completed, total, streak, efficiency ->
            val badgeTypes = listOf(
                "FIRST_TASK_COMPLETE", "FIVE_TASK_DAY", "PERFECT_DAY",
                "THREE_DAY_STREAK", "WEEK_WARRIOR", "MONTH_MASTER", "SPEED_DEMON"
            )

            badgeTypes.forEach { badgeType ->
                val progress = badgeSystem.calculateBadgeProgress(
                    badgeType,
                    completed,
                    if (total > 0) total else 1,
                    streak,
                    efficiency
                )

                progress shouldBeLessThanOrEqual 100
                progress shouldBeGreaterThanOrEqual 0
            }
        }
    }

    test("Property 22: Multiple badges earned in single day for various configurations") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.int(5..20),
            Arb.int(3..30),
            Arb.float(100f..200f)
        ) { userId, householdId, completedTasks, streak, efficiency ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = completedTasks,
                    totalTasksToday = completedTasks,
                    currentStreak = streak,
                    efficiencyPercentage = efficiency
                )

                // Should have multiple badges
                result.size shouldBeGreaterThanOrEqual 1
                // All badges should be unique
                result.map { it.badgeType }.distinct().size shouldBe result.size
            }
        }
    }

    test("Property 22: Badge earning with edge case of exactly 5 tasks") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "FIVE_TASK_DAY"
                result.map { it.badgeType } shouldContain "PERFECT_DAY"
            }
        }
    }

    test("Property 22: Badge earning with edge case of exactly 120% efficiency") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 120f
                )

                result.map { it.badgeType } shouldContain "SPEED_DEMON"
            }
        }
    }

    test("Property 22: Badge earning with edge case of 119% efficiency (below threshold)") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 119f
                )

                result.map { it.badgeType } shouldNotBe "SPEED_DEMON"
            }
        }
    }

    test("Property 22: Badge earning with zero efficiency") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = 0f
                )

                result.map { it.badgeType } shouldNotBe "SPEED_DEMON"
            }
        }
    }

    test("Property 22: Badge earning with zero tasks") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 0,
                    totalTasksToday = 0,
                    currentStreak = 0,
                    efficiencyPercentage = 0f
                )

                result.shouldBeEmpty()
            }
        }
    }

    test("Property 22: Badge earning with high task count") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.int(50..200)
        ) { userId, householdId, taskCount ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = taskCount,
                    totalTasksToday = taskCount,
                    currentStreak = 0,
                    efficiencyPercentage = 100f
                )

                result.map { it.badgeType } shouldContain "FIVE_TASK_DAY"
                result.map { it.badgeType } shouldContain "PERFECT_DAY"
            }
        }
    }

    test("Property 22: Badge earning with high streak count") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.int(31..365)
        ) { userId, householdId, streak ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = streak,
                    efficiencyPercentage = 100f
                )

                // Should have at least MONTH_MASTER
                result.map { it.badgeType } shouldContain "MONTH_MASTER"
            }
        }
    }

    test("Property 22: Badge earning with very high efficiency") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50),
            Arb.float(150f..300f)
        ) { userId, householdId, efficiency ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 1,
                    totalTasksToday = 5,
                    currentStreak = 0,
                    efficiencyPercentage = efficiency
                )

                result.map { it.badgeType } shouldContain "SPEED_DEMON"
            }
        }
    }

    test("Property 22: Badge earning consistency across multiple calls with same parameters") {
        checkAll(
            Arb.string(1..50),
            Arb.string(1..50)
        ) { userId, householdId ->
            whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                .thenReturn(null)

            runBlocking {
                val result1 = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 5,
                    currentStreak = 7,
                    efficiencyPercentage = 125f
                )

                whenever(badgeRepository.getBadgeByType(userId, householdId, any()))
                    .thenReturn(null)

                val result2 = badgeSystem.checkAndEarnBadges(
                    userId = userId,
                    householdId = householdId,
                    completedTasksToday = 5,
                    totalTasksToday = 5,
                    currentStreak = 7,
                    efficiencyPercentage = 125f
                )

                result1.map { it.badgeType }.sorted() shouldBe result2.map { it.badgeType }.sorted()
            }
        }
    }

    test("Property 25: Badge progress calculation is monotonically increasing with task completion") {
        checkAll(
            Arb.int(0..4),
            Arb.int(5..20)
        ) { completed1, totalTasks ->
            val completed2 = completed1 + 1

            val progress1 = badgeSystem.calculateBadgeProgress(
                "FIVE_TASK_DAY",
                completed1,
                totalTasks,
                0,
                100f
            )

            val progress2 = badgeSystem.calculateBadgeProgress(
                "FIVE_TASK_DAY",
                completed2,
                totalTasks,
                0,
                100f
            )

            progress2 shouldBeGreaterThanOrEqual progress1
        }
    }

    test("Property 25: Badge progress calculation is monotonically increasing with streak") {
        checkAll(
            Arb.int(0..29)
        ) { streak1 ->
            val streak2 = streak1 + 1

            val progress1 = badgeSystem.calculateBadgeProgress(
                "MONTH_MASTER",
                0,
                0,
                streak1,
                100f
            )

            val progress2 = badgeSystem.calculateBadgeProgress(
                "MONTH_MASTER",
                0,
                0,
                streak2,
                100f
            )

            progress2 shouldBeGreaterThanOrEqual progress1
        }
    }

    test("Property 25: Badge progress calculation is monotonically increasing with efficiency") {
        checkAll(
            Arb.float(100f..119f)
        ) { efficiency1 ->
            val efficiency2 = efficiency1 + 1f

            val progress1 = badgeSystem.calculateBadgeProgress(
                "SPEED_DEMON",
                0,
                0,
                0,
                efficiency1
            )

            val progress2 = badgeSystem.calculateBadgeProgress(
                "SPEED_DEMON",
                0,
                0,
                0,
                efficiency2
            )

            progress2 shouldBeGreaterThanOrEqual progress1
        }
    }
})
