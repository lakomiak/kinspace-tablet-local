package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.google.gson.Gson
import java.time.Instant
import javax.inject.Inject

/**
 * Serializes and deserializes sync changes for transmission to calendar-cloud.
 *
 * Responsibilities:
 * - Convert Task objects to JSON payloads
 * - Create SyncChange objects from tasks and operations
 * - Handle timestamp management
 */
class SyncChangeSerializer @Inject constructor(
    private val gson: Gson
) {
    /**
     * Creates a SyncChange from a task and operation.
     *
     * @param task Task to serialize
     * @param operation Type of operation (CREATE, UPDATE, DELETE)
     * @return SyncChange with serialized payload
     */
    fun createSyncChange(task: Task, operation: SyncOperation): SyncChange {
        val payload = gson.toJson(task)
        return SyncChange(
            taskId = task.id,
            operation = operation,
            payload = payload,
            timestamp = Instant.now().toEpochMilli()
        )
    }

    /**
     * Deserializes a task from a sync change payload.
     *
     * @param syncChange Sync change containing serialized task
     * @return Deserialized Task
     * @throws IllegalArgumentException if payload is invalid JSON
     */
    fun deserializeTask(syncChange: SyncChange): Task {
        return try {
            gson.fromJson(syncChange.payload, Task::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to deserialize task from sync change: ${e.message}", e)
        }
    }

    /**
     * Serializes a task to JSON string.
     *
     * @param task Task to serialize
     * @return JSON string representation
     */
    fun serializeTask(task: Task): String {
        return gson.toJson(task)
    }

    /**
     * Deserializes a task from JSON string.
     *
     * @param json JSON string
     * @return Deserialized Task
     * @throws IllegalArgumentException if JSON is invalid
     */
    fun deserializeTaskFromJson(json: String): Task {
        return try {
            gson.fromJson(json, Task::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to deserialize task from JSON: ${e.message}", e)
        }
    }
}
