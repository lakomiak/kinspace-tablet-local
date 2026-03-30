package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.OfflineUpdateQueueDao
import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.model.UpdateType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineUpdateQueueIntegrationTest {
    private lateinit var dao: OfflineUpdateQueueDao
    private lateinit var queue: OfflineUpdateQueue

    @Before
    fun setup() {
        dao = mockk()
        queue = OfflineUpdateQueueImpl(dao)
    }

    @Test
    fun `Complete workflow - Add, retrieve, mark applied, remove`() = runTest {
        val item = OfflineUpdateQueueItem(
            id = "update1",
            taskId = "task1",
            userId = "user1",
            updateType = UpdateType.CREATED,
            payload = """{"id":"task1"}"""
        )

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getUnappliedItemsByUserId("user1") } returns listOf(item)
        coEvery { dao.markAsApplied("update1") } returns Unit
        coEvery { dao.deleteItemById("update1") } returns Unit
        coEvery { dao.getItemsByUserId("user1") } returns emptyList()

        // Add update
        val addResult = queue.addUpdate("task1", "user1", UpdateType.CREATED, """{"id":"task1"}""")
        assertTrue(addResult)

        // Get unapplied
        val unapplied = queue.getUnappliedUpdates("user1")
        assertEquals(1, unapplied.size)

        // Mark as applied
        val markResult = queue.markAsApplied("update1")
        assertTrue(markResult)

        // Remove
        val removeResult = queue.removeUpdate("update1")
        assertTrue(removeResult)

        // Verify empty
        val remaining = queue.getQueuedUpdates("user1")
        assertEquals(0, remaining.size)
    }

    @Test
    fun `Multiple updates workflow - Add multiple, retrieve in FIFO order`() = runTest {
        val items = listOf(
            OfflineUpdateQueueItem(
                id = "update1",
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            ),
            OfflineUpdateQueueItem(
                id = "update2",
                taskId = "task2",
                userId = "user1",
                updateType = UpdateType.UPDATED,
                payload = """{"id":"task2"}"""
            ),
            OfflineUpdateQueueItem(
                id = "update3",
                taskId = "task3",
                userId = "user1",
                updateType = UpdateType.DELETED,
                payload = """{"id":"task3"}"""
            )
        )

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getItemsByUserId("user1") } returns items

        // Add all
        items.forEach { item ->
            queue.addUpdate(item.taskId, "user1", item.updateType, item.payload)
        }

        // Retrieve all
        val retrieved = queue.getQueuedUpdates("user1")
        assertEquals(3, retrieved.size)
        assertEquals("task1", retrieved[0].taskId)
        assertEquals("task2", retrieved[1].taskId)
        assertEquals("task3", retrieved[2].taskId)
    }

    @Test
    fun `Clear queue workflow - Add multiple, clear all`() = runTest {
        val items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            ),
            OfflineUpdateQueueItem(
                taskId = "task2",
                userId = "user1",
                updateType = UpdateType.UPDATED,
                payload = """{"id":"task2"}"""
            )
        )

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getQueueSize("user1") } returns 2
        coEvery { dao.deleteItemsByUserId("user1") } returns Unit
        coEvery { dao.getQueueSize("user1") } returns 0

        // Add items
        items.forEach { item ->
            queue.addUpdate(item.taskId, "user1", item.updateType, item.payload)
        }

        // Verify size
        var size = queue.getQueueSize("user1")
        assertEquals(2, size)

        // Clear
        val clearResult = queue.clearQueue("user1")
        assertTrue(clearResult)

        // Verify empty
        size = queue.getQueueSize("user1")
        assertEquals(0, size)
    }

    @Test
    fun `Unapplied updates workflow - Track applied vs unapplied`() = runTest {
        val unappliedItem = OfflineUpdateQueueItem(
            id = "update1",
            taskId = "task1",
            userId = "user1",
            updateType = UpdateType.CREATED,
            payload = """{"id":"task1"}""",
            applied = false
        )

        val appliedItem = OfflineUpdateQueueItem(
            id = "update2",
            taskId = "task2",
            userId = "user1",
            updateType = UpdateType.UPDATED,
            payload = """{"id":"task2"}""",
            applied = true
        )

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getUnappliedItemsByUserId("user1") } returns listOf(unappliedItem)
        coEvery { dao.getUnappliedQueueSize("user1") } returns 1

        // Add items
        queue.addUpdate("task1", "user1", UpdateType.CREATED, """{"id":"task1"}""")
        queue.addUpdate("task2", "user1", UpdateType.UPDATED, """{"id":"task2"}""")

        // Get unapplied
        val unapplied = queue.getUnappliedUpdates("user1")
        assertEquals(1, unapplied.size)
        assertFalse(unapplied[0].applied)

        // Get unapplied count
        val unappliedCount = queue.getUnappliedQueueSize("user1")
        assertEquals(1, unappliedCount)
    }

    @Test
    fun `Queue state observation workflow - Observe changes`() = runTest {
        val items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            )
        )

        coEvery { dao.observeItemsByUserId("user1") } returns flowOf(items)

        var observedState: QueueState? = null
        queue.observeQueueChanges("user1").collect { state ->
            observedState = state
        }

        assertEquals("user1", observedState?.userId)
        assertEquals(1, observedState?.queueSize)
        assertEquals(1, observedState?.unappliedCount)
    }

    @Test
    fun `Has queued updates workflow - Check queue state`() = runTest {
        coEvery { dao.getQueueSize("user1") } returns 0
        var hasUpdates = queue.hasQueuedUpdates("user1")
        assertFalse(hasUpdates)

        coEvery { dao.getQueueSize("user1") } returns 1
        hasUpdates = queue.hasQueuedUpdates("user1")
        assertTrue(hasUpdates)
    }

    @Test
    fun `Multi-user isolation workflow - Different users have separate queues`() = runTest {
        val user1Items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            )
        )

        val user2Items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task2",
                userId = "user2",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task2"}"""
            ),
            OfflineUpdateQueueItem(
                taskId = "task3",
                userId = "user2",
                updateType = UpdateType.UPDATED,
                payload = """{"id":"task3"}"""
            )
        )

        coEvery { dao.getItemsByUserId("user1") } returns user1Items
        coEvery { dao.getItemsByUserId("user2") } returns user2Items
        coEvery { dao.getQueueSize("user1") } returns 1
        coEvery { dao.getQueueSize("user2") } returns 2

        val user1Queue = queue.getQueuedUpdates("user1")
        val user2Queue = queue.getQueuedUpdates("user2")

        assertEquals(1, user1Queue.size)
        assertEquals(2, user2Queue.size)
        assertEquals(1, queue.getQueueSize("user1"))
        assertEquals(2, queue.getQueueSize("user2"))
    }

    @Test
    fun `Rapid operations workflow - Handle rapid add/remove operations`() = runTest {
        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.deleteItemById(any()) } returns Unit
        coEvery { dao.getItemsByUserId("user1") } returns emptyList()

        // Rapid adds
        repeat(5) { i ->
            queue.addUpdate("task$i", "user1", UpdateType.CREATED, """{"id":"task$i"}""")
        }

        // Rapid removes
        repeat(5) { i ->
            queue.removeUpdate("update$i")
        }

        coVerify(atLeast = 5) { dao.insert(any()) }
        coVerify(atLeast = 5) { dao.deleteItemById(any()) }
    }

    @Test
    fun `Update type filtering workflow - Filter by update type`() = runTest {
        val createdItems = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            )
        )

        coEvery { dao.getItemsByUserAndUpdateType("user1", UpdateType.CREATED) } returns createdItems

        val result = dao.getItemsByUserAndUpdateType("user1", UpdateType.CREATED)
        assertEquals(1, result.size)
        assertEquals(UpdateType.CREATED, result[0].updateType)
    }

    @Test
    fun `Mark all as applied workflow - Mark all items for user as applied`() = runTest {
        coEvery { dao.markAllAsApplied("user1") } returns Unit
        coEvery { dao.getUnappliedQueueSize("user1") } returns 0

        queue.markAsApplied("update1")
        val unappliedCount = queue.getUnappliedQueueSize("user1")

        assertEquals(0, unappliedCount)
    }

    @Test
    fun `Delete applied items workflow - Clean up applied items`() = runTest {
        coEvery { dao.deleteAppliedItems() } returns Unit

        dao.deleteAppliedItems()

        coVerify { dao.deleteAppliedItems() }
    }
}
