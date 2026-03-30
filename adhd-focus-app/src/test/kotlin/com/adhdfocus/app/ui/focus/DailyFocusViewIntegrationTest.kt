package com.adhdfocus.app.ui.focus

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.progress.ProgressTracker
import com.adhdfocus.app.domain.task.TaskFilterManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Integration Tests for Daily Focus View
 *
 * Tests the complete workflow of:
 * 1. Loading today's tasks
 * 2. Filtering and organizing tasks
 * 3. Calculating progress metrics
 * 4. Displaying task status with visual cues
 * 5. Handling task completion
 * 6. Updating progress in real-time
 */
class DailyFocusViewIntegrationTest : FunSpec({

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        status: TaskStatus = TaskStatus.INCOMPLETE,
        todoGroup: String = "Work",
        createdAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = "household-1",
            assignedUserId = "user-1",
            title = "Task $id",
            description = "Description for task $id",
            todoGroup = todoGroup,
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    test("Integration 1: Load today's tasks and display progress") {
        val filterManager = TaskFilterManager()
        val progressTracker = ProgressTracker(MockStreakRepository())

        val allTasks = (1..10).map { createTask(id = "task-$it") }
        val todaysTasks = filterManager.filterTodaysTasks(allTasks)
        val completionPercentage = progressTracker.calculateCompletionPercentage(todaysTasks)

        todaysTasks.size shouldBe 10
        completionPercentage shouldBe 0
    }

    test("Integration 2: Filter and organize tasks by group") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", todoGroup = "Work"),
            createTask(id = "task-2", todoGroup = "Personal"),
            createTask(id = "task-3", todoGroup = "Work"),
            createTask(id = "task-4", todoGroup = "Health")
        )

        val organized = filterManager.organizeByTodoGroup(tasks)

        organized.size shouldBe 3
        organized["Work"]?.size shouldBe 2
        organized["Personal"]?.size shouldBe 1
        organized["Health"]?.size shouldBe 1
    }

    test("Integration 3: Complete a task and update progress") {
        val filterManager = TaskFilterManager()
        val progressTracker = ProgressTracker(MockStreakRepository())

        var tasks = (1..5).map { createTask(id = "task-$it") }
        var percentage = progressTracker.calculateCompletionPercentage(tasks)
        percentage shouldBe 0

        // Complete first task
        tasks = tasks.mapIndexed { index, task ->
            if (index == 0) task.copy(status = TaskStatus.COMPLETED) else task
        }

        percentage = progressTracker.calculateCompletionPercentage(tasks)
        percentage shouldBe 20
    }

    test("Integration 4: Display task count with completion") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.COMPLETED),
            createTask(id = "task-2", status = TaskStatus.COMPLETED),
            createTask(id = "task-3", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-4", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-5", status = TaskStatus.INCOMPLETE)
        )

        val display = progressTracker.getTaskCountDisplay(tasks)

        display shouldBe "2 of 5 complete"
    }

    test("Integration 5: Handle mixed task statuses") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-2", status = TaskStatus.IN_PROGRESS),
            createTask(id = "task-3", status = TaskStatus.COMPLETED),
            createTask(id = "task-4", status = TaskStatus.INCOMPLETE)
        )

        val incomplete = filterManager.filterIncomplete(tasks)
        val inProgress = filterManager.filterInProgress(tasks)
        val completed = filterManager.filterCompleted(tasks)

        incomplete.size shouldBe 2
        inProgress.size shouldBe 1
        completed.size shouldBe 1
    }

    test("Integration 6: Display day complete indicator") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val allCompletedTasks = (1..5).map { 
            createTask(id = "task-$it", status = TaskStatus.COMPLETED)
        }

        val isComplete = progressTracker.isAllTasksCompleted(allCompletedTasks)

        isComplete shouldBe true
    }

    test("Integration 7: Handle empty task list") {
        val filterManager = TaskFilterManager()
        val progressTracker = ProgressTracker(MockStreakRepository())

        val emptyTasks = emptyList<Task>()
        val todaysTasks = filterManager.filterTodaysTasks(emptyTasks)
        val percentage = progressTracker.calculateCompletionPercentage(todaysTasks)

        todaysTasks.size shouldBe 0
        percentage shouldBe 0
    }

    test("Integration 8: Filter pending sync tasks") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.INCOMPLETE).copy(syncStatus = SyncStatus.PENDING),
            createTask(id = "task-2", status = TaskStatus.INCOMPLETE).copy(syncStatus = SyncStatus.SYNCED),
            createTask(id = "task-3", status = TaskStatus.INCOMPLETE).copy(syncStatus = SyncStatus.PENDING)
        )

        val pendingTasks = filterManager.filterPendingSync(tasks)

        pendingTasks.size shouldBe 2
    }

    test("Integration 9: Sort tasks by status") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.COMPLETED),
            createTask(id = "task-2", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-3", status = TaskStatus.IN_PROGRESS),
            createTask(id = "task-4", status = TaskStatus.INCOMPLETE)
        )

        val sorted = filterManager.sortByStatus(tasks)

        sorted[0].status shouldBe TaskStatus.INCOMPLETE
        sorted[1].status shouldBe TaskStatus.INCOMPLETE
        sorted[2].status shouldBe TaskStatus.IN_PROGRESS
        sorted[3].status shouldBe TaskStatus.COMPLETED
    }

    test("Integration 10: Get task count by status") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-2", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-3", status = TaskStatus.IN_PROGRESS),
            createTask(id = "task-4", status = TaskStatus.COMPLETED)
        )

        val counts = filterManager.getCountByStatus(tasks)

        counts[TaskStatus.INCOMPLETE] shouldBe 2
        counts[TaskStatus.IN_PROGRESS] shouldBe 1
        counts[TaskStatus.COMPLETED] shouldBe 1
    }

    test("Integration 11: Get task count by group") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", todoGroup = "Work"),
            createTask(id = "task-2", todoGroup = "Work"),
            createTask(id = "task-3", todoGroup = "Personal"),
            createTask(id = "task-4", todoGroup = "Health")
        )

        val counts = filterManager.getCountByTodoGroup(tasks)

        counts["Work"] shouldBe 2
        counts["Personal"] shouldBe 1
        counts["Health"] shouldBe 1
    }

    test("Integration 12: Complete workflow - load, filter, organize, calculate progress") {
        val filterManager = TaskFilterManager()
        val progressTracker = ProgressTracker(MockStreakRepository())

        // Load all tasks
        val allTasks = (1..20).map { 
            createTask(
                id = "task-$it",
                status = when (it % 3) {
                    0 -> TaskStatus.COMPLETED
                    1 -> TaskStatus.IN_PROGRESS
                    else -> TaskStatus.INCOMPLETE
                },
                todoGroup = "Group-${it % 4}"
            )
        }

        // Filter today's tasks
        val todaysTasks = filterManager.filterTodaysTasks(allTasks)
        todaysTasks.size shouldBe 20

        // Organize by group
        val organized = filterManager.organizeByTodoGroup(todaysTasks)
        organized.size shouldBe 4

        // Calculate progress
        val percentage = progressTracker.calculateCompletionPercentage(todaysTasks)
        percentage shouldBe >= 0
        percentage shouldBe <= 100

        // Get task count
        val display = progressTracker.getTaskCountDisplay(todaysTasks)
        display shouldNotBe null
    }

    test("Integration 13: Handle task status transitions") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        var tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-2", status = TaskStatus.INCOMPLETE)
        )

        var percentage = progressTracker.calculateCompletionPercentage(tasks)
        percentage shouldBe 0

        // Transition to in-progress
        tasks = tasks.mapIndexed { index, task ->
            if (index == 0) task.copy(status = TaskStatus.IN_PROGRESS) else task
        }

        percentage = progressTracker.calculateCompletionPercentage(tasks)
        percentage shouldBe 0 // In-progress doesn't count as complete

        // Transition to completed
        tasks = tasks.mapIndexed { index, task ->
            if (index == 0) task.copy(status = TaskStatus.COMPLETED) else task
        }

        percentage = progressTracker.calculateCompletionPercentage(tasks)
        percentage shouldBe 50
    }

    test("Integration 14: Handle large task lists") {
        checkAll(
            Arb.int(min = 50, max = 500)
        ) { count ->
            val filterManager = TaskFilterManager()
            val progressTracker = ProgressTracker(MockStreakRepository())

            val tasks = (1..count).map { 
                createTask(
                    id = "task-$it",
                    status = if (it % 2 == 0) TaskStatus.COMPLETED else TaskStatus.INCOMPLETE
                )
            }

            val todaysTasks = filterManager.filterTodaysTasks(tasks)
            val percentage = progressTracker.calculateCompletionPercentage(todaysTasks)

            todaysTasks.size shouldBe count
            percentage shouldBe >= 0
            percentage shouldBe <= 100
        }
    }

    test("Integration 15: Handle deleted tasks") {
        val filterManager = TaskFilterManager()
        val progressTracker = ProgressTracker(MockStreakRepository())

        val tasks = listOf(
            createTask(id = "task-1", status = TaskStatus.COMPLETED),
            createTask(id = "task-2", status = TaskStatus.INCOMPLETE),
            createTask(id = "task-3", status = TaskStatus.INCOMPLETE).copy(isDeleted = true)
        )

        val todaysTasks = filterManager.filterTodaysTasks(tasks)
        val percentage = progressTracker.calculateCompletionPercentage(todaysTasks)

        // Deleted tasks should not be counted
        todaysTasks.size shouldBe 2
        percentage shouldBe 50
    }

    test("Integration 16: Real-time progress updates") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        var tasks = (1..10).map { createTask(id = "task-$it") }
        val percentages = mutableListOf<Int>()

        for (i in 0..9) {
            tasks = tasks.mapIndexed { index, task ->
                if (index <= i) task.copy(status = TaskStatus.COMPLETED) else task
            }
            percentages.add(progressTracker.calculateCompletionPercentage(tasks))
        }

        // Percentages should increase monotonically
        for (i in 1 until percentages.size) {
            percentages[i] shouldBe >= percentages[i - 1]
        }

        percentages.last() shouldBe 100
    }

    test("Integration 17: Task organization with multiple groups") {
        val filterManager = TaskFilterManager()

        val tasks = (1..30).map { 
            createTask(
                id = "task-$it",
                todoGroup = "Group-${it % 5}"
            )
        }

        val organized = filterManager.organizeByTodoGroup(tasks)

        organized.size shouldBe 5
        organized.values.forEach { group ->
            group.size shouldBe 6
        }
    }

    test("Integration 18: Sync status indicator") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1").copy(syncStatus = SyncStatus.SYNCED),
            createTask(id = "task-2").copy(syncStatus = SyncStatus.PENDING),
            createTask(id = "task-3").copy(syncStatus = SyncStatus.SYNCED)
        )

        val pendingTasks = filterManager.filterPendingSync(tasks)
        val syncedTasks = filterManager.filterSynced(tasks)

        pendingTasks.size shouldBe 1
        syncedTasks.size shouldBe 2
    }

    test("Integration 19: Task filtering by group") {
        val filterManager = TaskFilterManager()

        val tasks = listOf(
            createTask(id = "task-1", todoGroup = "Work"),
            createTask(id = "task-2", todoGroup = "Personal"),
            createTask(id = "task-3", todoGroup = "Work"),
            createTask(id = "task-4", todoGroup = "Health")
        )

        val workTasks = filterManager.filterByTodoGroup(tasks, "Work")

        workTasks.size shouldBe 2
    }

    test("Integration 20: Complete daily focus workflow") {
        val filterManager = TaskFilterManager()
        val progressTracker = ProgressTracker(MockStreakRepository())

        // Simulate a complete day
        var tasks = (1..8).map { 
            createTask(
                id = "task-$it",
                status = TaskStatus.INCOMPLETE,
                todoGroup = "Group-${it % 2}"
            )
        }

        // Load and organize
        val todaysTasks = filterManager.filterTodaysTasks(tasks)
        val organized = filterManager.organizeByTodoGroup(todaysTasks)

        // Complete tasks throughout the day
        for (i in 0..7) {
            tasks = tasks.mapIndexed { index, task ->
                if (index == i) task.copy(status = TaskStatus.COMPLETED) else task
            }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)
            val display = progressTracker.getTaskCountDisplay(tasks)
            val isComplete = progressTracker.isAllTasksCompleted(tasks)

            percentage shouldBe ((i + 1) * 100 / 8)
            display shouldNotBe null
            isComplete shouldBe (i == 7)
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
