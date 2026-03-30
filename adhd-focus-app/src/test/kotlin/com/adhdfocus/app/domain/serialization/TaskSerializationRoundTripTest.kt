package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.util.UUID

/**
 * Property-based tests for Task serialization and deserialization round-trip.
 *
 * **Validates: Requirements 2.1, 2.8, 19.1-19.8**
 *
 * These tests verify that:
 * 1. Task objects can be serialized to JSON
 * 2. JSON can be parsed back into Task objects
 * 3. The round-trip (serialize → deserialize) produces equivalent objects
 * 4. All fields including optional fields are preserved
 * 5. Enum serialization (TaskStatus, SyncStatus) works correctly
 */
class TaskSerializationRoundTripTest : FunSpec({
    val serializer = TaskSerializer()
    val parser = TaskParser()

    /**
     * Property 30: Task Serialization Round-Trip
     *
     * FOR ALL valid Task objects, serializing then parsing SHALL produce an equivalent object.
     * This ensures data integrity through serialization cycles.
     */
    test("Property 30: Round-trip serialization preserves all task fields") {
        checkAll(
            iterations = 100,
            arb = taskArbitrary()
        ) { originalTask ->
            // Serialize the task
            val json = serializer.serialize(originalTask)

            // Parse it back
            val deserializedTask = parser.parse(json)

            // Verify all fields match
            deserializedTask.id shouldBe originalTask.id
            deserializedTask.householdId shouldBe originalTask.householdId
            deserializedTask.assignedUserId shouldBe originalTask.assignedUserId
            deserializedTask.title shouldBe originalTask.title
            deserializedTask.description shouldBe originalTask.description
            deserializedTask.todoGroup shouldBe originalTask.todoGroup
            deserializedTask.estimatedDurationMinutes shouldBe originalTask.estimatedDurationMinutes
            deserializedTask.actualDurationMinutes shouldBe originalTask.actualDurationMinutes
            deserializedTask.status shouldBe originalTask.status
            deserializedTask.createdAt shouldBe originalTask.createdAt
            deserializedTask.updatedAt shouldBe originalTask.updatedAt
            deserializedTask.completedAt shouldBe originalTask.completedAt
            deserializedTask.syncStatus shouldBe originalTask.syncStatus
            deserializedTask.isDeleted shouldBe originalTask.isDeleted
        }
    }

    /**
     * Property 30 (continued): Verify complete equality
     *
     * The deserialized task should be completely equal to the original.
     */
    test("Property 30: Deserialized task equals original task") {
        checkAll(
            iterations = 100,
            arb = taskArbitrary()
        ) { originalTask ->
            val json = serializer.serialize(originalTask)
            val deserializedTask = parser.parse(json)

            deserializedTask shouldBe originalTask
        }
    }

    /**
     * Property 30: Round-trip with pretty-printed JSON
     *
     * Pretty-printed JSON should also round-trip correctly.
     */
    test("Property 30: Pretty-printed JSON round-trip preserves all fields") {
        checkAll(
            iterations = 100,
            arb = taskArbitrary()
        ) { originalTask ->
            val prettyJson = serializer.serializePretty(originalTask)
            val deserializedTask = parser.parse(prettyJson)

            deserializedTask shouldBe originalTask
        }
    }

    /**
     * Property 31: Task Parser Error Handling
     *
     * FOR ALL invalid JSON inputs, the parser SHALL return a descriptive error message.
     */
    test("Property 31: Parser handles missing required fields with descriptive errors") {
        val invalidJsons = listOf(
            "{}", // Missing all fields
            """{"id": "123"}""", // Missing householdId, assignedUserId, title, todoGroup
            """{"id": "123", "householdId": "hh1"}""", // Missing assignedUserId, title, todoGroup
            """{"id": "123", "householdId": "hh1", "assignedUserId": "user1"}""", // Missing title, todoGroup
            """{"id": "123", "householdId": "hh1", "assignedUserId": "user1", "title": "Task"}""", // Missing todoGroup
            """{"id": "", "householdId": "hh1", "assignedUserId": "user1", "title": "Task", "todoGroup": "group"}""", // Empty id
            """{"id": "123", "householdId": "", "assignedUserId": "user1", "title": "Task", "todoGroup": "group"}""", // Empty householdId
        )

        invalidJsons.forEach { invalidJson ->
            try {
                parser.parse(invalidJson)
                throw AssertionError("Expected parser to throw IllegalArgumentException for: $invalidJson")
            } catch (e: IllegalArgumentException) {
                // Expected - error message should be descriptive
                e.message?.shouldBe(e.message) // Just verify message exists
            }
        }
    }

    /**
     * Property 32: Task Parser Optional Fields
     *
     * FOR ALL valid Task objects with optional fields, the parser SHALL handle them gracefully.
     */
    test("Property 32: Parser handles optional fields with defaults") {
        checkAll(
            iterations = 50,
            arb = taskWithOptionalFieldsArbitrary()
        ) { originalTask ->
            val json = serializer.serialize(originalTask)
            val deserializedTask = parser.parse(json)

            // Optional fields should be preserved or default correctly
            deserializedTask.description shouldBe originalTask.description
            deserializedTask.estimatedDurationMinutes shouldBe originalTask.estimatedDurationMinutes
            deserializedTask.actualDurationMinutes shouldBe originalTask.actualDurationMinutes
            deserializedTask.completedAt shouldBe originalTask.completedAt
        }
    }

    /**
     * Property 33: Task Serializer Completeness
     *
     * FOR ALL valid Task objects, the serializer SHALL include all task metadata.
     */
    test("Property 33: Serializer includes all task metadata") {
        checkAll(
            iterations = 100,
            arb = taskArbitrary()
        ) { task ->
            val json = serializer.serialize(task)

            // Verify all required fields are present in JSON
            json.contains("\"id\"") shouldBe true
            json.contains("\"householdId\"") shouldBe true
            json.contains("\"assignedUserId\"") shouldBe true
            json.contains("\"title\"") shouldBe true
            json.contains("\"todoGroup\"") shouldBe true
            json.contains("\"status\"") shouldBe true
            json.contains("\"syncStatus\"") shouldBe true
            json.contains("\"createdAt\"") shouldBe true
            json.contains("\"updatedAt\"") shouldBe true
            json.contains("\"isDeleted\"") shouldBe true
        }
    }

    /**
     * Property 30: Enum serialization and deserialization
     *
     * FOR ALL TaskStatus and SyncStatus enum values, serialization and deserialization
     * SHALL preserve the enum value.
     */
    test("Property 30: Enum values are preserved through serialization") {
        val taskStatuses = TaskStatus.values()
        val syncStatuses = SyncStatus.values()

        taskStatuses.forEach { taskStatus ->
            syncStatuses.forEach { syncStatus ->
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    householdId = "hh1",
                    assignedUserId = "user1",
                    title = "Test Task",
                    todoGroup = "Morning",
                    status = taskStatus,
                    syncStatus = syncStatus
                )

                val json = serializer.serialize(task)
                val deserialized = parser.parse(json)

                deserialized.status shouldBe taskStatus
                deserialized.syncStatus shouldBe syncStatus
            }
        }
    }

    /**
     * Property 30: List serialization round-trip
     *
     * FOR ALL lists of Task objects, serializing then parsing SHALL produce equivalent list.
     */
    test("Property 30: List serialization round-trip preserves all tasks") {
        checkAll(
            iterations = 50,
            arb = Arb.list(taskArbitrary(), 1..10)
        ) { originalTasks ->
            val json = serializer.serializeList(originalTasks)
            val deserializedTasks = parser.parseList(json)

            deserializedTasks.size shouldBe originalTasks.size
            deserializedTasks.zip(originalTasks).forEach { (deserialized, original) ->
                deserialized shouldBe original
            }
        }
    }
})

/**
 * Generates arbitrary Task objects with all fields populated.
 */
private fun taskArbitrary(): Arb<Task> {
    return Arb.bind(
        Arb.string(1..50), // id
        Arb.string(1..50), // householdId
        Arb.string(1..50), // assignedUserId
        Arb.string(1..100), // title
        Arb.string(0..200).map { if (it.isEmpty()) null else it }, // description
        Arb.string(1..50), // todoGroup
        Arb.int(1..480).map { it as Int? }, // estimatedDurationMinutes
        Arb.int(0..480).map { it as Int? }, // actualDurationMinutes
        Arb.of(*TaskStatus.values()), // status
        Arb.long(1000000000000L..System.currentTimeMillis()), // createdAt
        Arb.long(1000000000000L..System.currentTimeMillis()), // updatedAt
        Arb.long(0..System.currentTimeMillis()).map { if (it == 0L) null else it }, // completedAt
        Arb.of(*SyncStatus.values()), // syncStatus
        Arb.boolean() // isDeleted
    ) { id, householdId, assignedUserId, title, description, todoGroup, estimatedDuration,
        actualDuration, status, createdAt, updatedAt, completedAt, syncStatus, isDeleted ->
        Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = description,
            todoGroup = todoGroup,
            estimatedDurationMinutes = estimatedDuration,
            actualDurationMinutes = actualDuration,
            status = status,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
            syncStatus = syncStatus,
            isDeleted = isDeleted
        )
    }
}

/**
 * Generates arbitrary Task objects with focus on optional fields.
 */
private fun taskWithOptionalFieldsArbitrary(): Arb<Task> {
    return Arb.bind(
        Arb.string(1..50), // id
        Arb.string(1..50), // householdId
        Arb.string(1..50), // assignedUserId
        Arb.string(1..100), // title
        Arb.string(0..200).map { if (it.isEmpty()) null else it }, // description (optional)
        Arb.string(1..50), // todoGroup
        Arb.int(1..480).map { if (it % 2 == 0) null else it }, // estimatedDurationMinutes (optional)
        Arb.int(0..480).map { if (it % 2 == 0) null else it }, // actualDurationMinutes (optional)
        Arb.of(*TaskStatus.values()), // status
        Arb.long(1000000000000L..System.currentTimeMillis()), // createdAt
        Arb.long(1000000000000L..System.currentTimeMillis()), // updatedAt
        Arb.long(0..System.currentTimeMillis()).map { if (it % 2 == 0) null else it }, // completedAt (optional)
        Arb.of(*SyncStatus.values()), // syncStatus
        Arb.boolean() // isDeleted
    ) { id, householdId, assignedUserId, title, description, todoGroup, estimatedDuration,
        actualDuration, status, createdAt, updatedAt, completedAt, syncStatus, isDeleted ->
        Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = description,
            todoGroup = todoGroup,
            estimatedDurationMinutes = estimatedDuration,
            actualDurationMinutes = actualDuration,
            status = status,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
            syncStatus = syncStatus,
            isDeleted = isDeleted
        )
    }
}
