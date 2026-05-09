package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task

/**
 * Interface for REST API client operations with calendar-cloud.
 *
 * Responsibilities:
 * - Create, update, delete tasks via REST API
 * - Fetch tasks from calendar-cloud
 * - Batch sync operations
 * - Handle authentication and token management
 * - Implement exponential backoff for failed attempts
 */
interface RestApiClient {
    /**
     * Creates a new task on calendar-cloud.
     *
     * @param householdId Household ID
     * @param task Task to create
     * @return Created task with server-assigned ID and timestamps
     * @throws NetworkException if network error occurs
     * @throws ApiException if API returns error
     */
    suspend fun createTask(householdId: String, task: Task, memberName: String? = null): Task

    /**
     * Updates an existing task on calendar-cloud.
     *
     * @param householdId Household ID
     * @param taskId Task ID to update
     * @param updates Map of fields to update
     * @return Updated task
     * @throws NetworkException if network error occurs
     * @throws ApiException if API returns error
     */
    suspend fun updateTask(
        householdId: String,
        taskId: String,
        updates: Map<String, Any?>
    ): Task

    /**
     * Deletes a task on calendar-cloud.
     *
     * @param householdId Household ID
     * @param taskId Task ID to delete
     * @throws NetworkException if network error occurs
     * @throws ApiException if API returns error
     */
    suspend fun deleteTask(householdId: String, taskId: String)

    /**
     * Fetches all tasks for a household.
     *
     * @param householdId Household ID
     * @return List of tasks
     * @throws NetworkException if network error occurs
     * @throws ApiException if API returns error
     */
    suspend fun fetchTasks(householdId: String): List<Task>

    /**
     * Performs batch sync of multiple changes.
     *
     * @param householdId Household ID
     * @param changes List of sync changes to apply
     * @return Sync result with synced count, failed count, and conflicts
     * @throws NetworkException if network error occurs
     * @throws ApiException if API returns error
     */
    suspend fun batchSync(householdId: String, changes: List<SyncChange>): SyncResult
}

/**
 * Represents a single change to be synced.
 */
data class SyncChange(
    val taskId: String,
    val operation: SyncOperation,
    val payload: String, // JSON serialized task data
    val timestamp: Long
)

/**
 * Represents a conflict during sync.
 */
data class SyncConflict(
    val taskId: String,
    val localVersion: Task,
    val remoteVersion: Task
)

/**
 * Exception thrown when network error occurs.
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when API returns error.
 */
class ApiException(
    val code: Int,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
