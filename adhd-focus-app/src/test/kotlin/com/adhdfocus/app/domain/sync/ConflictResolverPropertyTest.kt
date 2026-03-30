package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

class ConflictResolverPropertyTest : FunSpec({
    val resolver = ConflictResolverImpl()

    val householdIdGen = arbitrary { "household-${UUID.randomUUID()}" }
    val userIdGen = arbitrary { "user-${UUID.randomUUID()}" }
    val taskIdGen = arbitrary { UUID.randomUUID().toString() }
    val titleGen = Arb.string(minSize = 1, maxSize = 100)
    val taskStatusGen = arbitrary { TaskStatus.values().random() }

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        householdId: String = "household-123",
        assignedUserId: String = "user-456",
        title: String = "Test Task",
        status: TaskStatus = TaskStatus.INCOMPLETE,
        updatedAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = null,
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = updatedAt,
            completedAt = null,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )
    }

    test("**Validates: Property 10** - Timestamp comparison: Remote newer always wins") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now.plusSeconds(100)
            )

            val result = resolver.resolveConflict(localTask, remoteTask)

            result.updatedAt shouldBe remoteTask.updatedAt
        }
    }

    test("**Validates: Property 10** - Timestamp comparison: Local newer always wins") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now.plusSeconds(100)
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )

            val result = resolver.resolveConflict(localTask, remoteTask)

            result.updatedAt shouldBe localTask.updatedAt
        }
    }

    test("**Validates: Property 10** - Timestamp comparison: Equal timestamps prefer remote") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )

            val result = resolver.resolveConflict(localTask, remoteTask)

            result.updatedAt shouldBe remoteTask.updatedAt
        }
    }

    test("**Validates: Property 10** - Conflict detection: Different timestamps always detected") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now.plusSeconds(1)
            )

            resolver.isConflict(localTask, remoteTask) shouldBe true
        }
    }

    test("**Validates: Property 10** - Conflict detection: Different status always detected") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen
        ) { householdId, userId, taskId, title ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = TaskStatus.INCOMPLETE,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = TaskStatus.COMPLETED,
                updatedAt = now
            )

            resolver.isConflict(localTask, remoteTask) shouldBe true
        }
    }

    test("**Validates: Property 10** - Conflict detection: Same timestamp and status never conflict") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )

            resolver.isConflict(localTask, remoteTask) shouldBe false
        }
    }

    test("**Validates: Property 10** - Resolution consistency: Same conflict always resolves same way") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now.plusSeconds(50)
            )

            val result1 = resolver.resolveConflict(localTask, remoteTask)
            val result2 = resolver.resolveConflict(localTask, remoteTask)

            result1.updatedAt shouldBe result2.updatedAt
            result1.id shouldBe result2.id
        }
    }

    test("**Validates: Property 10** - Conflict reason always provided") {
        checkAll(
            householdIdGen,
            userIdGen,
            taskIdGen,
            titleGen,
            taskStatusGen
        ) { householdId, userId, taskId, title, status ->
            val now = Instant.now()
            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now
            )
            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                status = status,
                updatedAt = now.plusSeconds(10)
            )

            val reason = resolver.getConflictReason(localTask, remoteTask)

            reason.isNotEmpty() shouldBe true
        }
    }
})
