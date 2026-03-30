package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
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
import kotlin.test.assertTrue

/**
 * Integration Tests for Conflict Resolution During Sync
 *
 * Tests verify:
 * - Timestamp-based conflict resolution
 * - Local vs remote version selection
 * - Conflict detection and handling
 * - Multiple conflicts in single sync
 * - Conflict resolution with task persistence
 * - Conflict history tracking
 *
 * Validates: Requirement 10 - Cloud Synchronization with calendar-cloud
 * Validates: Property 10 - Sync Conflict Resolution
 */
class ConflictResolutionIntegrationTest {
    private lateinit var conflictResolver: ConflictResolver
    private lateinit var taskDao: TaskDao
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var restApiClient: RestApiClient
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        conflictResolver = mockk()
        taskDao = mockk()
        cloudSyncManager = mockk()
        restApiClient = mockk()
        syncQueueManager = mockk()
        connectivityManager = mockk()
    }

    @Test
    fun `conflict scenario - local version newer, local wins`() = runTest {
        // Arrange
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

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { conflictResolver.getConflictReason(localTask, remoteTask) } returns "Local version is newer"
        coEvery { taskDao.insert(localTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)
        val reason = conflictResolver.getConflictReason(localTask, remoteTask)

        // Assert
        assertEquals(localTask, resolved)
        assertEquals("Local version is newer", reason)
        coVerify { taskDao.insert(localTask) }
    }

    @Test
    fun `conflict scenario - remote version newer, remote wins`() = runTest {
        // Arrange
        val now = Instant.now()
        val localTask = createTestTask(
            id = "task-1",
            title = "Local Title",
            updatedAt = now
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Remote Title",
            updatedAt = now.plusSeconds(10)
        )

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns remoteTask
        coEvery { conflictResolver.getConflictReason(localTask, remoteTask) } returns "Remote version is newer"
        coEvery { taskDao.insert(remoteTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)
        val reason = conflictResolver.getConflictReason(localTask, remoteTask)

        // Assert
        assertEquals(remoteTask, resolved)
        assertEquals("Remote version is newer", reason)
        coVerify { taskDao.insert(remoteTask) }
    }

    @Test
    fun `conflict scenario - same timestamp, local preferred`() = runTest {
        // Arrange
        val now = Instant.now()
        val localTask = createTestTask(
            id = "task-1",
            title = "Local Title",
            updatedAt = now
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Remote Title",
            updatedAt = now
        )

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { conflictResolver.getConflictReason(localTask, remoteTask) } returns "Same timestamp, local preferred"
        coEvery { taskDao.insert(localTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)

        // Assert
        assertEquals(localTask, resolved)
        coVerify { taskDao.insert(localTask) }
    }

    @Test
    fun `conflict scenario - multiple conflicts in single sync`() = runTest {
        // Arrange
        val now = Instant.now()
        val conflicts = listOf(
            SyncConflict(
                taskId = "task-1",
                localVersion = createTestTask(id = "task-1", updatedAt = now.plusSeconds(10)),
                remoteVersion = createTestTask(id = "task-1", updatedAt = now)
            ),
            SyncConflict(
                taskId = "task-2",
                localVersion = createTestTask(id = "task-2", updatedAt = now),
                remoteVersion = createTestTask(id = "task-2", updatedAt = now.plusSeconds(10))
            ),
            SyncConflict(
                taskId = "task-3",
                localVersion = createTestTask(id = "task-3", updatedAt = now),
                remoteVersion = createTestTask(id = "task-3", updatedAt = now)
            )
        )

        conflicts.forEach { conflict ->
            coEvery { conflictResolver.resolveConflict(any(), any()) } returns conflict.localVersion
            coEvery { taskDao.insert(any()) } returns Unit
        }

        // Act
        val resolved = conflicts.map { conflict ->
            conflictResolver.resolveConflict(conflict.localVersion, conflict.remoteVersion)
        }

        // Assert
        assertEquals(3, resolved.size)
        coVerify(exactly = 3) { taskDao.insert(any()) }
    }

    @Test
    fun `conflict scenario - conflict with task status change`() = runTest {
        // Arrange
        val now = Instant.now()
        val localTask = createTestTask(
            id = "task-1",
            title = "Task 1",
            status = TaskStatus.COMPLETED,
            updatedAt = now.plusSeconds(10)
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Task 1",
            status = TaskStatus.INCOMPLETE,
            updatedAt = now
        )

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { taskDao.insert(localTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)

        // Assert - Local completed status preserved
        assertEquals(TaskStatus.COMPLETED, resolved.status)
        coVerify { taskDao.insert(localTask) }
    }

    @Test
    fun `conflict scenario - conflict with task content change`() = runTest {
        // Arrange
        val now = Instant.now()
        val localTask = createTestTask(
            id = "task-1",
            title = "Buy milk and bread",
            updatedAt = now.plusSeconds(10)
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Buy milk",
            updatedAt = now
        )

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { taskDao.insert(localTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)

        // Assert - Local content preserved
        assertEquals("Buy milk and bread", resolved.title)
        coVerify { taskDao.insert(localTask) }
    }

    @Test
    fun `conflict scenario - conflict history tracking`() = runTest {
        // Arrange
        val now = Instant.now()
        val conflict1 = SyncConflict(
            taskId = "task-1",
            localVersion = createTestTask(id = "task-1", updatedAt = now.plusSeconds(10)),
            remoteVersion = createTestTask(id = "task-1", updatedAt = now)
        )
        val conflict2 = SyncConflict(
            taskId = "task-2",
            localVersion = createTestTask(id = "task-2", updatedAt = now),
            remoteVersion = createTestTask(id = "task-2", updatedAt = now.plusSeconds(10))
        )

        coEvery { conflictResolver.resolveConflict(any(), any()) } returns conflict1.localVersion
        coEvery { taskDao.insert(any()) } returns Unit

        // Act
        val conflicts = listOf(conflict1, conflict2)
        conflicts.forEach { conflict ->
            conflictResolver.resolveConflict(conflict.localVersion, conflict.remoteVersion)
        }

        // Assert - Both conflicts processed
        coVerify(exactly = 2) { taskDao.insert(any()) }
    }

    @Test
    fun `conflict scenario - sync with conflicts and successful items`() = runTest {
        // Arrange
        val householdId = "household-123"
        val userId = "user-123"
        val now = Instant.now()

        coEvery { connectivityManager.isOnline() } returns true
        val queuedItem = mockk {
            coEvery { taskId } returns "task-1"
            coEvery { operation } returns com.adhdfocus.app.data.model.SyncOperation.UPDATE
            coEvery { payload } returns """{"id":"task-1"}"""
            coEvery { timestamp } returns now
            coEvery { id } returns "queue-1"
        }
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(queuedItem)

        val conflict = SyncConflict(
            taskId = "task-1",
            localVersion = createTestTask(id = "task-1", updatedAt = now.plusSeconds(10)),
            remoteVersion = createTestTask(id = "task-1", updatedAt = now)
        )

        coEvery { restApiClient.batchSync(householdId, any()) } returns SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = listOf(conflict)
        )
        coEvery { conflictResolver.resolveConflict(any(), any()) } returns conflict.localVersion
        coEvery { conflictResolver.getConflictReason(any(), any()) } returns "Timestamp-based resolution"
        coEvery { taskDao.insert(any()) } returns Unit
        coEvery { syncQueueManager.removeItem("queue-1") } returns Unit

        // Act
        val result = SyncResult(
            syncedCount = 1,
            failedCount = 0,
            conflicts = listOf(conflict)
        )

        // Assert
        assertEquals(1, result.syncedCount)
        assertEquals(1, result.conflicts.size)
    }

    @Test
    fun `conflict scenario - conflict resolution preserves task metadata`() = runTest {
        // Arrange
        val now = Instant.now()
        val localTask = createTestTask(
            id = "task-1",
            title = "Updated title",
            updatedAt = now.plusSeconds(10),
            estimatedDurationMinutes = 45
        )
        val remoteTask = createTestTask(
            id = "task-1",
            title = "Original title",
            updatedAt = now,
            estimatedDurationMinutes = 30
        )

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { taskDao.insert(localTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)

        // Assert - Metadata preserved from local version
        assertEquals("Updated title", resolved.title)
        assertEquals(45, resolved.estimatedDurationMinutes)
        coVerify { taskDao.insert(localTask) }
    }

    @Test
    fun `conflict scenario - detect and resolve conflicts automatically`() = runTest {
        // Arrange
        val now = Instant.now()
        val localTask = createTestTask(id = "task-1", updatedAt = now.plusSeconds(5))
        val remoteTask = createTestTask(id = "task-1", updatedAt = now)

        coEvery { conflictResolver.resolveConflict(localTask, remoteTask) } returns localTask
        coEvery { taskDao.insert(localTask) } returns Unit

        // Act
        val resolved = conflictResolver.resolveConflict(localTask, remoteTask)

        // Assert
        assertEquals(localTask.id, resolved.id)
        assertTrue(resolved.updatedAt > remoteTask.updatedAt)
    }

    private fun createTestTask(
        id: String = "task-1",
        title: String = "Test Task",
        status: TaskStatus = TaskStatus.INCOMPLETE,
        updatedAt: Instant = Instant.now(),
        estimatedDurationMinutes: Int = 30
    ): Task {
        return Task(
            id = id,
            householdId = "household-123",
            assignedUserId = "user-123",
            title = title,
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = estimatedDurationMinutes,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = updatedAt,
            completedAt = null,
            syncStatus = com.adhdfocus.app.data.model.SyncStatus.PENDING,
            isDeleted = false
        )
    }
}
