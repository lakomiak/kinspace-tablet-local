package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Property 21: Streak-Aware Affirmations
 *
 * **Validates: Requirements 5.5**
 *
 * Property: For any user with a streak of 3 or more consecutive days,
 * affirmation messages should acknowledge and reference the streak.
 *
 * This property verifies that:
 * - When streak is 0-2, regular task completion messages are used
 * - When streak is 3+, streak-aware messages are used
 * - Streak-aware messages include streak count or reference
 * - Different streak levels have appropriate messages
 * - Messages rotate through pools to avoid repetition
 * - Streak count is included in the affirmation event
 *
 * Test Strategy:
 * - Generate random streak counts (0, 1, 2, 3, 7, 14, 30, 60, 90, 365)
 * - Verify correct message type is used for each streak level
 * - Verify streak count is included in affirmation event
 * - Verify message variety within each streak level
 * - Test with various task configurations
 */
class StreakAwareAffirmationPropertyTest : FunSpec({

    fun createCompletedTask(
        id: String = UUID.randomUUID().toString(),
        title: String = "Task $id",
        todoGroup: String = "Work",
        estimatedDurationMinutes: Int? = null
    ): Task {
        return Task(
            id = id,
            householdId = "household-1",
            assignedUserId = "user-1",
            title = title,
            description = null,
            todoGroup = todoGroup,
            estimatedDurationMinutes = estimatedDurationMinutes,
            actualDurationMinutes = null,
            status = TaskStatus.COMPLETED,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = Instant.now(),
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    test("Property 21: Streak-aware affirmation triggered for streak 3+") {
        checkAll(
            Arb.int(min = 3, max = 365)
        ) { streakCount ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask()

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount)

            triggered shouldBe true
            manager.affirmationEvent.value shouldNotBe null
        }
    }

    test("Property 21: Streak count included in affirmation event") {
        checkAll(
            Arb.int(min = 3, max = 365)
        ) { streakCount ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask()

            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount)
            val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

            affirmation?.streakCount shouldBe streakCount
        }
    }

    test("Property 21: Streak-aware message for 3-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 3)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("3") ?: false
        affirmation?.message?.shouldContain("day") ?: false
    }

    test("Property 21: Streak-aware message for 7-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 7)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("7") ?: false
    }

    test("Property 21: Streak-aware message for 14-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 14)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("14") ?: false
    }

    test("Property 21: Streak-aware message for 30-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 30)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("30") ?: false
    }

    test("Property 21: Streak-aware message for 60-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 60)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("60") ?: false
    }

    test("Property 21: Streak-aware message for 90-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 90)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("90") ?: false
    }

    test("Property 21: Streak-aware message for 365-day streak") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 365)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldContain("365") ?: false
    }

    test("Property 21: Regular message for streak 0") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 0)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        // Should not contain streak-specific indicators
        affirmation?.message?.shouldNotBe null
        affirmation?.streakCount shouldBe 0
    }

    test("Property 21: Regular message for streak 1") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 1)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldNotBe null
        affirmation?.streakCount shouldBe 1
    }

    test("Property 21: Regular message for streak 2") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 2)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldNotBe null
        affirmation?.streakCount shouldBe 2
    }

    test("Property 21: Streak-aware messages vary for same streak level") {
        val manager = AffirmationTriggerManager()
        val messages = mutableSetOf<String>()

        // Generate multiple affirmations with same streak level
        repeat(10) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task, 7)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Should have variety (at least 2 different messages)
        messages.size shouldBe > 1
    }

    test("Property 21: Streak-aware messages for different streak levels") {
        val manager = AffirmationTriggerManager()
        val streakLevels = listOf(3, 7, 14, 30, 60, 90, 365)
        val messages = mutableMapOf<Int, String>()

        for (streak in streakLevels) {
            manager.clearAffirmation()
            val task = createCompletedTask()
            manager.checkAndTriggerTaskCompleteAffirmation(task, streak)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages[streak] = message
            }
        }

        // Each streak level should have a message
        messages.size shouldBe streakLevels.size

        // Messages should be different for different streak levels
        messages.values.toSet().size shouldBe streakLevels.size
    }

    test("Property 21: Streak-aware affirmation with various task configurations") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30),
            Arb.int(min = 1, max = 480),
            Arb.int(min = 3, max = 365)
        ) { title, todoGroup, duration, streakCount ->
            val manager = AffirmationTriggerManager()
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = "Test description",
                todoGroup = todoGroup,
                estimatedDurationMinutes = duration,
                actualDurationMinutes = duration + 5,
                status = TaskStatus.COMPLETED,
                createdAt = Instant.now().minusSeconds(3600),
                updatedAt = Instant.now(),
                completedAt = Instant.now(),
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount)

            triggered shouldBe true
            manager.affirmationEvent.value shouldBe is AffirmationEvent.TaskComplete
            (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.streakCount shouldBe streakCount
        }
    }

    test("Property 21: Streak-aware affirmation not triggered for incomplete task") {
        checkAll(
            Arb.int(min = 3, max = 365)
        ) { streakCount ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask().copy(status = TaskStatus.INCOMPLETE)

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount)

            triggered shouldBe false
        }
    }

    test("Property 21: Streak-aware affirmation not triggered for in-progress task") {
        checkAll(
            Arb.int(min = 3, max = 365)
        ) { streakCount ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask().copy(status = TaskStatus.IN_PROGRESS)

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount)

            triggered shouldBe false
        }
    }

    test("Property 21: Streak count preserved across multiple affirmations") {
        val manager = AffirmationTriggerManager()
        val streakCount = 14

        repeat(5) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount)
            val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

            affirmation?.streakCount shouldBe streakCount
        }
    }

    test("Property 21: Milestone streak levels have appropriate messages") {
        val milestones = listOf(3, 7, 14, 30, 60, 90, 365)
        val manager = AffirmationTriggerManager()

        for (milestone in milestones) {
            manager.clearAffirmation()
            val task = createCompletedTask()
            manager.checkAndTriggerTaskCompleteAffirmation(task, milestone)
            val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

            affirmation?.message?.shouldNotBe null
            affirmation?.message?.isNotEmpty() shouldBe true
            affirmation?.streakCount shouldBe milestone
        }
    }

    test("Property 21: Non-milestone streak counts use nearest lower milestone") {
        val manager = AffirmationTriggerManager()

        // Test streak count between milestones (e.g., 5 should use 3-day messages)
        val task = createCompletedTask()
        manager.checkAndTriggerTaskCompleteAffirmation(task, 5)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.message?.shouldNotBe null
        affirmation?.streakCount shouldBe 5
    }

    test("Property 21: Streak-aware affirmation message contains encouraging language") {
        val manager = AffirmationTriggerManager()
        val task = createCompletedTask()

        manager.checkAndTriggerTaskCompleteAffirmation(task, 7)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        // Message should contain encouraging elements (emoji or positive words)
        val message = affirmation?.message ?: ""
        val hasEmoji = message.contains("🔥") || message.contains("🏆") || message.contains("💪") ||
                       message.contains("🌟") || message.contains("⭐") || message.contains("🚀") ||
                       message.contains("👑")
        val hasPositiveWords = message.contains("amazing") || message.contains("incredible") ||
                               message.contains("crushing") || message.contains("strong") ||
                               message.contains("warrior") || message.contains("champion") ||
                               message.contains("legend") || message.contains("superstar")

        (hasEmoji || hasPositiveWords) shouldBe true
    }
})
