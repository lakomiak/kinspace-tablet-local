package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.task.TaskManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for RealTimeUpdateManager.
 *
 * Tests complete workflows and interactions with other components.
 */
class RealTimeUpdateManagerIntegrationTest {

    private lateinit var webSocketTaskUpdateHandler: WebSocketTaskUpdateHandler
    private lateinit var taskDao: TaskDao
    private lateinit var taskManager: TaskManager
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var realTimeUpdateManager: RealTimeUpdateManager

    @Before
    fun setUp() {
        webSocketTaskUpdateHandler = mockk()
        taskDao = mockk()
        taskManager = mockk()
        webSocketManager = mockk()

        realTimeUpdateManager = RealTimeUpdateManagerImpl(
            webSocketTaskUpdateHandler,
            taskDao,
            taskManager,
            webSocketManager
        )
    }

    @Test
    fun testRealTimeUpdateWorkflow() = runTest {
        val task = createTestTask("task1", "Test Task")
        val event = UpdateEvent.TaskUpdated("task1", task)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event)

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates.size)
        assertEquals(event, updates[0])

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testMultipleUpdatesWorkflow() = runTest {
        val task1 = createTestTask("task1", "Task 1")
        val task2 = createTestTask("task2", "Task 2")
        val task3 = createTestTask("task3", "Task 3")

        val events = listOf(
            UpdateEvent.TaskUpdated("task1", task1),
            UpdateEvent.TaskUpdated("task2", task2),
            UpdateEvent.TaskUpdated("task3", task3)
        )

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(3, updates.size)

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testTaskDeletionWorkflow() = runTest {
        val task = createTestTask("task1", "Task to Delete")
        val deleteEvent = UpdateEvent.TaskDeleted("task1")

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(deleteEvent)

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates.size)
        assertEquals(deleteEvent, updates[0])

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testTaskCreationWorkflow() = runTest {
        val newTask = createTestTask("task1", "New Task")
        val createEvent = UpdateEvent.TaskCreated(newTask)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(createEvent)

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates.size)
        assertEquals(createEvent, updates[0])

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testConnectionStateTransitionWorkflow() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")
        assertEquals(ConnectionState.CONNECTED, realTimeUpdateManager.getConnectionState())

        val states = realTimeUpdateManager.observeConnectionState().toList()
        assertTrue(states.contains(ConnectionState.CONNECTED))

        realTimeUpdateManager.stopListening()
        assertEquals(ConnectionState.DISCONNECTED, realTimeUpdateManager.getConnectionState())
    }

    @Test
    fun testLatencyTrackingWorkflow() = runTest {
        val task1 = createTestTask("task1", "Task 1")
        val task2 = createTestTask("task2", "Task 2")

        val events = listOf(
            UpdateEvent.TaskUpdated("task1", task1),
            UpdateEvent.TaskUpdated("task2", task2)
        )

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")

        val latencies = realTimeUpdateManager.observeLatency().toList()
        assertEquals(2, latencies.size)
        assertTrue(latencies.all { it.latencyMs >= 0 })

        val avgLatency = realTimeUpdateManager.getAverageLatency()
        assertTrue(avgLatency >= 0)

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testMixedEventTypesWorkflow() = runTest {
        val task1 = createTestTask("task1", "Task 1")
        val task2 = createTestTask("task2", "Task 2")
        val task3 = createTestTask("task3", "Task 3")

        val events = listOf(
            UpdateEvent.TaskUpdated("task1", task1),
            UpdateEvent.TaskDeleted("task2"),
            UpdateEvent.TaskCreated(task3)
        )

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(3, updates.size)

        val latencies = realTimeUpdateManager.observeLatency().toList()
        assertEquals(3, latencies.size)

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testStartStopStartWorkflow() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")
        assertTrue(realTimeUpdateManager.isListening())

        realTimeUpdateManager.stopListening()
        assertTrue(!realTimeUpdateManager.isListening())

        realTimeUpdateManager.startListening("household1", "user1")
        assertTrue(realTimeUpdateManager.isListening())

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testListeningStateAfterMultipleUpdates() = runTest {
        val tasks = (1..5).map { createTestTask("task$it", "Task $it") }
        val events = tasks.map { UpdateEvent.TaskUpdated(it.id, it) }

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")
        assertTrue(realTimeUpdateManager.isListening())

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(5, updates.size)

        realTimeUpdateManager.stopListening()
        assertTrue(!realTimeUpdateManager.isListening())
    }

    @Test
    fun testLatencyAverageCalculation() = runTest {
        val tasks = (1..10).map { createTestTask("task$it", "Task $it") }
        val events = tasks.map { UpdateEvent.TaskUpdated(it.id, it) }

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")

        val avgLatency = realTimeUpdateManager.getAverageLatency()
        assertTrue(avgLatency >= 0)

        realTimeUpdateManager.stopListening()
    }

    @Test
    fun testConnectionStateInitiallyDisconnected() = runTest {
        val state = realTimeUpdateManager.getConnectionState()
        assertEquals(ConnectionState.DISCONNECTED, state)
    }

    @Test
    fun testIsListeningInitiallyFalse() = runTest {
        val isListening = realTimeUpdateManager.isListening()
        assertTrue(!isListening)
    }

    @Test
    fun testAverageLatencyZeroInitially() = runTest {
        val avgLatency = realTimeUpdateManager.getAverageLatency()
        assertEquals(0, avgLatency)
    }

    @Test
    fun testMultipleHouseholdsAndUsers() = runTest {
        val task1 = createTestTask("task1", "Task 1")
        val event1 = UpdateEvent.TaskUpdated("task1", task1)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event1)

        realTimeUpdateManager.startListening("household1", "user1")
        val updates1 = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates1.size)

        realTimeUpdateManager.stopListening()

        // Simulate switching to different household/user
        val task2 = createTestTask("task2", "Task 2")
        val event2 = UpdateEvent.TaskUpdated("task2", task2)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event2)

        realTimeUpdateManager.startListening("household2", "user2")
        val updates2 = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates2.size)

        realTimeUpdateManager.stopListening()
    }

    private fun createTestTask(id: String, title: String): Task {
        return Task(
            id = id,
            householdId = "household1",
            assignedUserId = "user1",
            title = title,
            description = "Test description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            status = TaskStatus.INCOMPLETE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
