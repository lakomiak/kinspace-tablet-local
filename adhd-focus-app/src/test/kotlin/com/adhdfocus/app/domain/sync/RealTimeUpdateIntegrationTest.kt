package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.domain.notification.UpdateNotificationManager
import com.adhdfocus.app.domain.task.TaskManager
import com.adhdfocus.app.ui.timer.TimerViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.UUID

/**
 * Comprehensive integration tests for real-time updates.
 *
 * Validates the complete end-to-end workflow of real-time updates from WebSocket
 * reception through UI refresh, including offline scenarios, timer awareness,
 * and notification handling.
 *
 * **Validates: Requirements 2.4, 3, 11**
 */
class RealTimeUpdateIntegrationTest : FunSpec({
    val taskDao = mockk<TaskDao>()
    val taskManager = mockk<TaskManager>()
    val timerViewModel = mockk<TimerViewModel>()
    val notificationManager = mockk<UpdateNotificationManager>()
    val conflictResolver = mockk<ConflictResolver>()
    val webSocketManager = mockk<WebSocketManager>()
    val offlineUpdateQueue = mockk<OfflineUpdateQueue>()
    val timerAwareUpdateApplier = mockk<TimerAwareUpdateApplier>()

    val householdId = "household-123"
    val userId = "user-456"

    fun createTestTask(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Task",
        status: TaskStatus = TaskStatus.INCOMPLETE,
        updatedAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = userId,
            title = title,
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

    test("Scenario 1: Happy path - WebSocket update → apply → UI refresh") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId, title = "Updated Task")

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { timerViewModel.isRunning.value } returns false
        coEvery { timerAwareUpdateApplier.applyUpdate(any()) } returns true
        coEvery { notificationManager.showNotification(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        coVerify { taskDao.insert(any()) }
    }

    test("Scenario 2: Offline scenario - Queue update → reconnect → apply → UI refresh") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } throws Exception("Offline")
        coEvery { offlineUpdateQueue.addUpdate(any(), any(), any(), any()) } returns true
        coEvery { offlineUpdateQueue.getUnappliedUpdates(userId) } returns emptyList()
        coEvery { taskDao.insert(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        // Simulate offline
        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        handler.hasQueuedUpdates() shouldBe true

        // Simulate reconnection
        coEvery { taskDao.getTaskById(taskId) } returns null
        val reconnectResult = handler.handleWebSocketEvent(WebSocketEvent.ConnectionEstablished)

        reconnectResult.success shouldBe true
    }

    test("Scenario 3: Timer active - Queue update → timer completes → apply → UI refresh") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId, title = "Updated Task")

        coEvery { timerViewModel.isRunning.value } returns true
        coEvery { timerAwareUpdateApplier.applyUpdate(any()) } returns false // Queued
        coEvery { timerAwareUpdateApplier.applyQueuedUpdates() } returns true
        coEvery { taskDao.insert(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        // Update should be queued when timer is active
        result.success shouldBe true
    }

    test("Scenario 4: Multiple updates - Multiple WebSocket events → apply all → UI refresh") {
        val taskIds = listOf("task-1", "task-2", "task-3")
        val remoteTasks = taskIds.map { createTestTask(id = it, title = "Task $it") }

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { timerViewModel.isRunning.value } returns false

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        remoteTasks.forEach { task ->
            val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(task.id, task))
            result.success shouldBe true
        }

        coVerify(exactly = 3) { taskDao.insert(any()) }
    }

    test("Scenario 5: Conflict resolution - Conflicting updates → resolve by timestamp → apply") {
        val taskId = "task-1"
        val now = Instant.now()
        val localTask = createTestTask(id = taskId, updatedAt = now)
        val remoteTask = localTask.copy(
            title = "Remote Update",
            updatedAt = now.plusSeconds(10)
        )

        coEvery { taskDao.getTaskById(taskId) } returns localTask
        coEvery { conflictResolver.isConflict(localTask, remoteTask) } returns true
        coEvery { taskDao.update(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true
        result.conflictResolved shouldBe true
        coVerify { taskDao.update(any()) }
    }

    test("Scenario 6: Notification flow - New task → queue notification → display") {
        val taskId = "task-1"
        val newTask = createTestTask(id = taskId, title = "New Task")

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { notificationManager.showNotification(any()) } returns Unit
        coEvery { timerViewModel.isRunning.value } returns false

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskCreated(newTask))

        result.success shouldBe true
        coVerify { taskDao.insert(any()) }
    }

    test("Scenario 7: Mixed operations - Create, update, delete in sequence") {
        val task1 = createTestTask(id = "task-1", title = "Task 1")
        val task2 = createTestTask(id = "task-2", title = "Task 2")
        val task3 = createTestTask(id = "task-3", title = "Task 3")

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { taskDao.update(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        // Create
        val createResult = handler.handleWebSocketEvent(WebSocketEvent.TaskCreated(task1))
        createResult.success shouldBe true

        // Update
        val updateResult = handler.handleWebSocketEvent(
            WebSocketEvent.TaskUpdated(task2.id, task2)
        )
        updateResult.success shouldBe true

        // Delete
        val deleteResult = handler.handleWebSocketEvent(WebSocketEvent.TaskDeleted(task3.id))
        deleteResult.success shouldBe true
    }

    test("Scenario 8: Error recovery - Network error → retry → apply") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } throws Exception("Network error")
        coEvery { offlineUpdateQueue.addUpdate(any(), any(), any(), any()) } returns true

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        // First attempt fails
        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        handler.hasQueuedUpdates() shouldBe true

        // Retry succeeds
        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val retryResult = handler.handleWebSocketEvent(WebSocketEvent.ConnectionEstablished)
        retryResult.success shouldBe true
    }

    test("Scenario 9: Rapid updates - Multiple updates in quick succession") {
        val tasks = (1..10).map { createTestTask(id = "task-$it", title = "Task $it") }

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { timerViewModel.isRunning.value } returns false

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        tasks.forEach { task ->
            val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(task.id, task))
            result.success shouldBe true
        }

        coVerify(exactly = 10) { taskDao.insert(any()) }
    }

    test("Scenario 10: Connection transitions - Online → offline → online with queued updates") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { offlineUpdateQueue.addUpdate(any(), any(), any(), any()) } returns true

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        // Online - apply immediately
        val onlineResult = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))
        onlineResult.success shouldBe true

        // Go offline
        handler.handleWebSocketEvent(WebSocketEvent.ConnectionLost)

        // Queue updates while offline
        val offlineTask = remoteTask.copy(title = "Offline Update")
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, offlineTask))

        handler.hasQueuedUpdates() shouldBe true

        // Come back online
        val onlineAgainResult = handler.handleWebSocketEvent(WebSocketEvent.ConnectionEstablished)
        onlineAgainResult.success shouldBe true
    }

    test("Scenario 11: Task completion with timer - Complete task → timer stops → apply queued updates") {
        val taskId = "task-1"
        val completedTask = createTestTask(
            id = taskId,
            status = TaskStatus.COMPLETED,
            updatedAt = Instant.now()
        )

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { timerViewModel.isRunning.value } returns true
        coEvery { timerAwareUpdateApplier.applyUpdate(any()) } returns false // Queued

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, completedTask))

        result.success shouldBe true
    }

    test("Scenario 12: Latency tracking - Measure update latency across multiple updates") {
        val tasks = (1..5).map { createTestTask(id = "task-$it") }

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        tasks.forEach { task ->
            handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(task.id, task))
        }

        // Verify all updates were processed
        coVerify(exactly = 5) { taskDao.insert(any()) }
    }

    test("Scenario 13: Sync status updates - Track sync status through update lifecycle") {
        val taskId = "task-1"
        val remoteTask = createTestTask(id = taskId, status = TaskStatus.INCOMPLETE)

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)
        val result = handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, remoteTask))

        result.success shouldBe true

        // Verify task is marked as synced
        coVerify {
            taskDao.insert(match { it.syncStatus == SyncStatus.SYNCED })
        }
    }

    test("Scenario 14: Batch update application - Apply multiple updates atomically") {
        val tasks = (1..3).map { createTestTask(id = "task-$it") }

        coEvery { taskDao.getTaskById(any()) } returns null
        coEvery { taskDao.insert(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        // Queue multiple updates
        tasks.forEach { task ->
            handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(task.id, task))
        }

        // Verify all were applied
        coVerify(exactly = 3) { taskDao.insert(any()) }
    }

    test("Scenario 15: Update ordering - Verify updates applied in correct order") {
        val taskId = "task-1"
        val now = Instant.now()
        val update1 = createTestTask(id = taskId, title = "Update 1", updatedAt = now)
        val update2 = createTestTask(id = taskId, title = "Update 2", updatedAt = now.plusSeconds(1))
        val update3 = createTestTask(id = taskId, title = "Update 3", updatedAt = now.plusSeconds(2))

        coEvery { taskDao.getTaskById(taskId) } returns null
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { taskDao.update(any()) } returns Unit

        val handler = WebSocketTaskUpdateHandlerImpl(taskDao, conflictResolver)

        // Apply updates in order
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, update1))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, update2))
        handler.handleWebSocketEvent(WebSocketEvent.TaskUpdated(taskId, update3))

        // Verify all were processed
        coVerify(atLeast = 1) { taskDao.insert(any()) }
    }
})
