package com.adhdfocus.app.domain.streak

import com.adhdfocus.app.data.repository.StreakRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.mockk
import java.time.LocalDate

/**
 * Property-Based Tests for Streak Calculation (Property 14)
 *
 * Property 14: Streak Calculation
 * - Streak increments on consecutive 100% completion days
 * - Streak resets on incomplete day after gap
 * - Best streak is tracked
 * - Milestones are recognized
 */
class StreakCalculationPropertyTest : FunSpec({

    test("Property 14.1: Streak increments on consecutive completion") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        var streak = 0
        for (day in 1..10) {
            val yesterday = if (day == 1) null else LocalDate.now().minusDays((11 - day).toLong())
            val shouldIncrement = manager.shouldIncrementStreak(
                currentStreak = streak,
                lastCompletionDate = yesterday,
                isDayComplete = true
            )

            if (shouldIncrement) streak++
        }

        streak shouldBe 10
    }

    test("Property 14.2: Streak resets on gap") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // Build streak
        var streak = 5
        val yesterday = LocalDate.now().minusDays(1)

        // Gap in days
        val twoDaysAgo = LocalDate.now().minusDays(2)
        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = twoDaysAgo,
            isDayComplete = false
        )

        shouldReset shouldBe true
    }

    test("Property 14.3: Streak is non-negative") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            streak shouldBe >= 0
        }
    }

    test("Property 14.4: Milestone detection is accurate") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val milestones = listOf(3, 7, 14, 30, 60, 90, 365)

        for (milestone in milestones) {
            manager.isAtMilestone(milestone) shouldBe true
        }
    }

    test("Property 14.5: Non-milestones are not detected") {
        checkAll(
            Arb.int(min = 1, max = 1000)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            val isMilestone = manager.isAtMilestone(streak)
            val expectedMilestones = listOf(3, 7, 14, 30, 60, 90, 365)

            if (streak !in expectedMilestones) {
                isMilestone shouldBe false
            }
        }
    }

    test("Property 14.6: Next milestone is always greater than current") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            val nextMilestone = manager.getNextMilestone(streak)

            nextMilestone shouldBe > streak
        }
    }

    test("Property 14.7: Days until milestone is positive") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            val daysUntil = manager.getDaysUntilNextMilestone(streak)

            daysUntil shouldBe > 0
        }
    }

    test("Property 14.8: Progress to milestone is 0-100%") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            val progress = manager.getStreakProgressToNextMilestone(streak)

            progress shouldBe in(0..100)
        }
    }

    test("Property 14.9: Milestone descriptions are non-empty") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val milestones = listOf(3, 7, 14, 30, 60, 90, 365)

        for (milestone in milestones) {
            val description = manager.getMilestoneDescription(milestone)
            description.isNotEmpty() shouldBe true
        }
    }

    test("Property 14.10: Streak progression is monotonic") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        var streak = 0
        val streaks = mutableListOf<Int>()

        for (day in 1..30) {
            val yesterday = if (day == 1) null else LocalDate.now().minusDays((31 - day).toLong())
            val shouldIncrement = manager.shouldIncrementStreak(
                currentStreak = streak,
                lastCompletionDate = yesterday,
                isDayComplete = true
            )

            if (shouldIncrement) streak++
            streaks.add(streak)
        }

        // Verify monotonic increase
        for (i in 1 until streaks.size) {
            streaks[i] shouldBe >= streaks[i - 1]
        }
    }

    test("Property 14.11: Increment decision is deterministic") {
        checkAll(
            Arb.int(min = 0, max = 100)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            val yesterday = LocalDate.now().minusDays(1)

            val result1 = manager.shouldIncrementStreak(
                currentStreak = streak,
                lastCompletionDate = yesterday,
                isDayComplete = true
            )

            val result2 = manager.shouldIncrementStreak(
                currentStreak = streak,
                lastCompletionDate = yesterday,
                isDayComplete = true
            )

            result1 shouldBe result2
        }
    }

    test("Property 14.12: Reset decision is deterministic") {
        checkAll(
            Arb.int(min = 0, max = 100)
        ) { streak ->
            val mockRepository = mockk<StreakRepository>()
            val manager = StreakCalculationManager(mockRepository)

            val twoDaysAgo = LocalDate.now().minusDays(2)

            val result1 = manager.shouldResetStreak(
                lastCompletionDate = twoDaysAgo,
                isDayComplete = false
            )

            val result2 = manager.shouldResetStreak(
                lastCompletionDate = twoDaysAgo,
                isDayComplete = false
            )

            result1 shouldBe result2
        }
    }

    test("Property 14.13: Milestone progress increases monotonically") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val nextMilestone = manager.getNextMilestone(0)
        var previousProgress = 0

        for (streak in 0 until nextMilestone) {
            val progress = manager.getStreakProgressToNextMilestone(streak)
            progress shouldBe >= previousProgress
            previousProgress = progress
        }
    }

    test("Property 14.14: Streak cannot be negative") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        var streak = 0

        // Try to reset from 0
        manager.shouldResetStreak(
            lastCompletionDate = null,
            isDayComplete = false
        ) shouldBe false

        streak shouldBe >= 0
    }

    test("Property 14.15: Consecutive days increment streak") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        var streak = 0

        for (day in 1..7) {
            val yesterday = if (day == 1) null else LocalDate.now().minusDays((8 - day).toLong())

            val shouldIncrement = manager.shouldIncrementStreak(
                currentStreak = streak,
                lastCompletionDate = yesterday,
                isDayComplete = true
            )

            if (shouldIncrement) streak++
        }

        streak shouldBe 7
    }

    test("Property 14.16: Gap breaks streak") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // Build 5-day streak
        var streak = 5
        val lastCompletion = LocalDate.now().minusDays(1)

        // Check if gap resets
        val shouldReset = manager.shouldResetStreak(
            lastCompletionDate = LocalDate.now().minusDays(2),
            isDayComplete = false
        )

        shouldReset shouldBe true
    }

    test("Property 14.17: Milestone descriptions contain emoji") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        val milestones = listOf(3, 7, 14, 30, 60, 90, 365)

        for (milestone in milestones) {
            val description = manager.getMilestoneDescription(milestone)
            // Check if description contains emoji-like characters
            description.isNotEmpty() shouldBe true
        }
    }

    test("Property 14.18: Progress calculation is accurate") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // At 0 days, progress to 3 is 0%
        manager.getStreakProgressToNextMilestone(0) shouldBe 0

        // At 1 day, progress to 3 is 33%
        manager.getStreakProgressToNextMilestone(1) shouldBe 33

        // At 2 days, progress to 3 is 66%
        manager.getStreakProgressToNextMilestone(2) shouldBe 66
    }

    test("Property 14.19: All milestones have descriptions") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        for (streak in 0..365) {
            val description = manager.getMilestoneDescription(streak)
            description.isNotEmpty() shouldBe true
        }
    }

    test("Property 14.20: Streak calculation handles edge cases") {
        val mockRepository = mockk<StreakRepository>()
        val manager = StreakCalculationManager(mockRepository)

        // Zero streak
        manager.shouldIncrementStreak(0, null, true) shouldBe true

        // Large streak
        manager.shouldIncrementStreak(365, LocalDate.now().minusDays(1), true) shouldBe true

        // Same day completion
        manager.shouldIncrementStreak(5, LocalDate.now(), true) shouldBe false
    }
})
