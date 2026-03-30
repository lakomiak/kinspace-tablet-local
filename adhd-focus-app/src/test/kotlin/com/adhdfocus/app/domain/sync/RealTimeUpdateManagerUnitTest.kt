package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.task.TaskManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for RealTimeUpdateManager.
 *
 * Tests individual functionality of the real-time update manager.
 */
class RealTimeUpdateManagerUnitTest {

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
    fun testStartListeningInitializesConnectionState() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")

        val state = realTimeUpdateManager.getConnectionState()
        assertEquals(ConnectionState.CONNECTED, state)
    }

    @Test
    fun testStopListeningChangesConnectionState() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")
        realTimeUpdateManager.stopListening()

        val state = realTimeUpdateManager.getConnectionState()
        assertEquals(ConnectionState.DISCONNECTED, state)
    }

    @Test
    fun testIsListeningReturnsTrueWhenListening() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")

        assertTrue(realTimeUpdateManager.isListening())
    }

    @Test
    fun testIsListeningReturnsFalseWhenNotListening() = runTest {
        assertFalse(realTimeUpdateManager.isListening())
    }

    @Test
    fun testObserveUpdatesEmitsTaskUpdatedEvent() = runTest {
        val task = createTestTask("task1", "Test Task")
        val event = UpdateEvent.TaskUpdated("task1", task)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event)

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates.size)
        assertEquals(event, updates[0])
    }

    @Test
    fun testObserveUpdatesEmitsTaskDeletedEvent() = runTest {
        val event = UpdateEvent.TaskDeleted("task1")

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event)

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates.size)
        assertEquals(event, updates[0])
    }

    @Test
    fun testObserveUpdatesEmitsTaskCreatedEvent() = runTest {
        val task = createTestTask("task1", "New Task")
        val event = UpdateEvent.TaskCreated(task)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event)

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(1, updates.size)
        assertEquals(event, updates[0])
    }

    @Test
    fun testObserveConnectionStateEmitsConnectedState() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")

        val states = realTimeUpdateManager.observeConnectionState().toList()
        assertTrue(states.contains(ConnectionState.CONNECTED))
    }

    @Test
    fun testObserveConnectionStateEmitsDisconnectedState() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")
        realTimeUpdateManager.stopListening()

        val states = realTimeUpdateManager.observeConnectionState().toList()
        assertTrue(states.contains(ConnectionState.DISCONNECTED))
    }

    @Test
    fun testLatencyTrackingRecordsMetrics() = runTest {
        val task = createTestTask("task1", "Test Task")
        val event = UpdateEvent.TaskUpdated("task1", task)

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(event)

        realTimeUpdateManager.startListening("household1", "user1")

        val latencies = realTimeUpdateManager.observeLatency().toList()
        assertTrue(latencies.isNotEmpty())
        assertEquals("task1", latencies[0].taskId)
        assertTrue(latencies[0].latencyMs >= 0)
    }

    @Test
    fun testAverageLatencyCalculation() = runTest {
        val task1 = createTestTask("task1", "Task 1")
        val task2 = createTestTask("task2", "Task 2")
        val events = listOf(
            UpdateEvent.TaskUpdated("task1", task1),
            UpdateEvent.TaskUpdated("task2", task2)
        )

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")

        val avgLatency = realTimeUpdateManager.getAverageLatency()
        assertTrue(avgLatency >= 0)
    }

    @Test
    fun testMultipleUpdatesProcessed() = runTest {
        val task1 = createTestTask("task1", "Task 1")
        val task2 = createTestTask("task2", "Task 2")
        val events = listOf(
            UpdateEvent.TaskUpdated("task1", task1),
            UpdateEvent.TaskUpdated("task2", task2)
        )

        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf(*events.toTypedArray())

        realTimeUpdateManager.startListening("household1", "user1")

        val updates = realTimeUpdateManager.observeUpdates().toList()
        assertEquals(2, updates.size)
    }

    @Test
    fun testStartListeningIdempotent() = runTest {
        coEvery { webSocketTaskUpdateHandler.observeUpdates() } returns flowOf()

        realTimeUpdateManager.startListening("household1", "user1")
        realTimeUpdateManager.startListening("household1", "user1")

        assertTrue(realTimeUpdateManager.isListening())
    }

    @Test
    fun testConnectionStateInitiallyDisconnected() = runTest {
        val state = realTimeUpdateManager.getConnectionState()
        assertEquals(ConnectionState.DISCONNECTED, state)
    }

    @Test
    fun testAverageLatencyZeroWhenNoUpdates() = runTest {
        val avgLatency = realTimeUpdateManager.getAverageLatency()
        assertEquals(0, avgLatency)
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
