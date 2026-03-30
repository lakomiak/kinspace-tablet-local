package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.task.TaskManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TimerAwareUpdateApplier.
 *
 * Tests individual functionality of update queuing and application.
 */
class TimerAwareUpdateApplierUnitTest {

    private lateinit var taskDao: TaskDao
    private lateinit var taskManager: TaskManager
    private lateinit var realTimeUpdateManager: RealTimeUpdateManager
    private lateinit var applier: TimerAwareUpdateApplier

    @Before
    fun setup() {
        taskDao = mockk(relaxed = true)
        taskManager = mockk(relaxed = true)
        realTimeUpdateManager = mockk(relaxed = true)
        applier = TimerAwareUpdateApplierImpl(taskDao, taskManager, realTimeUpdateManager)
    }

    @Test
    fun `update is queued when timer is active`() = runTest {
        // Arrange
        applier.setTimerActive(true)
        val task = createTestTask()
        val event = UpdateEvent.TaskUpdated("task1", task)

        // Act
        val result = applier.applyUpdate(event)

        // Assert
        assertTrue(result.success)
        assertEquals(1, applier.getQueuedUpdateCount())
    }

    @Test
    fun `update is applied immediately when timer is inactive`() = runTest {
        // Arrange
        applier.setTimerActive(false)
        val task = createTestTask()
        val event = UpdateEvent.TaskUpdated("task1", task)

        // Act
        val result = applier.applyUpdate(event)

        // Assert
        assertTrue(result.success)
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.update(task) }
    }

    @Test
    fun `queue update returns true on success`() = runTest {
        // Arrange
        val task = createTestTask()
        val event = UpdateEvent.TaskUpdated("task1", task)

        // Act
        val result = applier.queueUpdate(event)

        // Assert
        assertTrue(result)
        assertEquals(1, applier.getQueuedUpdateCount())
    }

    @Test
    fun `queue update returns false on exception`() = runTest {
        // Arrange
        val applier = object : TimerAwareUpdateApplier {
            override suspend fun applyUpdate(event: UpdateEvent) = UpdateResult(false)
            override suspend fun queueUpdate(event: UpdateEvent) = throw Exception("Test error")
            override suspend fun applyQueuedUpdates() = UpdateResult(false)
            override suspend fun getQueuedUpdateCount() = 0
            override fun observeQueuedUpdates() = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun isTimerActive() = false
            override suspend fun setTimerActive(active: Boolean) {}
            override suspend fun clearQueuedUpdates() = false
        }

        val task = createTestTask()
        val event = UpdateEvent.TaskUpdated("task1", task)

        // Act
        val result = applier.queueUpdate(event)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `apply queued updates applies all updates in FIFO order`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)

        applier.queueUpdate(event1)
        applier.queueUpdate(event2)

        // Act
        val result = applier.applyQueuedUpdates()

        // Assert
        assertTrue(result.success)
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.update(task1) }
        coVerify { taskDao.update(task2) }
    }

    @Test
    fun `apply queued updates clears queue after application`() = runTest {
        // Arrange
        val task = createTestTask()
        val event = UpdateEvent.TaskUpdated("task1", task)
        applier.queueUpdate(event)

        // Act
        applier.applyQueuedUpdates()

        // Assert
        assertEquals(0, applier.getQueuedUpdateCount())
    }

    @Test
    fun `get queued update count returns correct count`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)

        // Act
        applier.queueUpdate(event1)
        applier.queueUpdate(event2)

        // Assert
        assertEquals(2, applier.getQueuedUpdateCount())
    }

    @Test
    fun `is timer active returns correct state`() = runTest {
        // Arrange & Act
        applier.setTimerActive(true)

        // Assert
        assertTrue(applier.isTimerActive())

        // Act
        applier.setTimerActive(false)

        // Assert
        assertFalse(applier.isTimerActive())
    }

    @Test
    fun `set timer active to false applies queued updates`() = runTest {
        // Arrange
        applier.setTimerActive(true)
        val task = createTestTask()
        val event = UpdateEvent.TaskUpdated("task1", task)
        applier.queueUpdate(event)

        // Act
        applier.setTimerActive(false)

        // Assert
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.update(task) }
    }

    @Test
    fun `clear queued updates clears all updates`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)
        applier.queueUpdate(event1)
        applier.queueUpdate(event2)

        // Act
        val result = applier.clearQueuedUpdates()

        // Assert
        assertTrue(result)
        assertEquals(0, applier.getQueuedUpdateCount())
    }

    @Test
    fun `clear queued updates returns false on exception`() = runTest {
        // Arrange
        val applier = object : TimerAwareUpdateApplier {
            override suspend fun applyUpdate(event: UpdateEvent) = UpdateResult(false)
            override suspend fun queueUpdate(event: UpdateEvent) = true
            override suspend fun applyQueuedUpdates() = UpdateResult(false)
            override suspend fun getQueuedUpdateCount() = 0
            override fun observeQueuedUpdates() = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun isTimerActive() = false
            override suspend fun setTimerActive(active: Boolean) {}
            override suspend fun clearQueuedUpdates() = throw Exception("Test error")
        }

        // Act
        val result = applier.clearQueuedUpdates()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `multiple updates are handled correctly`() = runTest {
        // Arrange
        applier.setTimerActive(true)
        val tasks = (1..5).map { createTestTask("task$it") }
        val events = tasks.mapIndexed { index, task ->
            UpdateEvent.TaskUpdated("task${index + 1}", task)
        }

        // Act
        events.forEach { applier.queueUpdate(it) }

        // Assert
        assertEquals(5, applier.getQueuedUpdateCount())

        // Act
        applier.setTimerActive(false)

        // Assert
        assertEquals(0, applier.getQueuedUpdateCount())
    }

    private fun createTestTask(id: String = "task1"): Task {
        return Task(
            id = id,
            householdId = "household1",
            assignedUserId = "user1",
            title = "Test Task",
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            status = TaskStatus.INCOMPLETE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
