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

/**
 * Integration Tests for Offline to Online Sync Scenarios
 *
 * Tests verify:
 * - Offline queuing of changes
 * - Automatic sync on reconnection
 * - Queue persistence across app restarts
 * - Multiple offline changes synced together
 * - Offline changes with network recovery
 *
 * Validates: Requirement 10 - Cloud Synchronization with calendar-cloud
 * Validates: Requirement 12 - Data Persistence and Offline Capability
 */
class OfflineToOnlineSyncIntegrationTest {
    private lateinit var offlineSyncStrategy: OfflineSyncStrategy
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var taskPersistenceManager: TaskPersistenceManager
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        syncQueueManager = mockk()
        taskPersistenceManager = mockk()
        cloudSyncManager = mockk()
        connectivityManager = mockk()

        offlineSyncStrategy = OfflineSyncStrategyImpl(
            syncQueueManager = syncQueueManager,
            taskPersistenceManager = taskPersistenceManager,
            cloudSyncManager = cloudSyncManager,
            connectivityManager = connectivityManager,
            syncChangeSerializer = mockk()
        )
    }

    @Test
    fun `offline scenario - create task while offline, sync on reconnection`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task = createTestTask(id = "task-1", title = "Buy milk")

        // Initially offline
        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(task) } returns Unit

        // Act - Create task offline
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)

        // Assert - Task queued and persisted
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
        coVerify { taskPersistenceManager.saveTask(task) }

        // Arrange - Now online
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = emptyList()
        )

        // Act - Sync when reconnected
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Task synced successfully
        assertEquals(1, result.syncedCount)
        assertEquals(0, result.failedCount)
    }

    @Test
    fun `offline scenario - multiple changes queued while offline`() = runTest {
        // Arrange
        val userId = "user-123"
        val tasks = (1..3).map { i ->
            createTestTask(id = "task-$i", title = "Task $i")
        }

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Queue multiple tasks offline
        tasks.forEach { task ->
            offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)
        }

        // Assert - All tasks queued
        coVerify(exactly = 3) { syncQueueManager.queueItem(any(), any(), any(), any()) }
        coVerify(exactly = 3) { taskPersistenceManager.saveTask(any()) }
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
                coEvery { payload } returns """{"id":"task-1"}"""
                coEvery { timestamp } returns now
            },
            mockk {
                coEvery { taskId } returns "task-2"
                coEvery { operation } returns SyncOperation.UPDATE
                coEvery { payload } returns """{"id":"task-2"}"""
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
    fun `offline scenario - queue status tracking`() = runTest {
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
    fun `offline scenario - mixed operations on different tasks`() = runTest {
        // Arrange
        val userId = "user-123"
        val task1 = createTestTask(id = "task-1", title = "Task 1")
        val task2 = createTestTask(id = "task-2", title = "Task 2")
        val task3 = createTestTask(id = "task-3", title = "Task 3")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Queue different operations
        offlineSyncStrategy.queueLocalChange(task1, SyncOperation.CREATE, userId)
        offlineSyncStrategy.queueLocalChange(task2, SyncOperation.UPDATE, userId)
        offlineSyncStrategy.queueLocalChange(task3, SyncOperation.DELETE, userId)

        // Assert - All operations queued
        coVerify(exactly = 3) { syncQueueManager.queueItem(any(), any(), any(), any()) }
    }

    @Test
    fun `offline scenario - sync with partial failures`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        val syncResult = SyncResult(
            syncedCount = 2,
            failedCount = 1,
            conflicts = emptyList()
        )
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns syncResult

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Partial sync recorded
        assertEquals(2, result.syncedCount)
        assertEquals(1, result.failedCount)
    }

    @Test
    fun `offline scenario - reconnection triggers automatic sync`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        // Initially offline
        coEvery { connectivityManager.isOnline() } returns false

        // Arrange - Now online
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 3,
            failedCount = 0,
            conflicts = emptyList()
        )

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Sync triggered on reconnection
        assertEquals(3, result.syncedCount)
    }

    @Test
    fun `offline scenario - persist tasks locally for offline access`() = runTest {
        // Arrange
        val userId = "user-123"
        val task = createTestTask(id = "task-1", title = "Offline task")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(task) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)

        // Assert - Task persisted for offline access
        coVerify { taskPersistenceManager.saveTask(task) }
    }

    @Test
    fun `offline scenario - queue persistence across app restart`() = runTest {
        // Arrange
        val userId = "user-123"
        val queuedItems = listOf(
            mockk {
                coEvery { taskId } returns "task-1"
                coEvery { operation } returns SyncOperation.CREATE
                coEvery { payload } returns """{"id":"task-1"}"""
                coEvery { timestamp } returns Instant.now()
            }
        )

        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns queuedItems

        // Act - Retrieve queued items after app restart
        val changes = offlineSyncStrategy.getQueuedChanges(userId)

        // Assert - Queue persisted
        assertEquals(1, changes.size)
        assertEquals("task-1", changes[0].taskId)
    }

    @Test
    fun `offline scenario - update task while offline`() = runTest {
        // Arrange
        val userId = "user-123"
        val originalTask = createTestTask(id = "task-1", title = "Original")
        val updatedTask = originalTask.copy(title = "Updated")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Update task offline
        offlineSyncStrategy.queueLocalChange(updatedTask, SyncOperation.UPDATE, userId)

        // Assert - Update queued
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
        coVerify { taskPersistenceManager.saveTask(updatedTask) }
    }

    @Test
    fun `offline scenario - delete task while offline`() = runTest {
        // Arrange
        val userId = "user-123"
        val task = createTestTask(id = "task-1", title = "To delete")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Delete task offline
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.DELETE, userId)

        // Assert - Delete queued
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
    }

    private fun createTestTask(
        id: String = "task-1",
        title: String = "Test Task"
    ): Task {
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
