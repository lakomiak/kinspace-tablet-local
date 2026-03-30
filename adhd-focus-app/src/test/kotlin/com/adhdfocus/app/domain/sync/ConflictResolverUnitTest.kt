package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class ConflictResolverUnitTest : FunSpec({
    val resolver = ConflictResolverImpl()

    val householdId = "household-123"
    val userId = "user-456"
    val taskId = UUID.randomUUID().toString()

    fun createTask(
        id: String = taskId,
        status: TaskStatus = TaskStatus.INCOMPLETE,
        updatedAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = userId,
            title = "Test Task",
            description = "Test Description",
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

    test("isConflict returns true when timestamps differ") {
        val now = Instant.now()
        val localTask = createTask(updatedAt = now)
        val remoteTask = createTask(updatedAt = now.plusSeconds(10))

        resolver.isConflict(localTask, remoteTask) shouldBe true
    }

    test("isConflict returns true when status differs") {
        val now = Instant.now()
        val localTask = createTask(status = TaskStatus.INCOMPLETE, updatedAt = now)
        val remoteTask = createTask(status = TaskStatus.COMPLETED, updatedAt = now)

        resolver.isConflict(localTask, remoteTask) shouldBe true
    }

    test("isConflict returns false when timestamps and status are same") {
        val now = Instant.now()
        val localTask = createTask(status = TaskStatus.INCOMPLETE, updatedAt = now)
        val remoteTask = createTask(status = TaskStatus.INCOMPLETE, updatedAt = now)

        resolver.isConflict(localTask, remoteTask) shouldBe false
    }

    test("resolveConflict returns remote when remote is newer") {
        val now = Instant.now()
        val localTask = createTask(updatedAt = now)
        val remoteTask = createTask(updatedAt = now.plusSeconds(10))

        val result = resolver.resolveConflict(localTask, remoteTask)

        result.updatedAt shouldBe remoteTask.updatedAt
        result.id shouldBe remoteTask.id
    }

    test("resolveConflict returns remote when timestamps are equal") {
        val now = Instant.now()
        val localTask = createTask(updatedAt = now)
        val remoteTask = createTask(updatedAt = now)

        val result = resolver.resolveConflict(localTask, remoteTask)

        result.updatedAt shouldBe remoteTask.updatedAt
    }

    test("resolveConflict returns local when local is newer") {
        val now = Instant.now()
        val localTask = createTask(updatedAt = now.plusSeconds(10))
        val remoteTask = createTask(updatedAt = now)

        val result = resolver.resolveConflict(localTask, remoteTask)

        result.updatedAt shouldBe localTask.updatedAt
    }

    test("getConflictReason returns timestamp conflict message when timestamps differ") {
        val now = Instant.now()
        val localTask = createTask(updatedAt = now)
        val remoteTask = createTask(updatedAt = now.plusSeconds(10))

        val reason = resolver.getConflictReason(localTask, remoteTask)

        reason.contains("Timestamp conflict") shouldBe true
        reason.contains("remote") shouldBe true
    }

    test("getConflictReason returns status conflict message when status differs") {
        val now = Instant.now()
        val localTask = createTask(status = TaskStatus.INCOMPLETE, updatedAt = now)
        val remoteTask = createTask(status = TaskStatus.COMPLETED, updatedAt = now)

        val reason = resolver.getConflictReason(localTask, remoteTask)

        reason.contains("Status conflict") shouldBe true
        reason.contains("INCOMPLETE") shouldBe true
        reason.contains("COMPLETED") shouldBe true
    }
})
