package com.adhdfocus.app.domain.notification

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.ui.timer.TimerViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateNotificationManagerUnitTest {

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
    fun `showNotification shows notification when timer inactive`() = runBlocking {
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
    fun `showNotification queues notification when timer active`() = runBlocking {
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
    fun `dismissNotification removes notification from queue`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true
        manager.showNotification(testTask)
        val queueSizeBefore = manager.getQueueSize()

        // Act
        manager.dismissNotification("task-1")

        // Assert
        val queueSizeAfter = manager.getQueueSize()
        assertTrue(queueSizeBefore > 0)
        assertEquals(0, queueSizeAfter)
    }

    @Test
    fun `dismissNotification emits dismissal event`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false
        manager.showNotification(testTask)

        // Act
        manager.dismissNotification("task-1")

        // Assert
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 2) return@collect
        }
        assertTrue(events.any { it is NotificationEvent.NotificationDismissed })
    }

    @Test
    fun `isTimerActive returns correct state`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true

        // Act
        val isActive = manager.isTimerActive()

        // Assert
        assertTrue(isActive)
    }

    @Test
    fun `getQueueSize returns correct count`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true
        manager.showNotification(testTask)
        manager.showNotification(testTask.copy(id = "task-2"))

        // Act
        val queueSize = manager.getQueueSize()

        // Assert
        assertEquals(2, queueSize)
    }

    @Test
    fun `clearAll removes all notifications`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns true
        manager.showNotification(testTask)
        manager.showNotification(testTask.copy(id = "task-2"))

        // Act
        manager.clearAll()

        // Assert
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun `multiple notifications handled correctly`() = runBlocking {
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
    fun `notification contains task details`() = runBlocking {
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
        assertEquals(testTask.title, shownEvent.task.title)
        assertEquals(testTask.todoGroup, shownEvent.task.todoGroup)
    }

    @Test
    fun `notification ID is generated`() = runBlocking {
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
        assertTrue(shownEvent.notificationId.isNotEmpty())
    }

    @Test
    fun `timer state checked before showing notification`() = runBlocking {
        // Arrange
        coEvery { timerViewModel.isRunning.value } returns false

        // Act
        manager.showNotification(testTask)

        // Assert - notification should be shown, not queued
        val events = mutableListOf<NotificationEvent>()
        manager.observeNotifications().collect { event ->
            events.add(event)
            if (events.size == 1) return@collect
        }
        assertTrue(events.any { it is NotificationEvent.NotificationShown })
        assertFalse(events.any { it is NotificationEvent.NotificationQueued })
    }

    @Test
    fun `dismissing non-existent notification is safe`() = runBlocking {
        // Act & Assert - should not throw
        manager.dismissNotification("non-existent-id")
        assertEquals(0, manager.getQueueSize())
    }
}
