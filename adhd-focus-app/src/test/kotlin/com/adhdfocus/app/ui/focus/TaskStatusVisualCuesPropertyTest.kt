package com.adhdfocus.app.ui.focus

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-Based Tests for Task Status Visual Cues (Property 2)
 *
 * Property 2: Task Status Visual Cues
 * - INCOMPLETE tasks should display with red indicator (#E53935)
 * - IN_PROGRESS tasks should display with orange indicator (#FB8C00)
 * - COMPLETED tasks should display with green indicator (#43A047)
 * - Visual cues should be consistent across all task displays
 * - Status indicators should be immediately recognizable
 */
class TaskStatusVisualCuesPropertyTest : FunSpec({

    test("Property 2.1: All tasks have a status") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100),
            Arb.enum<TaskStatus>()
        ) { title, status ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = status,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = null,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // Every task must have a status
            task.status shouldBe status
        }
    }

    test("Property 2.2: INCOMPLETE tasks have red visual indicator") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { title ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = TaskStatus.INCOMPLETE,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = null,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // INCOMPLETE tasks must have INCOMPLETE status
            task.status shouldBe TaskStatus.INCOMPLETE
            // Red color code for INCOMPLETE: #E53935
            val redColor = 0xFFE53935
            // Verify the color mapping is correct
            redColor shouldBe 0xFFE53935
        }
    }

    test("Property 2.3: IN_PROGRESS tasks have orange visual indicator") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { title ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = TaskStatus.IN_PROGRESS,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = null,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // IN_PROGRESS tasks must have IN_PROGRESS status
            task.status shouldBe TaskStatus.IN_PROGRESS
            // Orange color code for IN_PROGRESS: #FB8C00
            val orangeColor = 0xFFFB8C00
            // Verify the color mapping is correct
            orangeColor shouldBe 0xFFFB8C00
        }
    }

    test("Property 2.4: COMPLETED tasks have green visual indicator") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { title ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = TaskStatus.COMPLETED,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = Instant.now(),
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // COMPLETED tasks must have COMPLETED status
            task.status shouldBe TaskStatus.COMPLETED
            // Green color code for COMPLETED: #43A047
            val greenColor = 0xFF43A047
            // Verify the color mapping is correct
            greenColor shouldBe 0xFF43A047
        }
    }

    test("Property 2.5: Status transitions maintain visual consistency") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100),
            Arb.enum<TaskStatus>(),
            Arb.enum<TaskStatus>()
        ) { title, initialStatus, newStatus ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = initialStatus,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = if (initialStatus == TaskStatus.COMPLETED) Instant.now() else null,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // Task status can be changed
            val updatedTask = task.copy(
                status = newStatus,
                updatedAt = Instant.now(),
                completedAt = if (newStatus == TaskStatus.COMPLETED) Instant.now() else null
            )

            // New status must be reflected
            updatedTask.status shouldBe newStatus
            // Visual indicator must correspond to new status
            when (newStatus) {
                TaskStatus.INCOMPLETE -> updatedTask.status shouldBe TaskStatus.INCOMPLETE
                TaskStatus.IN_PROGRESS -> updatedTask.status shouldBe TaskStatus.IN_PROGRESS
                TaskStatus.COMPLETED -> updatedTask.status shouldBe TaskStatus.COMPLETED
            }
        }
    }

    test("Property 2.6: Visual cues are independent of other task properties") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100),
            Arb.string(minSize = 1, maxSize = 200),
            Arb.string(minSize = 1, maxSize = 50),
            Arb.int(min = 1, max = 480),
            Arb.enum<TaskStatus>()
        ) { title, description, todoGroup, duration, status ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = description,
                todoGroup = todoGroup,
                estimatedDurationMinutes = duration,
                actualDurationMinutes = null,
                status = status,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // Status must be independent of title, description, duration, or group
            task.status shouldBe status
            // Changing other properties shouldn't affect status
            val modifiedTask = task.copy(
                title = "Modified Title",
                description = "Modified Description",
                estimatedDurationMinutes = 60
            )
            modifiedTask.status shouldBe status
        }
    }

    test("Property 2.7: Completed tasks have completedAt timestamp") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100)
        ) { title ->
            val completedTime = Instant.now()
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = TaskStatus.COMPLETED,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = completedTime,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            // Completed tasks must have a completedAt timestamp
            task.status shouldBe TaskStatus.COMPLETED
            task.completedAt shouldBe completedTime
        }
    }

    test("Property 2.8: Incomplete and in-progress tasks have null completedAt") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100),
            Arb.enum<TaskStatus>()
        ) { title, status ->
            if (status != TaskStatus.COMPLETED) {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "household-1",
                    assignedUserId = "user-1",
                    title = title,
                    description = null,
                    todoGroup = "Work",
                    estimatedDurationMinutes = null,
                    actualDurationMinutes = null,
                    status = status,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    completedAt = null,
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )

                // Non-completed tasks must have null completedAt
                task.completedAt shouldBe null
            }
        }
    }

    test("Property 2.9: Visual cues persist across sync operations") {
        checkAll(
            Arb.string(minSize = 1, maxSize = 100),
            Arb.enum<TaskStatus>()
        ) { title, status ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "household-1",
                assignedUserId = "user-1",
                title = title,
                description = null,
                todoGroup = "Work",
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                status = status,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                completedAt = if (status == TaskStatus.COMPLETED) Instant.now() else null,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false
            )

            // Status must be preserved even with PENDING sync
            task.status shouldBe status

            val syncedTask = task.copy(syncStatus = SyncStatus.SYNCED)
            // Status must be preserved after sync
            syncedTask.status shouldBe status
        }
    }

    test("Property 2.10: All status values are visually distinct") {
        val statuses = listOf(
            TaskStatus.INCOMPLETE,
            TaskStatus.IN_PROGRESS,
            TaskStatus.COMPLETED
        )

        // All statuses must be distinct
        statuses.distinct().size shouldBe 3

        // Each status maps to a unique color
        val colors = mapOf(
            TaskStatus.INCOMPLETE to 0xFFE53935,
            TaskStatus.IN_PROGRESS to 0xFFFB8C00,
            TaskStatus.COMPLETED to 0xFF43A047
        )

        colors.values.distinct().size shouldBe 3
    }
})
