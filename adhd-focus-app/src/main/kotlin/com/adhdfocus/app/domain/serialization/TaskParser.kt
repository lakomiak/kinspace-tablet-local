package com.adhdfocus.app.domain.serialization

import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
        val repeatRule = json.optString("repeatRule", json.optString("repeat", "once"))
            .takeIf { it.isNotBlank() } ?: "once"

        // Extract optional fields
        val description = json.optString("description", null).takeIf { it?.isNotBlank() == true }
        val estimatedDurationMinutes = json.optInt("estimatedDurationMinutes", -1).takeIf { it >= 0 }
        val estimatedDurationSeconds = json.optInt("estimatedDurationSeconds", -1).takeIf { it >= 0 }
        val actualDurationMinutes = json.optInt("actualDurationMinutes", -1).takeIf { it >= 0 }
        val timerDurationMs = parseTimer(json)

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
        val dueDate = parseFlexibleInstant(json, "dueDate")
        val createdAt = parseFlexibleInstant(json, "createdAt")
            ?: throw IllegalArgumentException("Missing required field: createdAt")
        val updatedAt = parseFlexibleInstant(json, "updatedAt")
            ?: throw IllegalArgumentException("Missing required field: updatedAt")
        val completedAt = parseFlexibleInstant(json, "completedAt")

        val isDeleted = json.optBoolean("isDeleted", false)

        return Task(
            id = id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = description,
            todoGroup = todoGroup,
            repeatRule = repeatRule,
            estimatedDurationMinutes = estimatedDurationMinutes,
            estimatedDurationSeconds = estimatedDurationSeconds,
            timerDurationMs = timerDurationMs,
            actualDurationMinutes = actualDurationMinutes,
            status = status,
            dueDate = dueDate,
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

    private fun parseFlexibleInstant(json: JSONObject, key: String): Instant? {
        if (!json.has(key) || json.isNull(key)) {
            return null
        }

        return when (val value = json.opt(key)) {
            is Number -> Instant.ofEpochMilli(value.toLong())
            is String -> value.takeIf { it.isNotBlank() }?.let {
                runCatching { Instant.parse(it) }.getOrNull()
                    ?: runCatching { Instant.ofEpochMilli(it.toLong()) }.getOrNull()
                    ?: runCatching { LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
            }
            else -> null
        }
    }

    private fun parseTimer(json: JSONObject): Long? {
        if (!json.has("timer") || json.isNull("timer")) return null
        val timerObj = json.optJSONObject("timer") ?: return null
        val durationMs = timerObj.optLong("durationMs", 0L)
        return durationMs.takeIf { it >= 0 }
    }
}
