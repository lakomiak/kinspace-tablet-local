package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Task Count Display (Property 12)
 *
 * Property 12: Task Count Display
 * - Display format: "X of Y complete"
 * - X = number of completed tasks
 * - Y = total number of tasks
 * - Display must be accurate and update in real-time
 * - Display must handle edge cases (0 tasks, all complete, none complete)
 */
class TaskCountDisplayPropertyTest : FunSpec({

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

    test("Property 12.1: Task count display format is correct") {
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

    test("Property 12.2: Empty task list displays 0 of 0") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val emptyTasks = emptyList<Task>()

        val display = progressTracker.getTaskCountDisplay(emptyTasks)

        display shouldBe "0 of 0 complete"
    }

    test("Property 12.3: All completed tasks display correctly") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { createTask(status = TaskStatus.COMPLETED) }

            val display = progressTracker.getTaskCountDisplay(tasks)

            display shouldBe "$count of $count complete"
        }
    }

    test("Property 12.4: No completed tasks display correctly") {
        checkAll(
            Arb.int(min = 1, max = 100)
        ) { count ->
            val progressTracker = ProgressTracker(MockStreakRepository())
            val tasks = (1..count).map { createTask(status = TaskStatus.INCOMPLETE) }

            val display = progressTracker.getTaskCountDisplay(tasks)

            display shouldBe "0 of $count complete"
        }
    }

    test("Property 12.5: Display contains 'of' separator") {
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

            display shouldContain " of "
        }
    }

    test("Property 12.6: Display contains 'complete' suffix") {
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

            display shouldContain "complete"
        }
    }

    test("Property 12.7: Display updates when task status changes") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = mutableListOf(
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        val initialDisplay = progressTracker.getTaskCountDisplay(tasks)
        initialDisplay shouldBe "0 of 3 complete"

        // Complete one task
        tasks[0] = tasks[0].copy(status = TaskStatus.COMPLETED)
        val afterOneComplete = progressTracker.getTaskCountDisplay(tasks)
        afterOneComplete shouldBe "1 of 3 complete"

        // Complete another task
        tasks[1] = tasks[1].copy(status = TaskStatus.COMPLETED)
        val afterTwoComplete = progressTracker.getTaskCountDisplay(tasks)
        afterTwoComplete shouldBe "2 of 3 complete"

        // Complete all tasks
        tasks[2] = tasks[2].copy(status = TaskStatus.COMPLETED)
        val allComplete = progressTracker.getTaskCountDisplay(tasks)
        allComplete shouldBe "3 of 3 complete"
    }

    test("Property 12.8: In-progress tasks are not counted as complete") {
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

            val display = progressTracker.getTaskCountDisplay(tasks)

            display shouldBe "$completedCount of ${completedCount + inProgressCount} complete"
        }
    }

    test("Property 12.9: Deleted tasks are not counted") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE, id = "deleted-task").copy(isDeleted = true)
        )

        val display = progressTracker.getTaskCountDisplay(tasks)

        // Should only count non-deleted tasks: 1 completed out of 2
        display shouldBe "1 of 2 complete"
    }

    test("Property 12.10: Display is deterministic") {
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

            val display1 = progressTracker.getTaskCountDisplay(tasks)
            val display2 = progressTracker.getTaskCountDisplay(tasks)

            display1 shouldBe display2
        }
    }

    test("Property 12.11: Completed count never exceeds total count") {
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
            val parts = display.split(" of ")
            val completed = parts[0].toInt()
            val total = parts[1].split(" ")[0].toInt()

            completed shouldBe <= total
        }
    }

    test("Property 12.12: Display format is human-readable") {
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

            // Should be readable: "X of Y complete"
            val regex = Regex("^\\d+ of \\d+ complete$")
            display shouldBe regex.find(display)?.value
        }
    }

    test("Property 12.13: Single task displays correctly") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        // Single incomplete task
        val incompleteTask = listOf(createTask(status = TaskStatus.INCOMPLETE))
        progressTracker.getTaskCountDisplay(incompleteTask) shouldBe "0 of 1 complete"

        // Single completed task
        val completedTask = listOf(createTask(status = TaskStatus.COMPLETED))
        progressTracker.getTaskCountDisplay(completedTask) shouldBe "1 of 1 complete"
    }

    test("Property 12.14: Large task counts display correctly") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = mutableListOf<Task>()

        // Add 100 completed tasks
        repeat(100) {
            tasks.add(createTask(status = TaskStatus.COMPLETED))
        }

        val display = progressTracker.getTaskCountDisplay(tasks)

        display shouldBe "100 of 100 complete"
    }
})
