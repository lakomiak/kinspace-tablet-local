package com.adhdfocus.app.domain.sync

import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.network.TaskService
import com.adhdfocus.app.data.network.SyncService
import com.adhdfocus.app.data.network.CreateTaskRequest
import com.adhdfocus.app.data.network.UpdateTaskRequest
import com.adhdfocus.app.data.network.BatchSyncRequest
import com.adhdfocus.app.data.network.SyncQueueItem as ApiSyncQueueItem
import com.google.gson.Gson
import kotlinx.coroutines.delay
import javax.inject.Inject
import java.time.Instant

/**
 * Implementation of RestApiClient using Retrofit.
 *
 * Handles:
 * - HTTP requests to calendar-cloud API
 * - Request/response serialization with Gson
 * - Authentication headers (Bearer token)
 * - Exponential backoff retry logic
 * - Error handling and conversion
 */
class RestApiClientImpl @Inject constructor(
    private val taskService: TaskService,
    private val syncService: SyncService,
    private val gson: Gson,
    private val tokenProvider: TokenProvider,
    private val retryPolicy: RetryPolicy = ExponentialBackoffRetryPolicy()
) : RestApiClient {

    override suspend fun createTask(householdId: String, task: Task): Task {
        return retryWithBackoff {
            val request = CreateTaskRequest(
                title = task.title,
                description = task.description,
                todoGroup = task.todoGroup,
                estimatedDurationMinutes = task.estimatedDurationMinutes,
                assignedUserId = task.assignedUserId
            )

            val response = taskService.createTask(householdId, request).execute()
            if (response.isSuccessful) {
                response.body()?.let { convertToTask(it, householdId) }
                    ?: throw ApiException(response.code(), "Empty response body")
            } else {
                throw ApiException(response.code(), "Failed to create task: ${response.message()}")
            }
        }
    }

    override suspend fun updateTask(
        householdId: String,
        taskId: String,
        updates: Map<String, Any?>
    ): Task {
        return retryWithBackoff {
            val request = UpdateTaskRequest(
                title = updates["title"] as? String,
                description = updates["description"] as? String,
                status = (updates["status"] as? TaskStatus)?.name,
                actualDurationMinutes = updates["actualDurationMinutes"] as? Int,
                completedAt = (updates["completedAt"] as? Instant)?.toString()
            )

            val response = taskService.updateTask(householdId, taskId, request).execute()
            if (response.isSuccessful) {
                response.body()?.let { convertToTask(it, householdId) }
                    ?: throw ApiException(response.code(), "Empty response body")
            } else {
                throw ApiException(response.code(), "Failed to update task: ${response.message()}")
            }
        }
    }

    override suspend fun deleteTask(householdId: String, taskId: String) {
        retryWithBackoff {
            val response = taskService.deleteTask(householdId, taskId).execute()
            if (!response.isSuccessful) {
                throw ApiException(response.code(), "Failed to delete task: ${response.message()}")
            }
        }
    }

    override suspend fun fetchTasks(householdId: String): List<Task> {
        return retryWithBackoff {
            val response = taskService.getTasks(householdId).execute()
            if (response.isSuccessful) {
                response.body()?.tasks?.map { convertToTask(it, householdId) }
                    ?: throw ApiException(response.code(), "Empty response body")
            } else {
                throw ApiException(response.code(), "Failed to fetch tasks: ${response.message()}")
            }
        }
    }

    override suspend fun batchSync(householdId: String, changes: List<SyncChange>): SyncResult {
        return retryWithBackoff {
            val apiItems = changes.map { change ->
                ApiSyncQueueItem(
                    taskId = change.taskId,
                    operation = change.operation.name,
                    payload = change.payload,
                    timestamp = change.timestamp
                )
            }

            val request = BatchSyncRequest(changes = apiItems)
            val response = syncService.batchSync(householdId, request).execute()

            if (response.isSuccessful) {
                response.body()?.let { syncResponse ->
                    SyncResult(
                        syncedCount = syncResponse.syncedCount,
                        failedCount = syncResponse.failedCount,
                        conflicts = syncResponse.conflicts?.map { conflict ->
                            SyncConflict(
                                taskId = conflict.taskId,
                                localVersion = convertToTask(conflict.localVersion, householdId),
                                remoteVersion = convertToTask(conflict.remoteVersion, householdId)
                            )
                        } ?: emptyList()
                    )
                } ?: throw ApiException(response.code(), "Empty response body")
            } else {
                throw ApiException(response.code(), "Failed to batch sync: ${response.message()}")
            }
        }
    }

    /**
     * Converts API TaskResponse to domain Task model.
     */
    private fun convertToTask(
        response: com.adhdfocus.app.data.network.TaskResponse,
        householdId: String
    ): Task {
        return Task(
            id = response.id,
            householdId = householdId,
            assignedUserId = response.assignedUserId,
            title = response.title,
            description = response.description,
            todoGroup = response.todoGroup,
            estimatedDurationMinutes = response.estimatedDurationMinutes,
            actualDurationMinutes = response.actualDurationMinutes,
            status = TaskStatus.valueOf(response.status),
            createdAt = Instant.parse(response.createdAt),
            updatedAt = Instant.parse(response.updatedAt),
            completedAt = response.completedAt?.let { Instant.parse(it) },
            syncStatus = SyncStatus.valueOf(response.syncStatus),
            isDeleted = response.isDeleted
        )
    }

    /**
     * Executes a suspend function with exponential backoff retry logic.
     *
     * Uses the configured RetryPolicy to determine retry behavior and backoff delays.
     */
    private suspend inline fun <T> retryWithBackoff(block: suspend () -> T): T {
        var lastException: Exception? = null

        repeat(retryPolicy.getMaxRetries()) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (retryPolicy.shouldRetry(attempt, e)) {
                    val delayMs = retryPolicy.getBackoffDelayMs(attempt)
                    delay(delayMs)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: Exception("Unknown error after ${retryPolicy.getMaxRetries()} retries")
    }
}

/**
 * Interface for providing authentication tokens.
 */
interface TokenProvider {
    suspend fun getAccessToken(): String
}
