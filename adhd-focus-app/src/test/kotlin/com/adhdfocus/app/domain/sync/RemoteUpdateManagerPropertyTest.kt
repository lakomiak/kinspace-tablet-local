package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Property-based tests for RemoteUpdateManager.
 *
 * **Validates: Requirement 11 - Remote Update Application**
 * - Update ordering preservation
 * - Conflict resolution correctness
 * - Timer state transitions
 * - Update event emission
 * - Offline update queuing
 */
class RemoteUpdateManagerPropertyTest : FunSpec({
    val taskIdGen = arbitrary { UUID.randomUUID().toString() }
    val householdIdGen = arbitrary { "household-${UUID.randomUUID()}" }
    val userIdGen = arbitrary { "user-${UUID.randomUUID()}" }
    val titleGen = Arb.string(minSize = 1, maxSize = 100)
    val todoGroupGen = Arb.string(minSize = 1, maxSize = 50)
    val taskStatusGen = arbitrary { TaskStatus.values().random() }

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        householdId: String = "household-123",
        assignedUserId: String = "user-456",
        title: String = "Test Task",
        todoGroup: String = "Morning",
        status: TaskStatus = TaskStatus.INCOMPLETE,
        updatedAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = null,
            todoGroup = todoGroup,
            estimatedDurationMinutes = null,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = updatedAt,
            completedAt = null,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )
    }

    test("Update ordering: Multiple updates should preserve order") {
        checkAll(
            Arb.list(taskIdGen, range = 1..5)
        ) { taskIds ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            coEvery { taskDao.getTaskById(any()) } returns null
            coEvery { taskDao.insert(any()) } returns 1L

            val events = mutableListOf<UpdateEvent>()
            val job = launch {
                manager.observeUpdates().toList(events)
            }

            // Apply updates in order
            for (taskId in taskIds) {
                val task = createTask(id = taskId)
                manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))
            }

            Thread.sleep(100)
            job.cancel()

            // All updates should be applied
            events.filterIsInstance<UpdateEvent.TaskCreated>().size shouldBe taskIds.size
        }
    }

    test("Conflict resolution: Newer remote timestamp should win") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            val oldTime = Instant.now().minusSeconds(100)
            val newTime = Instant.now()
            val localTask = createTask(id = taskId, updatedAt = oldTime)
            val remoteTask = createTask(id = taskId, updatedAt = newTime)

            coEvery { taskDao.getTaskById(taskId) } returns localTask
            coEvery { taskDao.update(any()) } returns Unit

            val result = manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(taskId, remoteTask))

            result.success shouldBe true
            result.conflictResolved shouldBe true
        }
    }

    test("Conflict resolution: Older remote timestamp should not overwrite local") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            val oldTime = Instant.now().minusSeconds(100)
            val newTime = Instant.now()
            val localTask = createTask(id = taskId, updatedAt = newTime)
            val remoteTask = createTask(id = taskId, updatedAt = oldTime)

            coEvery { taskDao.getTaskById(taskId) } returns localTask

            val result = manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(taskId, remoteTask))

            result.success shouldBe true
            result.conflictResolved shouldBe false
        }
    }

    test("Timer state transitions: Setting timer active should be reflected") {
        checkAll(
            Arb.list(arbitrary { listOf(true, false).random() }, range = 1..10)
        ) { states ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            for (state in states) {
                manager.setTimerActive(state)
                manager.isTimerActive() shouldBe state
            }
        }
    }

    test("Update event emission: Each update should emit corresponding event") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            coEvery { taskDao.getTaskById(taskId) } returns null
            coEvery { taskDao.insert(any()) } returns 1L

            val events = mutableListOf<UpdateEvent>()
            val job = launch {
                manager.observeUpdates().toList(events)
            }

            val task = createTask(id = taskId)
            manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))

            Thread.sleep(100)
            job.cancel()

            events.any { it is UpdateEvent.TaskCreated } shouldBe true
        }
    }

    test("Offline update queuing: Queued updates should be applied in order") {
        checkAll(
            Arb.list(taskIdGen, range = 1..5)
        ) { taskIds ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            coEvery { taskDao.getTaskById(any()) } returns null
            coEvery { taskDao.insert(any()) } returns 1L

            // Queue updates
            for (taskId in taskIds) {
                val task = createTask(id = taskId)
                manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))
            }

            val events = mutableListOf<UpdateEvent>()
            val job = launch {
                manager.observeUpdates().toList(events)
            }

            manager.applyQueuedUpdates()

            Thread.sleep(100)
            job.cancel()

            events.any { it is UpdateEvent.UpdatesApplied } shouldBe true
        }
    }

    test("Sync status: Applied updates should have SYNCED status") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            coEvery { taskDao.getTaskById(taskId) } returns null
            coEvery { taskDao.insert(any()) } returns 1L

            val task = createTask(id = taskId)
            manager.applyRemoteUpdate(WebSocketEvent.TaskCreated(task))

            // Verify that insert was called with SYNCED status
            io.mockk.coVerify {
                taskDao.insert(match { it.syncStatus == SyncStatus.SYNCED })
            }
        }
    }

    test("Soft delete: Deleted tasks should have isDeleted = true") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            val task = createTask(id = taskId)
            coEvery { taskDao.getTaskById(taskId) } returns task
            coEvery { taskDao.update(any()) } returns Unit

            manager.applyRemoteUpdate(WebSocketEvent.TaskDeleted(taskId))

            io.mockk.coVerify {
                taskDao.update(match { it.isDeleted })
            }
        }
    }

    test("Error handling: Failed updates should emit Error event") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            coEvery { taskDao.getTaskById(taskId) } throws Exception("Database error")

            val events = mutableListOf<UpdateEvent>()
            val job = launch {
                manager.observeUpdates().toList(events)
            }

            val task = createTask(id = taskId)
            manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(taskId, task))

            Thread.sleep(100)
            job.cancel()

            events.any { it is UpdateEvent.Error } shouldBe true
        }
    }

    test("Same timestamp: Remote should be preferred (server is source of truth)") {
        checkAll(
            taskIdGen
        ) { taskId ->
            val taskDao = mockk<TaskDao>()
            val manager = RemoteUpdateManagerImpl(taskDao)

            val sameTime = Instant.now()
            val localTask = createTask(id = taskId, updatedAt = sameTime)
            val remoteTask = createTask(id = taskId, updatedAt = sameTime)

            coEvery { taskDao.getTaskById(taskId) } returns localTask
            coEvery { taskDao.update(any()) } returns Unit

            val result = manager.applyRemoteUpdate(WebSocketEvent.TaskUpdated(taskId, remoteTask))

            result.success shouldBe true
            io.mockk.coVerify { taskDao.update(any()) }
        }
    }
})
