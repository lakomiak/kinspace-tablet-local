package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.persistence.TaskPersistenceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineSyncStrategyIntegrationTest {
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var taskPersistenceManager: TaskPersistenceManager
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var syncChangeSerializer: SyncChangeSerializer
    private lateinit var offlineSyncStrategy: OfflineSyncStrategy

    @Before
    fun setup() {
        syncQueueManager = mockk()
        taskPersistenceManager = mockk()
        cloudSyncManager = mockk()
        connectivityManager = mockk()
        syncChangeSerializer = mockk()

        offlineSyncStrategy = OfflineSyncStrategyImpl(
            syncQueueManager = syncQueueManager,
            taskPersistenceManager = taskPersistenceManager,
            cloudSyncManager = cloudSyncManager,
            connectivityManager = connectivityManager,
            syncChangeSerializer = syncChangeSerializer
        )
    }

    @Test
    fun `offline scenario - queue changes and sync on reconnection`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task1 = createTestTask(id = "task-1", title = "Task 1")
        val task2 = createTestTask(id = "task-2", title = "Task 2")

        // Initially offline
        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncChangeSerializer.serializeTask(any()) } returns """{"id":"task-1"}"""
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Queue changes while offline
        offlineSyncStrategy.queueLocalChange(task1, SyncOperation.CREATE, userId)
        offlineSyncStrategy.queueLocalChange(task2, SyncOperation.UPDATE, userId)

        // Assert - Changes queued
        coVerify(exactly = 2) { syncQueueManager.queueItem(any(), any(), any(), any()) }
        coVerify(exactly = 2) { taskPersistenceManager.saveTask(any()) }

        // Arrange - Now online
        coEvery { connectivityManager.isOnline() } returns true
        val syncResult = SyncResult(syncedCount = 2, failedCount = 0, conflicts = emptyList())
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns syncResult

        // Act - Sync when reconnected
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Sync successful
        assertEquals(2, result.syncedCount)
        assertEquals(0, result.failedCount)
        coVerify { cloudSyncManager.syncPendingChanges(householdId, userId) }
    }

    @Test
    fun `offline scenario - retrieve queued changes`() = runTest {
        // Arrange
        val userId = "user-123"
        val now = Instant.now()
        val queuedItems = listOf(
            mockk {
                coEvery { taskId } returns "task-1"
                coEvery { operation } returns SyncOperation.CREATE
                coEvery { payload } returns """{"id":"task-1","title":"Task 1"}"""
                coEvery { timestamp } returns now
            },
            mockk {
                coEvery { taskId } returns "task-2"
                coEvery { operation } returns SyncOperation.UPDATE
                coEvery { payload } returns """{"id":"task-2","title":"Task 2"}"""
                coEvery { timestamp } returns now
            }
        )

        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns queuedItems

        // Act
        val changes = offlineSyncStrategy.getQueuedChanges(userId)

        // Assert
        assertEquals(2, changes.size)
        assertEquals("task-1", changes[0].taskId)
        assertEquals(SyncOperation.CREATE, changes[0].operation)
        assertEquals("task-2", changes[1].taskId)
        assertEquals(SyncOperation.UPDATE, changes[1].operation)
    }

    @Test
    fun `offline scenario - clear queue after successful sync`() = runTest {
        // Arrange
        val userId = "user-123"

        coEvery { syncQueueManager.removeItemsByUser(userId) } returns Unit

        // Act
        offlineSyncStrategy.clearQueue(userId)

        // Assert
        coVerify { syncQueueManager.removeItemsByUser(userId) }
    }

    @Test
    fun `offline scenario - check queue status`() = runTest {
        // Arrange
        val userId = "user-123"

        coEvery { syncQueueManager.getPendingItemCount(userId) } returns 3
        coEvery { syncQueueManager.hasPendingItems(userId) } returns true

        // Act
        val count = offlineSyncStrategy.getQueuedChangeCount(userId)
        val hasChanges = offlineSyncStrategy.hasQueuedChanges(userId)

        // Assert
        assertEquals(3, count)
        assertTrue(hasChanges)
    }

    @Test
    fun `offline scenario - multiple operations on same task`() = runTest {
        // Arrange
        val userId = "user-123"
        val taskId = "task-1"
        val task = createTestTask(id = taskId)

        coEvery { syncChangeSerializer.serializeTask(any()) } returns """{"id":"$taskId"}"""
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Queue multiple operations on same task
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)
        offlineSyncStrategy.queueLocalChange(task.copy(title = "Updated"), SyncOperation.UPDATE, userId)

        // Assert
        coVerify(exactly = 2) { syncQueueManager.queueItem(taskId, userId, any(), any()) }
    }

    @Test
    fun `offline scenario - sync with conflicts`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val localTask = createTestTask(id = "task-1", title = "Local Title")
        val remoteTask = createTestTask(id = "task-1", title = "Remote Title")
        val conflict = SyncConflict(
            taskId = "task-1",
            localVersion = localTask,
            remoteVersion = remoteTask
        )

        coEvery { connectivityManager.isOnline() } returns true
        val syncResult = SyncResult(
            syncedCount = 0,
            failedCount = 0,
            conflicts = listOf(conflict)
        )
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns syncResult

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert
        assertEquals(0, result.syncedCount)
        assertEquals(1, result.conflicts.size)
        assertEquals("task-1", result.conflicts[0].taskId)
    }

    @Test
    fun `offline scenario - sync failure and retry`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        val failureResult = SyncResult(syncedCount = 0, failedCount = 2, conflicts = emptyList())
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns failureResult

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert
        assertEquals(0, result.syncedCount)
        assertEquals(2, result.failedCount)
    }

    @Test
    fun `offline scenario - persist and retrieve tasks locally`() = runTest {
        // Arrange
        val userId = "user-123"
        val task = createTestTask()

        coEvery { syncChangeSerializer.serializeTask(task) } returns """{"id":"task-1"}"""
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(task) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)

        // Assert - Task persisted locally
        coVerify { taskPersistenceManager.saveTask(task) }
    }

    private fun createTestTask(id: String = "task-1", title: String = "Test Task"): Task {
        return Task(
            id = id,
            householdId = "household-123",
            assignedUserId = "user-123",
            title = title,
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = TaskStatus.INCOMPLETE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = com.adhdfocus.app.data.model.SyncStatus.PENDING,
            isDeleted = false
        )
    }
}
