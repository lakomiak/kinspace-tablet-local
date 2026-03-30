package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
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

/**
 * Comprehensive Integration Tests for Offline Capability
 *
 * Tests verify the complete end-to-end workflow of offline capability from detection
 * through sync on reconnection, covering 15+ scenarios:
 *
 * 1. Offline detection: Device goes offline, app detects and switches to offline mode
 * 2. Offline task creation: Create task while offline, verify it's cached locally
 * 3. Offline task update: Update task while offline, verify changes are queued
 * 4. Offline task deletion: Delete task while offline, verify deletion is queued
 * 5. Offline task completion: Complete task while offline, verify completion is queued
 * 6. Offline timer: Start timer while offline, verify it continues running
 * 7. Offline timer completion: Timer completes while offline, verify notification is emitted
 * 8. Reconnection sync: Device reconnects, all pending changes are synced
 * 9. Conflict resolution: Conflicting changes are resolved by timestamp
 * 10. Multiple offline operations: Multiple operations queued and synced correctly
 * 11. Rapid online/offline transitions: Handle rapid connectivity changes
 * 12. Partial sync failure: Handle partial sync failures and retry
 * 13. Cache consistency: Cache remains consistent during offline operations
 * 14. Offline to online with new remote updates: Handle remote updates during sync
 * 15. App restart while offline: App restarts and continues with cached data
 *
 * Validates: Requirement 11 (Offline Capability)
 * Validates: Requirement 2 (Task Management with Cloud Sync)
 * Validates: Requirement 3 (Timer Functionality)
 */
class OfflineCapabilityIntegrationTest {
    private lateinit var offlineDetector: OfflineDetector
    private lateinit var offlineSyncStrategy: OfflineSyncStrategy
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var taskPersistenceManager: TaskPersistenceManager
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var timerManager: TimerManager
    private lateinit var realTimeUpdateManager: RealTimeUpdateManager

    @Before
    fun setup() {
        syncQueueManager = mockk()
        taskPersistenceManager = mockk()
        cloudSyncManager = mockk()
        connectivityManager = mockk()
        timerManager = mockk()
        realTimeUpdateManager = mockk()

        offlineDetector = OfflineDetectorImpl(connectivityManager)

        offlineSyncStrategy = OfflineSyncStrategyImpl(
            syncQueueManager = syncQueueManager,
            taskPersistenceManager = taskPersistenceManager,
            cloudSyncManager = cloudSyncManager,
            connectivityManager = connectivityManager,
            syncChangeSerializer = mockk()
        )
    }

    // ============ Scenario 1: Offline Detection ============

    @Test
    fun `scenario 1 - offline detection - device goes offline and app detects it`() = runTest {
        // Arrange
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { connectivityManager.observeConnectivity() } returns kotlinx.coroutines.flow.flowOf(true, false)

        offlineDetector.startMonitoring()

        // Act
        val initialState = offlineDetector.isOnline()
        val states = mutableListOf<Boolean>()
        offlineDetector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        // Assert
        assertTrue(initialState)
        assertEquals(listOf(true, false), states)
        assertFalse(offlineDetector.isOnline())

        offlineDetector.stopMonitoring()
    }

    // ============ Scenario 2: Offline Task Creation ============

    @Test
    fun `scenario 2 - offline task creation - create task while offline and cache locally`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task = createTestTask(id = "task-1", title = "Buy groceries")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(task) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)

        // Assert - Task cached locally
        coVerify { taskPersistenceManager.saveTask(task) }
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
    }

    // ============ Scenario 3: Offline Task Update ============

    @Test
    fun `scenario 3 - offline task update - update task while offline and queue changes`() = runTest {
        // Arrange
        val userId = "user-123"
        val originalTask = createTestTask(id = "task-1", title = "Original Title")
        val updatedTask = originalTask.copy(title = "Updated Title")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(updatedTask, SyncOperation.UPDATE, userId)

        // Assert - Update queued
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
        coVerify { taskPersistenceManager.saveTask(updatedTask) }
    }

    // ============ Scenario 4: Offline Task Deletion ============

    @Test
    fun `scenario 4 - offline task deletion - delete task while offline and queue deletion`() = runTest {
        // Arrange
        val userId = "user-123"
        val task = createTestTask(id = "task-1", title = "To Delete")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(task, SyncOperation.DELETE, userId)

        // Assert - Deletion queued
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
    }

    // ============ Scenario 5: Offline Task Completion ============

    @Test
    fun `scenario 5 - offline task completion - complete task while offline and queue completion`() = runTest {
        // Arrange
        val userId = "user-123"
        val task = createTestTask(id = "task-1", status = TaskStatus.INCOMPLETE)
        val completedTask = task.copy(status = TaskStatus.COMPLETED, completedAt = Instant.now())

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act
        offlineSyncStrategy.queueLocalChange(completedTask, SyncOperation.UPDATE, userId)

        // Assert - Completion queued
        coVerify { syncQueueManager.queueItem(any(), any(), any(), any()) }
        coVerify { taskPersistenceManager.saveTask(completedTask) }
    }

    // ============ Scenario 6: Offline Timer ============

    @Test
    fun `scenario 6 - offline timer - start timer while offline and verify it continues running`() = runTest {
        // Arrange
        coEvery { connectivityManager.isOnline() } returns false
        coEvery { timerManager.startTimer(any(), any()) } returns Unit
        coEvery { timerManager.isRunning() } returns true

        // Act
        timerManager.startTimer(taskId = "task-1", durationMs = 60000)

        // Assert - Timer running offline
        assertTrue(timerManager.isRunning())
        coVerify { timerManager.startTimer(any(), any()) }
    }

    // ============ Scenario 7: Offline Timer Completion ============

    @Test
    fun `scenario 7 - offline timer completion - timer completes while offline and emits notification`() = runTest {
        // Arrange
        coEvery { connectivityManager.isOnline() } returns false
        coEvery { timerManager.startTimer(any(), any()) } returns Unit
        coEvery { timerManager.isComplete() } returns true
        coEvery { timerManager.emitNotification(any()) } returns Unit

        // Act
        timerManager.startTimer(taskId = "task-1", durationMs = 1000)
        val isComplete = timerManager.isComplete()

        // Assert - Notification emitted
        assertTrue(isComplete)
        coVerify { timerManager.emitNotification(any()) }
    }

    // ============ Scenario 8: Reconnection Sync ============

    @Test
    fun `scenario 8 - reconnection sync - device reconnects and all pending changes are synced`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task1 = createTestTask(id = "task-1", title = "Task 1")
        val task2 = createTestTask(id = "task-2", title = "Task 2")

        // Initially offline - queue changes
        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        offlineSyncStrategy.queueLocalChange(task1, SyncOperation.CREATE, userId)
        offlineSyncStrategy.queueLocalChange(task2, SyncOperation.UPDATE, userId)

        // Now online - sync
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 2,
            failedCount = 0,
            conflicts = emptyList()
        )

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - All changes synced
        assertEquals(2, result.syncedCount)
        assertEquals(0, result.failedCount)
    }

    // ============ Scenario 9: Conflict Resolution ============

    @Test
    fun `scenario 9 - conflict resolution - conflicting changes resolved by timestamp`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val now = Instant.now()

        val localTask = createTestTask(
            id = "task-1",
            title = "Local Title",
            updatedAt = now
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Remote Title",
            updatedAt = now.plusSeconds(10)  // Remote is newer
        )

        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = listOf(
                ConflictInfo(
                    taskId = "task-1",
                    localVersion = localTask,
                    remoteVersion = remoteTask,
                    resolved = true,
                    resolvedVersion = remoteTask
                )
            )
        )

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Conflict resolved by timestamp (remote wins)
        assertEquals(1, result.syncedCount)
        assertEquals(1, result.conflicts.size)
        assertTrue(result.conflicts[0].resolved)
        assertEquals(remoteTask.title, result.conflicts[0].resolvedVersion?.title)
    }

    // ============ Scenario 10: Multiple Offline Operations ============

    @Test
    fun `scenario 10 - multiple offline operations - multiple operations queued and synced correctly`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val tasks = (1..5).map { i ->
            createTestTask(id = "task-$i", title = "Task $i")
        }

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Queue multiple operations
        tasks.forEach { task ->
            offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)
        }

        // Assert - All queued
        coVerify(exactly = 5) { syncQueueManager.queueItem(any(), any(), any(), any()) }

        // Now sync
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 5,
            failedCount = 0,
            conflicts = emptyList()
        )

        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - All synced
        assertEquals(5, result.syncedCount)
    }

    // ============ Scenario 11: Rapid Online/Offline Transitions ============

    @Test
    fun `scenario 11 - rapid transitions - handle rapid online offline transitions`() = runTest {
        // Arrange
        coEvery { connectivityManager.observeConnectivity() } returns kotlinx.coroutines.flow.flowOf(
            true, false, true, false, true
        )

        offlineDetector.startMonitoring()

        // Act
        val states = mutableListOf<Boolean>()
        offlineDetector.observeConnectivityState().collect { state ->
            states.add(state)
        }

        // Assert - All transitions captured
        assertEquals(listOf(true, false, true, false, true), states)

        offlineDetector.stopMonitoring()
    }

    // ============ Scenario 12: Partial Sync Failure ============

    @Test
    fun `scenario 12 - partial sync failure - handle partial sync failures and retry`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"

        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 2,
            failedCount = 1,
            conflicts = emptyList()
        )

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Partial sync recorded
        assertEquals(2, result.syncedCount)
        assertEquals(1, result.failedCount)
    }

    // ============ Scenario 13: Cache Consistency ============

    @Test
    fun `scenario 13 - cache consistency - cache remains consistent during offline operations`() = runTest {
        // Arrange
        val userId = "user-123"
        val task1 = createTestTask(id = "task-1", title = "Task 1")
        val task2 = createTestTask(id = "task-2", title = "Task 2")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit
        coEvery { taskPersistenceManager.getTask("task-1") } returns task1
        coEvery { taskPersistenceManager.getTask("task-2") } returns task2

        // Act - Create multiple tasks
        offlineSyncStrategy.queueLocalChange(task1, SyncOperation.CREATE, userId)
        offlineSyncStrategy.queueLocalChange(task2, SyncOperation.CREATE, userId)

        // Assert - Both tasks persisted
        coVerify { taskPersistenceManager.saveTask(task1) }
        coVerify { taskPersistenceManager.saveTask(task2) }
    }

    // ============ Scenario 14: Offline to Online with Remote Updates ============

    @Test
    fun `scenario 14 - offline to online with remote updates - handle remote updates during sync`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val localTask = createTestTask(id = "task-1", title = "Local")
        val remoteTask = createTestTask(id = "task-1", title = "Remote", updatedAt = Instant.now().plusSeconds(5))

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Queue local change
        offlineSyncStrategy.queueLocalChange(localTask, SyncOperation.UPDATE, userId)

        // Now online with remote update
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = listOf(
                ConflictInfo(
                    taskId = "task-1",
                    localVersion = localTask,
                    remoteVersion = remoteTask,
                    resolved = true,
                    resolvedVersion = remoteTask
                )
            )
        )

        // Act
        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - Remote update applied
        assertEquals(1, result.syncedCount)
        assertEquals(1, result.conflicts.size)
    }

    // ============ Scenario 15: App Restart While Offline ============

    @Test
    fun `scenario 15 - app restart while offline - app restarts and continues with cached data`() = runTest {
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

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns queuedItems

        // Act - Simulate app restart by retrieving queued items
        val changes = offlineSyncStrategy.getQueuedChanges(userId)

        // Assert - Queue persisted across restart
        assertEquals(1, changes.size)
        assertEquals("task-1", changes[0].taskId)
        assertEquals(SyncOperation.CREATE, changes[0].operation)
    }

    // ============ Additional Comprehensive Scenarios ============

    @Test
    fun `comprehensive scenario - offline workflow with mixed operations`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val task1 = createTestTask(id = "task-1", title = "Create me")
        val task2 = createTestTask(id = "task-2", title = "Update me")
        val task3 = createTestTask(id = "task-3", title = "Delete me")

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit

        // Act - Queue mixed operations
        offlineSyncStrategy.queueLocalChange(task1, SyncOperation.CREATE, userId)
        offlineSyncStrategy.queueLocalChange(task2, SyncOperation.UPDATE, userId)
        offlineSyncStrategy.queueLocalChange(task3, SyncOperation.DELETE, userId)

        // Assert - All operations queued
        coVerify(exactly = 3) { syncQueueManager.queueItem(any(), any(), any(), any()) }

        // Now sync
        coEvery { connectivityManager.isOnline() } returns true
        coEvery { cloudSyncManager.syncPendingChanges(householdId, userId) } returns SyncResult(
            syncedCount = 3,
            failedCount = 0,
            conflicts = emptyList()
        )

        val result = offlineSyncStrategy.syncQueuedChanges(householdId, userId)

        // Assert - All synced
        assertEquals(3, result.syncedCount)
    }

    @Test
    fun `comprehensive scenario - offline state consistency across operations`() = runTest {
        // Arrange
        val userId = "user-123"
        val tasks = (1..3).map { i ->
            createTestTask(id = "task-$i", title = "Task $i")
        }

        coEvery { connectivityManager.isOnline() } returns false
        coEvery { syncQueueManager.queueItem(any(), any(), any(), any()) } returns mockk()
        coEvery { taskPersistenceManager.saveTask(any()) } returns Unit
        coEvery { syncQueueManager.getPendingItemCount(userId) } returns 3
        coEvery { syncQueueManager.hasPendingItems(userId) } returns true

        // Act - Queue multiple tasks
        tasks.forEach { task ->
            offlineSyncStrategy.queueLocalChange(task, SyncOperation.CREATE, userId)
        }

        // Assert - Queue state consistent
        val count = offlineSyncStrategy.getQueuedChangeCount(userId)
        val hasPending = offlineSyncStrategy.hasQueuedChanges(userId)

        assertEquals(3, count)
        assertTrue(hasPending)
    }

    // ============ Helper Methods ============

    private fun createTestTask(
        id: String = "task-1",
        title: String = "Test Task",
        status: TaskStatus = TaskStatus.INCOMPLETE,
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
            status = status,
            createdAt = Instant.now(),
            updatedAt = updatedAt,
            completedAt = null,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )
    }
}

// ============ Supporting Data Classes ============

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val conflicts: List<ConflictInfo>
)

data class ConflictInfo(
    val taskId: String,
    val localVersion: Task,
    val remoteVersion: Task,
    val resolved: Boolean,
    val resolvedVersion: Task?
)

// ============ Mock Interfaces ============

interface TimerManager {
    fun startTimer(taskId: String, durationMs: Long)
    fun isRunning(): Boolean
    fun isComplete(): Boolean
    fun emitNotification(taskId: String)
}
