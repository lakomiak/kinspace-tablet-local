package com.adhdfocus.app.domain.streak

import com.adhdfocus.app.data.model.Streak
import com.adhdfocus.app.data.repository.StreakRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Unit Tests for StreakCalculationManager
 *
 * Tests:
 * - Streak increment logic
 * - Streak reset logic
 * - Milestone detection
 * - Progress calculation
 */
class StreakCalculationManagerTest : FunSpec({

    test("Should increment streak - first completion") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val shouldIncrement = manager.shouldIncrementStreak(
            currentStreak = 0,
            lastCompletionDate = null,
            isDayComplete = true
        )

        shouldIncrement shouldBe true
    }

    test("Should increment streak - consecutive day") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val yesterday = LocalDate.now().minusDays(1)
        val shouldIncrement = manager.shouldIncrementStreak(
            currentStreak = 5,
            lastCompletionDate = yesterday,
            isDayComplete = true
        )

        shouldIncrement shouldBe true
    }

    test("Should not increment streak - same day") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val today = LocalDate.now()
        val shouldIncrement = manager.shouldIncrementStreak(
            currentStreak = 5,
            lastCompletionDate = today,
            isDayComplete = true
        )

        shouldIncrement shouldBe false
    }

    test("Should not increment streak - day incomplete") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val yesterday = LocalDate.now().minusDays(1)
        val shouldIncrement = manager.shouldIncrementStreak(
            currentStreak = 5,
            lastCompletionDate = yesterday,
            isDayComplete = false
        )

        shouldIncrement shouldBe false
    }

    test("Should not increment streak - gap in days") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val twoDaysAgo = LocalDate.now().minusDays(2)
        val shouldIncrement = manager.shouldIncrementStreak(
            currentStreak = 5,
            lastCompletionDate = twoDaysAgo,
            isDayComplete = true
        )

        shouldIncrement shouldBe false
    }

    test("Should reset streak - gap in days and incomplete") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val twoDaysAgo = LocalDate.now().minusDays(2)
        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = twoDaysAgo,
            isDayComplete = false
        )

        shouldReset shouldBe true
    }

    test("Should not reset streak - day complete") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val yesterday = LocalDate.now().minusDays(1)
        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = yesterday,
            isDayComplete = true
        )

        shouldReset shouldBe false
    }

    test("Should not reset streak - consecutive day incomplete") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val yesterday = LocalDate.now().minusDays(1)
        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = yesterday,
            isDayComplete = false
        )

        shouldReset shouldBe false
    }

    test("Should not reset streak - no previous completion") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = null,
            isDayComplete = false
        )

        shouldReset shouldBe false
    }

    test("Milestone detection - 3 days") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.isAtMilestone(3) shouldBe true
    }

    test("Milestone detection - 7 days") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.isAtMilestone(7) shouldBe true
    }

    test("Milestone detection - 14 days") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.isAtMilestone(14) shouldBe true
    }

    test("Milestone detection - 30 days") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.isAtMilestone(30) shouldBe true
    }

    test("Milestone detection - non-milestone") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.isAtMilestone(5) shouldBe false
        manager.isAtMilestone(10) shouldBe false
        manager.isAtMilestone(20) shouldBe false
    }

    test("Next milestone calculation") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.getNextMilestone(0) shouldBe 3
        manager.getNextMilestone(2) shouldBe 3
        manager.getNextMilestone(3) shouldBe 7
        manager.getNextMilestone(5) shouldBe 7
        manager.getNextMilestone(7) shouldBe 14
        manager.getNextMilestone(14) shouldBe 30
        manager.getNextMilestone(30) shouldBe 60
        manager.getNextMilestone(60) shouldBe 90
        manager.getNextMilestone(90) shouldBe 365
    }

    test("Days until next milestone") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.getDaysUntilNextMilestone(0) shouldBe 3
        manager.getDaysUntilNextMilestone(1) shouldBe 2
        manager.getDaysUntilNextMilestone(3) shouldBe 4
        manager.getDaysUntilNextMilestone(7) shouldBe 7
        manager.getDaysUntilNextMilestone(14) shouldBe 16
    }

    test("Streak progress to next milestone") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.getStreakProgressToNextMilestone(0) shouldBe 0
        manager.getStreakProgressToNextMilestone(1) shouldBe 33 // 1/3 * 100
        manager.getStreakProgressToNextMilestone(3) shouldBe 0 // At milestone
        manager.getStreakProgressToNextMilestone(5) shouldBe 50 // 2/4 * 100
        manager.getStreakProgressToNextMilestone(7) shouldBe 0 // At milestone
    }

    test("Milestone descriptions") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        manager.getMilestoneDescription(3) shouldNotBe null
        manager.getMilestoneDescription(7) shouldNotBe null
        manager.getMilestoneDescription(14) shouldNotBe null
        manager.getMilestoneDescription(30) shouldNotBe null
        manager.getMilestoneDescription(365) shouldNotBe null
    }

    test("Streak calculation consistency") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // Streak progression
        var streak = 0
        for (day in 1..10) {
            val yesterday = LocalDate.now().minusDays((11 - day).toLong())
            val shouldIncrement = manager.shouldIncrementStreak(
                currentStreak = streak,
                lastCompletionDate = if (day == 1) null else yesterday,
                isDayComplete = true
            )

            if (shouldIncrement) {
                streak++
            }
        }

        streak shouldBe 10
    }

    test("Milestone progression") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val milestones = listOf(3, 7, 14, 30, 60, 90, 365)

        for (milestone in milestones) {
            manager.isAtMilestone(milestone) shouldBe true
        }
    }

    test("Progress calculation accuracy") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // At 0 days, progress to 3 is 0%
        manager.getStreakProgressToNextMilestone(0) shouldBe 0

        // At 1 day, progress to 3 is 33%
        manager.getStreakProgressToNextMilestone(1) shouldBe 33

        // At 2 days, progress to 3 is 66%
        manager.getStreakProgressToNextMilestone(2) shouldBe 66

        // At 3 days (milestone), progress resets to 0%
        manager.getStreakProgressToNextMilestone(3) shouldBe 0
    }

    test("Streak reset on gap") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // Day 1: Complete
        var lastCompletion = LocalDate.now()
        var streak = 1

        // Day 2: Complete (consecutive)
        lastCompletion = LocalDate.now()
        val shouldIncrement2 = manager.shouldIncrementStreak(
            currentStreak = streak,
            lastCompletionDate = LocalDate.now().minusDays(1),
            isDayComplete = true
        )
        if (shouldIncrement2) streak++

        // Day 3: Incomplete (gap)
        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = LocalDate.now().minusDays(1),
            isDayComplete = false
        )

        shouldReset shouldBe false // Still consecutive, no reset yet

        // Day 4: Incomplete (gap continues)
        val shouldReset2 = manager.shouldResetStreak(
            lastCompletionDate = LocalDate.now().minusDays(2),
            isDayComplete = false
        )

        shouldReset2 shouldBe true // Gap > 1 day, reset
    }
})
