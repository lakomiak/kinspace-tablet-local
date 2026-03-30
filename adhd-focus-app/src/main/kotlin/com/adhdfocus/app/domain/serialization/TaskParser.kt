package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import org.json.JSONObject
import java.time.Instant

/**
 * TaskParser converts JSON representations back into Task objects.
 *
 * Parses all task metadata including:
 * - Basic fields (id, title, description)
 * - Timing information (estimated/actual duration, timestamps)
 * - Status and sync information
 * - User and household associations
 *
 * Handles optional fields gracefully with default values.
 */
class TaskParser {
    /**
     * Parses a JSON string into a Task object.
     *
     * @param jsonString JSON string to parse
     * @return Parsed Task object
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    fun parse(jsonString: String): Task {
        return try {
            val json = JSONObject(jsonString)
            parseFromJson(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse Task JSON: ${e.message}", e)
        }
    }

    /**
     * Parses a JSONObject into a Task object.
     *
     * @param json JSONObject to parse
     * @return Parsed Task object
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    fun parseFromJson(json: JSONObject): Task {
        // Validate and extract required fields
        val id = json.optString("id", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: id")
        val householdId = json.optString("householdId", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: householdId")
        val assignedUserId = json.optString("assignedUserId", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: assignedUserId")
        val title = json.optString("title", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: title")
        val todoGroup = json.optString("todoGroup", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: todoGroup")

        // Extract optional fields
        val description = json.optString("description", null).takeIf { it?.isNotBlank() == true }
        val estimatedDurationMinutes = json.optInt("estimatedDurationMinutes", -1).takeIf { it > 0 }
        val actualDurationMinutes = json.optInt("actualDurationMinutes", -1).takeIf { it >= 0 }

        // Parse status enum
        val statusString = json.optString("status", "INCOMPLETE")
        val status = try {
            TaskStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid TaskStatus: $statusString")
        }

        // Parse sync status enum
        val syncStatusString = json.optString("syncStatus", "PENDING")
        val syncStatus = try {
            SyncStatus.valueOf(syncStatusString)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid SyncStatus: $syncStatusString")
        }

        // Parse timestamps
        val createdAt = parseInstant(json.optLong("createdAt", 0))
        val updatedAt = parseInstant(json.optLong("updatedAt", 0))
        val completedAt = json.optLong("completedAt", 0).takeIf { it > 0 }?.let { parseInstant(it) }

        val isDeleted = json.optBoolean("isDeleted", false)

        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = description,
            todoGroup = todoGroup,
            estimatedDurationMinutes = estimatedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            completedAt = completedAt,
            syncStatus = syncStatus,
            isDeleted = isDeleted
        )
    }

    /**
     * Parses a JSON array string into a list of Task objects.
     *
     * @param jsonArrayString JSON array string to parse
     * @return List of parsed Task objects
     * @throws IllegalArgumentException if parsing fails
     */
    fun parseList(jsonArrayString: String): List<Task> {
        return try {
            val jsonArray = org.json.JSONArray(jsonArrayString)
            val tasks = mutableListOf<Task>()
            for (i in 0 until jsonArray.length()) {
                tasks.add(parseFromJson(jsonArray.getJSONObject(i)))
            }
            tasks
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse Task JSON array: ${e.message}", e)
        }
    }

    /**
     * Converts a timestamp (milliseconds since epoch) to an Instant.
     *
     * @param timestamp Milliseconds since epoch
     * @return Instant object
     */
    private fun parseInstant(timestamp: Long): Instant {
        return Instant.ofEpochMilli(timestamp)
    }
}
