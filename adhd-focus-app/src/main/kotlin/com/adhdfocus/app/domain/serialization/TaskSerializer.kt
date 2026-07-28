package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import org.json.JSONObject
import java.time.Instant

/**
 * TaskSerializer converts Task objects to JSON representation.
 *
 * Serializes all task metadata including:
 * - Basic fields (id, title, description)
 * - Timing information (estimated/actual duration, timestamps)
 * - Status and sync information
 * - User and household associations
 */
class TaskSerializer {
    /**
     * Serializes a Task object to JSON.
     *
     * @param task Task to serialize
     * @return JSON string representation
     */
    fun serialize(task: Task): String {
        val json = buildTaskJson(task)
        return json.toString()
    }

    /**
     * Serializes a Task object to pretty-printed JSON.
     *
     * @param task Task to serialize
     * @return Pretty-printed JSON string
     */
    fun serializePretty(task: Task): String {
        val json = buildTaskJson(task)
        return json.toString(2)
    }

    /**
     * Serializes a list of Task objects to JSON array.
     *
     * @param tasks List of tasks to serialize
     * @return JSON array string
     */
    fun serializeList(tasks: List<Task>): String {
        val jsonArray = org.json.JSONArray()
        tasks.forEach { task ->
            jsonArray.put(buildTaskJson(task))
        }
        return jsonArray.toString()
    }

    /**
     * Builds a JSONObject from a Task, handling all field conversions.
     *
     * @param task Task to convert
     * @return JSONObject representation
     */
    private fun buildTaskJson(task: Task): JSONObject {
        val json = JSONObject()
        json.put("id", task.id)
        json.put("householdId", task.householdId)
        json.put("assignedUserId", task.assignedUserId)
        json.put("title", task.title)
        json.put("emoji", task.emoji)
        json.put("description", task.description)
        json.put("todoGroup", task.todoGroup)
        json.put("repeat", task.repeatRule)
        json.put("repeatRule", task.repeatRule)
        json.put("estimatedDurationMinutes", task.estimatedDurationMinutes)
        json.put("estimatedDurationSeconds", task.estimatedDurationSeconds)
        json.put("timer", task.timerDurationMs?.takeIf { it > 0 }?.let { JSONObject().put("durationMs", it) })
        json.put("tokenValue", task.tokenValue)
        json.put("actualDurationMinutes", task.actualDurationMinutes)
        json.put("status", task.status.name)
        json.put("createdAt", task.createdAt.toEpochMilli())
        json.put("updatedAt", task.updatedAt.toEpochMilli())
        json.put("completedAt", task.completedAt?.toEpochMilli())
        json.put("syncStatus", task.syncStatus.name)
        json.put("isDeleted", task.isDeleted)
        return json
    }
}
