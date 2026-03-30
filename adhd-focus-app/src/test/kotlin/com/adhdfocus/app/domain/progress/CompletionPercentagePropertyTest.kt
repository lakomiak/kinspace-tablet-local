package com.adhdfocus.app.domain.progress

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
 * Property-Based Tests for Completion Percentage Calculation (Property 11)
 *
 * Property 11: Completion Percentage Calculation
 * - Completion percentage = (completed tasks / total tasks) * 100
 * - Percentage must be between 0 and 100
 * - Empty task list should return 0%
 * - All completed tasks should return 100%
 * - Percentage should update in real-time as tasks are completed
 */
class CompletionPercentagePropertyTest : FunSpec({

    fun createTask(
        status: TaskStatus = TaskStatus.INCOMPLETE,
        id: String = UUID.randomUUID().toString()
    ): Task {
        return Task(
            id = id,
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Task $id",
            description = null,
            todoGroup = "Work",
            estimatedDurationMinutes = null,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    test("Property 11.1: Empty task list returns 0%") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val emptyTasks = emptyList<Task>()

        val percentage = progressTracker.calculateCompletionPercentage(emptyTasks)

        percentage shouldBe 0
    }

    test("Property 11.2: All completed tasks return 100%") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { createTask(status = TaskStatus.COMPLETED) }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)

            percentage shouldBe 100
        }
    }

    test("Property 11.3: No completed tasks return 0%") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { createTask(status = TaskStatus.INCOMPLETE) }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)

            percentage shouldBe 0
        }
    }

    test("Property 11.4: Percentage is between 0 and 100") {
        checkAll(
            Arb.int(min = 1, max = 100),
            Arb.int(min = 0, max = 100)
        ) { totalCount, completedCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val actualCompletedCount = minOf(completedCount, totalCount)
            val tasks = mutableListOf<Task>()

            // Add completed tasks
            repeat(actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.COMPLETED))
            }

            // Add incomplete tasks
            repeat(totalCount - actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.INCOMPLETE))
            }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)

            percentage shouldBe in(0..100)
        }
    }

    test("Property 11.5: Percentage calculation is accurate") {
        checkAll(
            Arb.int(min = 1, max = 100),
            Arb.int(min = 0, max = 100)
        ) { totalCount, completedCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val actualCompletedCount = minOf(completedCount, totalCount)
            val tasks = mutableListOf<Task>()

            // Add completed tasks
            repeat(actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.COMPLETED))
            }

            // Add incomplete tasks
            repeat(totalCount - actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.INCOMPLETE))
            }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)
            val expectedPercentage = (actualCompletedCount * 100) / totalCount

            percentage shouldBe expectedPercentage
        }
    }

    test("Property 11.6: In-progress tasks don't count as completed") {
        checkAll(
            Arb.int(min = 1, max = 50),
            Arb.int(min = 1, max = 50)
        ) { completedCount, inProgressCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = mutableListOf<Task>()

            // Add completed tasks
            repeat(completedCount) {
                tasks.add(createTask(status = TaskStatus.COMPLETED))
            }

            // Add in-progress tasks
            repeat(inProgressCount) {
                tasks.add(createTask(status = TaskStatus.IN_PROGRESS))
            }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)
            val expectedPercentage = (completedCount * 100) / (completedCount + inProgressCount)

            percentage shouldBe expectedPercentage
        }
    }

    test("Property 11.7: Percentage updates when task status changes") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = mutableListOf(
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        val initialPercentage = progressTracker.calculateCompletionPercentage(tasks)
        initialPercentage shouldBe 0

        // Complete one task
        tasks[0] = tasks[0].copy(status = TaskStatus.COMPLETED)
        val afterOneComplete = progressTracker.calculateCompletionPercentage(tasks)
        afterOneComplete shouldBe 33

        // Complete another task
        tasks[1] = tasks[1].copy(status = TaskStatus.COMPLETED)
        val afterTwoComplete = progressTracker.calculateCompletionPercentage(tasks)
        afterTwoComplete shouldBe 66

        // Complete all tasks
        tasks[2] = tasks[2].copy(status = TaskStatus.COMPLETED)
        val allComplete = progressTracker.calculateCompletionPercentage(tasks)
        allComplete shouldBe 100
    }

    test("Property 11.8: Deleted tasks are not counted") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE, id = "deleted-task").copy(isDeleted = true)
        )

        val percentage = progressTracker.calculateCompletionPercentage(tasks)

        // Should only count non-deleted tasks: 1 completed out of 2 = 50%
        percentage shouldBe 50
    }

    test("Property 11.9: Task count display is accurate") {
        checkAll(
            Arb.int(min = 1, max = 100),
            Arb.int(min = 0, max = 100)
        ) { totalCount, completedCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val actualCompletedCount = minOf(completedCount, totalCount)
            val tasks = mutableListOf<Task>()

            // Add completed tasks
            repeat(actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.COMPLETED))
            }

            // Add incomplete tasks
            repeat(totalCount - actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.INCOMPLETE))
            }

            val display = progressTracker.getTaskCountDisplay(tasks)

            display shouldBe "$actualCompletedCount of $totalCount complete"
        }
    }

    test("Property 11.10: Day complete indicator is accurate") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val allCompletedTasks = (1..count).map { createTask(status = TaskStatus.COMPLETED) }

            val isComplete = progressTracker.isAllTasksCompleted(allCompletedTasks)

            isComplete shouldBe true
        }
    }

    test("Property 11.11: Day not complete when any task incomplete") {
        checkAll(
            Arb.int(min = 1, max = 100),
            Arb.int(min = 1, max = 100)
        ) { completedCount, incompleteCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = mutableListOf<Task>()

            // Add completed tasks
            repeat(completedCount) {
                tasks.add(createTask(status = TaskStatus.COMPLETED))
            }

            // Add incomplete tasks
            repeat(incompleteCount) {
                tasks.add(createTask(status = TaskStatus.INCOMPLETE))
            }

            val isComplete = progressTracker.isAllTasksCompleted(tasks)

            isComplete shouldBe false
        }
    }

    test("Property 11.12: Empty task list is not complete") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val emptyTasks = emptyList<Task>()

        val isComplete = progressTracker.isAllTasksCompleted(emptyTasks)

        isComplete shouldBe false
    }

    test("Property 11.13: Percentage is monotonically increasing") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = mutableListOf(
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        var previousPercentage = 0
        for (i in tasks.indices) {
            tasks[i] = tasks[i].copy(status = TaskStatus.COMPLETED)
            val currentPercentage = progressTracker.calculateCompletionPercentage(tasks)
            currentPercentage shouldBe >= previousPercentage
            previousPercentage = currentPercentage
        }
    }

    test("Property 11.14: Percentage calculation is deterministic") {
        checkAll(
            Arb.int(min = 1, max = 100),
            Arb.int(min = 0, max = 100)
        ) { totalCount, completedCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val actualCompletedCount = minOf(completedCount, totalCount)
            val tasks = mutableListOf<Task>()

            // Add completed tasks
            repeat(actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.COMPLETED))
            }

            // Add incomplete tasks
            repeat(totalCount - actualCompletedCount) {
                tasks.add(createTask(status = TaskStatus.INCOMPLETE))
            }

            val percentage1 = progressTracker.calculateCompletionPercentage(tasks)
            val percentage2 = progressTracker.calculateCompletionPercentage(tasks)

            percentage1 shouldBe percentage2
        }
    }
})

// Mock implementation for testing
class MockStreakRepository : com.adhdfocus.app.data.repository.StreakRepository {
    override suspend fun getCurrentStreak(userId: String, householdId: String): Int = 0
    override suspend fun getBestStreak(userId: String, householdId: String): Int = 0
    override suspend fun incrementStreak(userId: String, householdId: String): Int = 0
    override suspend fun resetStreak(userId: String, householdId: String) {}
}
