package com.adhdfocus.app.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adhdfocus.app.data.database.AdhdfocusDatabase
import com.adhdfocus.app.data.model.SyncQueueItem
import com.adhdfocus.app.data.model.SyncOperation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SyncQueueDao CRUD operations and query methods.
 * Tests verify that all database operations work correctly including:
 * - Basic CRUD operations (Create, Read, Update, Delete)
 * - FIFO ordering by timestamp
 * - Retry tracking and filtering
 * - Operation type filtering (CREATE, UPDATE, DELETE)
 * - User-specific queries
 * - Batch operations
 */
@RunWith(AndroidJUnit4::class)
class SyncQueueDaoTest {

    private lateinit var database: AdhdfocusDatabase
    private lateinit var syncQueueDao: SyncQueueDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AdhdfocusDatabase::class.java
        ).build()
        syncQueueDao = database.syncQueueDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Basic CRUD Operations ====================

    @Test
    fun testInsertSyncQueueItem() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1","title":"Test Task"}"""
        )

        syncQueueDao.insert(item)
        val retrieved = syncQueueDao.getItemById("sync-1")

        assertNotNull(retrieved)
        assertEquals("task-1", retrieved.taskId)
        assertEquals("user-1", retrieved.userId)
        assertEquals(SyncOperation.CREATE, retrieved.operation)
    }

    @Test
    fun testUpdateSyncQueueItem() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1","title":"Test Task"}""",
            retryCount = 0
        )

        syncQueueDao.insert(item)
        val updated = item.copy(retryCount = 2)
        syncQueueDao.update(updated)

        val retrieved = syncQueueDao.getItemById("sync-1")
        assertNotNull(retrieved)
        assertEquals(2, retrieved.retryCount)
    }

    @Test
    fun testDeleteSyncQueueItem() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1","title":"Test Task"}"""
        )

        syncQueueDao.insert(item)
        syncQueueDao.delete(item)

        val retrieved = syncQueueDao.getItemById("sync-1")
        assertNull(retrieved)
    }

    @Test
    fun testGetItemById() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-1","status":"COMPLETED"}"""
        )

        syncQueueDao.insert(item)
        val retrieved = syncQueueDao.getItemById("sync-1")

        assertNotNull(retrieved)
        assertEquals(SyncOperation.UPDATE, retrieved.operation)
    }

    // ==================== Query Operations ====================

    @Test
    fun testGetItemsByTaskId() = runBlocking {
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-1","status":"COMPLETED"}"""
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)

        val items = syncQueueDao.getItemsByTaskId("task-1")
        assertEquals(2, items.size)
        assertTrue(items.any { it.operation == SyncOperation.CREATE })
        assertTrue(items.any { it.operation == SyncOperation.UPDATE })
    }

    @Test
    fun testGetItemsByUser() = runBlocking {
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}"""
        )
        val item3 = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-2",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-3"}"""
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)
        syncQueueDao.insert(item3)

        val userItems = syncQueueDao.getItemsByUserOnce("user-1")
        assertEquals(2, userItems.size)
        assertTrue(userItems.all { it.userId == "user-1" })
    }

    @Test
    fun testGetItemsByOperation() = runBlocking {
        val createItem = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val updateItem = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}"""
        )
        val deleteItem = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-1",
            operation = SyncOperation.DELETE,
            payload = """{"id":"task-3"}"""
        )

        syncQueueDao.insert(createItem)
        syncQueueDao.insert(updateItem)
        syncQueueDao.insert(deleteItem)

        val createItems = syncQueueDao.getItemsByOperationOnce(SyncOperation.CREATE)
        assertEquals(1, createItems.size)
        assertEquals(SyncOperation.CREATE, createItems[0].operation)

        val updateItems = syncQueueDao.getItemsByOperationOnce(SyncOperation.UPDATE)
        assertEquals(1, updateItems.size)
        assertEquals(SyncOperation.UPDATE, updateItems[0].operation)
    }

    // ==================== FIFO Ordering ====================

    @Test
    fun testGetPendingItemsByUserFifo() = runBlocking {
        val now = Instant.now()
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            timestamp = now.minusSeconds(100)
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}""",
            timestamp = now.minusSeconds(50)
        )
        val item3 = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-1",
            operation = SyncOperation.DELETE,
            payload = """{"id":"task-3"}""",
            timestamp = now
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)
        syncQueueDao.insert(item3)

        val items = syncQueueDao.getPendingItemsByUserFifo("user-1")
        assertEquals(3, items.size)
        assertEquals("sync-1", items[0].id) // Oldest first
        assertEquals("sync-2", items[1].id)
        assertEquals("sync-3", items[2].id) // Newest last
    }

    @Test
    fun testGetAllPendingItemsFifo() = runBlocking {
        val now = Instant.now()
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            timestamp = now.minusSeconds(100)
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-2",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}""",
            timestamp = now.minusSeconds(50)
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)

        val items = syncQueueDao.getAllPendingItemsFifo()
        assertEquals(2, items.size)
        assertEquals("sync-1", items[0].id) // Oldest first
        assertEquals("sync-2", items[1].id)
    }

    // ==================== Retry Tracking ====================

    @Test
    fun testGetRetryableItemsByUser() = runBlocking {
        val retryableItem = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            retryCount = 1
        )
        val maxedOutItem = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}""",
            retryCount = 5
        )

        syncQueueDao.insert(retryableItem)
        syncQueueDao.insert(maxedOutItem)

        val retryable = syncQueueDao.getRetryableItemsByUser("user-1", maxRetries = 3)
        assertEquals(1, retryable.size)
        assertEquals("sync-1", retryable[0].id)
    }

    @Test
    fun testGetAllRetryableItems() = runBlocking {
        val retryableItem1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            retryCount = 1
        )
        val retryableItem2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-2",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}""",
            retryCount = 2
        )
        val maxedOutItem = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-1",
            operation = SyncOperation.DELETE,
            payload = """{"id":"task-3"}""",
            retryCount = 5
        )

        syncQueueDao.insert(retryableItem1)
        syncQueueDao.insert(retryableItem2)
        syncQueueDao.insert(maxedOutItem)

        val retryable = syncQueueDao.getAllRetryableItems(maxRetries = 3)
        assertEquals(2, retryable.size)
        assertTrue(retryable.all { it.retryCount < 3 })
    }

    @Test
    fun testIncrementRetryCount() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            retryCount = 0
        )

        syncQueueDao.insert(item)
        syncQueueDao.incrementRetryCount("sync-1")

        val updated = syncQueueDao.getItemById("sync-1")
        assertNotNull(updated)
        assertEquals(1, updated.retryCount)
    }

    @Test
    fun testSetRetryCount() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            retryCount = 0
        )

        syncQueueDao.insert(item)
        syncQueueDao.setRetryCount("sync-1", 3)

        val updated = syncQueueDao.getItemById("sync-1")
        assertNotNull(updated)
        assertEquals(3, updated.retryCount)
    }

    // ==================== Count Operations ====================

    @Test
    fun testGetPendingItemCount() = runBlocking {
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}"""
        )
        val item3 = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-2",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-3"}"""
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)
        syncQueueDao.insert(item3)

        val user1Count = syncQueueDao.getPendingItemCount("user-1")
        val user2Count = syncQueueDao.getPendingItemCount("user-2")

        assertEquals(2, user1Count)
        assertEquals(1, user2Count)
    }

    @Test
    fun testGetPendingItemCountByOperation() = runBlocking {
        val createItem = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val updateItem = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}"""
        )
        val deleteItem = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-1",
            operation = SyncOperation.DELETE,
            payload = """{"id":"task-3"}"""
        )

        syncQueueDao.insert(createItem)
        syncQueueDao.insert(updateItem)
        syncQueueDao.insert(deleteItem)

        val createCount = syncQueueDao.getPendingItemCountByOperation("user-1", SyncOperation.CREATE)
        val updateCount = syncQueueDao.getPendingItemCountByOperation("user-1", SyncOperation.UPDATE)
        val deleteCount = syncQueueDao.getPendingItemCountByOperation("user-1", SyncOperation.DELETE)

        assertEquals(1, createCount)
        assertEquals(1, updateCount)
        assertEquals(1, deleteCount)
    }

    // ==================== Delete Operations ====================

    @Test
    fun testDeleteItemById() = runBlocking {
        val item = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )

        syncQueueDao.insert(item)
        syncQueueDao.deleteItemById("sync-1")

        val retrieved = syncQueueDao.getItemById("sync-1")
        assertNull(retrieved)
    }

    @Test
    fun testDeleteItemsByTaskId() = runBlocking {
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-1"}"""
        )
        val item3 = SyncQueueItem(
            id = "sync-3",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-2"}"""
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)
        syncQueueDao.insert(item3)

        syncQueueDao.deleteItemsByTaskId("task-1")

        val remaining = syncQueueDao.getAllPendingItemsFifo()
        assertEquals(1, remaining.size)
        assertEquals("sync-3", remaining[0].id)
    }

    @Test
    fun testDeleteItemsByUserId() = runBlocking {
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}"""
        )
        val item3 = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-2",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-3"}"""
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)
        syncQueueDao.insert(item3)

        syncQueueDao.deleteItemsByUserId("user-1")

        val remaining = syncQueueDao.getAllPendingItemsFifo()
        assertEquals(1, remaining.size)
        assertEquals("user-2", remaining[0].userId)
    }

    @Test
    fun testDeleteAllItems() = runBlocking {
        val item1 = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}"""
        )
        val item2 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-2",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}"""
        )

        syncQueueDao.insert(item1)
        syncQueueDao.insert(item2)

        syncQueueDao.deleteAllItems()

        val remaining = syncQueueDao.getAllPendingItemsFifo()
        assertEquals(0, remaining.size)
    }

    // ==================== Time Range Queries ====================

    @Test
    fun testGetItemsInTimeRange() = runBlocking {
        val now = Instant.now()
        val startTime = now.minusSeconds(200)
        val endTime = now.minusSeconds(50)

        val beforeRange = SyncQueueItem(
            id = "sync-1",
            taskId = "task-1",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-1"}""",
            timestamp = now.minusSeconds(300)
        )
        val inRange1 = SyncQueueItem(
            id = "sync-2",
            taskId = "task-2",
            userId = "user-1",
            operation = SyncOperation.UPDATE,
            payload = """{"id":"task-2"}""",
            timestamp = now.minusSeconds(150)
        )
        val inRange2 = SyncQueueItem(
            id = "sync-3",
            taskId = "task-3",
            userId = "user-1",
            operation = SyncOperation.DELETE,
            payload = """{"id":"task-3"}""",
            timestamp = now.minusSeconds(100)
        )
        val afterRange = SyncQueueItem(
            id = "sync-4",
            taskId = "task-4",
            userId = "user-1",
            operation = SyncOperation.CREATE,
            payload = """{"id":"task-4"}""",
            timestamp = now
        )

        syncQueueDao.insert(beforeRange)
        syncQueueDao.insert(inRange1)
        syncQueueDao.insert(inRange2)
        syncQueueDao.insert(afterRange)

        val items = syncQueueDao.getItemsInTimeRange("user-1", startTime, endTime)
        assertEquals(2, items.size)
        assertTrue(items.all { it.timestamp >= startTime && it.timestamp <= endTime })
    }
}
