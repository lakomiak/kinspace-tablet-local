package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus as TaskSyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudSyncManagerUnitTest {

    private lateinit var restApiClient: RestApiClient
    private lateinit var syncQueueManager: SyncQueueManager
    private lateinit var taskDao: TaskDao
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var manager: CloudSyncManagerImpl

    private val householdId = "household-1"
    private val userId = "user-1"

    @Before
    fun setup() {
        restApiClient = mockk()
        syncQueueManager = mockk()
        taskDao = mockk()
        connectivityManager = mockk()

        manager = CloudSyncManagerImpl(
            restApiClient,
            syncQueueManager,
            taskDao,
            connectivityManager
        )
    }

    @Test
    fun `getCurrentSyncStatus returns IDLE initially`() {
        assertEquals(SyncStatus.IDLE, manager.getCurrentSyncStatus())
    }

    @Test
    fun `observeSyncStatus emits IDLE initially`() {
        val status = runBlocking {
            manager.observeSyncStatus().first()
        }

        assertEquals(SyncStatus.IDLE, status)
    }

    @Test
    fun `syncPendingChanges returns OFFLINE when not connected`() {
        every { connectivityManager.isOnline() } returns false

        val result = runBlocking {
            manager.syncPendingChanges(householdId, userId)
        }

        assertEquals(0, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(SyncStatus.OFFLINE, manager.getCurrentSyncStatus())
    }

    @Test
    fun `syncPendingChanges returns empty result when no pending items`() {
        every { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns emptyList()

        val result = runBlocking {
            manager.syncPendingChanges(householdId, userId)
        }

        assertEquals(0, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(SyncStatus.SYNCED, manager.getCurrentSyncStatus())
    }

    @Test
    fun `syncPendingChanges syncs pending items successfully`() {
        val task = createTestTask()
        val syncQueueItem = createTestSyncQueueItem(task)

        every { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(syncQueueItem)
        coEvery {
            restApiClient.batchSync(householdId, any())
        } returns SyncResult(syncedCount = 1, failedCount = 0, conflicts = emptyList())
        coEvery { syncQueueManager.removeItem(syncQueueItem.id) } returns Unit

        val result = runBlocking {
            manager.syncPendingChanges(householdId, userId)
        }

        assertEquals(1, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(SyncStatus.SYNCED, manager.getCurrentSyncStatus())
        coVerify { syncQueueManager.removeItem(syncQueueItem.id) }
    }

    @Test
    fun `syncPendingChanges handles conflicts by applying remote version`() {
        val localTask = createTestTask()
        val remoteTask = createTestTask(updatedAt = Instant.now().plusSeconds(10))
        val syncQueueItem = createTestSyncQueueItem(localTask)

        val conflict = SyncConflict(
            taskId = localTask.id,
            localVersion = localTask,
            remoteVersion = remoteTask
        )

        every { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } returns listOf(syncQueueItem)
        coEvery {
            restApiClient.batchSync(householdId, any())
        } returns SyncResult(syncedCount = 0, failedCount = 0, conflicts = listOf(conflict))
        coEvery { taskDao.insert(remoteTask) } returns Unit
        coEvery { syncQueueManager.removeItem(syncQueueItem.id) } returns Unit

        val result = runBlocking {
            manager.syncPendingChanges(householdId, userId)
        }

        assertEquals(0, result.failedCount)
        coVerify { taskDao.insert(remoteTask) }
        coVerify { syncQueueManager.removeItem(syncQueueItem.id) }
    }

    @Test
    fun `syncPendingChanges sets status to ERROR on exception`() {
        every { connectivityManager.isOnline() } returns true
        coEvery { syncQueueManager.getPendingItemsByUser(userId) } throws Exception("Network error")

        val result = runBlocking {
            manager.syncPendingChanges(householdId, userId)
        }

        assertEquals(SyncStatus.ERROR, manager.getCurrentSyncStatus())
    }

    @Test
    fun `observeSyncStatus emits status changes`() {
        every { connectivityManager.isOnline() } returns false

        runBlocking {
            manager.syncPendingChanges(householdId, userId)
        }

        val statuses = mutableListOf<SyncStatus>()
        runBlocking {
            manager.observeSyncStatus().collect { status ->
                statuses.add(status)
                if (statuses.size >= 2) return@collect
            }
        }

        assertTrue(statuses.contains(SyncStatus.OFFLINE))
    }

    private fun createTestTask(
        id: String = "task-1",
        updatedAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = userId,
            title = "Test Task",
            todoGroup = "Morning",
            status = TaskStatus.INCOMPLETE,
            updatedAt = updatedAt
        )
    }

    private fun createTestSyncQueueItem(task: Task): com.adhdfocus.app.data.model.SyncQueueItem {
        return com.adhdfocus.app.data.model.SyncQueueItem(
            id = "queue-1",
            taskId = task.id,
            userId = userId,
            operation = SyncOperation.CREATE,
            payload = """{"id":"${task.id}","title":"${task.title}"}""",
            timestamp = Instant.now(),
            retryCount = 0
        )
    }
}
