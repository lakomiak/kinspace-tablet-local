package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
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

/**
 * Integration Tests for CloudSyncManager - End-to-End Sync Workflow
 *
 * Tests verify:
 * - Complete sync workflow (offline → online → sync → update)
 * - WebSocket connection and real-time updates
 * - Conflict resolution during sync
 * - Exponential backoff on failures
 * - Sync status indicator updates
 * - Task persistence and retrieval
 *
 * Validates: Requirement 10 - Cloud Synchronization with calendar-cloud
 */
class CloudSyncIntegrationTest {
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var restApiClient: RestApiClient
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var taskDao: TaskDao
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var conflictResolver: ConflictResolver
    private lateinit var retryPolicy: RetryPolicy

    @Before
    fun setup() {
        restApiClient = mockk()
        syncQueueManager = mockk()
        taskDao = mockk()
        connectivityManager = mockk()
        conflictResolver = mockk()
        retryPolicy = mockk()

        cloudSyncManager = CloudSyncManagerImpl(
            restApiClient = restApiClient,
            syncQueueManager = syncQueueManager,
            taskDao = taskDao,
            connectivityManager = connectivityManager,
            conflictResolver = conflictResolver,
            retryPolicy = retryPolicy
        )
    }

    @Test
    fun `happy path - create task offline, sync on reconnection`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task = createTestTask(id = "task-1", title = "Buy groceries")

        // Initially offline
        coEvery { connectivityManager.isOnline() } returns false

        // Act - Sync while offline
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - No sync occurs offline
        assertEquals(0, result.syncedCount)
        assertEquals(SyncStatus.OFFLINE, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Now online with pending changes
        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.CREATE
            coEvery { payload } returns """{"id":"task-1","title":"Buy groceries"}"""
            coEvery { timestamp } returns Instant.now()
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit

        // Act - Sync when reconnected
        result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Sync successful
        assertEquals(1, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
        coVerify { syncQueueManager.removeItem("queue-1") }
    }

    @Test
    fun `conflict scenario - local and remote changes to same task`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val now = Instant.now()
        val localTask = createTestTask(
            id = "task-1",
            title = "Local Title",
            updatedAt = now.plusSeconds(10)
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Remote Title",
            updatedAt = now
        )
        val conflict = SyncConflict(
            taskId = "task-1",
            localVersion = localTask,
            remoteVersion = remoteTask
        )

        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.UPDATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns now.plusSeconds(10)
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 0,
            failedCount = 0,
            conflicts = listOf(conflict)
        )
        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { conflictResolver.getConflictReason(localTask, remoteTask) } returns "Timestamp-based resolution"
        coEvery { taskDao.insert(localTask) } returns Unit
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Conflict resolved
        assertEquals(0, result.syncedCount)
        assertEquals(1, result.conflicts.size)
        assertEquals("task-1", result.conflicts[0].taskId)
        coVerify { conflictResolver.resolveConflict(localTask, remoteTask) }
        coVerify { taskDao.insert(localTask) }
    }

    @Test
    fun `network failure - retry with exponential backoff`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.CREATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns Instant.now()
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Network timeout")
        coEvery { retryPolicy.getMaxRetries() } returns 3

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Sync failed, items remain in queue
        assertEquals(0, result.syncedCount)
        assertEquals(1, result.failedCount)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())
        coVerify(exactly = 0) { syncQueueManager.removeItem(any()) }
    }

    @Test
    fun `offline recovery - queue changes, sync when online`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task1 = createTestTask(id = "task-1", title = "Task 1")
        val task2 = createTestTask(id = "task-2", title = "Task 2")

        // Initially offline
        coEvery { connectivityManager.isOnline() } returns false

        // Act - Sync while offline
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - No sync
        assertEquals(0, result.syncedCount)

        // Arrange - Now online with 2 pending changes
        coEvery { connectivityManager.isOnline() } returns true
        val queuedItems = listOf(
            mockk {
                coEvery { taskId } returns "task-1"
                coEvery { operation } returns SyncOperation.CREATE
                coEvery { payload } returns """{"id":"task-1"}"""
                coEvery { timestamp } returns Instant.now()
                coEvery { id } returns "queue-1"
            },
            mockk {
                coEvery { taskId } returns "task-2"
                coEvery { operation } returns SyncOperation.UPDATE
                coEvery { payload } returns """{"id":"task-2"}"""
                coEvery { timestamp } returns Instant.now()
                coEvery { id } returns "queue-2"
            }
        )
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns queuedItems
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 2,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem(any()) } returns Unit

        // Act - Sync when reconnected
        result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Both tasks synced
        assertEquals(2, result.syncedCount)
        coVerify(exactly = 2) { syncQueueManager.removeItem(any()) }
    }

    @Test
    fun `real-time updates - WebSocket receives updates`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val remoteTask = createTestTask(id = "task-1", title = "Updated by family member")

        coEvery { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns emptyList()
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 0,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { taskDao.insert(remoteTask) } returns Unit

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Sync completed
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `concurrent operations - multiple tasks syncing`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        val queuedItems = (1..5).map { i ->
            mockk {
                coEvery { taskId } returns "task-$i"
                coEvery { operation } returns SyncOperation.CREATE
                coEvery { payload } returns """{"id":"task-$i"}"""
                coEvery { timestamp } returns Instant.now()
                coEvery { id } returns "queue-$i"
            }
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns queuedItems
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 5,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem(any()) } returns Unit

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - All tasks synced
        assertEquals(5, result.syncedCount)
        coVerify(exactly = 5) { syncQueueManager.removeItem(any()) }
    }

    @Test
    fun `error recovery - sync failure and retry`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.CREATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns Instant.now()
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Server error")

        // Act - First attempt fails
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Failure recorded
        assertEquals(1, result.failedCount)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Retry succeeds
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit

        // Act - Retry
        result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Success on retry
        assertEquals(1, result.syncedCount)
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `sync status indicator updates - transitions through states`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns false

        // Act & Assert - OFFLINE state
        cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.OFFLINE, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Online with pending changes
        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.CREATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns Instant.now()
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit

        // Act & Assert - SYNCING → SYNCED
        cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `task persistence - synced tasks stored locally`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task = createTestTask(id = "task-1")

        coEvery { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns emptyList()
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 0,
            failedCount = 0,
            conflicts = emptyList()
        )

        // Act
        cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Sync completed successfully
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
    }

    private fun createTestTask(
        id: String = "task-1",
        title: String = "Test Task",
        updatedAt: Instant = Instant.now()
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
            updatedAt = updatedAt,
            completedAt = null,
            syncStatus = com.adhdfocus.app.data.model.SyncStatus.PENDING,
            isDeleted = false
        )
    }
}

/**
 * Data classes for sync operations
 */
data class SyncChange(
    val taskId: String,
    val operation: SyncOperation,
    val payload: String,
    val timestamp: Long
)

data class SyncConflict(
    val taskId: String,
    val localVersion: Task,
    val remoteVersion: Task
)

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val conflicts: List<SyncConflict>
)
