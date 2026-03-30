package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.google.gson.Gson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant
import java.util.UUID

class SyncChangeSerializerUnitTest : FunSpec({
    val gson = Gson()
    val serializer = SyncChangeSerializer(gson)

    val householdId = "household-123"
    val userId = "user-456"

    fun createTestTask(
        id: String = UUID.randomUUID().toString(),
        status: TaskStatus = TaskStatus.INCOMPLETE
    ): Task {
        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = userId,
            title = "Test Task",
            description = "Test Description",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )
    }

    test("createSyncChange should create SyncChange with serialized payload") {
        val task = createTestTask()
        val operation = SyncOperation.CREATE

        val syncChange = serializer.createSyncChange(task, operation)

        syncChange.taskId shouldBe task.id
        syncChange.operation shouldBe operation
        syncChange.payload shouldNotBe null
        syncChange.timestamp shouldNotBe null
    }

    test("createSyncChange payload should contain task data") {
        val task = createTestTask()
        val syncChange = serializer.createSyncChange(task, SyncOperation.CREATE)

        syncChange.payload shouldContain task.title
        syncChange.payload shouldContain task.todoGroup
    }

    test("deserializeTask should convert payload back to Task") {
        val originalTask = createTestTask()
        val syncChange = serializer.createSyncChange(originalTask, SyncOperation.UPDATE)

        val deserializedTask = serializer.deserializeTask(syncChange)

        deserializedTask.id shouldBe originalTask.id
        deserializedTask.title shouldBe originalTask.title
        deserializedTask.householdId shouldBe originalTask.householdId
        deserializedTask.assignedUserId shouldBe originalTask.assignedUserId
    }

    test("serializeTask should produce valid JSON") {
        val task = createTestTask()

        val json = serializer.serializeTask(task)

        json shouldContain "\"id\""
        json shouldContain "\"title\""
        json shouldContain "\"householdId\""
    }

    test("deserializeTaskFromJson should parse JSON string") {
        val originalTask = createTestTask()
        val json = serializer.serializeTask(originalTask)

        val deserializedTask = serializer.deserializeTaskFromJson(json)

        deserializedTask.id shouldBe originalTask.id
        deserializedTask.title shouldBe originalTask.title
        deserializedTask.status shouldBe originalTask.status
    }

    test("deserializeTaskFromJson should throw on invalid JSON") {
        try {
            serializer.deserializeTaskFromJson("invalid json")
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "Failed to deserialize task from JSON"
        }
    }

    test("deserializeTask should throw on invalid payload") {
        val syncChange = SyncChange(
            taskId = "task-123",
            operation = SyncOperation.CREATE,
            payload = "invalid json",
            timestamp = System.currentTimeMillis()
        )

        try {
            serializer.deserializeTask(syncChange)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldContain "Failed to deserialize task from sync change"
        }
    }

    test("createSyncChange should handle all operation types") {
        val task = createTestTask()

        val createChange = serializer.createSyncChange(task, SyncOperation.CREATE)
        val updateChange = serializer.createSyncChange(task, SyncOperation.UPDATE)
        val deleteChange = serializer.createSyncChange(task, SyncOperation.DELETE)

        createChange.operation shouldBe SyncOperation.CREATE
        updateChange.operation shouldBe SyncOperation.UPDATE
        deleteChange.operation shouldBe SyncOperation.DELETE
    }

    test("createSyncChange should preserve task status") {
        val completedTask = createTestTask(status = TaskStatus.COMPLETED)
        val syncChange = serializer.createSyncChange(completedTask, SyncOperation.UPDATE)

        val deserializedTask = serializer.deserializeTask(syncChange)

        deserializedTask.status shouldBe TaskStatus.COMPLETED
    }

    test("serializeTask and deserializeTask should be idempotent") {
        val originalTask = createTestTask()

        val json1 = serializer.serializeTask(originalTask)
        val task1 = serializer.deserializeTaskFromJson(json1)
        val json2 = serializer.serializeTask(task1)
        val task2 = serializer.deserializeTaskFromJson(json2)

        task1.id shouldBe task2.id
        task1.title shouldBe task2.title
        task1.status shouldBe task2.status
    }

    test("createSyncChange should include current timestamp") {
        val task = createTestTask()
        val beforeTime = System.currentTimeMillis()

        val syncChange = serializer.createSyncChange(task, SyncOperation.CREATE)

        val afterTime = System.currentTimeMillis()
        syncChange.timestamp shouldBe >= beforeTime
        syncChange.timestamp shouldBe <= afterTime
    }

    test("deserializeTask should preserve all task fields") {
        val task = createTestTask(
            id = "task-preserve",
            status = TaskStatus.IN_PROGRESS
        )
        val syncChange = serializer.createSyncChange(task, SyncOperation.UPDATE)

        val deserialized = serializer.deserializeTask(syncChange)

        deserialized.id shouldBe task.id
        deserialized.householdId shouldBe task.householdId
        deserialized.assignedUserId shouldBe task.assignedUserId
        deserialized.title shouldBe task.title
        deserialized.description shouldBe task.description
        deserialized.todoGroup shouldBe task.todoGroup
        deserialized.estimatedDurationMinutes shouldBe task.estimatedDurationMinutes
        deserialized.status shouldBe task.status
        deserialized.syncStatus shouldBe task.syncStatus
        deserialized.isDeleted shouldBe task.isDeleted
    }
})
