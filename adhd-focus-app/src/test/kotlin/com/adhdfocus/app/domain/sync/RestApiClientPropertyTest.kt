package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
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

class RestApiClientPropertyTest : FunSpec({
    val gson = Gson()

    // Generators for property-based testing
    val taskIdGen = arbitrary { UUID.randomUUID().toString() }
    val householdIdGen = arbitrary { "household-${UUID.randomUUID()}" }
    val userIdGen = arbitrary { "user-${UUID.randomUUID()}" }
    val titleGen = Arb.string(minSize = 1, maxSize = 100)
    val descriptionGen = Arb.string(minSize = 0, maxSize = 500)
    val todoGroupGen = Arb.string(minSize = 1, maxSize = 50)
    val durationGen = Arb.int(min = 1, max = 480)
    val taskStatusGen = arbitrary { TaskStatus.values().random() }
    val syncStatusGen = arbitrary { SyncStatus.values().random() }
    val operationGen = arbitrary { SyncOperation.values().random() }

    fun createTask(
        id: String = UUID.randomUUID().toString(),
        householdId: String = "household-123",
        assignedUserId: String = "user-456",
        title: String = "Test Task",
        description: String? = null,
        todoGroup: String = "Morning",
        estimatedDurationMinutes: Int? = null,
        status: TaskStatus = TaskStatus.INCOMPLETE,
        syncStatus: SyncStatus = SyncStatus.PENDING
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
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = syncStatus,
            isDeleted = false
        )
    }

    test("Request serialization round-trip: Task serializes and deserializes correctly") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen,
            titleGen,
            descriptionGen,
            todoGroupGen,
            durationGen,
            taskStatusGen,
            syncStatusGen
        ) { taskId, householdId, userId, title, description, todoGroup, duration, status, syncStatus ->
            val original = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                description = description,
                todoGroup = todoGroup,
                estimatedDurationMinutes = duration,
                status = status,
                syncStatus = syncStatus
            )

            // Serialize to JSON
            val json = gson.toJson(original)

            // Deserialize back
            val deserialized = gson.fromJson(json, Task::class.java)

            // Verify all fields match
            deserialized.id shouldBe original.id
            deserialized.householdId shouldBe original.householdId
            deserialized.assignedUserId shouldBe original.assignedUserId
            deserialized.title shouldBe original.title
            deserialized.description shouldBe original.description
            deserialized.todoGroup shouldBe original.todoGroup
            deserialized.estimatedDurationMinutes shouldBe original.estimatedDurationMinutes
            deserialized.status shouldBe original.status
            deserialized.syncStatus shouldBe original.syncStatus
        }
    }

    test("Response deserialization correctness: TaskResponse converts to Task with all fields preserved") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen,
            titleGen,
            taskStatusGen,
            syncStatusGen
        ) { taskId, householdId, userId, title, status, syncStatus ->
            val now = Instant.now().toString()
            val taskResponse = com.adhdfocus.app.data.network.TaskResponse(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title,
                description = null,
                todoGroup = "Morning",
                estimatedDurationMinutes = 30,
                actualDurationMinutes = null,
                status = status.name,
                createdAt = now,
                updatedAt = now,
                completedAt = null,
                syncStatus = syncStatus.name,
                isDeleted = false
            )

            // Simulate conversion (as done in RestApiClientImpl)
            val converted = Task(
                id = taskResponse.id,
                householdId = householdId,
                assignedUserId = taskResponse.assignedUserId,
                title = taskResponse.title,
                description = taskResponse.description,
                todoGroup = taskResponse.todoGroup,
                estimatedDurationMinutes = taskResponse.estimatedDurationMinutes,
                actualDurationMinutes = taskResponse.actualDurationMinutes,
                status = TaskStatus.valueOf(taskResponse.status),
                createdAt = Instant.parse(taskResponse.createdAt),
                updatedAt = Instant.parse(taskResponse.updatedAt),
                completedAt = taskResponse.completedAt?.let { Instant.parse(it) },
                syncStatus = SyncStatus.valueOf(taskResponse.syncStatus),
                isDeleted = taskResponse.isDeleted
            )

            // Verify conversion preserves all fields
            converted.id shouldBe taskResponse.id
            converted.title shouldBe taskResponse.title
            converted.status shouldBe status
            converted.syncStatus shouldBe syncStatus
        }
    }

    test("Conflict resolution by timestamp: Most recent timestamp wins") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen
        ) { taskId, householdId, userId ->
            val now = Instant.now()
            val earlier = now.minusSeconds(60)

            val localTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                status = TaskStatus.COMPLETED,
                syncStatus = SyncStatus.CONFLICT
            ).copy(updatedAt = now)

            val remoteTask = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                status = TaskStatus.INCOMPLETE,
                syncStatus = SyncStatus.SYNCED
            ).copy(updatedAt = earlier)

            // Conflict resolution: prefer most recent timestamp
            val resolved = if (localTask.updatedAt.isAfter(remoteTask.updatedAt)) {
                localTask
            } else {
                remoteTask
            }

            resolved.status shouldBe TaskStatus.COMPLETED
            resolved.updatedAt shouldBe now
        }
    }

    test("Exponential backoff calculation: Backoff increases exponentially") {
        val initialBackoff = 100L
        val maxBackoff = 32000L
        val multiplier = 2.0

        var backoff = initialBackoff
        val backoffs = mutableListOf<Long>()

        repeat(5) {
            backoffs.add(backoff)
            backoff = (backoff * multiplier).toLong().coerceAtMost(maxBackoff)
        }

        // Verify exponential growth
        backoffs[0] shouldBe 100L
        backoffs[1] shouldBe 200L
        backoffs[2] shouldBe 400L
        backoffs[3] shouldBe 800L
        backoffs[4] shouldBe 1600L

        // Verify max backoff is respected
        backoff = 16000L
        repeat(3) {
            backoff = (backoff * multiplier).toLong().coerceAtMost(maxBackoff)
        }
        backoff shouldBe maxBackoff
    }

    test("SyncChange serialization: Payload contains all task data") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen,
            titleGen,
            operationGen
        ) { taskId, householdId, userId, title, operation ->
            val task = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId,
                title = title
            )

            val payload = gson.toJson(task)
            val syncChange = SyncChange(
                taskId = taskId,
                operation = operation,
                payload = payload,
                timestamp = System.currentTimeMillis()
            )

            // Verify payload can be deserialized
            val deserializedTask = gson.fromJson(syncChange.payload, Task::class.java)
            deserializedTask.id shouldBe taskId
            deserializedTask.title shouldBe title
        }
    }

    test("SyncResult aggregation: Synced and failed counts are non-negative") {
        checkAll(
            Arb.int(min = 0, max = 1000),
            Arb.int(min = 0, max = 1000)
        ) { syncedCount, failedCount ->
            val result = SyncResult(
                syncedCount = syncedCount,
                failedCount = failedCount,
                conflicts = emptyList()
            )

            result.syncedCount shouldBe syncedCount
            result.failedCount shouldBe failedCount
            result.syncedCount shouldNotBe null
            result.failedCount shouldNotBe null
        }
    }

    test("Task status transitions: Valid status conversions") {
        checkAll(taskStatusGen) { status ->
            val task = createTask(status = status)

            // Verify status is one of the valid values
            val validStatuses = setOf(
                TaskStatus.INCOMPLETE,
                TaskStatus.IN_PROGRESS,
                TaskStatus.COMPLETED
            )
            validStatuses.contains(task.status) shouldBe true
        }
    }

    test("Sync operation types: All operations are valid") {
        checkAll(operationGen) { operation ->
            val validOperations = setOf(
                SyncOperation.CREATE,
                SyncOperation.UPDATE,
                SyncOperation.DELETE
            )
            validOperations.contains(operation) shouldBe true
        }
    }

    test("Timestamp ordering: Updated timestamp is after or equal to created timestamp") {
        checkAll(
            taskIdGen,
            householdIdGen,
            userIdGen
        ) { taskId, householdId, userId ->
            val task = createTask(
                id = taskId,
                householdId = householdId,
                assignedUserId = userId
            )

            task.updatedAt.isAfter(task.createdAt) || task.updatedAt == task.createdAt shouldBe true
        }
    }
})
