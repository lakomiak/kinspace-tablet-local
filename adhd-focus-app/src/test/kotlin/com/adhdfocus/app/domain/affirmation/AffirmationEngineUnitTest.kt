package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.Affirmation
import com.adhdfocus.app.data.model.AffirmationType
import com.adhdfocus.app.data.repository.AffirmationRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

/**
 * Unit Tests for AffirmationEngine
 *
 * Tests verify:
 * - Task completion affirmation retrieval
 * - Day completion affirmation retrieval
 * - Streak milestone affirmation retrieval
 * - Random affirmation selection
 * - Age-appropriate affirmation filtering
 * - Fallback affirmations when repository is empty
 * - Message customization with streak count
 */
class AffirmationEngineUnitTest : FunSpec({

    val affirmationRepository = mock<AffirmationRepository>()
    val engine = AffirmationEngine(affirmationRepository)

    // ============ Task Completion Affirmation Tests ============

    test("Get task completion affirmation returns valid affirmation") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result shouldNotBe null
            result.type shouldBe AffirmationType.TASK_COMPLETION
            result.message shouldBe "Great job!"
        }
    }

    test("Get task completion affirmation returns random from pool") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "You're on a roll!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Awesome work!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val results = mutableSetOf<String>()
            repeat(10) {
                val result = engine.getTaskCompletionAffirmation()
                results.add(result.message)
            }

            // Should have variety
            results.size shouldBe > 1
        }
    }

    test("Get task completion affirmation returns fallback when empty") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result shouldNotBe null
            result.type shouldBe AffirmationType.TASK_COMPLETION
            result.message shouldBe "Great job!"
        }
    }

    test("Get task completion affirmation fallback has correct tone") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result.tone shouldBe "ENCOURAGING"
        }
    }

    test("Get task completion affirmation fallback has correct age level") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result.ageAppropriatenessLevel shouldBe 3
        }
    }

    // ============ Day Completion Affirmation Tests ============

    test("Get day completion affirmation returns valid affirmation") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Perfect day! You crushed it!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getDayCompletionAffirmation()

            result shouldNotBe null
            result.type shouldBe AffirmationType.DAY_COMPLETION
            result.message shouldBe "Perfect day! You crushed it!"
        }
    }

    test("Get day completion affirmation returns random from pool") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Perfect day!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "You crushed it!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Amazing day!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val results = mutableSetOf<String>()
            repeat(10) {
                val result = engine.getDayCompletionAffirmation()
                results.add(result.message)
            }

            // Should have variety
            results.size shouldBe > 1
        }
    }

    test("Get day completion affirmation returns fallback when empty") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getDayCompletionAffirmation()

            result shouldNotBe null
            result.type shouldBe AffirmationType.DAY_COMPLETION
            result.message shouldBe "Perfect day! You crushed it!"
        }
    }

    test("Get day completion affirmation fallback has correct tone") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getDayCompletionAffirmation()

            result.tone shouldBe "CELEBRATORY"
        }
    }

    test("Get day completion affirmation fallback has correct age level") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getDayCompletionAffirmation()

            result.ageAppropriatenessLevel shouldBe 4
        }
    }

    // ============ Streak Milestone Affirmation Tests ============

    test("Get streak milestone affirmation returns valid affirmation") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "{streak}-day streak! Keep it up!",
                tone = "MOTIVATIONAL",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result shouldNotBe null
            result.type shouldBe AffirmationType.STREAK_MILESTONE
        }
    }

    test("Get streak milestone affirmation customizes message with streak count") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "{streak}-day streak! Keep it up!",
                tone = "MOTIVATIONAL",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result.message shouldContain "7"
        }
    }

    test("Get streak milestone affirmation customizes message for different streaks") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "{streak}-day streak! Keep it up!",
                tone = "MOTIVATIONAL",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(affirmations)

        runBlocking {
            val result3 = engine.getStreakMilestoneAffirmation(3)
            val result7 = engine.getStreakMilestoneAffirmation(7)
            val result30 = engine.getStreakMilestoneAffirmation(30)

            result3.message shouldContain "3"
            result7.message shouldContain "7"
            result30.message shouldContain "30"
        }
    }

    test("Get streak milestone affirmation returns fallback when empty") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result shouldNotBe null
            result.type shouldBe AffirmationType.STREAK_MILESTONE
            result.message shouldContain "7"
        }
    }

    test("Get streak milestone affirmation fallback has correct tone") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result.tone shouldBe "MOTIVATIONAL"
        }
    }

    test("Get streak milestone affirmation fallback has correct age level") {
        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result.ageAppropriatenessLevel shouldBe 4
        }
    }

    // ============ Random Affirmation Tests ============

    test("Get random affirmation returns valid affirmation") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAllAffirmations())
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getRandomAffirmation()

            result shouldNotBe null
            result.message shouldBe "Great job!"
        }
    }

    test("Get random affirmation returns random from all types") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Perfect day!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "Streak!",
                tone = "MOTIVATIONAL",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAllAffirmations())
            .thenReturn(affirmations)

        runBlocking {
            val results = mutableSetOf<String>()
            repeat(10) {
                val result = engine.getRandomAffirmation()
                results.add(result.message)
            }

            // Should have variety
            results.size shouldBe > 1
        }
    }

    test("Get random affirmation returns fallback when empty") {
        whenever(affirmationRepository.getAllAffirmations())
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getRandomAffirmation()

            result shouldNotBe null
            result.type shouldBe AffirmationType.TASK_COMPLETION
            result.message shouldBe "You're doing great!"
        }
    }

    // ============ Age-Appropriate Affirmation Tests ============

    test("Get affirmations for age level returns correct affirmations") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Awesome work!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByAgeLevel(3))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getAffirmationsForAgeLevel(3)

            result.shouldHaveSize(2)
            result.forEach { it.ageAppropriatenessLevel shouldBe 3 }
        }
    }

    test("Get affirmations for age level 1 returns age-appropriate affirmations") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Good job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 1
            )
        )

        whenever(affirmationRepository.getAffirmationsByAgeLevel(1))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getAffirmationsForAgeLevel(1)

            result.shouldHaveSize(1)
            result[0].ageAppropriatenessLevel shouldBe 1
        }
    }

    test("Get affirmations for age level 5 returns age-appropriate affirmations") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Excellent execution!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 5
            )
        )

        whenever(affirmationRepository.getAffirmationsByAgeLevel(5))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getAffirmationsForAgeLevel(5)

            result.shouldHaveSize(1)
            result[0].ageAppropriatenessLevel shouldBe 5
        }
    }

    test("Get affirmations for age level returns empty when none available") {
        whenever(affirmationRepository.getAffirmationsByAgeLevel(3))
            .thenReturn(emptyList())

        runBlocking {
            val result = engine.getAffirmationsForAgeLevel(3)

            result.shouldBeEmpty()
        }
    }

    // ============ Affirmation Type Tests ============

    test("Task completion affirmations have correct type") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result.type shouldBe AffirmationType.TASK_COMPLETION
        }
    }

    test("Day completion affirmations have correct type") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Perfect day!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getDayCompletionAffirmation()

            result.type shouldBe AffirmationType.DAY_COMPLETION
        }
    }

    test("Streak milestone affirmations have correct type") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "Streak!",
                tone = "MOTIVATIONAL",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result.type shouldBe AffirmationType.STREAK_MILESTONE
        }
    }

    // ============ Message Content Tests ============

    test("Affirmation messages are non-empty") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result.message.isNotEmpty() shouldBe true
        }
    }

    test("Affirmation messages can contain special characters") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job! 🎉",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result.message shouldContain "🎉"
        }
    }

    // ============ Affirmation ID Tests ============

    test("Affirmations have unique IDs") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            ),
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Awesome work!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result1 = engine.getTaskCompletionAffirmation()
            val result2 = engine.getTaskCompletionAffirmation()

            result1.id shouldNotBe result2.id
        }
    }

    // ============ Tone Tests ============

    test("Task completion affirmations have encouraging tone") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.TASK_COMPLETION,
                message = "Great job!",
                tone = "ENCOURAGING",
                ageAppropriatenessLevel = 3
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.TASK_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getTaskCompletionAffirmation()

            result.tone shouldBe "ENCOURAGING"
        }
    }

    test("Day completion affirmations have celebratory tone") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.DAY_COMPLETION,
                message = "Perfect day!",
                tone = "CELEBRATORY",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.DAY_COMPLETION))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getDayCompletionAffirmation()

            result.tone shouldBe "CELEBRATORY"
        }
    }

    test("Streak milestone affirmations have motivational tone") {
        val affirmations = listOf(
            Affirmation(
                id = UUID.randomUUID().toString(),
                type = AffirmationType.STREAK_MILESTONE,
                message = "Streak!",
                tone = "MOTIVATIONAL",
                ageAppropriatenessLevel = 4
            )
        )

        whenever(affirmationRepository.getAffirmationsByType(AffirmationType.STREAK_MILESTONE))
            .thenReturn(affirmations)

        runBlocking {
            val result = engine.getStreakMilestoneAffirmation(7)

            result.tone shouldBe "MOTIVATIONAL"
        }
    }
})
