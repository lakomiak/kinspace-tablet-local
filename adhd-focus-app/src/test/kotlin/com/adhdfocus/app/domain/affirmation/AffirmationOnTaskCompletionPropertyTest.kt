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
 * Property-Based Tests for Property 18: Affirmation on Task Completion
 *
 * **Validates: Requirements 5.1**
 *
 * Property: When a task is completed, an affirmation message is triggered.
 *
 * This property verifies that:
 * - Completing any task triggers a task completion affirmation
 * - The affirmation event is of type TaskComplete
 * - The affirmation contains a non-empty message
 * - The affirmation includes the correct task ID
 * - The affirmation has a valid timestamp
 *
 * Test Strategy:
 * - Generate random tasks with COMPLETED status
 * - Verify affirmation is triggered for each completed task
 * - Verify affirmation properties are correct
 * - Test with various task configurations (different IDs, groups, durations)
 */
class AffirmationOnTaskCompletionPropertyTest : FunSpec({

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

    test("Property 18: Affirmation triggered for any completed task") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30),
            Arb.int(min = 1, max = 480)
        ) { title, todoGroup, duration ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask(
                title = title,
                todoGroup = todoGroup,
                estimatedDurationMinutes = duration
            )

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

            triggered shouldBe true
            manager.affirmationEvent.value shouldNotBe null
        }
    }

    test("Property 18: Affirmation event is TaskComplete type") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30)
        ) { title, todoGroup ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask(title = title, todoGroup = todoGroup)

            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
            val affirmation = manager.affirmationEvent.value

            affirmation shouldBe is AffirmationEvent.TaskComplete
        }
    }

    test("Property 18: Affirmation message is non-empty") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30)
        ) { title, todoGroup ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask(title = title, todoGroup = todoGroup)

            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
            val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

            affirmation?.message?.isNotEmpty() shouldBe true
        }
    }

    test("Property 18: Affirmation includes correct task ID") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30)
        ) { title, todoGroup ->
            val manager = AffirmationTriggerManager()
            val taskId = UUID.randomUUID().toString()
            val task = createCompletedTask(id = taskId, title = title, todoGroup = todoGroup)

            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
            val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

            affirmation?.taskId shouldBe taskId
        }
    }

    test("Property 18: Affirmation has valid timestamp") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30)
        ) { title, todoGroup ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask(title = title, todoGroup = todoGroup)
            val beforeTime = System.currentTimeMillis()

            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
            val affirmation = manager.affirmationEvent.value

            val afterTime = System.currentTimeMillis()

            affirmation?.timestamp shouldNotBe null
            affirmation?.timestamp shouldBe > 0L
            affirmation?.timestamp!! >= beforeTime shouldBe true
            affirmation?.timestamp!! <= afterTime + 100 shouldBe true
        }
    }

    test("Property 18: Affirmation triggered for multiple different tasks") {
        checkAll(
            Arb.list(
                Arb.string(minSize = 1, maxSize = 50),
                range = 1..10
            )
        ) { titles ->
            val manager = AffirmationTriggerManager()

            for (title in titles) {
                manager.clearAffirmation()
                val task = createCompletedTask(title = title)

                val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

                triggered shouldBe true
                manager.affirmationEvent.value shouldBe is AffirmationEvent.TaskComplete
            }
        }
    }

    test("Property 18: Affirmation not triggered for incomplete task") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30)
        ) { title, todoGroup ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask(title = title, todoGroup = todoGroup).copy(
                status = TaskStatus.INCOMPLETE
            )

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

            triggered shouldBe false
        }
    }

    test("Property 18: Affirmation not triggered for in-progress task") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30)
        ) { title, todoGroup ->
            val manager = AffirmationTriggerManager()
            val task = createCompletedTask(title = title, todoGroup = todoGroup).copy(
                status = TaskStatus.IN_PROGRESS
            )

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

            triggered shouldBe false
        }
    }

    test("Property 18: Affirmation messages vary across multiple completions") {
        val manager = AffirmationTriggerManager()
        val messages = mutableSetOf<String>()

        // Generate multiple affirmations and collect unique messages
        repeat(20) {
            manager.clearAffirmation()
            val task = createCompletedTask(id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Should have at least 2 different messages (verifies variety)
        messages.size shouldBe > 1
    }

    test("Property 18: Affirmation triggered with various task configurations") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 50),
            Arb.string(minSize = 1, maxSize = 30),
            Arb.int(min = 1, max = 480)
        ) { title, todoGroup, duration ->
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

            val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

            triggered shouldBe true
            manager.affirmationEvent.value shouldBe is AffirmationEvent.TaskComplete
        }
    }
})
