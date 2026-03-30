package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.UUID

/**
 * Integration tests for WebSocketTaskUpdateHandler with WebSocket events.
 *
 * Tests the complete flow of receiving WebSocket events and applying updates.
 */
class WebSocketTaskUpdateHandlerIntegrationTest : FunSpec({
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

    test("Integration: WebSocket task update should emit UpdateEvent and update database") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val updateJob = kotlinx.coroutines.launch {
            val event = withTimeoutOrNull(1000) {
                handler.observeUpdates().first()
            }
            event shouldBe UpdateEvent.TaskUpdated(taskId, remoteTask)
        }

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        updateJob.join()
        coVerify { taskDao.insert(any()) }
    }

    test("Integration: Multiple WebSocket updates should be applied in sequence") {
        val taskId1 = "task-1"
        val taskId2 = "task-2"
        val remoteTask1 = createTestTask(id = taskId1)
        val remoteTask2 = createTestTask(id = taskId2)

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId1, remoteTask1))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId2, remoteTask2))

        coVerify(exactly = 2) { taskDao.insert(any()) }
    }

    test("Integration: Offline updates should be queued and applied on reconnection") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } throws Exception("Offline")

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        handler.hasQueuedUpdates() shouldBe true

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionEstablished)

        handler.hasQueuedUpdates() shouldBe false
    }

    test("Integration: Task deletion should soft delete and emit event") {
        val taskId = "task-1"
        val task = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns task
        coEvery { taskDao.update(any()) } returns Unit

        val updateJob = kotlinx.coroutines.launch {
            val event = withTimeoutOrNull(1000) {
                handler.observeUpdates().first()
            }
            event shouldBe UpdateEvent.TaskDeleted(taskId)
        }

        handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(taskId))

        updateJob.join()
        coVerify { taskDao.update(any()) }
    }

    test("Integration: Task creation should insert and emit event") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val updateJob = kotlinx.coroutines.launch {
            val event = withTimeoutOrNull(1000) {
                handler.observeUpdates().first()
            }
            event shouldBe UpdateEvent.TaskCreated(remoteTask)
        }

        handler.handleWebSocketEvent(WebSocketEvent.TaskCreated(remoteTask))

        updateJob.join()
        coVerify { taskDao.insert(any()) }
    }

    test("Integration: Conflict resolution should prefer newer timestamp") {
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
        coVerify { taskDao.update(any()) }
    }

    test("Integration: Connection lost should queue updates") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } throws Exception("Offline")

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        handler.hasQueuedUpdates() shouldBe true
    }

    test("Integration: Connection established should apply queued updates") {
        val taskId1 = "task-1"
        val taskId2 = "task-2"
        val remoteTask1 = createTestTask(id = taskId1)
        val remoteTask2 = createTestTask(id = taskId2)

        coEvery { taskDao.getTaskById(any()) } throws Exception("Offline")

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId1, remoteTask1))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId2, remoteTask2))

        handler.hasQueuedUpdates() shouldBe true

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val result = handler.handleWebSocketEvent(WebSocketEvent.ConnectionEstablished)

        result.success shouldBe true
        handler.hasQueuedUpdates() shouldBe false
    }

    test("Integration: Sync signal should be processed successfully") {
        val result = handler.handleWebSocketEvent(WebSocketEvent.SyncSignal)

        result.success shouldBe true
    }

    test("Integration: Error event should return failure") {
        val result = handler.handleWebSocketEvent(
            WebSocketEvent.Error("Test error", Exception("Test"))
        )

        result.success shouldBe false
    }

    test("Integration: Multiple event types in sequence") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { taskDao.update(any()) } returns Unit

        val result1 = handler.handleWebSocketEvent(WebSocketEvent.TaskCreated(remoteTask))
        val result2 = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))
        val result3 = handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(taskId))

        result1.success shouldBe true
        result2.success shouldBe true
        result3.success shouldBe true
    }

    test("Integration: Clear queued updates should remove all pending updates") {
        val taskId1 = "task-1"
        val taskId2 = "task-2"
        val remoteTask1 = createTestTask(id = taskId1)
        val remoteTask2 = createTestTask(id = taskId2)

        coEvery { taskDao.getTaskById(any()) } throws Exception("Offline")

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId1, remoteTask1))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId2, remoteTask2))

        handler.hasQueuedUpdates() shouldBe true

        handler.clearQueuedUpdates()

        handler.hasQueuedUpdates() shouldBe false
    }

    test("Integration: Apply queued updates should process all pending updates") {
        val taskId1 = "task-1"
        val taskId2 = "task-2"
        val remoteTask1 = createTestTask(id = taskId1)
        val remoteTask2 = createTestTask(id = taskId2)

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId1, remoteTask1))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId2, remoteTask2))

        val result = handler.applyQueuedUpdates()

        result.success shouldBe true
        handler.hasQueuedUpdates() shouldBe false
    }

    test("Integration: Task update should mark task as synced") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        coVerify {
            taskDao.insert(match { it.syncStatus == SyncStatus.SYNCED })
        }
    }

    test("Integration: Task deletion should mark task as synced") {
        val taskId = "task-1"
        val task = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns task
        coEvery { taskDao.update(any()) } returns Unit

        handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(taskId))

        coVerify {
            taskDao.update(match { it.syncStatus == SyncStatus.SYNCED && it.isDeleted })
        }
    }
})
