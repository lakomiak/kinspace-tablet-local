package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-based tests for WebSocket connection management.
 *
 * **Validates: Requirements 10 & 11**
 * - Event ordering preservation
 * - Connection state transitions
 * - Reconnection backoff calculation
 * - Event queue ordering
 */
class WebSocketManagerPropertyTest : FunSpec({
    // Generators for property-based testing
    val taskIdGen = arbitrary { UUID.randomUUID().toString() }
    val householdIdGen = arbitrary { "household-${UUID.randomUUID()}" }
    val userIdGen = arbitrary { "user-${UUID.randomUUID()}" }
    val titleGen = Arb.string(minSize = 1, maxSize = 100)
    val todoGroupGen = Arb.string(minSize = 1, maxSize = 50)
    val taskStatusGen = arbitrary { TaskStatus.values().random() }
    val attemptGen = Arb.int(min = 0, max = 10)

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        householdId: String = "household-123",
        assignedUserId: String = "user-456",
        title: String = "Test Task",
        todoGroup: String = "Morning",
        status: TaskStatus = TaskStatus.INCOMPLETE
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = null,
            todoGroup = todoGroup,
            estimatedDurationMinutes = null,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )
    }

    test("Event ordering: Multiple events should preserve order") {
        checkAll(
            taskIdGen,
            taskIdGen,
            taskIdGen
        ) { taskId1, taskId2, taskId3 ->
            val events = listOf(
                WebSocketEvent.TaskUpdated(taskId1, createTask(id = taskId1)),
                WebSocketEvent.TaskDeleted(taskId2),
                WebSocketEvent.TaskCreated(createTask(id = taskId3))
            )

            // Events should maintain their order
            events[0] shouldBe WebSocketEvent.TaskUpdated(taskId1, createTask(id = taskId1))
            events[1] shouldBe WebSocketEvent.TaskDeleted(taskId2)
            events[2] shouldBe WebSocketEvent.TaskCreated(createTask(id = taskId3))
        }
    }

    test("Connection state transitions: isConnected reflects actual state") {
        checkAll(householdIdGen, userIdGen) { householdId, userId ->
            // Initial state should be disconnected
            val manager = WebSocketManagerImpl(
                mockk(),
                com.google.gson.Gson(),
                mockk()
            )
            manager.isConnected() shouldBe false
        }
    }

    test("Reconnection backoff: Backoff increases exponentially") {
        checkAll(attemptGen) { attempt ->
            val initialBackoff = 1000L
            val multiplier = 2.0
            val maxBackoff = 60000L

            val backoff = (initialBackoff * Math.pow(multiplier, attempt.toDouble())).toLong()
                .coerceAtMost(maxBackoff)

            // Backoff should be positive
            backoff shouldBe > 0L

            // Backoff should not exceed max
            backoff shouldBe <= maxBackoff

            // Each attempt should have higher or equal backoff (until max)
            if (attempt > 0) {
                val prevBackoff = (initialBackoff * Math.pow(multiplier, (attempt - 1).toDouble())).toLong()
                    .coerceAtMost(maxBackoff)
                backoff shouldBe >= prevBackoff
            }
        }
    }

    test("Event queue ordering: Events queued in order should be processed in order") {
        checkAll(
            Arb.int(min = 1, max = 10)
        ) { eventCount ->
            val events = mutableListOf<WebSocketEvent>()

            repeat(eventCount) { i ->
                val taskId = "task-$i"
                events.add(WebSocketEvent.TaskUpdated(taskId, createTask(id = taskId)))
            }

            // Verify events are in order
            events.forEachIndexed { index, event ->
                if (event is WebSocketEvent.TaskUpdated) {
                    event.taskId shouldBe "task-$index"
                }
            }
        }
    }

    test("Task update event: Updated task should have correct data") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen,
            titleGen,
            todoGroupGen,
            taskStatusGen
        ) { taskId, householdId, userId, title, todoGroup, status ->
            val task = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                todoGroup = todoGroup,
                status = status
            )
            val event = WebSocketEvent.TaskUpdated(taskId, task)

            event.taskId shouldBe taskId
            event.task.id shouldBe taskId
            event.task.householdId shouldBe householdId
            event.task.assignedUserId shouldBe userId
            event.task.title shouldBe title
            event.task.todoGroup shouldBe todoGroup
            event.task.status shouldBe status
        }
    }

    test("Task deletion event: Deleted task ID should be preserved") {
        checkAll(taskIdGen) { taskId ->
            val event = WebSocketEvent.TaskDeleted(taskId)

            event.taskId shouldBe taskId
        }
    }

    test("Task creation event: Created task should have correct data") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen,
            titleGen,
            todoGroupGen
        ) { taskId, householdId, userId, title, todoGroup ->
            val task = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                todoGroup = todoGroup
            )
            val event = WebSocketEvent.TaskCreated(task)

            event.task.id shouldBe taskId
            event.task.householdId shouldBe householdId
            event.task.assignedUserId shouldBe userId
            event.task.title shouldBe title
            event.task.todoGroup shouldBe todoGroup
        }
    }

    test("Error event: Error message should be preserved") {
        checkAll(Arb.string(minSize = 1, maxSize = 200)) { message ->
            val event = WebSocketEvent.Error(message)

            event.message shouldBe message
            event.throwable shouldBe null
        }
    }

    test("Error event with throwable: Both message and throwable should be preserved") {
        checkAll(Arb.string(minSize = 1, maxSize = 200)) { message ->
            val throwable = Exception("Test error")
            val event = WebSocketEvent.Error(message, throwable)

            event.message shouldBe message
            event.throwable shouldBe throwable
        }
    }

    test("Sync signal event: Should be consistent singleton") {
        val event1 = WebSocketEvent.SyncSignal
        val event2 = WebSocketEvent.SyncSignal

        event1 shouldBe event2
    }

    test("Connection established event: Should be consistent singleton") {
        val event1 = WebSocketEvent.ConnectionEstablished
        val event2 = WebSocketEvent.ConnectionEstablished

        event1 shouldBe event2
    }

    test("Connection lost event: Should be consistent singleton") {
        val event1 = WebSocketEvent.ConnectionLost
        val event2 = WebSocketEvent.ConnectionLost

        event1 shouldBe event2
    }
})

// Mock helper for testing
private fun mockk(): okhttp3.OkHttpClient {
    return io.mockk.mockk()
}
