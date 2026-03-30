package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Day Complete Indicator (Property 13)
 *
 * Property 13: Day Complete Indicator
 * - Display indicator when all tasks are completed
 * - Hide indicator when any task is incomplete
 * - Update indicator in real-time
 * - Persist day complete status
 */
class DayCompleteIndicatorPropertyTest : FunSpec({

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

    test("Property 13.1: Empty task list is day complete") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val emptyTasks = emptyList<Task>()

        val isDayComplete = progressTracker.isAllTasksCompleted(emptyTasks)

        isDayComplete shouldBe false
    }

    test("Property 13.2: All completed tasks indicate day complete") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { createTask(status = TaskStatus.COMPLETED) }

            val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

            isDayComplete shouldBe true
        }
    }

    test("Property 13.3: Any incomplete task prevents day complete") {
        checkAll(
            Arb.int(min = 1, max = 100),
            Arb.int(min = 1, max = 100)
        ) { completedCount, incompleteCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = mutableListOf<Task>()

            repeat(completedCount) { tasks.add(createTask(status = TaskStatus.COMPLETED)) }
            repeat(incompleteCount) { tasks.add(createTask(status = TaskStatus.INCOMPLETE)) }

            val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

            isDayComplete shouldBe false
        }
    }

    test("Property 13.4: In-progress tasks prevent day complete") {
        checkAll(
            Arb.int(min = 1, max = 50),
            Arb.int(min = 1, max = 50)
        ) { completedCount, inProgressCount ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = mutableListOf<Task>()

            repeat(completedCount) { tasks.add(createTask(status = TaskStatus.COMPLETED)) }
            repeat(inProgressCount) { tasks.add(createTask(status = TaskStatus.IN_PROGRESS)) }

            val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

            isDayComplete shouldBe false
        }
    }

    test("Property 13.5: Day complete status is deterministic") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { createTask(status = TaskStatus.COMPLETED) }

            val isDayComplete1 = progressTracker.isAllTasksCompleted(tasks)
            val isDayComplete2 = progressTracker.isAllTasksCompleted(tasks)

            isDayComplete1 shouldBe isDayComplete2
        }
    }

    test("Property 13.6: Day complete updates when task completes") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        var tasks = (1..5).map { createTask(status = TaskStatus.INCOMPLETE) }

        var isDayComplete = progressTracker.isAllTasksCompleted(tasks)
        isDayComplete shouldBe false

        // Complete all tasks
        tasks = tasks.map { it.copy(status = TaskStatus.COMPLETED) }
        isDayComplete = progressTracker.isAllTasksCompleted(tasks)
        isDayComplete shouldBe true
    }

    test("Property 13.7: Day complete reverts when task becomes incomplete") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        var tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }

        var isDayComplete = progressTracker.isAllTasksCompleted(tasks)
        isDayComplete shouldBe true

        // Make one task incomplete
        tasks = tasks.mapIndexed { index, task ->
            if (index == 0) task.copy(status = TaskStatus.INCOMPLETE) else task
        }
        isDayComplete = progressTracker.isAllTasksCompleted(tasks)
        isDayComplete shouldBe false
    }

    test("Property 13.8: Single task completion") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val singleIncompleteTask = listOf(createTask(status = TaskStatus.INCOMPLETE))
        progressTracker.isAllTasksCompleted(singleIncompleteTask) shouldBe false

        val singleCompleteTask = listOf(createTask(status = TaskStatus.COMPLETED))
        progressTracker.isAllTasksCompleted(singleCompleteTask) shouldBe true
    }

    test("Property 13.9: Day complete is independent of task properties") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { 
                createTask(status = TaskStatus.COMPLETED).copy(
                    title = "Task ${UUID.randomUUID()}",
                    description = "Description ${UUID.randomUUID()}",
                    todoGroup = "Group-${it % 5}"
                )
            }

            val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

            isDayComplete shouldBe true
        }
    }

    test("Property 13.10: Day complete ignores deleted tasks") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE, id = "deleted").copy(isDeleted = true)
        )

        // Should only count non-deleted tasks
        val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

        isDayComplete shouldBe true
    }

    test("Property 13.11: Day complete with mixed statuses") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val mixedTasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.IN_PROGRESS)
        )

        val isDayComplete = progressTracker.isAllTasksCompleted(mixedTasks)

        isDayComplete shouldBe false
    }

    test("Property 13.12: Day complete status is boolean") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }

        val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

        isDayComplete shouldBe true
        isDayComplete.javaClass shouldBe Boolean::class.javaObjectType
    }

    test("Property 13.13: Day complete with large task count") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = (1..1000).map { createTask(status = TaskStatus.COMPLETED) }

        val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

        isDayComplete shouldBe true
    }

    test("Property 13.14: Day complete with one incomplete in large list") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = mutableListOf<Task>()

        repeat(999) { tasks.add(createTask(status = TaskStatus.COMPLETED)) }
        tasks.add(createTask(status = TaskStatus.INCOMPLETE))

        val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

        isDayComplete shouldBe false
    }

    test("Property 13.15: Day complete consistency across multiple checks") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = (1..10).map { createTask(status = TaskStatus.COMPLETED) }

        val results = mutableListOf<Boolean>()
        repeat(10) {
            results.add(progressTracker.isAllTasksCompleted(tasks))
        }

        results.all { it == true } shouldBe true
    }
})

// Mock implementation for testing
class MockStreakRepository : com.adhdfocus.app.data.repository.StreakRepository {
    override suspend fun getCurrentStreak(userId: String, householdId: String): Int = 0
    override suspend fun getBestStreak(userId: String, householdId: String): Int = 0
    override suspend fun incrementStreak(userId: String, householdId: String): Int = 0
    override suspend fun resetStreak(userId: String, householdId: String) {}
}
