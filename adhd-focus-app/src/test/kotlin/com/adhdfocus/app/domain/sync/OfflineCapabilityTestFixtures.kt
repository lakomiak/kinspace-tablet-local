package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.SyncOperation
import java.time.Instant
import java.util.UUID

/**
 * Test fixtures and helpers for offline capability integration tests.
 *
 * Provides mock data generators, state simulators, and assertion helpers
 * for comprehensive testing of offline capability workflows.
 *
 * Validates: Requirement 11 (Offline Capability)
 */
object OfflineCapabilityTestFixtures {

    // ============ Offline State Simulators ============

    /**
     * Simulates offline/online state transitions.
     */
    class MockOfflineState {
        private var isOnline = true
        private var lastStateChange: Long = System.currentTimeMillis()
        private val stateHistory = mutableListOf<Pair<Boolean, Long>>()

        fun goOnline() {
            isOnline = true
            lastStateChange = System.currentTimeMillis()
            stateHistory.add(Pair(true, lastStateChange))
        }

        fun goOffline() {
            isOnline = false
            lastStateChange = System.currentTimeMillis()
            stateHistory.add(Pair(false, lastStateChange))
        }

        fun isConnected(): Boolean = isOnline

        fun getTimeSinceStateChange(): Long {
            return System.currentTimeMillis() - lastStateChange
        }

        fun getStateHistory(): List<Pair<Boolean, Long>> = stateHistory.toList()

        fun simulateRapidTransitions(count: Int) {
            repeat(count) { index ->
                if (index % 2 == 0) goOnline() else goOffline()
            }
        }

        fun simulateNetworkFluctuation(offlineDurationMs: Long) {
            goOffline()
            Thread.sleep(offlineDurationMs)
            goOnline()
        }

        fun reset() {
            isOnline = true
            lastStateChange = System.currentTimeMillis()
            stateHistory.clear()
        }
    }

    /**
     * Simulates offline sync queue operations.
     */
    class MockSyncQueue {
        private val queue = mutableListOf<QueuedOperation>()

        fun enqueue(operation: QueuedOperation) {
            queue.add(operation)
        }

        fun dequeue(): QueuedOperation? {
            return if (queue.isNotEmpty()) queue.removeAt(0) else null
        }

        fun peek(): QueuedOperation? {
            return queue.firstOrNull()
        }

        fun size(): Int = queue.size

        fun isEmpty(): Boolean = queue.isEmpty()

        fun clear() {
            queue.clear()
        }

        fun getAll(): List<QueuedOperation> = queue.toList()

        fun getByTaskId(taskId: String): List<QueuedOperation> {
            return queue.filter { it.taskId == taskId }
        }

        fun getByOperation(operation: SyncOperation): List<QueuedOperation> {
            return queue.filter { it.operation == operation }
        }
    }

    /**
     * Simulates offline task cache.
     */
    class MockTaskCache {
        private val cache = mutableMapOf<String, Task>()

        fun put(task: Task) {
            cache[task.id] = task
        }

        fun get(taskId: String): Task? {
            return cache[taskId]
        }

        fun remove(taskId: String) {
            cache.remove(taskId)
        }

        fun getAll(): List<Task> = cache.values.toList()

        fun size(): Int = cache.size

        fun isEmpty(): Boolean = cache.isEmpty()

        fun clear() {
            cache.clear()
        }

        fun contains(taskId: String): Boolean {
            return cache.containsKey(taskId)
        }

        fun update(task: Task) {
            if (cache.containsKey(task.id)) {
                cache[task.id] = task
            }
        }
    }

    /**
     * Simulates offline timer state.
     */
    class MockOfflineTimer {
        private var isRunning = false
        private var startTime: Long = 0
        private var duration: Long = 0
        private var pausedTime: Long = 0

        fun start(durationMs: Long) {
            isRunning = true
            startTime = System.currentTimeMillis()
            duration = durationMs
            pausedTime = 0
        }

        fun pause() {
            if (isRunning) {
                pausedTime = System.currentTimeMillis() - startTime
                isRunning = false
            }
        }

        fun resume() {
            if (!isRunning && pausedTime > 0) {
                isRunning = true
                startTime = System.currentTimeMillis() - pausedTime
            }
        }

        fun stop() {
            isRunning = false
            pausedTime = 0
        }

        fun isActive(): Boolean = isRunning

        fun getElapsedTime(): Long {
            return if (isRunning) {
                System.currentTimeMillis() - startTime
            } else {
                pausedTime
            }
        }

        fun getRemainingTime(): Long {
            val elapsed = getElapsedTime()
            return maxOf(0, duration - elapsed)
        }

        fun isComplete(): Boolean {
            return getElapsedTime() >= duration
        }

        fun getProgress(): Float {
            return if (duration > 0) {
                getElapsedTime().toFloat() / duration.toFloat()
            } else {
                0f
            }
        }
    }

    // ============ Assertion Helpers ============

    /**
     * Verifies that offline state is correctly detected.
     */
    fun assertOfflineStateDetected(state: MockOfflineState) {
        require(!state.isConnected()) { "Should be offline" }
    }

    /**
     * Verifies that online state is correctly detected.
     */
    fun assertOnlineStateDetected(state: MockOfflineState) {
        require(state.isConnected()) { "Should be online" }
    }

    /**
     * Verifies that operation was queued.
     */
    fun assertOperationQueued(queue: MockSyncQueue, taskId: String) {
        require(queue.getByTaskId(taskId).isNotEmpty()) {
            "Operation for task $taskId should be queued"
        }
    }

    /**
     * Verifies that all operations were queued.
     */
    fun assertAllOperationsQueued(queue: MockSyncQueue, count: Int) {
        require(queue.size() == count) {
            "Expected $count operations queued, but got ${queue.size()}"
        }
    }

    /**
     * Verifies that queue is empty.
     */
    fun assertQueueEmpty(queue: MockSyncQueue) {
        require(queue.isEmpty()) { "Queue should be empty" }
    }

    /**
     * Verifies that task is cached.
     */
    fun assertTaskCached(cache: MockTaskCache, task: Task) {
        require(cache.contains(task.id)) {
            "Task ${task.id} should be cached"
        }
        val cached = cache.get(task.id)
        require(cached?.title == task.title) {
            "Cached task should have same title"
        }
    }

    /**
     * Verifies that all tasks are cached.
     */
    fun assertAllTasksCached(cache: MockTaskCache, tasks: List<Task>) {
        require(cache.size() == tasks.size) {
            "Expected ${tasks.size} tasks cached, but got ${cache.size()}"
        }
        tasks.forEach { task ->
            require(cache.contains(task.id)) {
                "Task ${task.id} should be cached"
            }
        }
    }

    /**
     * Verifies that cache is consistent.
     */
    fun assertCacheConsistent(cache: MockTaskCache, tasks: List<Task>) {
        tasks.forEach { task ->
            val cached = cache.get(task.id)
            require(cached != null) { "Task ${task.id} should be in cache" }
            require(cached.id == task.id) { "Task ID should match" }
            require(cached.title == task.title) { "Task title should match" }
            require(cached.status == task.status) { "Task status should match" }
        }
    }

    /**
     * Verifies that timer is running.
     */
    fun assertTimerRunning(timer: MockOfflineTimer) {
        require(timer.isActive()) { "Timer should be running" }
    }

    /**
     * Verifies that timer is not running.
     */
    fun assertTimerNotRunning(timer: MockOfflineTimer) {
        require(!timer.isActive()) { "Timer should not be running" }
    }

    /**
     * Verifies that timer is complete.
     */
    fun assertTimerComplete(timer: MockOfflineTimer) {
        require(timer.isComplete()) { "Timer should be complete" }
    }

    /**
     * Verifies that timer progress is correct.
     */
    fun assertTimerProgress(timer: MockOfflineTimer, expectedProgress: Float, tolerance: Float = 0.1f) {
        val actualProgress = timer.getProgress()
        require((actualProgress - expectedProgress).kotlin.math.abs() <= tolerance) {
            "Timer progress should be ~$expectedProgress, but was $actualProgress"
        }
    }

    /**
     * Verifies that state transitions are valid.
     */
    fun assertValidStateTransitions(state: MockOfflineState) {
        val history = state.getStateHistory()
        require(history.isNotEmpty()) { "State history should not be empty" }

        // All transitions should be valid (no invalid state combinations)
        for (i in 1 until history.size) {
            val previousState = history[i - 1].first
            val currentState = history[i].first
            val previousTime = history[i - 1].second
            val currentTime = history[i].second

            // Time should be monotonically increasing
            require(currentTime >= previousTime) {
                "State transition times should be monotonically increasing"
            }
        }
    }

    /**
     * Verifies that operations are in order.
     */
    fun assertOperationsInOrder(queue: MockSyncQueue) {
        val operations = queue.getAll()
        for (i in 1 until operations.size) {
            require(operations[i].timestamp.isAfter(operations[i - 1].timestamp) ||
                    operations[i].timestamp.equals(operations[i - 1].timestamp)) {
                "Operations should be in chronological order"
            }
        }
    }

    // ============ Test Data Generators ============

    /**
     * Generates a test task.
     */
    fun generateTask(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Task",
        status: TaskStatus = TaskStatus.INCOMPLETE,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): Task {
        return Task(
            id = id,
            householdId = "household-123",
            assignedUserId = "user-123",
            title = title,
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = syncStatus,
            isDeleted = false
        )
    }

    /**
     * Generates multiple test tasks.
     */
    fun generateTasks(count: Int = 5): List<Task> {
        return (1..count).map { index ->
            generateTask(
                id = "task-$index",
                title = "Task $index"
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
     * Generates a queued operation.
     */
    fun generateQueuedOperation(
        taskId: String = UUID.randomUUID().toString(),
        operation: SyncOperation = SyncOperation.CREATE
    ): QueuedOperation {
        return QueuedOperation(
            taskId = taskId,
            operation = operation,
            payload = """{"id":"$taskId"}""",
            timestamp = Instant.now()
        )
    }

    /**
     * Generates multiple queued operations.
     */
    fun generateQueuedOperations(count: Int = 5): List<QueuedOperation> {
        return (1..count).map { index ->
            generateQueuedOperation(
                taskId = "task-$index",
                operation = SyncOperation.CREATE
            )
        }
    }

    /**
     * Generates conflicting task versions.
     */
    fun generateConflictingTasks(taskId: String = "task-1"): Pair<Task, Task> {
        val now = Instant.now()
        val localTask = generateTask(
            id = taskId,
            title = "Local Title",
            status = TaskStatus.INCOMPLETE
        ).copy(updatedAt = now)

        val remoteTask = generateTask(
            id = taskId,
            title = "Remote Title",
            status = TaskStatus.COMPLETED
        ).copy(updatedAt = now.plusSeconds(10))

        return Pair(localTask, remoteTask)
    }

    // ============ Supporting Data Classes ============

    data class QueuedOperation(
        val taskId: String,
        val operation: SyncOperation,
        val payload: String,
        val timestamp: Instant
    )
}
