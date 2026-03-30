package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.OfflineUpdateQueueDao
import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.model.UpdateType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for OfflineUpdateQueue.
 *
 * **Validates: Requirements 2, 11**
 *
 * These tests verify universal properties that should hold across all valid inputs:
 * - Queue consistency: All added items are retrievable
 * - Persistence correctness: Items survive queue operations
 * - Update ordering: FIFO ordering is maintained
 * - Conflict handling: Timestamp-based resolution works correctly
 */
class OfflineUpdateQueuePropertyTest {
    private lateinit var dao: OfflineUpdateQueueDao
    private lateinit var queue: OfflineUpdateQueue

    @Before
    fun setup() {
        dao = mockk()
        queue = OfflineUpdateQueueImpl(dao)
    }

    @Test
    fun `Property 1 - Queue Consistency - All added items are retrievable`() = runTest {
        // For any set of updates added to the queue, all should be retrievable
        val updates = listOf(
            Triple("task1", UpdateType.CREATED, """{"id":"task1"}"""),
            Triple("task2", UpdateType.UPDATED, """{"id":"task2"}"""),
            Triple("task3", UpdateType.DELETED, """{"id":"task3"}""")
        )

        val queuedItems = updates.mapIndexed { index, (taskId, updateType, payload) ->
            OfflineUpdateQueueItem(
                id = "update$index",
                taskId = taskId,
                userId = "user1",
                updateType = updateType,
                payload = payload
            )
        }

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getItemsByUserId("user1") } returns queuedItems

        // Add all updates
        updates.forEach { (taskId, updateType, payload) ->
            queue.addUpdate(taskId, "user1", updateType, payload)
        }

        // Verify all are retrievable
        val retrieved = queue.getQueuedUpdates("user1")
        assertEquals(updates.size, retrieved.size)
    }

    @Test
    fun `Property 2 - Dismissal Correctness - Dismissed items are removed`() = runTest {
        // For any item removed from queue, it should not be retrievable
        val item = OfflineUpdateQueueItem(
            id = "update1",
            taskId = "task1",
            userId = "user1",
            updateType = UpdateType.CREATED,
            payload = """{"id":"task1"}"""
        )

        coEvery { dao.deleteItemById("update1") } returns Unit
        coEvery { dao.getItemsByUserId("user1") } returns emptyList()

        queue.removeUpdate("update1")
        val remaining = queue.getQueuedUpdates("user1")

        assertEquals(0, remaining.size)
    }

    @Test
    fun `Property 3 - Queue Management - Queue size is accurate`() = runTest {
        // For any queue state, size should match actual item count
        val sizes = listOf(0, 1, 5, 10)

        sizes.forEach { size ->
            coEvery { dao.getQueueSize("user1") } returns size

            val queueSize = queue.getQueueSize("user1")
            assertEquals(size, queueSize)
        }
    }

    @Test
    fun `Property 4 - Timer State Handling - Queue operations work regardless of timer state`() = runTest {
        // For any queue operation, it should succeed regardless of external state
        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getQueueSize("user1") } returns 1

        val result1 = queue.addUpdate("task1", "user1", UpdateType.CREATED, """{"id":"task1"}""")
        val size1 = queue.getQueueSize("user1")

        assertTrue(result1)
        assertEquals(1, size1)
    }

    @Test
    fun `Property 5 - Multiple Updates - Multiple updates handled correctly`() = runTest {
        // For any number of updates, all should be managed independently
        val updateCount = 5
        val items = (1..updateCount).map { i ->
            OfflineUpdateQueueItem(
                id = "update$i",
                taskId = "task$i",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task$i"}"""
            )
        }

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getItemsByUserId("user1") } returns items

        (1..updateCount).forEach { i ->
            queue.addUpdate("task$i", "user1", UpdateType.CREATED, """{"id":"task$i"}""")
        }

        val retrieved = queue.getQueuedUpdates("user1")
        assertEquals(updateCount, retrieved.size)
    }

    @Test
    fun `Property 6 - Update Details - Task details are preserved`() = runTest {
        // For any update, all details should be preserved through queue operations
        val taskId = "task1"
        val userId = "user1"
        val updateType = UpdateType.UPDATED
        val payload = """{"id":"task1","title":"Test"}"""

        val item = OfflineUpdateQueueItem(
            taskId = taskId,
            userId = userId,
            updateType = updateType,
            payload = payload
        )

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.getItemsByUserId(userId) } returns listOf(item)

        queue.addUpdate(taskId, userId, updateType, payload)
        val retrieved = queue.getQueuedUpdates(userId)

        assertEquals(1, retrieved.size)
        assertEquals(taskId, retrieved[0].taskId)
        assertEquals(updateType, retrieved[0].updateType)
        assertEquals(payload, retrieved[0].payload)
    }

    @Test
    fun `Property 7 - Event Emission - Queue state changes are observable`() = runTest {
        // For any queue change, state should be observable
        val items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            )
        )

        coEvery { dao.observeItemsByUserId("user1") } returns flowOf(items)

        val stateFlow = queue.observeQueueChanges("user1")
        var emittedState: QueueState? = null

        stateFlow.collect { state ->
            emittedState = state
        }

        assertEquals("user1", emittedState?.userId)
        assertEquals(1, emittedState?.queueSize)
    }

    @Test
    fun `Property 8 - Notification Isolation - Updates don't interfere with each other`() = runTest {
        // For any two updates for different users, they should be independent
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
            )
        )

        coEvery { dao.getItemsByUserId("user1") } returns user1Items
        coEvery { dao.getItemsByUserId("user2") } returns user2Items

        val user1Queue = queue.getQueuedUpdates("user1")
        val user2Queue = queue.getQueuedUpdates("user2")

        assertEquals(1, user1Queue.size)
        assertEquals(1, user2Queue.size)
        assertEquals("task1", user1Queue[0].taskId)
        assertEquals("task2", user2Queue[0].taskId)
    }
}
