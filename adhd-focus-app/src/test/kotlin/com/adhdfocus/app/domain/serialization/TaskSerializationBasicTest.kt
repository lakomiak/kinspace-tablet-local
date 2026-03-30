package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.util.UUID

/**
 * Basic unit tests for Task serialization and deserialization.
 *
 * These tests verify core functionality without property-based testing.
 */
class TaskSerializationBasicTest {
    private val serializer = TaskSerializer()
    private val parser = TaskParser()

    @Test
    fun testSerializeAndParseCompleteTask() {
        val now = Instant.now()
        val task = Task(
            id = "task-123",
            householdId = "hh-1",
            assignedUserId = "user-1",
            title = "Complete Task",
            description = "A complete task with all fields",
            todoGroup = "Morning",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 25,
            status = TaskStatus.COMPLETED,
            createdAt = now,
            updatedAt = now,
            completedAt = now,
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )

        val json = serializer.serialize(task)
        val parsed = parser.parse(json)

        assertEquals(task.id, parsed.id)
        assertEquals(task.householdId, parsed.householdId)
        assertEquals(task.assignedUserId, parsed.assignedUserId)
        assertEquals(task.title, parsed.title)
        assertEquals(task.description, parsed.description)
        assertEquals(task.todoGroup, parsed.todoGroup)
        assertEquals(task.estimatedDurationMinutes, parsed.estimatedDurationMinutes)
        assertEquals(task.actualDurationMinutes, parsed.actualDurationMinutes)
        assertEquals(task.status, parsed.status)
        assertEquals(task.createdAt, parsed.createdAt)
        assertEquals(task.updatedAt, parsed.updatedAt)
        assertEquals(task.completedAt, parsed.completedAt)
        assertEquals(task.syncStatus, parsed.syncStatus)
        assertEquals(task.isDeleted, parsed.isDeleted)
    }

    @Test
    fun testSerializeAndParseTaskWithOptionalFields() {
        val now = Instant.now()
        val task = Task(
            id = "task-456",
            householdId = "hh-2",
            assignedUserId = "user-2",
            title = "Minimal Task",
            description = null,
            todoGroup = "Afternoon",
            estimatedDurationMinutes = null,
            actualDurationMinutes = null,
            status = TaskStatus.INCOMPLETE,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false
        )

        val json = serializer.serialize(task)
        val parsed = parser.parse(json)

        assertEquals(task, parsed)
    }

    @Test
    fun testSerializeAndParseTaskWithInProgressStatus() {
        val now = Instant.now()
        val task = Task(
            id = "task-789",
            householdId = "hh-3",
            assignedUserId = "user-3",
            title = "In Progress Task",
            todoGroup = "Evening",
            status = TaskStatus.IN_PROGRESS,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.CONFLICT
        )

        val json = serializer.serialize(task)
        val parsed = parser.parse(json)

        assertEquals(TaskStatus.IN_PROGRESS, parsed.status)
        assertEquals(SyncStatus.CONFLICT, parsed.syncStatus)
    }

    @Test
    fun testPrettyPrintedJsonRoundTrip() {
        val now = Instant.now()
        val task = Task(
            id = "task-pretty",
            householdId = "hh-4",
            assignedUserId = "user-4",
            title = "Pretty Task",
            todoGroup = "Morning",
            createdAt = now,
            updatedAt = now
        )

        val prettyJson = serializer.serializePretty(task)
        val parsed = parser.parse(prettyJson)

        assertEquals(task, parsed)
    }

    @Test
    fun testSerializeListOfTasks() {
        val now = Instant.now()
        val tasks = listOf(
            Task(
                id = "task-1",
                householdId = "hh-1",
                assignedUserId = "user-1",
                title = "Task 1",
                todoGroup = "Morning",
                createdAt = now,
                updatedAt = now
            ),
            Task(
                id = "task-2",
                householdId = "hh-1",
                assignedUserId = "user-1",
                title = "Task 2",
                todoGroup = "Afternoon",
                createdAt = now,
                updatedAt = now
            )
        )

        val json = serializer.serializeList(tasks)
        val parsed = parser.parseList(json)

        assertEquals(2, parsed.size)
        assertEquals(tasks[0], parsed[0])
        assertEquals(tasks[1], parsed[1])
    }

    @Test
    fun testParserRejectsInvalidJson() {
        val invalidJsons = listOf(
            "{}",
            """{"id": "123"}""",
            """{"id": "", "householdId": "hh1", "assignedUserId": "user1", "title": "Task", "todoGroup": "group"}"""
        )

        invalidJsons.forEach { invalidJson ->
            try {
                parser.parse(invalidJson)
                fail("Expected IllegalArgumentException for: $invalidJson")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("Failed to parse") == true || e.message?.contains("Missing") == true)
            }
        }
    }

    @Test
    fun testParserHandlesInvalidEnumValues() {
        val invalidStatusJson = """
        {
            "id": "task-1",
            "householdId": "hh-1",
            "assignedUserId": "user-1",
            "title": "Task",
            "todoGroup": "Morning",
            "status": "INVALID_STATUS",
            "syncStatus": "SYNCED",
            "createdAt": 1000000000000,
            "updatedAt": 1000000000000,
            "isDeleted": false
        }
        """.trimIndent()

        try {
            parser.parse(invalidStatusJson)
            fail("Expected IllegalArgumentException for invalid status")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid TaskStatus") == true)
        }
    }

    @Test
    fun testAllTaskStatusEnumsSerializeCorrectly() {
        val now = Instant.now()
        TaskStatus.values().forEach { status ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "hh-1",
                assignedUserId = "user-1",
                title = "Task",
                todoGroup = "Morning",
                status = status,
                createdAt = now,
                updatedAt = now
            )

            val json = serializer.serialize(task)
            val parsed = parser.parse(json)

            assertEquals(status, parsed.status)
        }
    }

    @Test
    fun testAllSyncStatusEnumsSerializeCorrectly() {
        val now = Instant.now()
        SyncStatus.values().forEach { syncStatus ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                householdId = "hh-1",
                assignedUserId = "user-1",
                title = "Task",
                todoGroup = "Morning",
                syncStatus = syncStatus,
                createdAt = now,
                updatedAt = now
            )

            val json = serializer.serialize(task)
            val parsed = parser.parse(json)

            assertEquals(syncStatus, parsed.syncStatus)
        }
    }

    @Test
    fun testTimestampSerializationPreservesMilliseconds() {
        val specificTime = Instant.ofEpochMilli(1609459200000L) // 2021-01-01 00:00:00 UTC
        val task = Task(
            id = "task-time",
            householdId = "hh-1",
            assignedUserId = "user-1",
            title = "Task",
            todoGroup = "Morning",
            createdAt = specificTime,
            updatedAt = specificTime
        )

        val json = serializer.serialize(task)
        val parsed = parser.parse(json)

        assertEquals(specificTime, parsed.createdAt)
        assertEquals(specificTime, parsed.updatedAt)
    }

    @Test
    fun testDeletedTaskFlagPreserved() {
        val now = Instant.now()
        val deletedTask = Task(
            id = "task-deleted",
            householdId = "hh-1",
            assignedUserId = "user-1",
            title = "Deleted Task",
            todoGroup = "Morning",
            createdAt = now,
            updatedAt = now,
            isDeleted = true
        )

        val json = serializer.serialize(deletedTask)
        val parsed = parser.parse(json)

        assertTrue(parsed.isDeleted)
    }
}
