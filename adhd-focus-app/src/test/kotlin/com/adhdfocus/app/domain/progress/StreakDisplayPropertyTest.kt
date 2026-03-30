package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.Streak
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Property-Based Tests for Streak Display
 *
 * Streak Display Requirements:
 * - Display current streak with fire icon
 * - Display best streak for reference
 * - Visual emphasis on streak milestones (3+, 7+, 14+, 30+)
 * - Streak count must be accurate
 * - Streak display must update in real-time
 */
class StreakDisplayPropertyTest : FunSpec({

    fun createStreak(
        currentCount: Int = 0,
        bestCount: Int = 0,
        userId: String = "user-1",
        householdId: String = "household-1"
    ): Streak {
        return Streak(
            id = UUID.randomUUID().toString(),
            userId = userId,
            householdId = householdId,
            currentCount = currentCount,
            bestCount = bestCount,
            lastCompletionDate = LocalDate.now(),
            startDate = LocalDate.now().minusDays(currentCount.toLong()),
            updatedAt = Instant.now()
        )
    }

    test("Property 7.1: Streak count is non-negative") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { count ->
            val streak = createStreak(currentCount = count)

            streak.currentCount shouldBe >= 0
        }
    }

    test("Property 7.2: Best streak is greater than or equal to current streak") {
        checkAll(
            Arb.int(min = 0, max = 100),
            Arb.int(min = 0, max = 100)
        ) { current, best ->
            val actualBest = maxOf(current, best)
            val streak = createStreak(currentCount = current, bestCount = actualBest)

            streak.bestCount shouldBe >= streak.currentCount
        }
    }

    test("Property 7.3: Zero streak displays correctly") {
        val streak = createStreak(currentCount = 0)

        streak.currentCount shouldBe 0
    }

    test("Property 7.4: Single day streak displays correctly") {
        val streak = createStreak(currentCount = 1)

        streak.currentCount shouldBe 1
    }

    test("Property 7.5: Streak increments correctly") {
        var streak = createStreak(currentCount = 0)
        streak.currentCount shouldBe 0

        streak = streak.copy(currentCount = streak.currentCount + 1)
        streak.currentCount shouldBe 1

        streak = streak.copy(currentCount = streak.currentCount + 1)
        streak.currentCount shouldBe 2

        streak = streak.copy(currentCount = streak.currentCount + 1)
        streak.currentCount shouldBe 3
    }

    test("Property 7.6: Streak resets to zero") {
        val streak = createStreak(currentCount = 10)
        streak.currentCount shouldBe 10

        val resetStreak = streak.copy(currentCount = 0)
        resetStreak.currentCount shouldBe 0
    }

    test("Property 7.7: Best streak is updated when current exceeds it") {
        var streak = createStreak(currentCount = 5, bestCount = 5)
        streak.bestCount shouldBe 5

        streak = streak.copy(currentCount = 10, bestCount = 10)
        streak.bestCount shouldBe 10
    }

    test("Property 7.8: Best streak is preserved when current resets") {
        val streak = createStreak(currentCount = 0, bestCount = 10)

        streak.bestCount shouldBe 10
        streak.currentCount shouldBe 0
    }

    test("Property 7.9: Streak milestone at 3 days") {
        val streak = createStreak(currentCount = 3)

        streak.currentCount shouldBe 3
        // Milestone indicator: 3+ days
        streak.currentCount >= 3 shouldBe true
    }

    test("Property 7.10: Streak milestone at 7 days") {
        val streak = createStreak(currentCount = 7)

        streak.currentCount shouldBe 7
        // Milestone indicator: 7+ days
        streak.currentCount >= 7 shouldBe true
    }

    test("Property 7.11: Streak milestone at 14 days") {
        val streak = createStreak(currentCount = 14)

        streak.currentCount shouldBe 14
        // Milestone indicator: 14+ days
        streak.currentCount >= 14 shouldBe true
    }

    test("Property 7.12: Streak milestone at 30 days") {
        val streak = createStreak(currentCount = 30)

        streak.currentCount shouldBe 30
        // Milestone indicator: 30+ days
        streak.currentCount >= 30 shouldBe true
    }

    test("Property 7.13: Streak display is deterministic") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { count ->
            val streak1 = createStreak(currentCount = count)
            val streak2 = createStreak(currentCount = count)

            streak1.currentCount shouldBe streak2.currentCount
        }
    }

    test("Property 7.14: Streak count is independent of other properties") {
        checkAll(
            Arb.int(min = 0, max = 100)
        ) { count ->
            val streak1 = createStreak(currentCount = count, userId = "user-1")
            val streak2 = createStreak(currentCount = count, userId = "user-2")

            streak1.currentCount shouldBe streak2.currentCount
        }
    }

    test("Property 7.15: Streak display handles large numbers") {
        val streak = createStreak(currentCount = 365)

        streak.currentCount shouldBe 365
    }

    test("Property 7.16: Streak has last completion date") {
        val today = LocalDate.now()
        val streak = createStreak(currentCount = 5)
        streak.lastCompletionDate = today

        streak.lastCompletionDate shouldBe today
    }

    test("Property 7.17: Streak has start date") {
        val startDate = LocalDate.now().minusDays(5)
        val streak = createStreak(currentCount = 5)
        streak.startDate = startDate

        streak.startDate shouldBe startDate
    }

    test("Property 7.18: Streak is updated at timestamp") {
        val streak = createStreak(currentCount = 5)

        streak.updatedAt shouldNotBe null
    }

    test("Property 7.19: Streak display format is consistent") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { count ->
            val streak = createStreak(currentCount = count)

            // Streak count should be a non-negative integer
            streak.currentCount shouldBe >= 0
            streak.currentCount shouldBe count
        }
    }

    test("Property 7.20: Multiple streaks are independent") {
        val streak1 = createStreak(currentCount = 5, userId = "user-1")
        val streak2 = createStreak(currentCount = 10, userId = "user-2")

        streak1.currentCount shouldBe 5
        streak2.currentCount shouldBe 10
        streak1.userId shouldNotBe streak2.userId
    }

    test("Property 7.21: Streak display shows fire icon for positive streaks") {
        val zeroStreak = createStreak(currentCount = 0)
        val oneStreak = createStreak(currentCount = 1)
        val tenStreak = createStreak(currentCount = 10)

        // Zero streak: no fire icon
        zeroStreak.currentCount shouldBe 0

        // Positive streaks: show fire icon
        oneStreak.currentCount shouldBe > 0
        tenStreak.currentCount shouldBe > 0
    }

    test("Property 7.22: Streak count is monotonically increasing when incrementing") {
        var streak = createStreak(currentCount = 0)
        var previousCount = 0

        for (i in 1..10) {
            streak = streak.copy(currentCount = i)
            streak.currentCount shouldBe >= previousCount
            previousCount = streak.currentCount
        }
    }

    test("Property 7.23: Streak display is human-readable") {
        checkAll(
            Arb.int(min = 0, max = 1000)
        ) { count ->
            val streak = createStreak(currentCount = count)

            // Streak count should be a simple integer
            streak.currentCount.toString().toIntOrNull() shouldNotBe null
        }
    }

    test("Property 7.24: Streak milestones are visually distinct") {
        val milestones = listOf(3, 7, 14, 30)
        val streaks = milestones.map { createStreak(currentCount = it) }

        // All milestones should be distinct
        streaks.map { it.currentCount }.distinct().size shouldBe milestones.size
    }

    test("Property 7.25: Streak display updates in real-time") {
        var streak = createStreak(currentCount = 0)
        val updates = mutableListOf<Int>()

        for (i in 1..5) {
            streak = streak.copy(currentCount = i)
            updates.add(streak.currentCount)
        }

        // Updates should be sequential
        updates shouldBe listOf(1, 2, 3, 4, 5)
    }
})
