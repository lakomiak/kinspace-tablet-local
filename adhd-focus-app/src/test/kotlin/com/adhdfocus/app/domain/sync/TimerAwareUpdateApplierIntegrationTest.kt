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
import kotlin.test.assertTrue

/**
 * Integration tests for TimerAwareUpdateApplier.
 *
 * Tests complete workflows and interactions with other components.
 */
class TimerAwareUpdateApplierIntegrationTest {

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
    fun `complete workflow - queue update, apply when timer completes`() = runTest {
        // Arrange
        val task = createTestTask("task1")
        val event = UpdateEvent.TaskUpdated("task1", task)

        // Act - Start timer and queue update
        applier.setTimerActive(true)
        val queueResult = applier.applyUpdate(event)

        // Assert - Update is queued
        assertTrue(queueResult.success)
        assertEquals(1, applier.getQueuedUpdateCount())

        // Act - Timer completes
        applier.setTimerActive(false)

        // Assert - Update is applied
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.update(task) }
    }

    @Test
    fun `multiple updates workflow - queue multiple, apply all on timer completion`() = runTest {
        // Arrange
        val tasks = (1..3).map { createTestTask("task$it") }
        val events = tasks.mapIndexed { index, task ->
            UpdateEvent.TaskUpdated("task${index + 1}", task)
        }

        // Act - Start timer and queue updates
        applier.setTimerActive(true)
        events.forEach { applier.applyUpdate(it) }

        // Assert - All updates queued
        assertEquals(3, applier.getQueuedUpdateCount())

        // Act - Timer completes
        applier.setTimerActive(false)

        // Assert - All updates applied
        assertEquals(0, applier.getQueuedUpdateCount())
        tasks.forEach { task ->
            coVerify { taskDao.update(task) }
        }
    }

    @Test
    fun `timer cancellation workflow - clear queued updates`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)

        // Act - Queue updates
        applier.setTimerActive(true)
        applier.queueUpdate(event1)
        applier.queueUpdate(event2)

        // Assert - Updates queued
        assertEquals(2, applier.getQueuedUpdateCount())

        // Act - Clear queue (timer cancelled)
        applier.clearQueuedUpdates()

        // Assert - Queue cleared
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify(exactly = 0) { taskDao.update(any()) }
    }

    @Test
    fun `mixed operations workflow - apply some immediately, queue others`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val task3 = createTestTask("task3")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)
        val event3 = UpdateEvent.TaskUpdated("task3", task3)

        // Act - Apply first update with timer inactive
        applier.setTimerActive(false)
        applier.applyUpdate(event1)

        // Assert - First update applied immediately
        coVerify { taskDao.update(task1) }

        // Act - Queue updates with timer active
        applier.setTimerActive(true)
        applier.applyUpdate(event2)
        applier.applyUpdate(event3)

        // Assert - Updates queued
        assertEquals(2, applier.getQueuedUpdateCount())

        // Act - Timer completes
        applier.setTimerActive(false)

        // Assert - Queued updates applied
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.update(task2) }
        coVerify { taskDao.update(task3) }
    }

    @Test
    fun `task deletion workflow - queue and apply deletion`() = runTest {
        // Arrange
        val event = UpdateEvent.TaskDeleted("task1")

        // Act - Queue deletion
        applier.setTimerActive(true)
        applier.queueUpdate(event)

        // Assert - Deletion queued
        assertEquals(1, applier.getQueuedUpdateCount())

        // Act - Timer completes
        applier.setTimerActive(false)

        // Assert - Deletion applied
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.deleteById("task1") }
    }

    @Test
    fun `task creation workflow - queue and apply creation`() = runTest {
        // Arrange
        val task = createTestTask("task1")
        val event = UpdateEvent.TaskCreated(task)

        // Act - Queue creation
        applier.setTimerActive(true)
        applier.queueUpdate(event)

        // Assert - Creation queued
        assertEquals(1, applier.getQueuedUpdateCount())

        // Act - Timer completes
        applier.setTimerActive(false)

        // Assert - Creation applied
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.insert(task) }
    }

    @Test
    fun `rapid timer state changes workflow - handle multiple state transitions`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)

        // Act - Rapid state changes
        applier.setTimerActive(true)
        applier.queueUpdate(event1)
        applier.setTimerActive(false)
        applier.setTimerActive(true)
        applier.queueUpdate(event2)
        applier.setTimerActive(false)

        // Assert - All updates applied
        assertEquals(0, applier.getQueuedUpdateCount())
        coVerify { taskDao.update(task1) }
        coVerify { taskDao.update(task2) }
    }

    @Test
    fun `queue observation workflow - observe queued update events`() = runTest {
        // Arrange
        val task = createTestTask("task1")
        val event = UpdateEvent.TaskUpdated("task1", task)
        val events = mutableListOf<QueuedUpdateEvent>()

        // Act - Observe queue changes
        val job = kotlinx.coroutines.launch {
            applier.observeQueuedUpdates().collect { events.add(it) }
        }

        applier.setTimerActive(true)
        applier.queueUpdate(event)
        kotlinx.coroutines.delay(100)

        // Assert - Event emitted
        assertTrue(events.isNotEmpty())
        assertTrue(events[0] is QueuedUpdateEvent.UpdateQueued)

        job.cancel()
    }

    @Test
    fun `empty queue workflow - apply queued updates on empty queue`() = runTest {
        // Arrange
        applier.setTimerActive(true)

        // Act - Apply queued updates with empty queue
        val result = applier.applyQueuedUpdates()

        // Assert - Success with 0 updates
        assertTrue(result.success)
        assertEquals(0, applier.getQueuedUpdateCount())
    }

    @Test
    fun `large batch workflow - handle large number of updates`() = runTest {
        // Arrange
        val taskCount = 100
        val tasks = (1..taskCount).map { createTestTask("task$it") }
        val events = tasks.mapIndexed { index, task ->
            UpdateEvent.TaskUpdated("task${index + 1}", task)
        }

        // Act - Queue all updates
        applier.setTimerActive(true)
        events.forEach { applier.queueUpdate(it) }

        // Assert - All queued
        assertEquals(taskCount, applier.getQueuedUpdateCount())

        // Act - Apply all
        applier.setTimerActive(false)

        // Assert - All applied
        assertEquals(0, applier.getQueuedUpdateCount())
    }

    @Test
    fun `partial failure workflow - handle failures during application`() = runTest {
        // Arrange
        val task1 = createTestTask("task1")
        val task2 = createTestTask("task2")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)
        val event2 = UpdateEvent.TaskUpdated("task2", task2)

        // Mock failure on first update
        coEvery { taskDao.update(task1) } throws Exception("Database error")

        // Act - Queue updates
        applier.setTimerActive(true)
        applier.queueUpdate(event1)
        applier.queueUpdate(event2)

        // Act - Apply updates
        applier.setTimerActive(false)

        // Assert - Queue cleared despite failure
        assertEquals(0, applier.getQueuedUpdateCount())
    }

    private fun createTestTask(id: String): Task {
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
