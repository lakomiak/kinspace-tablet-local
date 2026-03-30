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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineSyncStrategyUnitTest {
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
    fun `queueLocalChange should queue task and persist locally`() = runTest {
        // Arrange
        val task = createTestTask()
        val userId = "user-123"
        val operation = SyncOperation.CREATE
        val payload = """{"id":"task-1","title":"Test Task"}"""

        coEvery { syncChangeSerializer.serializeTask(task) } returns payload
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(task) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(task, operation, userId)

        // Assert
        coVerify { syncQueueManager.queueItem(task.id, userId, operation, payload) }
        coVerify { taskPersistenceManager.saveTask(task) }
    }

    @Test
    fun `queueLocalChange should throw on empty task ID`() = runTest {
        // Arrange
        val task = createTestTask().copy(id = "")
        val userId = "user-123"

        // Act & Assert
        try {
            offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Task ID") ?: false)
        }
    }

    @Test
    fun `queueLocalChange should throw on empty user ID`() = runTest {
        // Arrange
        val task = createTestTask()
        val userId = ""

        // Act & Assert
        try {
            offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("User ID") ?: false)
        }
    }

    @Test
    fun `syncQueuedChanges should return empty result when offline`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns false

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert
        assertEquals(0, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `syncQueuedChanges should call cloudSyncManager when online`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val expectedResult = SyncResult(syncedCount = 2, failedCount = 0, conflicts = emptyList())

        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns expectedResult

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert
        assertEquals(expectedResult, result)
        coVerify { cloudSyncManager.syncPendingChanges(householdId, userId) }
    }

    @Test
    fun `syncQueuedChanges should throw on empty household ID`() = runTest {
        // Arrange
        val householdId = ""
        val userId = "user-123"

        // Act & Assert
        try {
            offlineSyncStrategy.syncQueuedChanges(householdId, userId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Household ID") ?: false)
        }
    }

    @Test
    fun `getQueuedChanges should return list of sync changes`() = runTest {
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

        // Act
        val changes = offlineSyncStrategy.getQueuedChanges(userId)

        // Assert
        assertEquals(1, changes.size)
        assertEquals("task-1", changes[0].taskId)
        assertEquals(SyncOperation.CREATE, changes[0].operation)
    }

    @Test
    fun `getQueuedChanges should return empty list when no queued items`() = runTest {
        // Arrange
        val userId = "user-123"

        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns emptyList()

        // Act
        val changes = offlineSyncStrategy.getQueuedChanges(userId)

        // Assert
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `clearQueue should remove all queued items for user`() = runTest {
        // Arrange
        val userId = "user-123"

        coEvery { syncQueueManager.removeItemsByUser(userId) } returns Unit

        // Act
        offlineSyncStrategy.clearQueue(userId)

        // Assert
        coVerify { syncQueueManager.removeItemsByUser(userId) }
    }

    @Test
    fun `getQueuedChangeCount should return count from queue manager`() = runTest {
        // Arrange
        val userId = "user-123"
        val expectedCount = 5

        coEvery { syncQueueManager.getPendingItemCount(userId) } returns expectedCount

        // Act
        val count = offlineSyncStrategy.getQueuedChangeCount(userId)

        // Assert
        assertEquals(expectedCount, count)
    }

    @Test
    fun `hasQueuedChanges should return true when items exist`() = runTest {
        // Arrange
        val userId = "user-123"

        coEvery { syncQueueManager.hasPendingItems(userId) } returns true

        // Act
        val hasChanges = offlineSyncStrategy.hasQueuedChanges(userId)

        // Assert
        assertTrue(hasChanges)
    }

    @Test
    fun `hasQueuedChanges should return false when no items exist`() = runTest {
        // Arrange
        val userId = "user-123"

        coEvery { syncQueueManager.hasPendingItems(userId) } returns false

        // Act
        val hasChanges = offlineSyncStrategy.hasQueuedChanges(userId)

        // Assert
        assertFalse(hasChanges)
    }

    @Test
    fun `clearQueue should throw on empty user ID`() = runTest {
        // Arrange
        val userId = ""

        // Act & Assert
        try {
            offlineSyncStrategy.clearQueue(userId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("User ID") ?: false)
        }
    }

    @Test
    fun `getQueuedChangeCount should throw on empty user ID`() = runTest {
        // Arrange
        val userId = ""

        // Act & Assert
        try {
            offlineSyncStrategy.getQueuedChangeCount(userId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("User ID") ?: false)
        }
    }

    @Test
    fun `hasQueuedChanges should throw on empty user ID`() = runTest {
        // Arrange
        val userId = ""

        // Act & Assert
        try {
            offlineSyncStrategy.hasQueuedChanges(userId)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("User ID") ?: false)
        }
    }

    private fun createTestTask(): Task {
        return Task(
            id = "task-1",
            householdId = "household-123",
            assignedUserId = "user-123",
            title = "Test Task",
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
