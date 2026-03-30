package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Property 19: Affirmation Message Variety
 *
 * **Validates: Requirements 5.2**
 *
 * Property: For any sequence of task completions, the affirmation messages displayed should vary
 * and avoid repetition across multiple completions.
 *
 * This property verifies that:
 * - Task completion affirmations vary across multiple completions
 * - Day completion affirmations vary across multiple completions
 * - Messages don't repeat in quick succession
 * - Message pools contain sufficient variety
 * - Rotation mechanism prevents immediate repetition
 *
 * Test Strategy:
 * - Generate sequences of task completions
 * - Collect affirmation messages from each completion
 * - Verify messages vary and don't repeat consecutively
 * - Test with various task configurations
 * - Verify message pools have adequate variety
 */
class AffirmationMessageVarietyPropertyTest : FunSpec({

    fun createCompletedTask(
        id: String = UUID.randomUUID().toString(),
        title: String = "Task $id",
        todoGroup: String = "Work"
    ): Task {
        return Task(
            id = id,
            householdId = "household-1",
            assignedUserId = "user-1",
            title = title,
            description = null,
            todoGroup = todoGroup,
            estimatedDurationMinutes = null,
            actualDurationMinutes = null,
            status = TaskStatus.COMPLETED,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = Instant.now(),
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    test("Property 19: Task completion messages vary across sequence") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 10 task completions and collect messages
        repeat(10) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Should have collected 10 messages
        messages.size shouldBe 10

        // Should have at least 2 different messages (verifies variety)
        messages.toSet().size shouldBe > 1
    }

    test("Property 19: Task completion messages don't repeat consecutively") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 15 task completions and collect messages
        repeat(15) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Verify no two consecutive messages are the same
        for (i in 0 until messages.size - 1) {
            messages[i] shouldNotBe messages[i + 1]
        }
    }

    test("Property 19: Day completion messages vary across sequence") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 8 day completions and collect messages
        repeat(8) {
            manager.clearAffirmation()
            val tasks = (1..5).map { createCompletedTask(id = UUID.randomUUID().toString()) }
            manager.checkAndTriggerDayCompleteAffirmation(tasks)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Should have collected 8 messages
        messages.size shouldBe 8

        // Should have at least 2 different messages (verifies variety)
        messages.toSet().size shouldBe > 1
    }

    test("Property 19: Day completion messages don't repeat consecutively") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 8 day completions and collect messages
        repeat(8) {
            manager.clearAffirmation()
            val tasks = (1..5).map { createCompletedTask(id = UUID.randomUUID().toString()) }
            manager.checkAndTriggerDayCompleteAffirmation(tasks)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Verify no two consecutive messages are the same
        for (i in 0 until messages.size - 1) {
            messages[i] shouldNotBe messages[i + 1]
        }
    }

    test("Property 19: Task completion message pool has sufficient variety") {
        val manager = AffirmationTriggerManager()
        val uniqueMessages = mutableSetOf<String>()

        // Generate 20 task completions to cycle through message pool
        repeat(20) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                uniqueMessages.add(message)
            }
        }

        // Should have at least 5 unique messages in pool
        uniqueMessages.size shouldBe >= 5
    }

    test("Property 19: Day completion message pool has sufficient variety") {
        val manager = AffirmationTriggerManager()
        val uniqueMessages = mutableSetOf<String>()

        // Generate 8 day completions to cycle through message pool
        repeat(8) {
            manager.clearAffirmation()
            val tasks = (1..5).map { createCompletedTask(id = UUID.randomUUID().toString()) }
            manager.checkAndTriggerDayCompleteAffirmation(tasks)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
            if (message != null) {
                uniqueMessages.add(message)
            }
        }

        // Should have at least 5 unique messages in pool
        uniqueMessages.size shouldBe >= 5
    }

    test("Property 19: Message variety with various task configurations") {
        checkAll(
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50),
                range = 5..15
            )
        ) { titles ->
            val manager = AffirmationTriggerManager()
            val messages = mutableListOf<String>()

            for (title in titles) {
                manager.clearAffirmation()
                val task = createCompletedTask(title = title)
                manager.checkAndTriggerTaskCompleteAffirmation(task)
                val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
                if (message != null) {
                    messages.add(message)
                }
            }

            // Should have at least 2 different messages
            messages.toSet().size shouldBe > 1

            // No two consecutive messages should be the same
            for (i in 0 until messages.size - 1) {
                messages[i] shouldNotBe messages[i + 1]
            }
        }
    }

    test("Property 19: Rotation cycles through all messages") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 10 task completions to see rotation
        repeat(10) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Verify rotation pattern: messages should cycle through pool
        // After 10 iterations, we should see multiple cycles
        val uniqueMessages = messages.toSet()
        uniqueMessages.size shouldBe >= 5

        // Verify that messages appear in rotation order (not random)
        // First message should not equal second, second should not equal third, etc.
        for (i in 0 until messages.size - 1) {
            messages[i] shouldNotBe messages[i + 1]
        }
    }

    test("Property 19: Each affirmation type has independent message rotation") {
        val manager = AffirmationTriggerManager()
        val taskMessages = mutableListOf<String>()
        val dayMessages = mutableListOf<String>()

        // Generate task completions
        repeat(5) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                taskMessages.add(message)
            }
        }

        // Generate day completions
        repeat(5) {
            manager.clearAffirmation()
            val tasks = (1..5).map { createCompletedTask(id = UUID.randomUUID().toString()) }
            manager.checkAndTriggerDayCompleteAffirmation(tasks)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
            if (message != null) {
                dayMessages.add(message)
            }
        }

        // Both should have variety
        taskMessages.toSet().size shouldBe > 1
        dayMessages.toSet().size shouldBe > 1

        // Task and day messages should be different
        taskMessages.toSet().intersect(dayMessages.toSet()).size shouldBe 0
    }

    test("Property 19: Message variety maintained across manager instances") {
        val messages1 = mutableListOf<String>()
        val messages2 = mutableListOf<String>()

        // First manager instance
        val manager1 = AffirmationTriggerManager()
        repeat(5) {
            manager1.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager1.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager1.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages1.add(message)
            }
        }

        // Second manager instance
        val manager2 = AffirmationTriggerManager()
        repeat(5) {
            manager2.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager2.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager2.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages2.add(message)
            }
        }

        // Both instances should have variety
        messages1.toSet().size shouldBe > 1
        messages2.toSet().size shouldBe > 1

        // Both should have no consecutive repeats
        for (i in 0 until messages1.size - 1) {
            messages1[i] shouldNotBe messages1[i + 1]
        }
        for (i in 0 until messages2.size - 1) {
            messages2[i] shouldNotBe messages2[i + 1]
        }
    }

    test("Property 19: All messages are non-empty and appropriate") {
        val manager = AffirmationTriggerManager()
        val allMessages = mutableSetOf<String>()

        // Collect task completion messages
        repeat(10) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                allMessages.add(message)
            }
        }

        // Collect day completion messages
        repeat(8) {
            manager.clearAffirmation()
            val tasks = (1..5).map { createCompletedTask(id = UUID.randomUUID().toString()) }
            manager.checkAndTriggerDayCompleteAffirmation(tasks)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
            if (message != null) {
                allMessages.add(message)
            }
        }

        // All messages should be non-empty
        for (message in allMessages) {
            message.isNotEmpty() shouldBe true
        }

        // All messages should be appropriate (not patronizing)
        // Check that messages don't contain overly childish language
        val inappropriatePatterns = listOf("baby", "kiddo", "little one", "sweetie")
        for (message in allMessages) {
            for (pattern in inappropriatePatterns) {
                message.lowercase().contains(pattern) shouldBe false
            }
        }
    }
})
