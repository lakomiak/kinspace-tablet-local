package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.OfflineUpdateQueueDao
import com.adhdfocus.app.data.model.OfflineUpdateQueueItem
import com.adhdfocus.app.data.model.UpdateType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineUpdateQueueUnitTest {
    private lateinit var dao: OfflineUpdateQueueDao
    private lateinit var queue: OfflineUpdateQueue

    @Before
    fun setup() {
        dao = mockk()
        queue = OfflineUpdateQueueImpl(dao)
    }

    @Test
    fun `addUpdate should insert item and return true`() = runTest {
        coEvery { dao.insert(any()) } returns 1L

        val result = queue.addUpdate(
            taskId = "task1",
            userId = "user1",
            updateType = UpdateType.CREATED,
            payload = """{"id":"task1"}"""
        )

        assertTrue(result)
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `addUpdate should return false on exception`() = runTest {
        coEvery { dao.insert(any()) } throws Exception("DB error")

        val result = queue.addUpdate(
            taskId = "task1",
            userId = "user1",
            updateType = UpdateType.CREATED,
            payload = """{"id":"task1"}"""
        )

        assertFalse(result)
    }

    @Test
    fun `getQueuedUpdates should return items from dao`() = runTest {
        val items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.CREATED,
                payload = """{"id":"task1"}"""
            )
        )
        coEvery { dao.getItemsByUserId("user1") } returns items

        val result = queue.getQueuedUpdates("user1")

        assertEquals(1, result.size)
        assertEquals("task1", result[0].taskId)
    }

    @Test
    fun `getUnappliedUpdates should return unapplied items`() = runTest {
        val items = listOf(
            OfflineUpdateQueueItem(
                taskId = "task1",
                userId = "user1",
                updateType = UpdateType.UPDATED,
                payload = """{"id":"task1"}""",
                applied = false
            )
        )
        coEvery { dao.getUnappliedItemsByUserId("user1") } returns items

        val result = queue.getUnappliedUpdates("user1")

        assertEquals(1, result.size)
        assertFalse(result[0].applied)
    }

    @Test
    fun `removeUpdate should delete item and return true`() = runTest {
        coEvery { dao.deleteItemById("update1") } returns Unit

        val result = queue.removeUpdate("update1")

        assertTrue(result)
        coVerify { dao.deleteItemById("update1") }
    }

    @Test
    fun `removeUpdate should return false on exception`() = runTest {
        coEvery { dao.deleteItemById("update1") } throws Exception("DB error")

        val result = queue.removeUpdate("update1")

        assertFalse(result)
    }

    @Test
    fun `markAsApplied should mark item and return true`() = runTest {
        coEvery { dao.markAsApplied("update1") } returns Unit

        val result = queue.markAsApplied("update1")

        assertTrue(result)
        coVerify { dao.markAsApplied("update1") }
    }

    @Test
    fun `markAsApplied should return false on exception`() = runTest {
        coEvery { dao.markAsApplied("update1") } throws Exception("DB error")

        val result = queue.markAsApplied("update1")

        assertFalse(result)
    }

    @Test
    fun `clearQueue should delete all items for user and return true`() = runTest {
        coEvery { dao.deleteItemsByUserId("user1") } returns Unit

        val result = queue.clearQueue("user1")

        assertTrue(result)
        coVerify { dao.deleteItemsByUserId("user1") }
    }

    @Test
    fun `clearQueue should return false on exception`() = runTest {
        coEvery { dao.deleteItemsByUserId("user1") } throws Exception("DB error")

        val result = queue.clearQueue("user1")

        assertFalse(result)
    }

    @Test
    fun `getQueueSize should return count from dao`() = runTest {
        coEvery { dao.getQueueSize("user1") } returns 5

        val result = queue.getQueueSize("user1")

        assertEquals(5, result)
    }

    @Test
    fun `getUnappliedQueueSize should return unapplied count`() = runTest {
        coEvery { dao.getUnappliedQueueSize("user1") } returns 3

        val result = queue.getUnappliedQueueSize("user1")

        assertEquals(3, result)
    }

    @Test
    fun `hasQueuedUpdates should return true when queue not empty`() = runTest {
        coEvery { dao.getQueueSize("user1") } returns 1

        val result = queue.hasQueuedUpdates("user1")

        assertTrue(result)
    }

    @Test
    fun `hasQueuedUpdates should return false when queue empty`() = runTest {
        coEvery { dao.getQueueSize("user1") } returns 0

        val result = queue.hasQueuedUpdates("user1")

        assertFalse(result)
    }
}
