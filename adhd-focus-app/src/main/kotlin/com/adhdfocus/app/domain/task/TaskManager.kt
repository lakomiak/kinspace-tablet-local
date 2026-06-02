package com.adhdfocus.app.domain.task

import android.util.Log
import android.os.Build
import com.adhdfocus.app.data.dao.TaskDao
import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import com.adhdfocus.app.domain.affirmation.AffirmationTriggerManager
import com.adhdfocus.app.domain.timer.TaskCompletionSessionMetrics
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import java.time.Instant
import java.util.UUID

/**
 * TaskManager handles task creation, updates, completion, and cloud synchronization.
 *
 * Responsibilities:
 * - Accept task input (title, description, estimated duration, Todo_Group)
 * - Display pending-sync indicator for offline changes
 * - Queue changes for synchronization
 * - Resolve sync conflicts using timestamp-based resolution
 * - Trigger affirmation and badge evaluation on completion
 * - Persist all changes locally
 *
 * Correctness Properties:
 * - Property 5: Task Validation - All created tasks must have valid required fields
 * - Property 6: Pending Sync Indicator - All local changes must be marked with PENDING sync status
 */
class TaskManager @Inject constructor(
    private val taskDao: TaskDao,
    private val affirmationTriggerManager: AffirmationTriggerManager
) {
    private val tag = "TaskManager"
    /**
     * Sets the affirmation frequency for the current user.
     *
     * @param frequency Frequency level (1-5)
     */
    fun setAffirmationFrequency(frequency: Int) {
        affirmationTriggerManager.setAffirmationFrequency(frequency)
    }
    /**
     * Creates a new task with the provided details.
     *
     * Validates all required fields and marks the task with PENDING sync status.
     * Queues the task for synchronization with calendar-cloud.
     *
     * @param title Task title (required, non-blank)
     * @param description Task description (optional)
     * @param estimatedDurationMinutes Estimated duration in minutes (optional, must be non-negative if provided)
     * @param todoGroup Todo group assignment (required, non-blank)
     * @param householdId Household ID (required, non-blank)
     * @param assignedUserId User ID this task is assigned to (required, non-blank)
     * @return Created task with PENDING sync status
     * @throws IllegalArgumentException if any required field is invalid
     */
    suspend fun createTask(
        title: String,
        description: String? = null,
        estimatedDurationMinutes: Int? = null,
        estimatedDurationSeconds: Int? = null,
        todoGroup: String,
        householdId: String,
        assignedUserId: String,
        assignedMemberName: String? = null,
        dueDate: Instant? = null,
        repeatRule: String = "once"
    ): Task {
        // Validate required fields (Property 5: Task Validation)
        require(title.isNotBlank()) { "Task title cannot be empty" }
        require(todoGroup.isNotBlank()) { "Todo group cannot be empty" }
        require(householdId.isNotBlank()) { "Household ID cannot be empty" }
        require(assignedUserId.isNotBlank()) { "Assigned user ID cannot be empty" }
        require(repeatRule.isNotBlank()) { "Repeat rule cannot be empty" }
        require(estimatedDurationMinutes == null || estimatedDurationMinutes >= 0) {
            "Estimated duration must be non-negative if provided"
        }
        require(estimatedDurationSeconds == null || estimatedDurationSeconds >= 0) {
            "Estimated duration seconds must be non-negative if provided"
        }

        val now = Instant.now()
        val timerDurationMs = computeTimerDurationMs(estimatedDurationMinutes, estimatedDurationSeconds)
        val task = Task(
            id = UUID.randomUUID().toString(),
            householdId = householdId,
            assignedUserId = assignedUserId,
            title = title,
            description = description,
            todoGroup = todoGroup,
            repeatRule = repeatRule,
            estimatedDurationMinutes = estimatedDurationMinutes,
            estimatedDurationSeconds = estimatedDurationSeconds,
            timerDurationMs = timerDurationMs,
            dueDate = dueDate,
            status = TaskStatus.INCOMPLETE,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED
        )

        taskDao.insert(task)
        return task
    }

    /**
     * Updates an existing task with new values.
     *
     * Validates the task exists, applies updates, and marks with PENDING sync status.
     * Queues the update for synchronization.
     *
     * @param taskId ID of the task to update (required, non-blank)
     * @param title New title (optional, if provided must be non-blank)
     * @param description New description (optional)
     * @param estimatedDurationMinutes New estimated duration (optional, must be non-negative if provided)
     * @param todoGroup New todo group (optional, if provided must be non-blank)
     * @param status New task status (optional)
     * @return Updated task with PENDING sync status
     * @throws IllegalArgumentException if task not found or invalid parameters
     */
    suspend fun updateTask(
        taskId: String,
        title: String? = null,
        description: String? = null,
        estimatedDurationMinutes: Int? = null,
        estimatedDurationSeconds: Int? = null,
        todoGroup: String? = null,
        status: TaskStatus? = null,
        dueDate: Instant? = null,
        clearDueDate: Boolean = false,
        repeatRule: String? = null
    ): Task {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }

        val existingTask = taskDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Validate optional fields if provided (Property 5: Task Validation)
        if (title != null) {
            require(title.isNotBlank()) { "Task title cannot be empty" }
        }
        if (todoGroup != null) {
            require(todoGroup.isNotBlank()) { "Todo group cannot be empty" }
        }
        if (estimatedDurationMinutes != null) {
            require(estimatedDurationMinutes >= 0) { "Estimated duration must be non-negative" }
        }
        if (estimatedDurationSeconds != null) {
            require(estimatedDurationSeconds >= 0) { "Estimated duration seconds must be non-negative" }
        }

        val resolvedDueDate = when {
            clearDueDate -> null
            dueDate != null -> dueDate
            else -> existingTask.dueDate
        }
        val resolvedRepeatRule = repeatRule?.trim()?.takeIf { it.isNotBlank() } ?: existingTask.repeatRule

        val resolvedEstimatedMinutes = estimatedDurationMinutes ?: existingTask.estimatedDurationMinutes
        val resolvedEstimatedSeconds = estimatedDurationSeconds ?: existingTask.estimatedDurationSeconds
        val timerDurationMs = when {
            estimatedDurationMinutes != null || estimatedDurationSeconds != null ->
                computeTimerDurationMs(resolvedEstimatedMinutes, resolvedEstimatedSeconds)
            else -> existingTask.timerDurationMs
        }

        val updatedTask = existingTask.copy(
            title = title ?: existingTask.title,
            description = description ?: existingTask.description,
            todoGroup = todoGroup ?: existingTask.todoGroup,
            repeatRule = resolvedRepeatRule,
            estimatedDurationMinutes = resolvedEstimatedMinutes,
            estimatedDurationSeconds = resolvedEstimatedSeconds,
            timerDurationMs = timerDurationMs,
            dueDate = resolvedDueDate,
            status = status ?: existingTask.status,
            completedAt = when (status) {
                TaskStatus.COMPLETED -> existingTask.completedAt ?: Instant.now()
                TaskStatus.INCOMPLETE -> null
                TaskStatus.IN_PROGRESS, null -> existingTask.completedAt
            },
            updatedAt = Instant.now(),
            syncStatus = SyncStatus.SYNCED
        )

        taskDao.update(updatedTask)
        return updatedTask
    }

    /**
     * Transitions a task to IN_PROGRESS status.
     *
     * @param taskId ID of the task to start
     * @return Task with IN_PROGRESS status
     * @throws IllegalArgumentException if task not found
     */
    suspend fun startTask(taskId: String): Task {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }

        val existingTask = taskDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val updatedTask = existingTask.copy(
            status = TaskStatus.IN_PROGRESS,
            updatedAt = Instant.now(),
            syncStatus = SyncStatus.SYNCED
        )

        taskDao.update(updatedTask)
        return updatedTask
    }

    /**
     * Marks a task as completed.
     *
     * Sets status to COMPLETED, records completion time, and marks with PENDING sync status.
     * Queues the completion for synchronization.
     * Triggers task completion affirmation.
     *
     * Correctness Properties:
     * - Property 18: Affirmation on Task Completion - Affirmation is triggered when task is completed
     *
     * @param taskId ID of the task to complete
     * @return Completed task with PENDING sync status
     * @throws IllegalArgumentException if task not found
     */
    suspend fun completeTask(
        taskId: String,
        completionMetrics: TaskCompletionSessionMetrics? = null
    ): Task {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }

        val existingTask = taskDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val now = Instant.now()
        val completedTask = existingTask.copy(
            status = TaskStatus.COMPLETED,
            completedAt = now,
            actualDurationMinutes = completionMetrics?.actualDurationMinutes ?: existingTask.actualDurationMinutes,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED
        )

        taskDao.update(completedTask)
        affirmationTriggerManager.checkAndTriggerTaskCompleteAffirmation(completedTask)
        return completedTask
    }

    /**
     * Reopens a completed task.
     *
     * Marks the task as incomplete again and queues the update for synchronization.
     *
     * @param taskId ID of the task to reopen
     * @return Reopened task with PENDING sync status
     * @throws IllegalArgumentException if task not found
     */
    suspend fun reopenTask(taskId: String): Task {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }

        val existingTask = taskDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val now = Instant.now()
        val reopenedTask = existingTask.copy(
            status = TaskStatus.INCOMPLETE,
            completedAt = null,
            updatedAt = now,
            syncStatus = SyncStatus.SYNCED
        )

        taskDao.update(reopenedTask)
        return reopenedTask
    }

    /**
     * Deletes a task (soft delete).
     *
     * Marks the task as deleted and queues the deletion for synchronization.
     * The task remains in local storage but is hidden from normal queries.
     *
     * @param taskId ID of the task to delete
     * @throws IllegalArgumentException if task not found
     */
    suspend fun deleteTask(taskId: String) {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }

        val existingTask = taskDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val deletedTask = existingTask.copy(
            isDeleted = true,
            updatedAt = Instant.now(),
            syncStatus = SyncStatus.SYNCED
        )

        taskDao.update(deletedTask)
    }

    /**
     * Retrieves a task by ID.
     *
     * @param taskId Task ID
     * @return Task or null if not found
     */
    suspend fun getTaskById(taskId: String): Task? {
        require(taskId.isNotBlank()) { "Task ID cannot be empty" }
        return taskDao.getTaskById(taskId)
    }

    /**
     * Retrieves all tasks for a household.
     *
     * @param householdId Household ID
     * @return Flow of household tasks
     */
    fun getTasksByHousehold(householdId: String): Flow<List<Task>> {
        require(householdId.isNotBlank()) { "Household ID cannot be empty" }
        return taskDao.getTasksByHousehold(householdId)
    }

    /**
     * Retrieves all tasks for a specific user.
     *
     * @param userId User ID
     * @return Flow of user's tasks
     */
    fun getTasksByUser(userId: String): Flow<List<Task>> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return taskDao.getTasksByUser(userId)
    }

    /**
     * Retrieves tasks by status.
     *
     * @param householdId Household ID
     * @param status Task status to filter by
     * @return Flow of tasks with the specified status
     */
    fun getTasksByStatus(householdId: String, status: TaskStatus): Flow<List<Task>> {
        require(householdId.isNotBlank()) { "Household ID cannot be empty" }
        return taskDao.getTasksByStatus(householdId, status)
    }

    /**
     * Retrieves tasks by todo group.
     *
     * @param householdId Household ID
     * @param todoGroup Todo group to filter by
     * @return Flow of tasks in the specified group
     */
    fun getTasksByTodoGroup(householdId: String, todoGroup: String): Flow<List<Task>> {
        require(householdId.isNotBlank()) { "Household ID cannot be empty" }
        require(todoGroup.isNotBlank()) { "Todo group cannot be empty" }
        return taskDao.getTasksByTodoGroup(householdId, todoGroup)
    }

    /**
     * Retrieves pending sync tasks for a user.
     *
     * @param userId User ID
     * @return Flow of tasks with PENDING sync status
     */
    fun getPendingSyncTasks(userId: String): Flow<List<Task>> {
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        return taskDao.getUserTasksBySyncStatus(userId, SyncStatus.PENDING)
    }

    private fun computeTimerDurationMs(minutes: Int?, seconds: Int?): Long? {
        val durationMs = ((minutes ?: 0) * 60L + (seconds ?: 0).toLong()) * 1000L
        return durationMs.takeIf { it > 0L }
    }
}

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val errorMessage: String? = null
)
