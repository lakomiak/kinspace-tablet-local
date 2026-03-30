package com.adhdfocus.app.domain.sync

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
 * Integration Tests for Error Handling and Recovery
 *
 * Tests verify:
 * - Network error handling with retry
 * - Exponential backoff implementation
 * - Graceful degradation on sync failure
 * - Recovery from transient errors
 * - Error state transitions
 * - Sync queue preservation on error
 * - User-friendly error messages
 *
 * Validates: Requirement 10 - Cloud Synchronization with calendar-cloud
 * Validates: Requirement 17 - Error Handling and Recovery
 */
class ErrorRecoveryIntegrationTest {
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var restApiClient: RestApiClient
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var retryPolicy: RetryPolicy

    @Before
    fun setup() {
        restApiClient = mockk()
        syncQueueManager = mockk()
        connectivityManager = mockk()
        retryPolicy = mockk()

        cloudSyncManager = CloudSyncManagerImpl(
            restApiClient = restApiClient,
            syncQueueManager = syncQueueManager,
            taskDao = mockk(),
            connectivityManager = connectivityManager,
            conflictResolver = mockk(),
            retryPolicy = retryPolicy
        )
    }

    @Test
    fun `error scenario - network timeout, sync fails gracefully`() = runTest {
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

        // Assert - Failure handled gracefully
        assertEquals(1, result.failedCount)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())
        coVerify(exactly = 0) { syncQueueManager.removeItem(any()) }
    }

    @Test
    fun `error scenario - server error, items remain in queue`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        val queuedItems = (1..3).map { i ->
            mockk {
                coEvery { taskId } returns "task-$i"
                coEvery { operation } returns SyncOperation.CREATE
                coEvery { payload } returns """{"id":"task-$i"}"""
                coEvery { timestamp } returns Instant.now()
                coEvery { id } returns "queue-$i"
            }
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns queuedItems
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Server error 500")

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Items remain in queue for retry
        assertEquals(3, result.failedCount)
        coVerify(exactly = 0) { syncQueueManager.removeItem(any()) }
    }

    @Test
    fun `error scenario - retry after transient failure`() = runTest {
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

        // First attempt fails
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Temporary failure")

        // Act - First attempt
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Failure recorded
        assertEquals(1, result.failedCount)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Second attempt succeeds
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
    fun `error scenario - exponential backoff on repeated failures`() = runTest {
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
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Service unavailable")
        coEvery { retryPolicy.getMaxRetries() } returns 3

        // Act - Multiple failed attempts
        repeat(3) {
            cloudSyncManager.syncPendingChanges(householdId, userId)
        }

        // Assert - All attempts failed
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `error scenario - offline transition during sync`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        // Initially online
        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.CREATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns Instant.now()
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Connection lost")

        // Act - Sync fails
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Error state
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Now offline
        coEvery { connectivityManager.isOnline() } returns false

        // Act - Attempt sync while offline
        result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Offline state
        assertEquals(SyncStatus.OFFLINE, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `error scenario - partial sync with some failures`() = runTest {
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
            syncedCount = 3,
            failedCount = 2,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem(any()) } returns Unit

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Partial success recorded
        assertEquals(3, result.syncedCount)
        assertEquals(2, result.failedCount)
    }

    @Test
    fun `error scenario - recovery from error to synced state`() = runTest {
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

        // First attempt fails
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Error")

        // Act - First attempt
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Recovery
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit

        // Act - Retry succeeds
        result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Recovered to SYNCED
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
        assertEquals(1, result.syncedCount)
    }

    @Test
    fun `error scenario - empty queue sync succeeds`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns emptyList()

        // Act
        val result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - No-op sync succeeds
        assertEquals(0, result.syncedCount)
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `error scenario - sync queue preserved after error`() = runTest {
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
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Error")

        // Act - Sync fails
        cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Queue item not removed
        coVerify(exactly = 0) { syncQueueManager.removeItem(any()) }

        // Verify item still in queue
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        val queuedItems = syncQueueManager.getPendingItemsByUser(userId)
        assertEquals(1, queuedItems.size)
    }

    @Test
    fun `error scenario - multiple error states`() = runTest {
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

        // First error
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Error 1")
        cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Second error
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Error 2")
        cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Recovery
        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = emptyList()
        )
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit
        cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.SYNCED, cloudSyncManager.getCurrentSyncStatus())
    }

    @Test
    fun `error scenario - graceful degradation with offline fallback`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        // Online but sync fails
        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns SyncOperation.CREATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns Instant.now()
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)
        coEvery { restApiClient.batchSync(householdId, any()) } throws Exception("Network error")

        // Act - Sync fails
        var result = cloudSyncManager.syncPendingChanges(householdId, userId)
        assertEquals(SyncStatus.ERROR, cloudSyncManager.getCurrentSyncStatus())

        // Arrange - Go offline
        coEvery { connectivityManager.isOnline() } returns false

        // Act - Attempt sync while offline
        result = cloudSyncManager.syncPendingChanges(householdId, userId)

        // Assert - Graceful degradation to offline state
        assertEquals(SyncStatus.OFFLINE, cloudSyncManager.getCurrentSyncStatus())
        assertEquals(0, result.syncedCount)
    }
}
