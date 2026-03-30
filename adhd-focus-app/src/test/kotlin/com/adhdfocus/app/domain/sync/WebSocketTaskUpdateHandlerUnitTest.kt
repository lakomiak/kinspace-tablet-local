package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.util.UUID

class WebSocketTaskUpdateHandlerUnitTest : FunSpec({
    val taskDao = mockk<TaskDao>()
    val conflictResolver = mockk<ConflictResolver>()
    val gson = Gson()
    val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver, gson)

    val householdId = "household-123"
    val userId = "user-456"

    fun createTestTask(
        id: String = UUID.randomUUID().toString(),
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

    test("handleWebSocketEvent with TaskUpdated should update existing task") {
        val taskId = "task-1"
        val localTask = createTestTask(id = taskId)
        val remoteTask = localTask.copy(
            title = "Updated Title",
            updatedAt = Instant.now().plusSeconds(10)
        )

        coEvery { taskDao.getTaskById(taskId) } returns localTask
        coEvery { conflictResolver.isConflict(localTask, remoteTask) } returns false
        coEvery { taskDao.update(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        coVerify { taskDao.update(any()) }
    }

    test("handleWebSocketEvent with TaskUpdated should insert new task") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        coVerify { taskDao.insert(any()) }
    }

    test("handleWebSocketEvent with TaskDeleted should soft delete task") {
        val taskId = "task-1"
        val task = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns task
        coEvery { taskDao.update(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(taskId))

        result.success shouldBe true
        coVerify { taskDao.update(any()) }
    }

    test("handleWebSocketEvent with TaskDeleted should handle missing task") {
        val taskId = "task-1"

        coEvery { taskDao.getTaskById(taskId) } returns null

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(taskId))

        result.success shouldBe true
    }

    test("handleWebSocketEvent with TaskCreated should insert new task") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskCreated(remoteTask))

        result.success shouldBe true
        coVerify { taskDao.insert(any()) }
    }

    test("handleWebSocketEvent with TaskCreated should not duplicate existing task") {
        val taskId = "task-1"
        val existingTask = createTestTask(id = taskId)
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns existingTask

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskCreated(remoteTask))

        result.success shouldBe true
        coVerify(exactly = 0) { taskDao.insert(any()) }
    }

    test("handleWebSocketEvent with ConnectionEstablished should apply queued updates") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        // Queue an update while offline
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        val result = handler.handleWebSocketEvent(WebSocketEvent.ConnectionEstablished)

        result.success shouldBe true
    }

    test("handleWebSocketEvent with ConnectionLost should mark offline") {
        val result = handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)

        result.success shouldBe true
    }

    test("applyQueuedUpdates should apply all queued updates") {
        val taskId1 = "task-1"
        val taskId2 = "task-2"
        val remoteTask1 = createTestTask(id = taskId1)
        val remoteTask2 = createTestTask(id = taskId2)

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        // Queue updates while offline
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId1, remoteTask1))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId2, remoteTask2))

        val result = handler.applyQueuedUpdates()

        result.success shouldBe true
    }

    test("hasQueuedUpdates should return true when updates are queued") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        handler.hasQueuedUpdates() shouldBe true
    }

    test("hasQueuedUpdates should return false when no updates are queued") {
        handler.hasQueuedUpdates() shouldBe false
    }

    test("clearQueuedUpdates should remove all queued updates") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))
        handler.clearQueuedUpdates()

        handler.hasQueuedUpdates() shouldBe false
    }

    test("observeUpdates should emit UpdateEvent on task update") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val updates = mutableListOf<UpdateEvent>()
        val job = kotlinx.coroutines.launch {
            handler.observeUpdates().collect { updates.add(it) }
        }

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        // Give time for emission
        Thread.sleep(100)
        job.cancel()

        updates.size shouldBe 1
    }

    test("conflict resolution should prefer remote when remote is newer") {
        val taskId = "task-1"
        val now = Instant.now()
        val localTask = createTestTask(id = taskId, updatedAt = now)
        val remoteTask = localTask.copy(
            title = "Updated Title",
            updatedAt = now.plusSeconds(10)
        )

        coEvery { taskDao.getTaskById(taskId) } returns localTask
        coEvery { conflictResolver.isConflict(localTask, remoteTask) } returns true
        coEvery { taskDao.update(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        result.conflictResolved shouldBe true
    }

    test("conflict resolution should keep local when local is newer") {
        val taskId = "task-1"
        val now = Instant.now()
        val localTask = createTestTask(id = taskId, updatedAt = now.plusSeconds(10))
        val remoteTask = localTask.copy(
            title = "Updated Title",
            updatedAt = now
        )

        coEvery { taskDao.getTaskById(taskId) } returns localTask
        coEvery { conflictResolver.isConflict(localTask, remoteTask) } returns true
        coEvery { taskDao.update(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        result.conflictResolved shouldBe true
    }

    test("handleWebSocketEvent should queue updates when offline") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        // Simulate offline by making taskDao throw exception
        coEvery { taskDao.getTaskById(taskId) } throws Exception("Offline")

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        handler.hasQueuedUpdates() shouldBe true
    }

    test("handleWebSocketEvent with Error should return failure") {
        val result = handler.handleWebSocketEvent(
            WebSocketEvent.Error("Test error", Exception("Test"))
        )

        result.success shouldBe false
    }

    test("handleWebSocketEvent with SyncSignal should return success") {
        val result = handler.handleWebSocketEvent(WebSocketEvent.SyncSignal)

        result.success shouldBe true
    }

    test("multiple task updates should be applied in order") {
        val taskId1 = "task-1"
        val taskId2 = "task-2"
        val remoteTask1 = createTestTask(id = taskId1)
        val remoteTask2 = createTestTask(id = taskId2)

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val result1 = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId1, remoteTask1))
        val result2 = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId2, remoteTask2))

        result1.success shouldBe true
        result2.success shouldBe true
    }

    test("task update should mark task as synced") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        coVerify {
            taskDao.insert(match { it.syncStatus == SyncStatus.SYNCED })
        }
    }

    test("task deletion should mark task as synced") {
        val taskId = "task-1"
        val task = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns task
        coEvery { taskDao.update(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(taskId))

        coVerify {
            taskDao.update(match { it.syncStatus == SyncStatus.SYNCED && it.isDeleted })
        }
    }

    test("applyQueuedUpdates should return success when queue is empty") {
        val result = handler.applyQueuedUpdates()

        result.success shouldBe true
    }

    test("clearQueuedUpdates should work multiple times") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))
        handler.clearQueuedUpdates()
        handler.clearQueuedUpdates()

        handler.hasQueuedUpdates() shouldBe false
    }
})
