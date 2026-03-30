package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import java.time.Instant
import java.util.UUID

/**
 * Test fixtures and helpers for real-time update integration tests.
 *
 * Provides mock data generators and assertion helpers for comprehensive
 * testing of real-time update workflows.
 */
object RealTimeUpdateTestFixtures {

    // ============ Mock WebSocket Event Generators ============

    /**
     * Generates a mock WebSocket task update event.
     */
    fun generateTaskUpdateEvent(
        taskId: String = UUID.randomUUID().toString(),
        title: String = "Test Task",
        status: TaskStatus = TaskStatus.INCOMPLETE
    ): WebSocketEvent.TaskUpdated {
        val task = generateTask(id = taskId, title = title, status = status)
        return WebSocketEvent.TaskUpdated(taskId, task)
    }

    /**
     * Generates a mock WebSocket task creation event.
     */
    fun generateTaskCreationEvent(
        taskId: String = UUID.randomUUID().toString(),
        title: String = "New Task"
    ): WebSocketEvent.TaskCreated {
        val task = generateTask(id = taskId, title = title)
        return WebSocketEvent.TaskCreated(task)
    }

    /**
     * Generates a mock WebSocket task deletion event.
     */
    fun generateTaskDeletionEvent(
        taskId: String = UUID.randomUUID().toString()
    ): WebSocketEvent.TaskDeleted {
        return WebSocketEvent.TaskDeleted(taskId)
    }

    /**
     * Generates a sequence of mixed WebSocket events.
     */
    fun generateMixedEventSequence(count: Int = 5): List<WebSocketEvent> {
        return (1..count).map { index ->
            when (index % 3) {
                0 -> generateTaskUpdateEvent(taskId = "task-$index")
                1 -> generateTaskCreationEvent(taskId = "task-$index")
                else -> generateTaskDeletionEvent(taskId = "task-$index")
            }
        }
    }

    /**
     * Generates a sequence of rapid updates for stress testing.
     */
    fun generateRapidUpdateSequence(count: Int = 100): List<WebSocketEvent> {
        return (1..count).map { index ->
            generateTaskUpdateEvent(taskId = "task-$index", title = "Task $index")
        }
    }

    // ============ Mock Timer State Management ============

    /**
     * Simulates timer state transitions.
     */
    class MockTimerState {
        private var isActive = false
        private var startTime: Long = 0
        private var duration: Long = 0

        fun startTimer(durationMs: Long) {
            isActive = true
            startTime = System.currentTimeMillis()
            duration = durationMs
        }

        fun stopTimer() {
            isActive = false
        }

        fun isTimerActive(): Boolean = isActive

        fun getElapsedTime(): Long {
            return if (isActive) System.currentTimeMillis() - startTime else 0
        }

        fun getRemainingTime(): Long {
            val elapsed = getElapsedTime()
            return maxOf(0, duration - elapsed)
        }

        fun isTimerComplete(): Boolean {
            return isActive && getElapsedTime() >= duration
        }
    }

    // ============ Mock Connectivity State Management ============

    /**
     * Simulates connectivity state transitions.
     */
    class MockConnectivityState {
        private var isOnline = true
        private var lastStateChange: Long = System.currentTimeMillis()

        fun goOnline() {
            isOnline = true
            lastStateChange = System.currentTimeMillis()
        }

        fun goOffline() {
            isOnline = false
            lastStateChange = System.currentTimeMillis()
        }

        fun isConnected(): Boolean = isOnline

        fun getTimeSinceStateChange(): Long {
            return System.currentTimeMillis() - lastStateChange
        }

        fun simulateNetworkFluctuation(offlineDurationMs: Long) {
            goOffline()
            Thread.sleep(offlineDurationMs)
            goOnline()
        }
    }

    // ============ Assertion Helpers ============

    /**
     * Verifies that an update was applied successfully.
     */
    fun assertUpdateApplied(result: WebSocketEventResult) {
        require(result.success) { "Update should have been applied successfully" }
    }

    /**
     * Verifies that an update was queued (not applied immediately).
     */
    fun assertUpdateQueued(result: WebSocketEventResult) {
        require(!result.success) { "Update should have been queued" }
    }

    /**
     * Verifies that a conflict was resolved.
     */
    fun assertConflictResolved(result: WebSocketEventResult) {
        require(result.conflictResolved) { "Conflict should have been resolved" }
    }

    /**
     * Verifies that a task has the expected status.
     */
    fun assertTaskStatus(task: Task, expectedStatus: TaskStatus) {
        require(task.status == expectedStatus) {
            "Task status should be $expectedStatus, but was ${task.status}"
        }
    }

    /**
     * Verifies that a task is marked as synced.
     */
    fun assertTaskSynced(task: Task) {
        require(task.syncStatus == SyncStatus.SYNCED) {
            "Task should be marked as SYNCED, but was ${task.syncStatus}"
        }
    }

    /**
     * Verifies that a task is marked as pending sync.
     */
    fun assertTaskPendingSync(task: Task) {
        require(task.syncStatus == SyncStatus.PENDING) {
            "Task should be marked as PENDING, but was ${task.syncStatus}"
        }
    }

    /**
     * Verifies that a task is not deleted.
     */
    fun assertTaskNotDeleted(task: Task) {
        require(!task.isDeleted) { "Task should not be deleted" }
    }

    /**
     * Verifies that a task is deleted.
     */
    fun assertTaskDeleted(task: Task) {
        require(task.isDeleted) { "Task should be deleted" }
    }

    /**
     * Verifies that two tasks have the same content (ignoring timestamps).
     */
    fun assertTasksEqual(task1: Task, task2: Task) {
        require(task1.id == task2.id) { "Task IDs should match" }
        require(task1.title == task2.title) { "Task titles should match" }
        require(task1.status == task2.status) { "Task statuses should match" }
        require(task1.todoGroup == task2.todoGroup) { "Task groups should match" }
    }

    /**
     * Verifies that a task was updated (has newer timestamp).
     */
    fun assertTaskUpdated(oldTask: Task, newTask: Task) {
        require(newTask.updatedAt.isAfter(oldTask.updatedAt)) {
            "New task should have newer timestamp"
        }
    }

    /**
     * Verifies that updates were applied in order.
     */
    fun assertUpdatesInOrder(tasks: List<Task>) {
        for (i in 1 until tasks.size) {
            require(tasks[i].updatedAt.isAfter(tasks[i - 1].updatedAt)) {
                "Updates should be in chronological order"
            }
        }
    }

    // ============ Test Data Generators ============

    /**
     * Generates a test task with customizable properties.
     */
    fun generateTask(
        id: String = UUID.randomUUID().toString(),
        householdId: String = "household-123",
        assignedUserId: String = "user-456",
        title: String = "Test Task",
        description: String = "Test Description",
        todoGroup: String = "Morning",
        estimatedDurationMinutes: Int = 30,
        status: TaskStatus = TaskStatus.INCOMPLETE,
        syncStatus: SyncStatus = SyncStatus.PENDING,
        isDeleted: Boolean = false,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = description,
            todoGroup = todoGroup,
            estimatedDurationMinutes = estimatedDurationMinutes,
            actualDurationMinutes = null,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            completedAt = null,
            syncStatus = syncStatus,
            isDeleted = isDeleted
        )
    }

    /**
     * Generates a batch of test tasks.
     */
    fun generateTaskBatch(count: Int = 5): List<Task> {
        return (1..count).map { index ->
            generateTask(
                id = "task-$index",
                title = "Task $index",
                updatedAt = Instant.now().plusSeconds(index.toLong())
            )
        }
    }

    /**
     * Generates tasks with different statuses.
     */
    fun generateTasksWithVariousStatuses(): List<Task> {
        return listOf(
            generateTask(id = "task-1", status = TaskStatus.INCOMPLETE),
            generateTask(id = "task-2", status = TaskStatus.IN_PROGRESS),
            generateTask(id = "task-3", status = TaskStatus.COMPLETED)
        )
    }

    /**
     * Generates tasks with different sync statuses.
     */
    fun generateTasksWithVariousSyncStatuses(): List<Task> {
        return listOf(
            generateTask(id = "task-1", syncStatus = SyncStatus.PENDING),
            generateTask(id = "task-2", syncStatus = SyncStatus.SYNCED),
            generateTask(id = "task-3", syncStatus = SyncStatus.CONFLICT)
        )
    }

    /**
     * Generates conflicting task versions.
     */
    fun generateConflictingTaskVersions(taskId: String = "task-1"): Pair<Task, Task> {
        val now = Instant.now()
        val localTask = generateTask(
            id = taskId,
            title = "Local Title",
            updatedAt = now
        )
        val remoteTask = generateTask(
            id = taskId,
            title = "Remote Title",
            updatedAt = now.plusSeconds(10)
        )
        return Pair(localTask, remoteTask)
    }

    /**
     * Generates a sequence of task updates simulating a workflow.
     */
    fun generateTaskWorkflow(taskId: String = "task-1"): List<Task> {
        val now = Instant.now()
        return listOf(
            generateTask(id = taskId, status = TaskStatus.INCOMPLETE, updatedAt = now),
            generateTask(id = taskId, status = TaskStatus.IN_PROGRESS, updatedAt = now.plusSeconds(5)),
            generateTask(id = taskId, status = TaskStatus.COMPLETED, updatedAt = now.plusSeconds(10))
        )
    }
}
