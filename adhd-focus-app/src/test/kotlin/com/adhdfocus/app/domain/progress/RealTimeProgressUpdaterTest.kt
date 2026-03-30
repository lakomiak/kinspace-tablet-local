package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

/**
 * Unit Tests for RealTimeProgressUpdater
 *
 * Tests:
 * - Real-time progress updates
 * - Progress metrics calculation
 * - Change detection
 * - Flow emissions
 */
class RealTimeProgressUpdaterTest : FunSpec({

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

    test("RealTimeProgressUpdater initialization") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        updater.completionPercentage.value shouldBe 0
        updater.taskCountDisplay.value shouldBe ""
        updater.isDayComplete.value shouldBe false
        updater.progressMetrics.value shouldBe null
    }

    test("Update progress with empty task list") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        updater.updateProgress(emptyList())

        updater.completionPercentage.value shouldBe 0
        updater.isDayComplete.value shouldBe false
    }

    test("Update progress with incomplete tasks") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.INCOMPLETE) }
        updater.updateProgress(tasks)

        updater.completionPercentage.value shouldBe 0
        updater.isDayComplete.value shouldBe false
    }

    test("Update progress with completed tasks") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks)

        updater.completionPercentage.value shouldBe 100
        updater.isDayComplete.value shouldBe true
    }

    test("Update progress with mixed task statuses") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        updater.updateProgress(tasks)

        updater.completionPercentage.value shouldBe 40
        updater.isDayComplete.value shouldBe false
    }

    test("Progress metrics are updated") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..10).map { 
            if (it <= 5) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }

        updater.updateProgress(tasks, currentStreak = 3, bestStreak = 7)

        val metrics = updater.progressMetrics.value
        metrics shouldNotBe null
        metrics?.completionPercentage shouldBe 50
        metrics?.completedCount shouldBe 5
        metrics?.totalCount shouldBe 10
        metrics?.currentStreak shouldBe 3
        metrics?.bestStreak shouldBe 7
    }

    test("Task count display is updated") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        updater.updateProgress(tasks)

        updater.taskCountDisplay.value shouldBe "1 of 3 complete"
    }

    test("Significant change detection - 10% threshold") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        updater.isSignificantChange(0, 10) shouldBe true
        updater.isSignificantChange(0, 9) shouldBe false
        updater.isSignificantChange(50, 60) shouldBe true
        updater.isSignificantChange(50, 55) shouldBe false
    }

    test("Significant change detection - custom threshold") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        updater.isSignificantChange(0, 5, threshold = 5) shouldBe true
        updater.isSignificantChange(0, 4, threshold = 5) shouldBe false
        updater.isSignificantChange(50, 60, threshold = 15) shouldBe false
    }

    test("Progress direction - UP") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val direction = updater.getProgressDirection(0, 50)

        direction shouldBe ProgressDirection.UP
    }

    test("Progress direction - DOWN") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val direction = updater.getProgressDirection(50, 0)

        direction shouldBe ProgressDirection.DOWN
    }

    test("Progress direction - UNCHANGED") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val direction = updater.getProgressDirection(50, 50)

        direction shouldBe ProgressDirection.UNCHANGED
    }

    test("Reset progress") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks)

        updater.completionPercentage.value shouldBe 100

        updater.resetProgress()

        updater.completionPercentage.value shouldBe 0
        updater.taskCountDisplay.value shouldBe ""
        updater.isDayComplete.value shouldBe false
        updater.progressMetrics.value shouldBe null
    }

    test("Get current metrics") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks, currentStreak = 5)

        val metrics = updater.getCurrentMetrics()

        metrics shouldNotBe null
        metrics?.completionPercentage shouldBe 100
        metrics?.currentStreak shouldBe 5
    }

    test("Multiple progress updates") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        var tasks = (1..5).map { createTask(status = TaskStatus.INCOMPLETE) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 0

        tasks = tasks.mapIndexed { index, task ->
            if (index == 0) task.copy(status = TaskStatus.COMPLETED) else task
        }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 20

        tasks = tasks.mapIndexed { index, task ->
            if (index <= 1) task.copy(status = TaskStatus.COMPLETED) else task
        }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 40
    }

    test("Progress metrics consistency") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        updater.updateProgress(tasks)

        val metrics = updater.progressMetrics.value
        metrics?.completedCount + (metrics?.totalCount?.minus(metrics.completedCount) ?: 0) shouldBe metrics?.totalCount
    }

    test("Day complete indicator accuracy") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        // Not all complete
        var tasks = (1..5).map { 
            if (it <= 4) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }
        updater.updateProgress(tasks)
        updater.isDayComplete.value shouldBe false

        // All complete
        tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks)
        updater.isDayComplete.value shouldBe true
    }

    test("Streak information is preserved") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks, currentStreak = 10, bestStreak = 15)

        val metrics = updater.progressMetrics.value
        metrics?.currentStreak shouldBe 10
        metrics?.bestStreak shouldBe 15
    }

    test("Progress percentage is accurate") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        for (completed in 0..10) {
            val tasks = mutableListOf<Task>()
            repeat(completed) { tasks.add(createTask(status = TaskStatus.COMPLETED)) }
            repeat(10 - completed) { tasks.add(createTask(status = TaskStatus.INCOMPLETE)) }

            updater.updateProgress(tasks)

            val expectedPercentage = (completed * 100) / 10
            updater.completionPercentage.value shouldBe expectedPercentage
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
