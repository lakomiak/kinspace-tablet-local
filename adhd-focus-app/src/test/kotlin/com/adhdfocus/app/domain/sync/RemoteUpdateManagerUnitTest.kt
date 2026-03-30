package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class RemoteUpdateManagerUnitTest : FunSpec({
    val taskDao = mockk<TaskDao>()
    val manager = RemoteUpdateManagerImpl(taskDao)

    val householdId = "household-123"
    val userId = "user-456"

    fun createTestTask(
        id: String = UUID.randomUUID().toString(),
        status: TaskStatus = TaskStatus.INCOMPLETE,
        updatedAt: Instant = Instant.now(),
        syncStatus: SyncStatus = SyncStatus.PENDING
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
            syncStatus = syncStatus,
            isDeleted = false
        )
    }

    test("applyRemoteUpdate with TaskUpdated event should insert new task") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(task.id, task))

        result.success shouldBe true
        coVerify { taskDao.insert(any()) }
    }

    test("applyRemoteUpdate with TaskUpdated event should update existing task if remote is newer") {
        val oldTime = Instant.now().minusSeconds(100)
        val newTime = Instant.now()
        val localTask = createTestTask(updatedAt = oldTime)
        val remoteTask = createTestTask(updatedAt = newTime)

        coEvery { taskDao.getTaskById(localTask.id) } returns localTask
        coEvery { taskDao.update(any()) } returns Unit

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(remoteTask.id, remoteTask))

        result.success shouldBe true
        result.conflictResolved shouldBe true
        coVerify { taskDao.update(any()) }
    }

    test("applyRemoteUpdate with TaskUpdated event should keep local if local is newer") {
        val oldTime = Instant.now().minusSeconds(100)
        val newTime = Instant.now()
        val localTask = createTestTask(updatedAt = newTime)
        val remoteTask = createTestTask(updatedAt = oldTime)

        coEvery { taskDao.getTaskById(localTask.id) } returns localTask

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(remoteTask.id, remoteTask))

        result.success shouldBe true
        result.conflictResolved shouldBe false
        coVerify(exactly = 0) { taskDao.update(any()) }
    }

    test("applyRemoteUpdate with TaskDeleted event should soft delete task") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns task
        coEvery { taskDao.update(any()) } returns Unit

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskDeleted(task.id))

        result.success shouldBe true
        coVerify { taskDao.update(match { it.isDeleted }) }
    }

    test("applyRemoteUpdate with TaskDeleted event should handle missing task") {
        val taskId = UUID.randomUUID().toString()
        coEvery { taskDao.getTaskById(taskId) } returns null

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskDeleted(taskId))

        result.success shouldBe false
    }

    test("applyRemoteUpdate with TaskCreated event should insert new task") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))

        result.success shouldBe true
        coVerify { taskDao.insert(any()) }
    }

    test("applyRemoteUpdate with TaskCreated event should not insert if task exists") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns task

        val result = manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))

        result.success shouldBe false
        coVerify(exactly = 0) { taskDao.insert(any()) }
    }

    test("isTimerActive should return false initially") {
        manager.isTimerActive() shouldBe false
    }

    test("setTimerActive should update timer state") {
        manager.setTimerActive(true)
        manager.isTimerActive() shouldBe true

        manager.setTimerActive(false)
        manager.isTimerActive() shouldBe false
    }

    test("observeUpdates should emit TaskUpdated event") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        val events = mutableListOf<UpdateEvent>()
        val job = launch {
            manager.observeUpdates().toList(events)
        }

        manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(task.id, task))

        // Give time for event emission
        Thread.sleep(100)
        job.cancel()

        events.any { it is UpdateEvent.TaskUpdated } shouldBe true
    }

    test("observeUpdates should emit TaskDeleted event") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns task
        coEvery { taskDao.update(any()) } returns Unit

        val events = mutableListOf<UpdateEvent>()
        val job = launch {
            manager.observeUpdates().toList(events)
        }

        manager.applyRemoteUpdate(WebSocketEvent.TaskDeleted(task.id))

        Thread.sleep(100)
        job.cancel()

        events.any { it is UpdateEvent.TaskDeleted } shouldBe true
    }

    test("observeUpdates should emit TaskCreated event") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        val events = mutableListOf<UpdateEvent>()
        val job = launch {
            manager.observeUpdates().toList(events)
        }

        manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))

        Thread.sleep(100)
        job.cancel()

        events.any { it is UpdateEvent.TaskCreated } shouldBe true
    }

    test("applyQueuedUpdates should apply all queued updates") {
        val task1 = createTestTask()
        val task2 = createTestTask()

        coEvery { taskDao.getTaskById(task1.id) } returns null
        coEvery { taskDao.getTaskById(task2.id) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task1))
        manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task2))

        val events = mutableListOf<UpdateEvent>()
        val job = launch {
            manager.observeUpdates().toList(events)
        }

        manager.applyQueuedUpdates()

        Thread.sleep(100)
        job.cancel()

        events.any { it is UpdateEvent.UpdatesApplied } shouldBe true
    }

    test("applyRemoteUpdate should emit Error event on exception") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } throws Exception("Database error")

        val events = mutableListOf<UpdateEvent>()
        val job = launch {
            manager.observeUpdates().toList(events)
        }

        manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(task.id, task))

        Thread.sleep(100)
        job.cancel()

        events.any { it is UpdateEvent.Error } shouldBe true
    }

    test("applyRemoteUpdate should set syncStatus to SYNCED") {
        val task = createTestTask()
        coEvery { taskDao.getTaskById(task.id) } returns null
        coEvery { taskDao.insert(any()) } returns 1L

        manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(task.id, task))

        coVerify {
            taskDao.insert(match { it.syncStatus == SyncStatus.SYNCED })
        }
    }
})
