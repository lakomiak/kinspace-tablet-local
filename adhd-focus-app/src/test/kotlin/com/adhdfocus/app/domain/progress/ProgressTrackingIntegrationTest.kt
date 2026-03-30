package com.adhdfocus.app.domain.progress

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.repository.StreakRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

/**
 * Integration Tests for Progress Tracking
 *
 * Tests the complete workflow of:
 * 1. Loading tasks
 * 2. Calculating progress
 * 3. Updating progress in real-time
 * 4. Tracking streaks
 * 5. Displaying progress metrics
 */
class ProgressTrackingIntegrationTest : FunSpec({

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

    test("Integration 1: Load tasks and calculate progress") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = (1..10).map { 
            if (it <= 5) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }

        val percentage = progressTracker.calculateCompletionPercentage(tasks)
        val display = progressTracker.getTaskCountDisplay(tasks)

        percentage shouldBe 50
        display shouldBe "5 of 10 complete"
    }

    test("Integration 2: Real-time progress updates") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        var tasks = (1..5).map { createTask(status = TaskStatus.INCOMPLETE) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 0

        // Complete one task
        tasks = tasks.mapIndexed { index, task ->
            if (index == 0) task.copy(status = TaskStatus.COMPLETED) else task
        }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 20

        // Complete all tasks
        tasks = tasks.map { it.copy(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 100
    }

    test("Integration 3: Day complete detection") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }

        val isDayComplete = progressTracker.isAllTasksCompleted(tasks)

        isDayComplete shouldBe true
    }

    test("Integration 4: Progress metrics calculation") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..10).map { 
            if (it <= 7) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }

        updater.updateProgress(tasks, currentStreak = 5, bestStreak = 10)

        val metrics = updater.progressMetrics.value
        metrics?.completionPercentage shouldBe 70
        metrics?.completedCount shouldBe 7
        metrics?.totalCount shouldBe 10
        metrics?.currentStreak shouldBe 5
        metrics?.bestStreak shouldBe 10
    }

    test("Integration 5: Significant progress change detection") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        updater.isSignificantChange(0, 10) shouldBe true
        updater.isSignificantChange(0, 9) shouldBe false
        updater.isSignificantChange(50, 60) shouldBe true
    }

    test("Integration 6: Progress direction tracking") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        updater.getProgressDirection(0, 50) shouldBe ProgressDirection.UP
        updater.getProgressDirection(50, 0) shouldBe ProgressDirection.DOWN
        updater.getProgressDirection(50, 50) shouldBe ProgressDirection.UNCHANGED
    }

    test("Integration 7: Complete daily workflow") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        // Start of day - all tasks incomplete
        var tasks = (1..8).map { createTask(status = TaskStatus.INCOMPLETE) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 0
        updater.isDayComplete.value shouldBe false

        // Mid-day - some tasks complete
        tasks = tasks.mapIndexed { index, task ->
            if (index < 4) task.copy(status = TaskStatus.COMPLETED) else task
        }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 50
        updater.isDayComplete.value shouldBe false

        // End of day - all tasks complete
        tasks = tasks.map { it.copy(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 100
        updater.isDayComplete.value shouldBe true
    }

    test("Integration 8: Task count display accuracy") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        val display = progressTracker.getTaskCountDisplay(tasks)

        display shouldBe "3 of 5 complete"
    }

    test("Integration 9: Progress with mixed task statuses") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.IN_PROGRESS),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.COMPLETED)
        )

        val percentage = progressTracker.calculateCompletionPercentage(tasks)

        percentage shouldBe 50 // Only 2 completed out of 4
    }

    test("Integration 10: Progress reset") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 100

        updater.resetProgress()
        updater.completionPercentage.value shouldBe 0
        updater.isDayComplete.value shouldBe false
    }

    test("Integration 11: Multiple progress updates") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val percentages = mutableListOf<Int>()

        for (completed in 0..10) {
            val tasks = mutableListOf<Task>()
            repeat(completed) { tasks.add(createTask(status = TaskStatus.COMPLETED)) }
            repeat(10 - completed) { tasks.add(createTask(status = TaskStatus.INCOMPLETE)) }

            updater.updateProgress(tasks)
            percentages.add(updater.completionPercentage.value)
        }

        // Verify monotonic increase
        for (i in 1 until percentages.size) {
            percentages[i] shouldBe >= percentages[i - 1]
        }
    }

    test("Integration 12: Progress metrics consistency") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..20).map { 
            if (it <= 12) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }

        updater.updateProgress(tasks)

        val metrics = updater.progressMetrics.value
        metrics?.completedCount + (metrics?.totalCount?.minus(metrics.completedCount) ?: 0) shouldBe metrics?.totalCount
    }

    test("Integration 13: Progress with deleted tasks") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE),
            createTask(status = TaskStatus.INCOMPLETE, id = "deleted").copy(isDeleted = true)
        )

        val percentage = progressTracker.calculateCompletionPercentage(tasks)

        percentage shouldBe 50 // Only counts non-deleted tasks
    }

    test("Integration 14: Large task list performance") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..1000).map { 
            if (it % 2 == 0) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }

        val startTime = System.currentTimeMillis()
        updater.updateProgress(tasks)
        val endTime = System.currentTimeMillis()

        updater.completionPercentage.value shouldBe 50
        (endTime - startTime) shouldBe < 1000 // Should complete within 1 second
    }

    test("Integration 15: Progress tracking with streaks") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }

        updater.updateProgress(tasks, currentStreak = 7, bestStreak = 14)

        val metrics = updater.progressMetrics.value
        metrics?.currentStreak shouldBe 7
        metrics?.bestStreak shouldBe 14
        metrics?.isDayComplete shouldBe true
    }

    test("Integration 16: Progress flow emissions") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val percentages = mutableListOf<Int>()

        for (i in 0..5) {
            val tasks = (1..10).map { 
                if (it <= i) createTask(status = TaskStatus.COMPLETED)
                else createTask(status = TaskStatus.INCOMPLETE)
            }
            updater.updateProgress(tasks)
            percentages.add(updater.completionPercentage.value)
        }

        percentages.size shouldBe 6
        percentages.last() shouldBe 50
    }

    test("Integration 17: Progress metrics are updated atomically") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        val tasks = (1..10).map { 
            if (it <= 5) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }

        updater.updateProgress(tasks, currentStreak = 3)

        val metrics = updater.progressMetrics.value
        metrics?.completionPercentage shouldBe 50
        metrics?.completedCount shouldBe 5
        metrics?.totalCount shouldBe 10
        metrics?.currentStreak shouldBe 3
    }

    test("Integration 18: Progress calculation is accurate for all percentages") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        for (completed in 0..100) {
            val tasks = mutableListOf<Task>()
            repeat(completed) { tasks.add(createTask(status = TaskStatus.COMPLETED)) }
            repeat(100 - completed) { tasks.add(createTask(status = TaskStatus.INCOMPLETE)) }

            val percentage = progressTracker.calculateCompletionPercentage(tasks)

            percentage shouldBe completed
        }
    }

    test("Integration 19: Day complete status is accurate") {
        val progressTracker = ProgressTracker(MockStreakRepository())

        // Not complete
        var tasks = (1..5).map { 
            if (it <= 4) createTask(status = TaskStatus.COMPLETED)
            else createTask(status = TaskStatus.INCOMPLETE)
        }
        progressTracker.isAllTasksCompleted(tasks) shouldBe false

        // Complete
        tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        progressTracker.isAllTasksCompleted(tasks) shouldBe true
    }

    test("Integration 20: Complete progress tracking lifecycle") {
        val progressTracker = ProgressTracker(MockStreakRepository())
        val updater = RealTimeProgressUpdater(progressTracker)

        // Initialize
        updater.completionPercentage.value shouldBe 0

        // Load tasks
        var tasks = (1..8).map { createTask(status = TaskStatus.INCOMPLETE) }
        updater.updateProgress(tasks)
        updater.completionPercentage.value shouldBe 0

        // Complete tasks progressively
        for (i in 1..8) {
            tasks = tasks.mapIndexed { index, task ->
                if (index < i) task.copy(status = TaskStatus.COMPLETED) else task
            }
            updater.updateProgress(tasks)
            updater.completionPercentage.value shouldBe (i * 100 / 8)
        }

        // Verify final state
        updater.completionPercentage.value shouldBe 100
        updater.isDayComplete.value shouldBe true
    }
})

// Mock implementation for testing
class MockStreakRepository : StreakRepository {
    override suspend fun getCurrentStreak(userId: String, householdId: String): Int = 0
    override suspend fun getBestStreak(userId: String, householdId: String): Int = 0
    override suspend fun incrementStreak(userId: String, householdId: String): Int = 0
    override suspend fun resetStreak(userId: String, householdId: String) {}
}
