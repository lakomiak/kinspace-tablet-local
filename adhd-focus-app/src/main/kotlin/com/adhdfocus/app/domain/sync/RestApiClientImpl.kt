package com.adhdfocus.app.domain.sync

import android.util.Log
import com.adhdfocus.app.data.model.SyncOperation
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.network.TaskService
import com.adhdfocus.app.data.network.SyncService
import com.adhdfocus.app.data.network.CreateTaskRequest
import com.adhdfocus.app.data.network.UpdateTaskRequest
import com.adhdfocus.app.data.network.TimerRequest
import com.adhdfocus.app.data.network.BatchSyncRequest
import com.adhdfocus.app.data.network.SyncQueueItem as ApiSyncQueueItem
import com.google.gson.Gson
import kotlinx.coroutines.delay
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.io.IOException

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

    private val tag = "RestApiClient"

    override suspend fun createTask(householdId: String, task: Task, memberName: String?): Task {
        return retryWithBackoff {
            val request = CreateTaskRequest(
                id = task.id,
                title = task.title,
                text = task.title,
                description = task.description,
                todoGroup = task.todoGroup,
                group = task.todoGroup,
                category = task.todoGroup,
                estimatedDurationMinutes = task.estimatedDurationMinutes,
                estimatedDurationSeconds = task.estimatedDurationSeconds?.takeIf { it > 0 },
                actualDurationMinutes = task.actualDurationMinutes,
                repeat = task.repeatRule,
                repeatRule = task.repeatRule,
                dueDate = task.dueDate?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString(),
                member = memberName?.trim()?.takeIf { it.isNotBlank() },
                assignedUserId = task.assignedUserId,
                timer = buildTimerRequest(task.timerDurationMs, task.estimatedDurationMinutes, task.estimatedDurationSeconds)
            )

            val response = taskService.createTask(householdId, request).execute()
            if (response.isSuccessful) {
                response.body()?.todo?.let { convertTodoToTask(it, householdId) }
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
                text = (updates["text"] as? String) ?: (updates["title"] as? String),
                description = updates["description"] as? String,
                status = when (val status = updates["status"]) {
                    is TaskStatus -> status.name
                    is String -> status
                    else -> null
                },
                done = updates["done"] as? Boolean,
                actualDurationMinutes = updates["actualDurationMinutes"] as? Int,
                estimatedDurationMinutes = updates["estimatedDurationMinutes"] as? Int,
                estimatedDurationSeconds = (updates["estimatedDurationSeconds"] as? Int)?.takeIf { it > 0 },
                todoGroup = (updates["todoGroup"] as? String),
                group = (updates["group"] as? String) ?: (updates["todoGroup"] as? String),
                category = (updates["category"] as? String) ?: (updates["group"] as? String) ?: (updates["todoGroup"] as? String),
                repeat = updates["repeat"] as? String,
                repeatRule = updates["repeatRule"] as? String,
                dueDate = (updates["dueDate"] as? Instant)?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString(),
                completedAt = (updates["completedAt"] as? Instant)?.toString(),
                timer = extractTimerRequest(updates["timer"])
            )

            val response = taskService.updateTask(householdId, taskId, request).execute()
            if (response.isSuccessful) {
                response.body()?.todo?.let { convertTodoToTask(it, householdId) }
                    ?: throw ApiException(response.code(), "Empty response body")
            } else {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                Log.e(
                    tag,
                    "updateTask failed householdId=$householdId taskId=$taskId code=${response.code()} message=${response.message()} errorBody=$errorBody"
                )
                throw ApiException(
                    response.code(),
                    "Failed to update task: ${response.code()} ${response.message()}${if (!errorBody.isNullOrBlank()) " - $errorBody" else ""}"
                )
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

    override suspend fun fetchTasks(
        householdId: String,
        date: LocalDate?,
        familyMemberId: String?
    ): FetchedTasksSnapshot {
        return retryWithBackoff {
            val response = taskService.getTasks(householdId, date?.toString(), familyMemberId).execute()
            if (response.isSuccessful) {
                val payload = response.body()
                val sourceTodos = when {
                    payload?.todos?.isNotEmpty() == true -> payload.todos
                    payload?.todayTodos?.isNotEmpty() == true -> payload.todayTodos
                    payload?.todos != null -> payload.todos
                    else -> null
                } ?: throw ApiException(response.code(), "Empty response body")
                val tasks = sourceTodos.map { convertTodoToTask(it, householdId) }
                val dayCompletions = payload?.dayCompletions.orEmpty().mapNotNull { responseCompletion ->
                    runCatching {
                        CloudTaskDayCompletion(
                            householdId = responseCompletion.householdId,
                            familyMemberId = responseCompletion.familyMemberId,
                            targetDate = LocalDate.parse(responseCompletion.targetDate),
                            taskId = responseCompletion.taskId,
                            completedAt = responseCompletion.completedAt?.let { parseInstant(it) },
                            updatedAt = responseCompletion.updatedAt?.let { parseInstant(it) },
                            isCompleted = responseCompletion.isCompleted == true
                        )
                    }.getOrNull()
                }
                Log.d(
                    tag,
                    "fetchTasks householdId=$householdId date=${date?.toString() ?: "current"} familyMemberId=${familyMemberId ?: "all"} count=${tasks.size} dayCompletionCount=${dayCompletions.size} todayCount=${payload?.todayTodos?.size ?: -1} totalCount=${payload?.todos?.size ?: -1}"
                )
                tasks.forEachIndexed { index, task ->
                    Log.d(
                        tag,
                        "fetchTasks[$index] id=${task.id} title=${task.title} assignedUserId=${task.assignedUserId} status=${task.status} dueDate=${task.dueDate}"
                    )
                }
                FetchedTasksSnapshot(
                    tasks = tasks,
                    dayCompletions = dayCompletions
                )
            } else {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                Log.e(
                    tag,
                    "fetchTasks failed householdId=$householdId code=${response.code()} message=${response.message()} errorBody=$errorBody"
                )
                throw ApiException(
                    response.code(),
                    "Failed to fetch tasks: ${response.code()} ${response.message()}${if (!errorBody.isNullOrBlank()) " - $errorBody" else ""}"
                )
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
            repeatRule = normalizeRepeatRule(response.repeatRule ?: response.repeat),
            estimatedDurationMinutes = response.estimatedDurationMinutes
                ?: durationFromTimer(response.timer)?.first,
            estimatedDurationSeconds = durationFromTimer(response.timer)?.second,
            timerDurationMs = response.timer?.durationMs?.takeIf { it >= 0 },
            actualDurationMinutes = response.actualDurationMinutes,
            status = TaskStatus.valueOf(response.status),
            dueDate = parseDueDate(response.dueDate),
            createdAt = parseInstant(response.createdAt),
            updatedAt = parseInstant(response.updatedAt),
            completedAt = response.completedAt?.let { parseInstant(it) },
            syncStatus = SyncStatus.valueOf(response.syncStatus),
            isDeleted = response.isDeleted
        )
    }

    private fun convertTodoToTask(
        response: com.adhdfocus.app.data.network.TodoResponse,
        householdId: String
    ): Task {
            val assignedUserId = response.familyMemberId
            ?: response.assignedUserId
            ?: response.assignedTo
            ?: response.member
            ?: householdId
        return Task(
            id = response.id,
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = response.text,
            description = response.category,
            todoGroup = response.group ?: response.category ?: "General",
            repeatRule = normalizeRepeatRule(response.repeat ?: response.repeatRule),
            estimatedDurationMinutes = response.estimatedDurationMinutes
                ?: durationFromTimer(response.timer)?.first,
            estimatedDurationSeconds = durationFromTimer(response.timer)?.second?.takeIf { it >= 0 },
            timerDurationMs = response.timer?.durationMs?.takeIf { it >= 0 },
            actualDurationMinutes = response.actualDurationMinutes,
            status = if (response.done) TaskStatus.COMPLETED else TaskStatus.INCOMPLETE,
            dueDate = parseDueDate(response.dueDate),
            createdAt = parseInstant(response.createdAt),
            updatedAt = parseInstant(response.updatedAt),
            completedAt = when {
                !response.done -> null
                !response.completedAt.isNullOrBlank() -> parseInstant(response.completedAt)
                else -> parseInstant(response.updatedAt)
            },
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    private fun parseDueDate(value: String?): Instant? {
        if (value.isNullOrBlank()) {
            return null
        }

        return runCatching { Instant.parse(value) }
            .getOrElse {
                runCatching {
                    LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant()
                }.getOrNull()
            }
    }

    private fun parseInstant(value: String): Instant {
        return runCatching { Instant.parse(value) }
            .getOrElse {
                throw ApiException(
                    500,
                    "Invalid timestamp format: $value"
                )
            }
    }

    private fun durationFromTimer(timer: com.adhdfocus.app.data.network.TimerResponse?): Pair<Int, Int>? {
        val durationMs = timer?.durationMs ?: return null
        if (durationMs < 0) return null
        val totalSeconds = (durationMs / 1000L).toInt()
        if (totalSeconds < 0) return null
        return (totalSeconds / 60) to (totalSeconds % 60)
    }

    private fun buildTimerRequest(
        timerDurationMs: Long?,
        estimatedDurationMinutes: Int?,
        estimatedDurationSeconds: Int?
    ): TimerRequest? {
        val durationMs = when {
            timerDurationMs != null && timerDurationMs > 0 -> timerDurationMs
            (estimatedDurationMinutes ?: 0) > 0 || (estimatedDurationSeconds ?: 0) > 0 ->
                ((estimatedDurationMinutes ?: 0) * 60L + (estimatedDurationSeconds ?: 0).toLong()) * 1000L
            else -> 0L
        }
        return durationMs.takeIf { it > 0L }?.let { TimerRequest(it) }
    }

    private fun extractTimerRequest(timerValue: Any?): TimerRequest? {
        return when (timerValue) {
            null -> null
            is TimerRequest -> timerValue
            is Map<*, *> -> {
                val durationMs = when (val raw = timerValue["durationMs"]) {
                    is Number -> raw.toLong()
                    is String -> raw.toLongOrNull()
                    else -> null
                }
                durationMs?.takeIf { it > 0L }?.let { TimerRequest(it) }
            }
            is Number -> timerValue.toLong().takeIf { it > 0L }?.let { TimerRequest(it) }
            else -> null
        }
    }

    private fun normalizeRepeatRule(value: String?): String {
        return value?.trim()?.takeIf { it.isNotBlank() } ?: "once"
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
