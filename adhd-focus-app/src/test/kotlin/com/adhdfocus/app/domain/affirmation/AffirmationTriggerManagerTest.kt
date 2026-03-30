package com.adhdfocus.app.domain.affirmation

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

/**
 * Unit Tests for AffirmationTriggerManager
 *
 * Tests:
 * - Day completion affirmation trigger
 * - Task completion affirmation trigger
 * - Streak milestone affirmation trigger
 * - Affirmation event management
 */
class AffirmationTriggerManagerTest : FunSpec({

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

    test("AffirmationTriggerManager initialization") {
        val manager = AffirmationTriggerManager()

        manager.affirmationEvent.value shouldBe null
    }

    test("Trigger day complete affirmation") {
        val manager = AffirmationTriggerManager()
        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }

        val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)

        triggered shouldBe true
        manager.affirmationEvent.value shouldNotBe null
        manager.affirmationEvent.value shouldBe is AffirmationEvent.DayComplete
    }

    test("Do not trigger day complete with incomplete tasks") {
        val manager = AffirmationTriggerManager()
        val tasks = listOf(
            createTask(status = TaskStatus.COMPLETED),
            createTask(status = TaskStatus.INCOMPLETE)
        )

        val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)

        triggered shouldBe false
    }

    test("Do not trigger day complete with empty task list") {
        val manager = AffirmationTriggerManager()
        val tasks = emptyList<Task>()

        val triggered = manager.checkAndTriggerDayCompleteAffirmation(tasks)

        triggered shouldBe false
    }

    test("Trigger task complete affirmation") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED)

        val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

        triggered shouldBe true
        manager.affirmationEvent.value shouldNotBe null
        manager.affirmationEvent.value shouldBe is AffirmationEvent.TaskComplete
    }

    test("Do not trigger task complete for incomplete task") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.INCOMPLETE)

        val triggered = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)

        triggered shouldBe false
    }

    test("Trigger streak milestone at 3 days") {
        val manager = AffirmationTriggerManager()

        val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(3)

        triggered shouldBe true
        manager.affirmationEvent.value shouldBe is AffirmationEvent.StreakMilestone
    }

    test("Trigger streak milestone at 7 days") {
        val manager = AffirmationTriggerManager()

        val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(7)

        triggered shouldBe true
    }

    test("Trigger streak milestone at 30 days") {
        val manager = AffirmationTriggerManager()

        val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(30)

        triggered shouldBe true
    }

    test("Do not trigger streak milestone at non-milestone") {
        val manager = AffirmationTriggerManager()

        val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(5)

        triggered shouldBe false
    }

    test("Clear affirmation") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED)

        manager.checkAndTriggerTaskCompleteAffirmation(task)
        manager.affirmationEvent.value shouldNotBe null

        manager.clearAffirmation()
        manager.affirmationEvent.value shouldBe null
    }

    test("Get current affirmation") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED)

        manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        val affirmation = manager.getCurrentAffirmation()

        affirmation shouldNotBe null
        affirmation shouldBe is AffirmationEvent.TaskComplete
    }

    test("Affirmation event has message") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED)

        manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        val affirmation = manager.affirmationEvent.value

        affirmation?.message shouldNotBe null
        affirmation?.message?.isNotEmpty() shouldBe true
    }

    test("Affirmation event has timestamp") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED)

        manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        val affirmation = manager.affirmationEvent.value

        affirmation?.timestamp shouldNotBe null
        affirmation?.timestamp shouldBe > 0L
    }

    test("Task complete affirmation includes task ID") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED, id = "task-123")

        manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.TaskComplete

        affirmation?.taskId shouldBe "task-123"
    }

    test("Streak milestone affirmation includes streak count") {
        val manager = AffirmationTriggerManager()

        manager.checkAndTriggerStreakMilestoneAffirmation(7)
        val affirmation = manager.affirmationEvent.value as? AffirmationEvent.StreakMilestone

        affirmation?.streakCount shouldBe 7
    }

    test("Multiple affirmation triggers") {
        val manager = AffirmationTriggerManager()

        // Trigger task complete
        val task = createTask(status = TaskStatus.COMPLETED)
        manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        manager.affirmationEvent.value shouldBe is AffirmationEvent.TaskComplete

        // Clear and trigger day complete
        manager.clearAffirmation()
        val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        manager.checkAndTriggerDayCompleteAffirmation(tasks)
        manager.affirmationEvent.value shouldBe is AffirmationEvent.DayComplete

        // Clear and trigger streak milestone
        manager.clearAffirmation()
        manager.checkAndTriggerStreakMilestoneAffirmation(14)
        manager.affirmationEvent.value shouldBe is AffirmationEvent.StreakMilestone
    }

    test("Affirmation messages are not empty") {
        val manager = AffirmationTriggerManager()

        // Test day complete message
        val dayCompleteTasks = (1..5).map { createTask(status = TaskStatus.COMPLETED) }
        manager.checkAndTriggerDayCompleteAffirmation(dayCompleteTasks)
        val dayCompleteMsg = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
        dayCompleteMsg?.isNotEmpty() shouldBe true

        // Test task complete message
        manager.clearAffirmation()
        val task = createTask(status = TaskStatus.COMPLETED)
        manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        val taskCompleteMsg = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
        taskCompleteMsg?.isNotEmpty() shouldBe true

        // Test streak milestone message
        manager.clearAffirmation()
        manager.checkAndTriggerStreakMilestoneAffirmation(30)
        val streakMsg = (manager.affirmationEvent.value as? AffirmationEvent.StreakMilestone)?.message
        streakMsg?.isNotEmpty() shouldBe true
    }

    test("Task complete messages rotate through pool") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 10 task completions
        repeat(10) {
            manager.clearAffirmation()
            val task = createTask(status = TaskStatus.COMPLETED, id = UUID.randomUUID().toString())
            manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.TaskComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Should have variety
        messages.toSet().size shouldBe > 1

        // No consecutive messages should be the same
        for (i in 0 until messages.size - 1) {
            messages[i] shouldNotBe messages[i + 1]
        }
    }

    test("Day complete messages rotate through pool") {
        val manager = AffirmationTriggerManager()
        val messages = mutableListOf<String>()

        // Generate 8 day completions
        repeat(8) {
            manager.clearAffirmation()
            val tasks = (1..5).map { createTask(status = TaskStatus.COMPLETED, id = UUID.randomUUID().toString()) }
            manager.checkAndTriggerDayCompleteAffirmation(tasks)
            val message = (manager.affirmationEvent.value as? AffirmationEvent.DayComplete)?.message
            if (message != null) {
                messages.add(message)
            }
        }

        // Should have variety
        messages.toSet().size shouldBe > 1

        // No consecutive messages should be the same
        for (i in 0 until messages.size - 1) {
            messages[i] shouldNotBe messages[i + 1]
        }
    }

    test("Affirmation event types are distinct") {
        val taskCompleteEvent = AffirmationEvent.TaskComplete("message", "task-1", System.currentTimeMillis())
        val dayCompleteEvent = AffirmationEvent.DayComplete("message", System.currentTimeMillis())
        val streakEvent = AffirmationEvent.StreakMilestone("message", 7, System.currentTimeMillis())

        taskCompleteEvent::class shouldNotBe dayCompleteEvent::class
        dayCompleteEvent::class shouldNotBe streakEvent::class
        taskCompleteEvent::class shouldNotBe streakEvent::class
    }

    test("Affirmation prevents duplicate triggers") {
        val manager = AffirmationTriggerManager()
        val task = createTask(status = TaskStatus.COMPLETED)

        // First trigger should succeed
        val triggered1 = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        triggered1 shouldBe true

        // Immediate second trigger should fail (within 500ms)
        val triggered2 = manager.checkAndTriggerTaskCompleteAffirmation(task, streakCount = 0)
        triggered2 shouldBe false
    }

    test("All streak milestones trigger affirmation") {
        val manager = AffirmationTriggerManager()
        val milestones = listOf(3, 7, 14, 30, 60, 90, 365)

        for (milestone in milestones) {
            manager.clearAffirmation()
            val triggered = manager.checkAndTriggerStreakMilestoneAffirmation(milestone)
            triggered shouldBe true
        }
    }
})
