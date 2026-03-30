package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.ui.timer.TimerViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateNotificationManagerIntegrationTest {

    private lateinit var timerViewModel: TimerViewModel
    private lateinit var manager: UpdateNotificationManager

    private val testTask = Task(
        id = "task-1",
        householdId = "household-1",
        assignedUserId = "user-1",
        title = "Buy groceries",
        description = "Get milk and bread",
        todoGroup = "Errands",
        estimatedDurationMinutes = 30,
        status = "INCOMPLETE"
    )

    @Before
    fun setup() {
        timerViewModel = mockk()
        manager = UpdateNotificationManagerImpl(timerViewModel)
    }

    @Test
    fun `notification workflow with timer inactive`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false

        // Act
        manager.showNotification(testTask)

        // Assert
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 1) return@collect
        }
        assertTrue(events.any { it is NotificationEvent.NotificationShown })
    }

    @Test
    fun `notification workflow with timer active`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act
        manager.showNotification(testTask)

        // Assert
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 1) return@collect
        }
        assertTrue(events.any { it is NotificationEvent.NotificationQueued })
    }

    @Test
    fun `multiple notifications workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act
        manager.showNotification(testTask)
        manager.showNotification(testTask.copy(id = "task-2"))
        manager.showNotification(testTask.copy(id = "task-3"))

        // Assert
        assertEquals(3, manager.getQueueSize())
    }

    @Test
    fun `notification dismissal workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true
        manager.showNotification(testTask)
        manager.showNotification(testTask.copy(id = "task-2"))

        // Act
        manager.dismissNotification("task-1")

        // Assert
        assertEquals(1, manager.getQueueSize())
    }

    @Test
    fun `timer state transition workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true
        manager.showNotification(testTask)
        assertEquals(1, manager.getQueueSize())

        // Act - timer becomes inactive
        coEvery { timerViewModel.isRunning.value } returns false
        manager.showNotification(testTask.copy(id = "task-2"))

        // Assert - new notification shown, not queued
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 2) return@collect
        }
        assertTrue(events.any { it is NotificationEvent.NotificationShown })
    }

    @Test
    fun `clear all notifications workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true
        manager.showNotification(testTask)
        manager.showNotification(testTask.copy(id = "task-2"))
        manager.showNotification(testTask.copy(id = "task-3"))
        assertEquals(3, manager.getQueueSize())

        // Act
        manager.clearAll()

        // Assert
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun `notification with task details workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false
        val taskWithDetails = testTask.copy(
            title = "Important task",
            todoGroup = "Morning",
            estimatedDurationMinutes = 45
        )

        // Act
        manager.showNotification(taskWithDetails)

        // Assert
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 1) return@collect
        }
        val shownEvent = events.first() as NotificationEvent.NotificationShown
        assertEquals("Important task", shownEvent.task.title)
        assertEquals("Morning", shownEvent.task.todoGroup)
        assertEquals(45, shownEvent.task.estimatedDurationMinutes)
    }

    @Test
    fun `mixed operations workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act - add notifications
        manager.showNotification(testTask)
        manager.showNotification(testTask.copy(id = "task-2"))
        manager.showNotification(testTask.copy(id = "task-3"))
        assertEquals(3, manager.getQueueSize())

        // Dismiss one
        manager.dismissNotification("task-2")
        assertEquals(2, manager.getQueueSize())

        // Add another
        manager.showNotification(testTask.copy(id = "task-4"))
        assertEquals(3, manager.getQueueSize())

        // Clear all
        manager.clearAll()
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun `notification events workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false

        // Act
        val events = mutableListOf<NotificationEvent>()
        val job = kotlinx.coroutines.launch {
            manager.observeNotifications().collect { event ->
                events.add(event)
            }
        }

        manager.showNotification(testTask)
        manager.dismissNotification("task-1")

        // Assert
        kotlinx.coroutines.delay(100) // Allow events to be collected
        assertTrue(events.any { it is NotificationEvent.NotificationShown })
        assertTrue(events.any { it is NotificationEvent.NotificationDismissed })

        job.cancel()
    }

    @Test
    fun `timer state check workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act
        val isActive = manager.isTimerActive()

        // Assert
        assertTrue(isActive)
    }

    @Test
    fun `queue size tracking workflow`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act & Assert
        assertEquals(0, manager.getQueueSize())

        manager.showNotification(testTask)
        assertEquals(1, manager.getQueueSize())

        manager.showNotification(testTask.copy(id = "task-2"))
        assertEquals(2, manager.getQueueSize())

        manager.dismissNotification("task-1")
        assertEquals(1, manager.getQueueSize())
    }

    @Test
    fun `notification with no estimated duration`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false
        val taskNoEstimate = testTask.copy(estimatedDurationMinutes = null)

        // Act
        manager.showNotification(taskNoEstimate)

        // Assert
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 1) return@collect
        }
        val shownEvent = events.first() as NotificationEvent.NotificationShown
        assertEquals(null, shownEvent.task.estimatedDurationMinutes)
    }

    @Test
    fun `rapid notification additions`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act
        repeat(10) { i ->
            manager.showNotification(testTask.copy(id = "task-$i"))
        }

        // Assert
        assertEquals(10, manager.getQueueSize())
    }

    @Test
    fun `notification dismissal with empty queue`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act & Assert - should not throw
        manager.dismissNotification("non-existent")
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun `clear all with empty queue`() = runBlocking {
        // Act & Assert - should not throw
        manager.clearAll()
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun `notification timestamp tracking`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false

        // Act
        manager.showNotification(testTask)

        // Assert
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 1) return@collect
        }
        val shownEvent = events.first() as NotificationEvent.NotificationShown
        assertTrue(shownEvent.timestamp > 0)
    }
}
